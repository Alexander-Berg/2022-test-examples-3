package ru.yandex.market.markup3.onetime.MBOASSORT2772

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.util.JsonFormat
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.YtUtils
import ru.yandex.inside.yt.kosher.impl.transactions.utils.YtTransactionsUtils
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.tables.TableWriterOptions
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.type_info.TiType
import ru.yandex.yt.ytclient.proxy.request.CreateNode
import ru.yandex.yt.ytclient.tables.TableSchema
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Optional

class YtFacade(
    val yt: Yt
) {
    companion object {
        val transactionDuration: Duration = Duration.of(1, ChronoUnit.HOURS)
        val transactionPing: Duration = Duration.of(60, ChronoUnit.SECONDS)
    }

    fun writeToYt(
        results: Collection<Markup3Api.TasksResultPollResponse.TaskResult>,
        tablePath: String
    ) {
        val ytTablePath = YPath.simple(tablePath)
        initTable(ytTablePath)

        val ytRows = convert(results)

        YtTransactionsUtils.withTransaction(yt, transactionDuration, Optional.of(transactionPing)) { tx ->
            yt.tables().write(
                Optional.of(tx.id),
                true,
                ytTablePath.append(true),
                YTableEntryTypes.YSON,
                ytRows.iterator()
            )
        }
    }

    private fun convert(results: Collection<Markup3Api.TasksResultPollResponse.TaskResult>): List<YTreeMapNode> =
        results.flatMap { result ->
            val mmResult = result.result.yangMappingModerationResult
            mmResult.resultsList.map { mmOfferResult ->
                val ytMapBuilder = YTree.mapBuilder()
                ytMapBuilder.key("offer_id").value(mmOfferResult.offerId.toLong())
                ytMapBuilder.key("msku").value(mmOfferResult.msku.value)
                ytMapBuilder.key("staff_login").value(mmResult.staffLogin)
                ytMapBuilder.key("status").value(mmOfferResult.status.name)

                val commentsBuilder = YTree.listBuilder()
                mmOfferResult.contentCommentList.forEach { comment ->
                    val json = ObjectMapper().readTree(JsonFormat.printer().print(comment))
                    val yson = YtUtils.json2yson(YTree.builder(), json)
                    commentsBuilder.value(yson.build())
                }
                ytMapBuilder.key("content_comment").value(commentsBuilder.buildList())
                ytMapBuilder.buildMap()
            }
        }

    private fun initTable(ytTablePath: YPath) {
        if (!yt.cypress().exists(ytTablePath)) {
            val schema = TableSchema.Builder()
                .addValue("offer_id", TiType.uint64())
                .addValue("msku", TiType.uint64())
                .addValue("staff_login", TiType.string())
                .addValue("status", TiType.optional(TiType.string()))
                .addValue("content_comment", TiType.optional(TiType.yson()))
                .setUniqueKeys(false)
                .build()
            val attributes = YTree.attributesBuilder()
                .key("strict").value(true)
                .key("optimize_for").value("scan")
                .key("schema").value(schema.toYTree())
                .buildAttributes()
            yt.cypress().create(
                CreateNode(
                    ytTablePath,
                    CypressNodeType.TABLE,
                    attributes
                )
            )
        }
    }
}
