package ru.yandex.market.sc.test.data.common.sortingcenter

import ru.yandex.market.sc.core.utils.data.Identifiable

data class SortingCenter(
    override val id: Long,
    val address: String,
    val scName: String,
    val regionTagSuffix: String,
    val token: String,
    val partnerName: String,
    val partnerId: String? = null,
) : Identifiable
