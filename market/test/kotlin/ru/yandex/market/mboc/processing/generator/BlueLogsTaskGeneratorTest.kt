package ru.yandex.market.mboc.processing.generator

import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.processing.OfferId
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.bluelogs.generator.BlueLogsTaskGenerator
import ru.yandex.market.mboc.processing.task.AbstractTaskGenerator
import java.time.LocalDateTime
import kotlin.random.Random

class BlueLogsTaskGeneratorTest : AbstractTaskGeneratorTest(ProcessingStrategyType.BLUE_LOGS) {

    private val random = Random(100)

    override fun initGenerator(): AbstractTaskGenerator =
        BlueLogsTaskGenerator(
            skv,
            offerProcessingTaskRepository,
            offerProcessingAssignmentAndTaskRepository,
            markup3ApiServiceMock,
            categoryCachingServiceMock
        )

    override fun generateOneGroupAssignments(from: OfferId, to: OfferId): List<OfferProcessingAssignment> {
        val businessId = random.nextInt()
        val categoryId = random.nextInt()
        return (from..to).map{
            OfferProcessingAssignment.builder().apply {
                offerId(it)
                businessId(businessId)
                categoryId(categoryId.toLong())
                processingTicketId(123)
                priority(1)
                target(strategy.offerTarget)
                type(strategy.offerProcessingType)
                assignedTs(LocalDateTime.now().plusSeconds(10))
            }.build()
        }.let{ offerProcessingAssignmentRepository.insertOrUpdateAll(it) }
    }
}
