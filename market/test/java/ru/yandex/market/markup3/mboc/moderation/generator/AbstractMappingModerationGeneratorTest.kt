package ru.yandex.market.markup3.mboc.moderation.generator

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3ApiService
import ru.yandex.market.markup3.mboc.category.info.service.CategoryInfoService
import ru.yandex.market.markup3.mboc.moderation.MbocMappingModerationConstants
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.mboc.offertask.service.OfferTaskService
import ru.yandex.market.markup3.mboc.taskOffer
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper
import ru.yandex.market.mboc.http.SupplierOffer

class AbstractMappingModerationGeneratorTest : CommonTaskTest() {
    @Autowired
    lateinit var categoryInfoService: CategoryInfoService

    @Autowired
    private lateinit var offerTaskRepository: OfferTaskRepository

    @Autowired
    private lateinit var offerTaskService: OfferTaskService

    @Autowired
    private lateinit var mbocMock: MboCategoryServiceMock

    @Autowired
    private lateinit var markup3ApiService: Markup3ApiService

    private lateinit var mappingModerationGenerator: MappingModerationGenerator

    private lateinit var mappingRecheckModerationGenerator: MappingRecheckModerationGenerator

    @Before
    fun setUp() {
        mbocMock.taskOffersMap.clear()

        mappingModerationGenerator = MappingModerationGenerator(
            keyValueService,
            mbocMock,
            offerTaskService,
            TransactionHelper.MOCK,
            categoryInfoService,
            taskGroupRegistry,
            markup3ApiService
        )

        mappingRecheckModerationGenerator = MappingRecheckModerationGenerator(
            keyValueService,
            mbocMock,
            offerTaskService,
            TransactionHelper.MOCK,
            categoryInfoService,
            taskGroupRegistry,
            markup3ApiService
        )
    }

    @Test
    fun aboba() {
        mbocMock.addTaskOffers(
            taskOffer(
                20,
                15,
                20,
                skuType = SupplierOffer.SkuType.TYPE_PARTNER,
                priority = 120.0,
                ticketId = 0
            ).setTargetSkuId(12),
            taskOffer(
                21,
                15,
                20,
                skuType = SupplierOffer.SkuType.TYPE_PARTNER,
                priority = 120.0,
                ticketId = 0
            ).setTargetSkuId(12),
            taskOffer(1, 15, 1, priority = 270.0, ticketId = 1).setTargetSkuId(12),
            taskOffer(2, 15, 1, priority = 280.0, ticketId = 1).setTargetSkuId(12),
            taskOffer(3, 25, 1, priority = 290.0, ticketId = 2).setTargetSkuId(12),
            taskOffer(4, 25, 1, priority = 300.0, ticketId = 2).setTargetSkuId(12),
            taskOffer(5, 15, 2, priority = 300.0, ticketId = 2).setTargetSkuId(12),
            taskOffer(
                10,
                15,
                2,
                skuType = SupplierOffer.SkuType.TYPE_PARTNER,
                priority = 200.0,
                ticketId = 3
            ).setTargetSkuId(12),
            taskOffer(
                11,
                16,
                2,
                skuType = SupplierOffer.SkuType.TYPE_PARTNER,
                priority = 200.0,
                ticketId = 4
            ).setTargetSkuId(12),
        )
        mappingModerationGenerator.generate()
        var all = offerTaskRepository.findAll()
        all.groupBy { it.taskId }
            .mapValues { (_, offers) ->
                offers.map { it.offerId }.toSet() to offers.maxOf { it.priority }
            }.values shouldContainExactlyInAnyOrder listOf(
            setOf(20L, 21L) to AbstractMappingModerationGenerator.convertMbocPriority(120.0).toDouble(),
            setOf(1L, 2L, 3L, 4L) to AbstractMappingModerationGenerator.convertMbocPriority(300.0).toDouble(),
            setOf(5L) to AbstractMappingModerationGenerator.convertMbocPriority(300.0).toDouble(),
            setOf(10L, 11L) to AbstractMappingModerationGenerator.convertMbocPriority(200.0).toDouble(),
        )
        all shouldHaveSize 9
        all.all { it.status == OfferTaskStatus.WAITING_FOR_RESULTS } shouldBe true
        all.all { it.groupKey == MbocMappingModerationConstants.YANG_GROUP_KEY } shouldBe true


        mbocMock.taskOffersMap.clear()
        mbocMock.addTaskOffers(
            taskOffer(1001, 19, 1, priority = 270.0, ticketId = 1).setTargetSkuId(12),
            taskOffer(1002, 19, 1, priority = 280.0, ticketId = 1).setTargetSkuId(12),
            taskOffer(1003, 35, 1, priority = 290.0, ticketId = 2).setTargetSkuId(12),
            taskOffer(1004, 35, 1, priority = 300.0, ticketId = 2).setTargetSkuId(12),
            taskOffer(1005, 45, 2, priority = 300.0, ticketId = 2).setTargetSkuId(12),
        )

        mappingRecheckModerationGenerator.generate()
        all = offerTaskRepository.findAll()
        all shouldHaveSize 14
        var mappingModerationCount = 0
        var mappingRecheckModerationCount = 0
        all.forEach { i ->
            when (i.groupKey) {
                MbocMappingModerationConstants.YANG_GROUP_KEY -> {
                    mappingModerationCount++
                }
                MbocMappingModerationConstants.RECHECK_GROUP_KEY -> {
                    mappingRecheckModerationCount++
                }
            }
        }
        mappingModerationCount shouldBe 9
        mappingRecheckModerationCount shouldBe 5
    }

    @After
    fun cleanup() {
        mbocMock.taskOffersMap.clear()
    }
}
