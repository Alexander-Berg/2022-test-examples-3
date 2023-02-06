package ru.yandex.market.markup3.onetime.MBOASSORT2772

import com.google.protobuf.Int64Value
import com.google.protobuf.StringValue
import com.google.protobuf.util.JsonFormat
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.markup3.api.Markup3ApiTaskServiceGrpc
import ru.yandex.market.markup3.utils.getLoggerForEnclosingClass

fun convertExternalKey(task: SendToManualMappingModerationTool.OfferTask): StringValue =
    StringValue.of("${task.taskKey}.${task.workerIds.size}")

fun convertExternalKey(externalKey: StringValue) = externalKey.value.substringBeforeLast(".")

class Markup3ApiFacade(
    val markup3Api: Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceBlockingStub
) {
    companion object {
        private val logger = getLoggerForEnclosingClass()
        private const val DEFAULT_PRIORITY = 1000.0
    }

    fun send(task: SendToManualMappingModerationTool.OfferTask): List<Markup3Api.CreateTaskResponseItem> {
        val request = Markup3Api.CreateTasksRequest.newBuilder().apply {

            taskTypeIdentity = createTaskTypeIdentity()

            val uniqueKeys = task.taskItems.map { "${task.taskKey}.${it.offerId}.${task.workerIds.size}" }

            val mmInputData = Markup3Api.YangMappingModerationInput.newBuilder().apply {
                val yangMMInputDataOffers = task.taskItems.map { taskItem ->
                    Markup3Api.YangMappingModerationInput.YangMappingModerationInputDataOffer.newBuilder().apply {
                        id = taskItem.offerId
                        offerId = taskItem.offerId.toString()
                        categoryId = task.categoryId
                        categoryName = task.categoryName
                        targetSkuId = Int64Value.of(taskItem.targetSkuId)
                    }.build()
                }

                data = Markup3Api.YangMappingModerationInput.YangMappingModerationInputData.newBuilder().apply {
                    addAllOffers(yangMMInputDataOffers)
                    taskType = Markup3Api.YangMappingModerationInput.ModerationTaskType.MAPPING_MODERATION
                    //TODO: taskSubtype = convertToProtoSubtype(toPsku)
                }.build()

                categoryId = task.categoryId
                priority = DEFAULT_PRIORITY

                task.workerIds.forEach { workerId -> addUnavailableFor(workerId) }

            }.build()

            val taskInputData = Markup3Api.TaskInputData.newBuilder().apply {
                yangMappingModerationInput = mmInputData
            }.build()

            val taskForCreate = Markup3Api.TaskForCreate.newBuilder().apply {
                externalKey = convertExternalKey(task)
                addAllUniqKeys(uniqueKeys)
                input = taskInputData
            }.build()

            addTasks(taskForCreate)
        }.build()

        logger.info(JsonFormat.printer().print(request))
        val response = markup3Api.createTask(request)

        logger.info(JsonFormat.printer().print(response))
        if (response.hasSystemFail()) {
            throw RuntimeException(response.systemFail.toString())
        }
        return response.responseItemsList
    }

    fun poll(): List<Markup3Api.TasksResultPollResponse.TaskResult> {
        val request = Markup3Api.TasksResultPollRequest.newBuilder().apply {
            taskTypeIdentity = createTaskTypeIdentity()
            count = 1000
        }

        logger.info(JsonFormat.printer().print(request.build()))
        val response = markup3Api.pollResults(request.build())

        if (response.hasSystemFail()) {
            throw RuntimeException(response.systemFail.toString())
        }

        return response.resultsList
    }

    fun consume(taskResultIds: List<Long>) {
        val request = Markup3Api.ConsumeResultRequest.newBuilder().apply {
            taskTypeIdentity = createTaskTypeIdentity()
            addAllTaskResultIds(taskResultIds)
        }

        logger.info(JsonFormat.printer().print(request.build()))
        val response = markup3Api.consumeResults(request.build())

        if (response.hasSystemFail()) {
            throw RuntimeException(response.systemFail.toString())
        }
    }

    private fun createTaskTypeIdentity() =
        Markup3Api.TaskTypeIdentity.newBuilder().apply {
            type = Markup3Api.TaskType.YANG_MAPPING_MODERATION
            groupKey = "manual_mapping_moderation"
        }.build()
}
