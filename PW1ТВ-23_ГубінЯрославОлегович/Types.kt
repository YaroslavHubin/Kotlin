package com.example.lib

fun main() {

    // Приклад роботи з оголошенням змінних та типами
    val age: Int = 23       // ціле число
    val pi: Double = 3.1415926    // число з плаваючою точкою
    val message: String = "Привіт Kotlin!" // рядок
    val letter: Char = 'C' // буква
    val isActive: Boolean = true // булеве значення

    val num = 123
    val name = "Yaroslav"
    val isMan = true

    println(num)
    println(name)
    println(isMan)

    println("Приклад роботи з типами:")
    println("Ціле число: $age")
    println("Число Double: $pi")
    println("Рядок: $message")
    println("Буква: $letter")
    println("Булеве значення: $isActive")

    // Приклад введення з консолі
    print("Введіть число: ")
    val input = readLine()    // введення рядка з консолі
    println("Ви ввели: $input")
}