package ru.yandex.market.wms.core.service.impl

import org.dbunit.dataset.Column
import org.dbunit.dataset.filter.IColumnFilter
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.jms.core.JmsTemplate

const val sku = "ROV01"
const val storerKey = "0001"
const val lot = "123001"
const val user = "TESTUSER"
const val picking_loc = "LOC01"
const val picking_id = ""
const val picking_id01 = "ID01"
const val consolidation_loc = "PICKTO"
const val consolidation_id = "CART01"

fun JmsTemplate.verifyMessageSent(queue: String, message: Any) {
    Mockito.verify(this, Mockito.times(1))
        .convertAndSend(ArgumentMatchers.eq(queue), ArgumentMatchers.eq(message))
}

fun JmsTemplate.verifyNothingSent(queue: String, requestClass: Class<*>) {
    Mockito.verify(this, Mockito.times(0))
        .convertAndSend(
            ArgumentMatchers.eq(queue),
            ArgumentMatchers.any(requestClass)
        )
}

class ItrnKeyFilter : IColumnFilter {
    override fun accept(tableName: String, column: Column): Boolean {
        return !(tableName == "ITRN" && column.columnName == "ITRNKEY" ||
            tableName == "ITRNSERIAL" && (column.columnName == "ITRNKEY" || column.columnName == "ITRNSERIALKEY"))
    }
}
