package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.utils.data.ExternalId

object IdManager {
    private var idCounter = 0L

    fun getId(): Long = ++idCounter

    fun getExternalId(id: Long = getId()): ExternalId {
        return ExternalId("demo-$id")
    }

    fun getIndexedExternalId(externalId: ExternalId = getExternalId(), index: Int = 0): ExternalId {
        return ExternalId("$externalId-$index")
    }

    fun generateId(short: Boolean = false): Long =
        if (short) System.currentTimeMillis() else System.nanoTime()

    fun generateExternalId(prefix: String = "", short: Boolean = false): ExternalId =
        ExternalId("$prefix${generateId(short)}")
}
