package ru.yandex.market.sc.test.network.utils

import ru.yandex.market.sc.core.utils.data.ExternalId

object InboundInformationListCodeGenerator {

    fun generate(): ExternalId {
        return ExternalId("Зп-${(100000000..999999999).random()}")
    }

    fun generateList(count: Int): List<ExternalId> {
        val list = mutableListOf<ExternalId>()
        repeat(count) {
            var next = generate()
            while (list.contains(next)) {
                next = generate()
            }
            list.add(next)
        }
        return list
    }
}