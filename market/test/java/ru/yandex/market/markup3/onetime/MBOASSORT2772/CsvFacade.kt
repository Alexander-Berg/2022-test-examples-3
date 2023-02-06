package ru.yandex.market.markup3.onetime.MBOASSORT2772

import com.opencsv.CSVWriter
import com.opencsv.CSVWriterBuilder
import ru.yandex.common.util.csv.CSVReader
import ru.yandex.market.markup3.api.Markup3Api
import java.io.File
import java.io.FileWriter
import java.util.function.BiConsumer
import java.util.function.Consumer

fun readOfferTasks(filePath: String): List<SendToManualMappingModerationTool.OfferTask> {
    val rawOfferTasks = mutableListOf<SendToManualMappingModerationTool.OfferTask>()
    CSVReader(File(filePath))
        .use { csvReader ->
            csvReader.delimiter = ','
            csvReader.readHeaders()
            csvReader.readRecord()
            while (csvReader.fieldsCount > 0) {
                val task = SendToManualMappingModerationTool.OfferTask(
                    categoryId = csvReader.getField("category_id").toLong(),
                    categoryName = csvReader.getField("category_name"),
                    taskKey = csvReader.getField("task_key")
                ).apply {
                    taskItems.add(
                        SendToManualMappingModerationTool.OfferTaskItem(
                            offerId = csvReader.getField("offer_id").toLong(),
                            targetSkuId = csvReader.getField("target_sku_id").toLong()
                        )
                    )
                }
                rawOfferTasks.add(task)
                csvReader.readRecord()
            }
        }
    return rawOfferTasks
}

fun dumpOfferTasks(
    tasks: List<SendToManualMappingModerationTool.OfferTask>,
    filePath: String
) {
    CSVWriterBuilder(FileWriter(filePath))
        .withSeparator(',')
        .build()
        .use { csvWriter ->
            csvWriter.writeNext(
                arrayOf(
                    "offer_id",
                    "target_sku_id",
                    "category_id",
                    "category_name",
                    "task_key"
                )
            )
            tasks.forEach { task ->
                task.taskItems.forEach { taskItem ->
                    csvWriter.writeNext(
                        arrayOf(
                            taskItem.offerId.toString(),
                            taskItem.targetSkuId.toString(),
                            task.categoryId.toString(),
                            task.categoryName,
                            task.taskKey
                        )
                    )
                }
            }
        }
}

fun initTaskWorkerIdsFile(path: String) {
    if (!File(path).exists()) {
        CSVWriter(FileWriter(path)).use { csvWriter ->
            csvWriter.writeNext(
                arrayOf(
                    "external_key",
                    "worker_id"
                )
            )
        }
    }
}

fun readTaskWorkerIds(
    csvReader: CSVReader,
    consumer: BiConsumer<String, String>
) {
    csvReader.readHeaders()
    csvReader.readRecord()
    while (csvReader.fieldsCount > 0) {
        consumer.accept(
            csvReader.getField("external_key"),
            csvReader.getField("worker_id"),
        )
        csvReader.readRecord()
    }
}

fun writeTaskWorkerIds(
    csvWriter: CSVWriter,
    pollResult: Markup3Api.TasksResultPollResponse.TaskResult
) {
    csvWriter.writeNext(
        arrayOf(
            convertExternalKey(pollResult.externalKey),
            pollResult.result.yangMappingModerationResult.workerId
        )
    )
}

fun initCreatedTasksFile(path: String) {
    if (!File(path).exists()) {
        CSVWriter(FileWriter(path)).use { csvWriter ->
            csvWriter.writeNext(
                arrayOf(
                    "external_key",
                    "markup_task_id",
                    "markup_external_key",
                    "status"
                )
            )
        }
    }
}

fun readCreatedTasks(
    csvReader: CSVReader,
    consumer: Consumer<String>
) {
    csvReader.readHeaders()
    csvReader.readRecord()
    while (csvReader.fieldsCount > 0) {
        consumer.accept(
            csvReader.getField("external_key")
        )
        csvReader.readRecord()
    }
}

fun writeCreatedTasks(csvWriter: CSVWriter, createdTasks: List<Markup3Api.CreateTaskResponseItem>) {
    createdTasks.forEach {
        csvWriter.writeNext(
            arrayOf(
                convertExternalKey(it.externalKey),
                it.taskId.value.toString(),
                it.externalKey.value,
                it.result.toString()
            )
        )
    }
}
