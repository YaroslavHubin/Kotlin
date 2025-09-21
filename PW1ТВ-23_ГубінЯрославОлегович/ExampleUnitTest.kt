package com.example.pw1tv_23

import org.junit.Assert.*
import org.junit.Test

class BatteryScreenLogicTest {

    @Test
    fun testBatteryLevels() {
        // Логіка перевірки рівнів (імітація, як у when в BatteryScreen)
        fun getMessage(charge: Int): String = when {
            charge in 81..100 -> "Повний заряд!"
            charge in 51..80 -> "Заряд нормальний."
            charge in 21..50 -> "Заряд низький."
            charge in 0..20 -> "Дуже низький заряд! Потрібна підзарядка."
            else -> "Некоректне значення!"
        }

        assertEquals("Повний заряд!", getMessage(90))
        assertEquals("Заряд нормальний.", getMessage(70))
        assertEquals("Заряд низький.", getMessage(40))
        assertEquals("Дуже низький заряд! Потрібна підзарядка.", getMessage(10))
        assertEquals("Некоректне значення!", getMessage(-5))
        assertEquals("Некоректне значення!", getMessage(150))
    }
}