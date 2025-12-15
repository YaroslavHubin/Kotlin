from pymongo import MongoClient
from cfg import name, AES_KEY
import os, uuid, datetime, json
from flask import Flask, request, jsonify
from flask_bcrypt import Bcrypt
from flask_cors import CORS
import jwt
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

app = Flask(__name__)
app.secret_key = 'supersecretkey'

app.config["AES_KEY"] = AES_KEY
app.config["JWT_SECRET"] = "your-jwt-secret"

bcrypt = Bcrypt(app)
CORS(app)

client = MongoClient(name)
db = client['light_control']
users_collection = db['users']
lamps_collection = db['lamps']

def aes_encrypt(plaintext: dict) -> dict:
    a = AESGCM(app.config["AES_KEY"])
    nonce = os.urandom(12)
    ct = a.encrypt(nonce, json.dumps(plaintext).encode("utf-8"), None)
    return {"nonce": nonce.hex(), "ct": ct.hex()}

def aes_decrypt(blob: dict) -> dict:
    a = AESGCM(app.config["AES_KEY"])
    nonce = bytes.fromhex(blob["nonce"])
    ct    = bytes.fromhex(blob["ct"])
    return json.loads(a.decrypt(nonce, ct, None).decode("utf-8"))

def make_jwt(user_id: str) -> str:
    payload = {"sub": user_id, "iat": datetime.datetime.utcnow(), "exp": datetime.datetime.utcnow() + datetime.timedelta(hours=12)}
    return jwt.encode(payload, app.config["JWT_SECRET"], algorithm="HS256")

def auth_user():
    hdr = request.headers.get("Authorization", "")
    if not hdr.startswith("Bearer "): return None
    token = hdr.split(" ", 2)[1]
    try:
        payload = jwt.decode(token, app.config["JWT_SECRET"], algorithms=["HS256"])
        return payload["sub"]
    except Exception:
        return None
    
@app.post("/auth/register")
def register():
    data = request.get_json(force=True)
    email = data.get("email", "").strip().lower()
    password = data.get("password", "")
    if not email or not password:
        return jsonify({"error": "Email/пароль обов'язкові"}), 400

    if db.users.find_one({"email": email}):
        return jsonify({"error": "Даний акаунт уже існує"}), 400

    pwd_hash = bcrypt.generate_password_hash(password).decode("utf-8")
    user_id = str(uuid.uuid4())
    user_doc = {
        "user_id": user_id,
        "email": email,
        "password_hash": pwd_hash,
        "lamp_ids": [],
        "tariffs_enc": aes_encrypt({"tariffs": []})
    }
    db.users.insert_one(user_doc)
    return jsonify({"token": make_jwt(user_id), "user_id": user_id})

@app.post("/auth/login")
def login():
    data = request.get_json(force=True)
    email = data.get("email", "").strip().lower()
    password = data.get("password", "")
    user = db.users.find_one({"email": email})
    if not user or not bcrypt.check_password_hash(user["password_hash"], password):
        return jsonify({"error": "Невірні дані"}), 401
    return jsonify({"token": make_jwt(user["user_id"]), "user_id": user["user_id"]})

@app.get("/auth/me")
def me():
    uid = auth_user()
    if not uid:
        return jsonify({"error": "Unauthorized"}), 401
    user = db.users.find_one({"user_id": uid}, {"password_hash": 0})
    tariffs = []
    if "tariffs_enc" in user:
        try:
            tariffs = aes_decrypt(user["tariffs_enc"])["tariffs"]
        except Exception:
            tariffs = []
    return jsonify({
        "user_id": uid,
        "email": user["email"],
        "lamp_ids": user.get("lamp_ids", []),
        "tariffs": tariffs
    })

@app.get("/lamps")
def lamps_list():
    uid = auth_user()
    if not uid: return jsonify({"error": "Unauthorized"}), 401
    update_energy(uid)
    user = db.users.find_one({"user_id": uid})
    lamps = list(db.lamps.find({"lamp_id": {"$in": user["lamp_ids"]}}, {"_id": 0}))
    return jsonify(lamps)

