package ru.yandex.market.sc.test.network.data.manual.route

data class ShipRouteRequest(
    val cellId: Int,
    val comment: String,
    val force: Boolean,
    val orderShipped: String,
    val placeShipped: String?,
)