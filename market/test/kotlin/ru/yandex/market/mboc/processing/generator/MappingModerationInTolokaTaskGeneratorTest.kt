package ru.yandex.market.mboc.processing.generator

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.processing.OfferId
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.moderation.generator.MappingModerationInTolokaTaskGenerator
import ru.yandex.market.mboc.processing.task.AbstractTaskGenerator
import java.time.LocalDateTime
import kotlin.random.Random

class MappingModerationInTolokaTaskGeneratorTest : AbstractTaskGeneratorTest(
    ProcessingStrategyType.TOLOKA_MAPPING_MODERATION
) {

    private val random = Random(100)

    @Test
    override fun `generator should regenerate only optimal by size failed groups`() {
        initUniqFailMarkupApi()
        // There is no special grouping for toloka.
        // With boundaries [22; 40] two created groups for 20 tasks will merge to one group for 40 tasks
        setUpOptimalGroupBoundaries(min = 22, max = 40)
        generateSeveralGroups(2, 20)
        val expectingNotFailedAssignments = emptyList<OfferProcessingAssignment>()

        generator.generate()

        val existingTasks = offerProcessingTaskRepository.findAll()
        checkTasksCorrectness(expectingNotFailedAssignments, existingTasks)
    }

    @Test
    fun `generator should fill target sku id`() {
        initOkMarkupApi()
        setUpOptimalGroupBoundaries(min = 22, max = 40)
        generateSeveralGroups(2, 20)
        clearInvocations(markup3ApiServiceMock)

        generator.generate()

        val assignments = offerProcessingAssignmentRepository.findAll()

        argumentCaptor<Markup3Api.CreateTasksRequest>().apply {
            verify(markup3ApiServiceMock).createTask(capture())

            val groupBy = allValues[0].tasksList[0].input.tolokaMappingModerationInput.offersList
                .associateBy({ it.offerId }, { it.targetSkuId.value })
            assignments.forEach {
                groupBy[it.offerId] shouldBe it.targetSkuId
            }
        }
    }

    override fun initGenerator(): AbstractTaskGenerator =
        MappingModerationInTolokaTaskGenerator(
            skv,
            offerProcessingTaskRepository,
            offerProcessingAssignmentAndTaskRepository,
            markup3ApiServiceMock,
            categoryCachingServiceMock
        )

    override fun generateOneGroupAssignments(from: OfferId, to: OfferId): List<OfferProcessingAssignment> {
        return (from..to).map{
            OfferProcessingAssignment.builder().apply {
                offerId(it)
                businessId(random.nextInt())
                categoryId(random.nextInt().toLong())
                processingTicketId(123)
                priority(1)
                target(strategy.offerTarget)
                type(strategy.offerProcessingType)
                assignedTs(LocalDateTime.now().plusSeconds(10))
                targetSkuId(random.nextLong())
            }.build()
        }.let{ offerProcessingAssignmentRepository.insertOrUpdateAll(it) }
    }
}
