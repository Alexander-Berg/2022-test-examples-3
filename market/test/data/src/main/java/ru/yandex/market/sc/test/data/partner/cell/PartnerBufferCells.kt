package ru.yandex.market.sc.test.data.partner.cell

data class PartnerBufferCells(
    val cells: List<PartnerBufferCell>,
    val ordersToSortTotal: Int,
    val ordersTotal: Int,
)