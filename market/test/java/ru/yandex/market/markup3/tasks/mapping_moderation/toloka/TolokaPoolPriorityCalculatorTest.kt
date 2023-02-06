package ru.yandex.market.markup3.tasks.mapping_moderation.toloka

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.core.dto.TaskGroupConfig
import ru.yandex.market.markup3.mboc.OfferId
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.TolokaPoolPriorityCalculator.Companion.DEFAULT_CONFIG
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaInstruction
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationOffer
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationOfferInfo
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationProperties
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationSkuInfo
import ru.yandex.market.markup3.testutils.CommonTaskTest

class TolokaPoolPriorityCalculatorTest : CommonTaskTest() {
    @Autowired
    lateinit var calculator: TolokaPoolPriorityCalculator

    @Test
    fun `Test config works`() {
        val taskGroup = createTestTaskGroup(
            TaskGroupConfig(
                taskTypeProps = TolokaMappingModerationProperties(priorityConfig = DEFAULT_CONFIG)
            )
        )
        // 4 offers: 1 blue 1 white 1 target sku 1 dsbs
        // 40 + (1/4 * (90 - 40)) = 52.5
        calculator.calculatePoolPriority(
            taskGroup, mapOf(
                offer(1, "BLUE") to null,
                offer(2, "WHITE") to null,
                offer(3, "DSBS") to null,
                offer(4, "BLUE") to 1,
            )
        ) shouldBe 52L
    }

    private fun offer(id: OfferId, dest: String) = TolokaMappingModerationOffer(
        1, dest, 3, id, 5,
        TolokaMappingModerationOfferInfo(
            emptyList(), "2", "3", "4", emptyList(), hashMapOf(), dest,
        ),
        TolokaMappingModerationSkuInfo(
            "1", 2, emptyList(), "4", emptyList(), emptyList(), emptyList(), "8", "9"
        ),
        TolokaInstruction(null, null, null, null)
    )
}
