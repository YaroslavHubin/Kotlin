package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel керує симуляцією зарядки автомобіля в реальному часі.
 * Оновлення даних відбувається щосекунди.
 */
class ChargingViewModel : ViewModel() {

    // Автомобіль із батареєю 75 кВт·год, зарядженою на 20 кВт·год
    private val _car = MutableStateFlow(Car("1", "Tesla Model 3", Battery(75.0, 20.0)))
    val car = _car.asStateFlow()

    // Поточний зарядний пристрій (за замовчуванням — швидкий)
    private val _charger = MutableStateFlow<ICharger>(FastCharger())
    val charger = _charger.asStateFlow()

    // Чи триває зарядка зараз
    private val _isCharging = MutableStateFlow(false)
    val isCharging = _isCharging.asStateFlow()

    // Скільки енергії додано за поточну сесію
    private val _addedKWh = MutableStateFlow(0.0)
    val addedKWh = _addedKWh.asStateFlow()

    // Скільки секунд триває зарядка
    private val _chargingSeconds = MutableStateFlow(0L)
    val chargingSeconds = _chargingSeconds.asStateFlow()

    private var chargingJob: Job? = null

    /**
     * Запуск симуляції зарядки — оновлення відбувається кожну секунду.
     */
    fun startCharging() {
        if (_isCharging.value) return
        _isCharging.value = true
        _addedKWh.value = 0.0
        _chargingSeconds.value = 0

        chargingJob = viewModelScope.launch {
            while (isActive && _isCharging.value) {
                delay(1000) // 1 секунда
                val carValue = _car.value
                val chargerValue = _charger.value

                // Заряджаємо батарею на 1 секунду
                val added = chargerValue.charge(carValue.battery, 1)
                _addedKWh.value += added
                _chargingSeconds.value += 1

                // Якщо батарея повна — зупиняємо
                if (carValue.battery.isFull()) {
                    _isCharging.value = false
                    break
                }

                // Оновлюємо стан автомобіля
                _car.value = carValue.copy(
                    battery = carValue.battery.copy(
                        currentKWh = carValue.battery.currentKWh
                    )
                )
            }
        }
    }

    /**
     * Зупинити зарядку вручну.
     */
    fun stopCharging() {
        _isCharging.value = false
        chargingJob?.cancel()
    }

    /**
     * Встановити інший зарядний пристрій.
     */
    fun selectCharger(charger: ICharger) {
        _charger.value = charger
    }

    /**
     * Встановити заряд батареї на певний відсоток (для демонстрації).
     */
    fun setBatteryPercent(percent: Int) {
        val carValue = _car.value
        val newKWh = carValue.battery.capacityKWh * (percent / 100.0)
        _car.value = carValue.copy(
            battery = carValue.battery.copy(currentKWh = newKWh)
        )
    }

    override fun onCleared() {
        super.onCleared()
        chargingJob?.cancel()
    }
}
