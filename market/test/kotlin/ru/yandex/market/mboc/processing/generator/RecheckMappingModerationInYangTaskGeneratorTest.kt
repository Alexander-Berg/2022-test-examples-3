package ru.yandex.market.mboc.processing.generator

import com.nhaarman.mockitokotlin2.any
import org.mockito.Mockito
import ru.yandex.market.mboc.common.offers.model.Offer
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.common.services.category.CategoryCachingService
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock
import ru.yandex.market.mboc.processing.OfferId
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.moderation.generator.RecheckMappingModerationInYangTaskGenerator
import ru.yandex.market.mboc.processing.task.AbstractTaskGenerator
import java.time.LocalDateTime
import kotlin.random.Random

class RecheckMappingModerationInYangTaskGeneratorTest : AbstractTaskGeneratorTest(
    ProcessingStrategyType.YANG_RECHECK_MAPPING_MODERATION
) {

    private val random = Random(100)
    override var categoryCachingServiceMock = Mockito.mock(CategoryCachingServiceMock::class.java)


    override fun setUp() {
        Mockito.doAnswer { invocation -> "abc" }.`when`(categoryCachingServiceMock).getCategoryName(any())
        super.setUp()
    }

    override fun initGenerator(): AbstractTaskGenerator =
        RecheckMappingModerationInYangTaskGenerator(
            skv,
            offerProcessingTaskRepository,
            offerProcessingAssignmentAndTaskRepository,
            markup3ApiServiceMock,
            categoryCachingServiceMock
        )

    override fun generateOneGroupAssignments(from: OfferId, to: OfferId): List<OfferProcessingAssignment> {
        val businessId = random.nextInt()
        val categoryId = random.nextInt()
        val skuType = Offer.SkuType.values().toList().shuffled().first()
        return (from..to).map{
            OfferProcessingAssignment.builder().apply {
                offerId(it)
                businessId(businessId)
                categoryId(categoryId.toLong())
                processingTicketId(123)
                priority(1)
                target(strategy.offerTarget)
                type(strategy.offerProcessingType)
                skuType(skuType)
                targetSkuId(Random.nextLong())
                assignedTs(LocalDateTime.now().plusSeconds(10))
            }.build()
        }.let{ offerProcessingAssignmentRepository.insertOrUpdateAll(it) }
    }
}