@app.post("/lamps")
def lamp_add():
    uid = auth_user()
    if not uid: return jsonify({"error": "Unauthorized"}), 401
    data = request.get_json(force=True)
    lamp_id = data.get("lamp_id") or str(uuid.uuid4())
    lamp = {
        "lamp_id": lamp_id,
        "owner_id": uid,
        "name": data.get("name", ""),  # ← name
        "state": bool(data.get("state", False)),
        "brightness": int(data.get("brightness", 50)),
        "power_w": float(data.get("power_w", 8.5)),
        "work_time_min": int(data.get("work_time_min", 0)),
        "energy_kwh": float(data.get("energy_kwh", 0.0)),
    }
    db.lamps.insert_one(lamp)
    db.users.update_one({"user_id": uid}, {"$addToSet": {"lamp_ids": lamp_id}})
    return jsonify({"lamp_id": lamp_id})

@app.patch("/lamps/<lamp_id>")
def lamp_update(lamp_id):
    uid = auth_user()
    if not uid: return jsonify({"error": "Unauthorized"}), 401
    data = request.get_json(force=True)
    allowed_fields = ["name", "state", "brightness", "work_time_min", "energy_kwh", "power_w"]
    allowed = {k: v for k, v in data.items() if k in allowed_fields}
    res = db.lamps.update_one({"lamp_id": lamp_id, "owner_id": uid}, {"$set": allowed})
    if res.matched_count == 0: return jsonify({"error": "Not found"}), 404
    return jsonify({"ok": True})

@app.get("/lamps/<lamp_id>")
def lamp_get(lamp_id):
    uid = auth_user()
    if not uid: return jsonify({"error": "Unauthorized"}), 401
    lamp = db.lamps.find_one({"lamp_id": lamp_id, "owner_id": uid}, {"_id": 0})
    if not lamp: return jsonify({"error": "Not found"}), 404
    return jsonify(lamp)


@app.delete("/lamps/<lamp_id>")
def lamp_delete(lamp_id):
    uid = auth_user()
    if not uid: return jsonify({"error": "Unauthorized"}), 401
    db.lamps.delete_one({"lamp_id": lamp_id, "owner_id": uid})
    db.users.update_one({"user_id": uid}, {"$pull": {"lamp_ids": lamp_id}})
    return jsonify({"ok": True})

@app.get("/energy/tariffs")
def tariffs_get():
    uid = auth_user()
    if not uid: return jsonify({"error": "Unauthorized"}), 401
    user = db.users.find_one({"user_id": uid})
    return jsonify(aes_decrypt(user["tariffs_enc"])["tariffs"])

@app.put("/energy/tariffs")
def tariffs_put():
    uid = auth_user()
    if not uid: return jsonify({"error": "Unauthorized"}), 401
    tariffs = request.get_json(force=True)
    db.users.update_one({"user_id": uid}, {"$set": {"tariffs_enc": aes_encrypt({"tariffs": tariffs})}})
    return jsonify({"ok": True})

@app.get("/energy/consumption")
def energy_consumption():
    uid = auth_user()
    if not uid: return jsonify({"error": "Unauthorized"}), 401
    lamps = list(db.lamps.find({"owner_id": uid}, {"_id": 0, "energy_kwh": 1}))
    total_kwh = sum(l["energy_kwh"] for l in lamps)
    tariffs = aes_decrypt(db.users.find_one({"user_id": uid})["tariffs_enc"])["tariffs"]
    avg_price = (sum(t["price"] for t in tariffs) / max(len(tariffs), 1)) if tariffs else 0.0
    return jsonify({"total_kwh": total_kwh, "estimated_cost": round(total_kwh * avg_price, 4)})

def update_energy(uid):
    lamps = db.lamps.find({"owner_id": uid})
    for lamp in lamps:
        if lamp.get("state"):
            new_time = lamp.get("work_time_min", 0) + 2
            brightness = lamp.get("brightness", 100)
            power = lamp.get("power_w", 0.0)
            energy = (power * brightness * new_time) / (100 * 60 * 1000)
            db.lamps.update_one(
                {"lamp_id": lamp["lamp_id"]},
                {"$set": {
                    "work_time_min": new_time,
                    "energy_kwh": round(energy, 4)
                }}
            )


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)