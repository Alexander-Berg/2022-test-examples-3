package ru.yandex.market.contentmapping.repository

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.dto.model.ShopModelExportEventType
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

class ExportEventRepositoryTest: BaseAppTestClass() {
    @Autowired
    lateinit var exportEventRepository: ExportEventRepository

    @Test
    fun `test insert`() {
        val r1 = exportEventRepository.add(1, ShopModelExportEventType.ENQUEUED)
        r1.shopModelId shouldBe 1
        r1.type shouldBe ShopModelExportEventType.ENQUEUED

        exportEventRepository.add(1, ShopModelExportEventType.EXPORTED)
        exportEventRepository.add(1, ShopModelExportEventType.SKIPPED)

        val details = "a"
        val r4 = exportEventRepository.add(1, ShopModelExportEventType.ERROR, details)
        r4.shopModelId shouldBe 1
        r4.type shouldBe ShopModelExportEventType.ERROR
        r4.details shouldBe details

        val r5 = exportEventRepository.findBy(1)
        r5 shouldHaveSize 4

        r5[0].asClue {
            it.shopModelId shouldBe 1
            it.type shouldBe ShopModelExportEventType.ERROR
            it.details shouldBe details
        }

        r5[3].asClue {
            it.shopModelId shouldBe 1
            it.type shouldBe ShopModelExportEventType.ENQUEUED
            it.details shouldBe null
        }

        val r6 = exportEventRepository.addAll(listOf(2, 3, 4), ShopModelExportEventType.SKIPPED)
        r6 shouldHaveSize 3

        r6[0].asClue {
            it.shopModelId shouldBe 2
            it.type shouldBe ShopModelExportEventType.SKIPPED
        }

        r6[2].asClue {
            it.shopModelId shouldBe 4
            it.type shouldBe ShopModelExportEventType.SKIPPED
        }
    }

    @After
    fun clear() {
        exportEventRepository.deleteAll()
    }
}
