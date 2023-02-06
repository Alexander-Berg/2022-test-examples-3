package ru.yandex.market.sc.test.data.partner.sort

data class SortParam(
    val param: String,
    val order: SortOrder = SortOrder.ASC,
) {
    override fun toString(): String {
        return "$param,${order.value}"
    }

    enum class SortOrder(val value: String) {
        ASC("asc"),
        DESC("desc"),
    }
}
