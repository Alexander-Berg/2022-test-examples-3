package ru.yandex.market.markup3.onetime.MBOASSORT2772

import com.opencsv.CSVWriter
import io.grpc.netty.NettyChannelBuilder
import ru.yandex.common.util.csv.CSVReader
import ru.yandex.market.markup3.api.Markup3ApiTaskServiceGrpc
import ru.yandex.market.markup3.utils.getLoggerForEnclosingClass
import ru.yandex.market.yt.util.client.YtHttpClientConfiguration
import ru.yandex.market.yt.util.client.YtHttpClientFactory
import ru.yandex.misc.thread.ThreadUtils
import java.io.File
import java.io.FileWriter
import java.util.concurrent.TimeUnit

object SendToManualMappingModerationTool {
    private val logger = getLoggerForEnclosingClass()

    private const val POLL_TASK_SLEEP_SECONDS = 60L
    private const val MAX_TASK_SIZE = 10
    private const val MAX_PROCESSING_COUNT = 5
    private const val MESSAGE_SIZE_LIMIT = 100 * 1024 * 1024;

    private const val MARKUP_HOST = "markup3.vs.market.yandex.net"

    // private const val MARKUP_HOST = "markup3.tst.vs.market.yandex.net"
    private const val MARKUP_PORT = 8080

    // private const val PREIFIX = "test_"
    // private const val PREIFIX = "one_"
    // private const val PREIFIX = "1_"
    private const val PREIFIX = "full_"

    private const val YT_HTTP_PROXY = "hahn.yt.yandex.net"
    private const val YT_TOKEN = "__PUT_YOUT_YT_TOKEN_HERE__"
    private const val YT_RESULTS_PATH = "//home/market/users/dergachevfv/MBOASSORT-2772_${PREIFIX}results"

    private const val WORKING_DIR_PATH = "/home/dergachevfv/work/MBOASSORT-2772"

    private const val OFFER_DATA_PATH = "${WORKING_DIR_PATH}/${PREIFIX}offer_data.csv"
    private const val OFFER_DATA_WIHT_KEYS_PATH = "${WORKING_DIR_PATH}/${PREIFIX}offer_data_with_keys.csv"
    private const val CREATED_TASK_IDS_PATH = "${WORKING_DIR_PATH}/${PREIFIX}created_tasks_ids.csv"
    private const val TASK_WORKER_IDS_PATH = "${WORKING_DIR_PATH}/${PREIFIX}task_worker_id.csv"

    @JvmStatic
    fun main(args: Array<String>) {
        initCreatedTasksFile(CREATED_TASK_IDS_PATH)
        initTaskWorkerIdsFile(TASK_WORKER_IDS_PATH)
        if (!File(OFFER_DATA_WIHT_KEYS_PATH).exists()) {
            val rawOfferTasks = readOfferTasks(OFFER_DATA_PATH)
            fillKeys(rawOfferTasks)
            dumpOfferTasks(rawOfferTasks, OFFER_DATA_WIHT_KEYS_PATH)
        }

        val tasksByKey = readOfferTasks(OFFER_DATA_WIHT_KEYS_PATH)
            .groupingBy { it.taskKey }
            .reduceTo(mutableMapOf()) { _, accumulator, element ->
                accumulator.taskItems.addAll(element.taskItems)
                accumulator
            }

        fun onWorkerResult(taskKey: String, workerId: String) {
            tasksByKey.compute(taskKey) { _, current ->
                if (current == null) {
                    logger.warn("unknown externalKey = ${taskKey}")
                    return@compute null
                }

                current.workerIds.add(workerId)
                if (current.workerIds.size < MAX_PROCESSING_COUNT) {
                    return@compute current
                } else {
                    logger.info("received ${current.workerIds.size} results, excluding task ${current.taskKey}")
                    return@compute null
                }
            }
        }

        // read previous results
        CSVReader(File(TASK_WORKER_IDS_PATH)).use { csvReader ->
            readTaskWorkerIds(csvReader) { taskKey, workerId -> onWorkerResult(taskKey, workerId) }
        }

        // read previously sent tasks
        val sentTaskKeys = mutableSetOf<String>()
        CSVReader(File(CREATED_TASK_IDS_PATH)).use { csvReader ->
            readCreatedTasks(csvReader) { taskKey -> sentTaskKeys.add(taskKey) }
        }

        val markup3Facade = markup3Facade()
        val ytFacade = ytFacade()

        CSVWriter(FileWriter(CREATED_TASK_IDS_PATH, true)).use { csvWriter ->
            tasksByKey.forEach {
                if (sentTaskKeys.contains(it.key)) {
                    logger.info("not sending already sent task ${it.key}")
                    return@forEach
                }
                logger.info("initially sending task ${it.key}")
                val createdTasks = markup3Facade.send(it.value)
                writeCreatedTasks(csvWriter, createdTasks)
                ThreadUtils.sleep(100, TimeUnit.MILLISECONDS)
            }
        }

        while (tasksByKey.isNotEmpty()) {
            val pollResults = markup3Facade.poll()

            if (pollResults.isEmpty()) {
                logger.info("still no results, sleeping ${POLL_TASK_SLEEP_SECONDS} sec")
                ThreadUtils.sleep(POLL_TASK_SLEEP_SECONDS, TimeUnit.SECONDS)
                continue
            } else {
                logger.info("polled ${pollResults.size} results")
            }

            val pollResultsToHandle = pollResults
                .associateBy {
                    convertExternalKey(it.externalKey)
                }
                .filter {
                    if (!tasksByKey.containsKey(it.key)) {
                        logger.info("polled unknown externalKey ${it.key}")
                        false
                    } else {
                        true
                    }
                }

            ytFacade.writeToYt(pollResultsToHandle.values, YT_RESULTS_PATH)

            CSVWriter(FileWriter(TASK_WORKER_IDS_PATH, true)).use { taskWidWriter ->
                CSVWriter(FileWriter(CREATED_TASK_IDS_PATH, true)).use { sentTaskWriter ->

                    pollResultsToHandle.forEach { (taskKey, pollResult) ->
                        writeTaskWorkerIds(taskWidWriter, pollResult)
                        onWorkerResult(
                            taskKey,
                            pollResult.result.yangMappingModerationResult.workerId
                        )
                        tasksByKey[taskKey]?.let {
                            logger.info("received ${it.workerIds.size} results, resending task ${it.taskKey}")
                            val createdTasks = markup3Facade.send(it)
                            writeCreatedTasks(sentTaskWriter, createdTasks)
                            ThreadUtils.sleep(100, TimeUnit.MILLISECONDS)
                        }
                    }
                }
            }

            // consume all polled results
            markup3Facade.consume(pollResults.map { it.taskResultId })
        }
    }

