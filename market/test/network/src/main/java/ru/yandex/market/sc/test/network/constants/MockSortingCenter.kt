package ru.yandex.market.sc.test.network.constants

import ru.yandex.market.sc.core.utils.data.Identifiable

data class MockSortingCenter(
    override val id: Long,
    val partnerId: Long,
    val name: String,
    val token: String,
    val credentials: AccountCredentials,
) : Identifiable
