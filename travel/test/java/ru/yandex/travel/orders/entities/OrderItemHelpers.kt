package ru.yandex.travel.orders.entities

fun setTrainTicketRefunds(orderItem: TrainOrderItem, refunds: List<TrainTicketRefund>) {
    orderItem.trainTicketRefunds = refunds
}
