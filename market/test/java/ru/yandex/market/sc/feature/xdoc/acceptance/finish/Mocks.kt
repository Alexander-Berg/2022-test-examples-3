package ru.yandex.market.sc.feature.xdoc.acceptance.finish

fun createSortables(count: Int = 3) = (0 until count).map {
    "XDOC-1110715491777-$it"
}
