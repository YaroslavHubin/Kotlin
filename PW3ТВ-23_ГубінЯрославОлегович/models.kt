package com.example.myapplication

/**
 * Моделі предметної області — зарядна станція та автомобіль.
 *
 * Тут реалізовано:
 * - Battery: клас, що зберігає ємність та поточний заряд (kWh та %).
 * - Car: містить Battery та основні метадані.
 * - ICharger: інтерфейс для зарядних пристроїв (поліморфізм).
 * - FastCharger та SlowCharger: реалізації ICharger з різними швидкостями (кВт).
 */

/**
 * Батерея електромобіля.
 * @param capacityKWh — повна ємність батареї в кВт·год.
 * @param currentKWh — поточна кількість кВт·год (може бути від 0 до capacityKWh).
 */
data class Battery(
    val capacityKWh: Double,
    var currentKWh: Double
) {
    init {
        if (currentKWh < 0.0) currentKWh = 0.0
        if (currentKWh > capacityKWh) currentKWh = capacityKWh
    }

    /**
     * Поточний відсоток заряду (0..100)
     */
    fun percent(): Int = ((currentKWh / capacityKWh) * 100).toInt()

    /**
     * Додає певну кількість кВт·год до батареї, але не перевищує capacityKWh.
     * Повертає фактично додану кількість кВт·год.
     */
    fun addKWh(amount: Double): Double {
        val before = currentKWh
        currentKWh = (currentKWh + amount).coerceAtMost(capacityKWh)
        return currentKWh - before
    }

    /**
     * Перевірка чи батарея вже повністю заряджена
     */
    fun isFull(): Boolean = currentKWh >= capacityKWh - 1e-6
}

/**
 * Простий об'єкт/модель автомобіля
 */
data class Car(
    val id: String,
    val model: String,
    val battery: Battery
)

/**
 * Інтерфейс зарядного пристрою.
 * Метод charge — імітує зарядження батареї протягом deltaSeconds.
 */
interface ICharger {
    val name: String
    val powerKW: Double // потужність зарядки в кВт (кіловати)

    /**
     * Викликається для зарядки батареї протягом певної кількості секунд.
     * Повертає кількість доданих кВт·год.
     *
     * Примітка: для простоти ми вважаємо, що 1 кВт працює 1 годину => 1 кВт * 1 год = 1 кВт·год
     * Тому, якщо powerKW = 50 кВт і deltaSeconds = 60, додано = 50 * (60 / 3600) кВт·год.
     */
    fun charge(battery: Battery, deltaSeconds: Long): Double
}

/**
 * Швидкий зарядний пристрій (fast)
 */
class FastCharger(override val name: String = "Fast Charger", override val powerKW: Double = 50.0) : ICharger {
    override fun charge(battery: Battery, deltaSeconds: Long): Double {
        val hours = deltaSeconds.toDouble() / 3600.0
        val energy = powerKW * hours
        return battery.addKWh(energy)
    }
}

/**
 * Повільний зарядний пристрій (slow, наприклад 7 kW)
 */
class SlowCharger(override val name: String = "Slow Charger", override val powerKW: Double = 7.0) : ICharger {
    override fun charge(battery: Battery, deltaSeconds: Long): Double {
        val hours = deltaSeconds.toDouble() / 3600.0
        val energy = powerKW * hours
        return battery.addKWh(energy)
    }
}