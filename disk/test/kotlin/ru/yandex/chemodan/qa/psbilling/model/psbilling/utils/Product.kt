package ru.yandex.chemodan.qa.psbilling.model.psbilling.utils

data class Product(
    val priceId: String,
    val name: String,
    val period: String,
    val amount: Double,
    val currency: String
)