    private fun fillKeys(offerTasks: List<OfferTask>) {
        val categoryCounters: MutableMap<Long, Int> = mutableMapOf()
        var lastCategoryId: Long? = null
        var currentGroupSize = 0
        offerTasks.asSequence()
            .sortedBy { it.categoryId }
            .forEach { offerTask ->
                if (lastCategoryId != offerTask.categoryId) {
                    currentGroupSize = 0
                }
                var currentTaskCounter = categoryCounters.getOrDefault(offerTask.categoryId, 0)
                if (currentGroupSize >= MAX_TASK_SIZE) {
                    currentGroupSize = 0
                    currentTaskCounter++
                }
                currentGroupSize++

                categoryCounters[offerTask.categoryId] = currentTaskCounter
                lastCategoryId = offerTask.categoryId

                offerTask.apply {
                    taskKey = "${PREIFIX}manual_mm.${offerTask.categoryId}.${currentTaskCounter}"
                }
            }
    }

    private fun markup3Facade(): Markup3ApiFacade {
        val channel = NettyChannelBuilder.forAddress(MARKUP_HOST, MARKUP_PORT)
            .usePlaintext()
            .maxInboundMessageSize(MESSAGE_SIZE_LIMIT)
            .userAgent("Tool_MBOASSORT_2772")
            .build()
        val markup3Api = Markup3ApiTaskServiceGrpc.newBlockingStub(channel)
        return Markup3ApiFacade(markup3Api)
    }

    fun ytFacade(): YtFacade {
        val config = YtHttpClientConfiguration()
            .setYtHttpProxy(YT_HTTP_PROXY)
            .setYtToken(YT_TOKEN)
        val yt = YtHttpClientFactory(config).instance
        return YtFacade(yt)
    }

    data class OfferTask(
        var taskKey: String? = null,
        val categoryId: Long,
        val categoryName: String,
        val taskItems: MutableList<OfferTaskItem> = mutableListOf(),
        val workerIds: MutableSet<String> = mutableSetOf()
    )

    data class OfferTaskItem(
        val offerId: Long,
        val targetSkuId: Long
    )

    /*val results: List<Markup3Api.TasksResultPollResponse.TaskResult> = listOf(
        Markup3Api.TasksResultPollResponse.TaskResult.newBuilder().apply {
            result = Markup3Api.TaskResultData.newBuilder().apply {
                yangMappingModerationResult = Markup3Api.YangMappingModerationResult.newBuilder().apply {
                    staffLogin = "test"
                    workerId = "testwid"
                    addResults(Markup3Api.YangMappingModerationResult.MappingModerationResultItem
                        .newBuilder().apply {
                            offerId = "1"
                            msku = Int64Value.of(111L)
                            status = Markup3Api.YangMappingModerationResult.MappingModerationStatus.ACCEPTED
                            staffLogin = "test"
                            workerId = "testwid"
                            addContentComment(Markup3Api.YangMappingModerationResult.YangContentComment
                                .newBuilder().apply {
                                    type = StringValue.of("SOME_COMMENT_TYPE")
                                    addAllItems(listOf("a", "b", "c"))
                                })
                        }
                    )
                }.build()
            }.build()
        }.build()
    )

    if (true) throw RuntimeException("STOP!!")*/
}
