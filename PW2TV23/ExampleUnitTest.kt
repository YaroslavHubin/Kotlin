package com.example.pw2tv_23

import org.junit.Test

import org.junit.Assert.*


class ApplianceCalculatorTest {

    @Test
    fun testCalculateEnergy() {
        // Потужність 2 кВт, час 3 год, коефіцієнт 100%
        val energy = calculateEnergy(2.0, 3.0, 100.0)
        assertEquals(6.0, energy, 0.001)
    }

    @Test
    fun testCalculateEnergyWithEfficiency() {
        // Потужність 1.5 кВт, час 2 год, ефективність 80%
        val energy = calculateEnergy(1.5, 2.0, 80.0)
        assertEquals(3.75, energy, 0.001) // 1.5*2*0.8 = 2.4
    }

    @Test
    fun testTotalEnergyForCars() {
        val result = totalEnergyForCars(2.0, 3.0, 5.0)
        assertEquals(10.0, result, 0.001)
    }

    @Test
    fun testCalculateCost() {
        val result = calculateCost(10.0, 6.5)
        assertEquals(65.0, result, 0.001)
    }

    @Test
    fun testApplianceSelection() {
        val appliance = Appliance("Фен", 1.8)
        appliance.selected.value = true
        appliance.hours.value = 2.0

        assertTrue(appliance.selected.value)
        assertEquals(2.0, appliance.hours.value, 0.001)
    }
}