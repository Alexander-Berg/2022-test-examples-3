package ru.yandex.market.markup3.mboc.moderation.saver

import com.google.protobuf.Int64Value
import com.google.protobuf.StringValue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api.TolokaMappingModerationResult
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.mboc.moderation.MbocMappingModerationConstants
import ru.yandex.market.markup3.mboc.moderation.saver.TolokaMappingModerationSaver.ResultToSave
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.testutils.CommonTaskTest

class TolokaMappingModerationSaverTest : CommonTaskTest() {
    @Autowired
    private lateinit var tolokaMappingModerationSaver: TolokaMappingModerationSaver

    @Autowired
    lateinit var mboCategoryService: MboCategoryServiceMock

    private lateinit var taskGroup: TaskGroup

    @Before
    fun setup() {
        taskGroup = taskGroupRegistry.getOrCreateTaskGroup(MbocMappingModerationConstants.TOLOKA_GROUP_KEY) {
            TaskGroup(
                key = MbocMappingModerationConstants.TOLOKA_GROUP_KEY,
                name = "toloka mm",
                taskType = TaskType.TOLOKA_MAPPING_MODERATION,
            )
        }
    }

    @Test
    fun `toloka moderation - save a lot`() {
        mboCategoryService.savedMappingModerationIds.clear()

        val results = mutableListOf<ResultToSave>()
        for (i in 1L..1000L) {
            results.add(
                ResultToSave(
                    resultId = i,
                    taskId = 1,
                    data = TolokaMappingModerationResult.newBuilder().apply {
                        addFinishedOffers(
                            TolokaMappingModerationResult.MappingModerationResultItem.newBuilder().apply {
                                msku = Int64Value.of(0)
                                status = TolokaMappingModerationResult.MappingModerationStatus.ACCEPTED
                                offerId = i.toString()
                                addContentComment(
                                    TolokaMappingModerationResult.YangContentComment.newBuilder().apply {
                                        type = StringValue.of("test")
                                        addItems("test")
                                    })
                                skuModifiedTs = Int64Value.of(6)
                            })
                    }.build()
                )
            )
        }

        tolokaMappingModerationSaver.saveResults(results)

        mboCategoryService.savedMappingModerationIds shouldHaveSize 1000
        mboCategoryService.savedMappingModerationIds shouldContainExactlyInAnyOrder (1..1000).map { it.toString() }
    }
}
