package ru.yandex.market.mdm.lib.service

import org.assertj.core.api.Assertions
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.lib.testutils.BaseAppTestClass
import kotlin.concurrent.thread

class PgSequenceMdmIdGeneratorTest: BaseAppTestClass() {
    @Autowired
    lateinit var mdmIdGenerator: PgSequenceMdmIdGenerator

    @Test
    fun `test single thread`() {
        Assertions.assertThat(mdmIdGenerator.getIds(150).toSet()).hasSize(150)
    }

    @Test
    fun `test for concurrent threads`() {
        var idSet1 = mutableSetOf<Long>()
        var idSet2 = mutableSetOf<Long>()

        val thread1 = thread(start = false) { idSet1 = mdmIdGenerator.getIds(150).toMutableSet() }
        val thread2 = thread(start = false) { idSet2 = mdmIdGenerator.getIds(150).toMutableSet() }

        thread1.start()
        thread2.start()

        thread1.join()
        thread2.join()

        idSet1.addAll(idSet2)
        Assertions.assertThat(idSet1).hasSize(300)
    }
}
