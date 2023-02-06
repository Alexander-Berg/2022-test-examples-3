package ru.yandex.market.mdm.lib.service

import ru.yandex.common.util.db.MultiIdGenerator
import java.util.concurrent.atomic.AtomicLong

class IdGeneratorMock: MultiIdGenerator {
    private val counter = AtomicLong()

    override fun getId(): Long {
        return counter.incrementAndGet()
    }

    override fun getIds(count: Int): MutableList<Long> {
        @Suppress("UsePropertyAccessSyntax")
        return IntRange(1, count).map { getId() }.toMutableList()
    }
}
