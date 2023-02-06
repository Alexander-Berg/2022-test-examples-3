package ru.yandex.market.partner.status.yt

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.impl.ytree.`object`.annotation.YTreeObject
import ru.yandex.inside.yt.kosher.tables.async.YtTables
import ru.yandex.inside.yt.kosher.transactions.async.YtTransactions
import ru.yandex.market.javaframework.yt.provider.YtProvider
import ru.yandex.market.starter.yt.provider.async.YtAsyncProvider
import ru.yandex.mj.generated.yt.YtClientId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

/**
 * Тесты для [YtTableReader].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class YtTableReaderTest {

    @Test
    fun `error while cluster selection`() {

        // Создаем два кластера: arnold и hahn
        // arnold падает на проверке, hahn работает ок
        // Поэтому для дальнейшего запроса выбираем hahn
        val failedTransaction = mock<YtTransactions> {
            on { start() } doReturn CompletableFuture.failedFuture(TimeoutException())
        }
        val successTransaction = mock<YtTransactions> {
            on { start() } doReturn CompletableFuture.completedFuture(GUID.create())
            on { abort(any()) } doReturn CompletableFuture.completedFuture(null)
        }
        val hahnTable = mock<YtTables> {
            on {
                read<YtTableReaderTestData>(
                    anyOrNull(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } doReturn CompletableFuture.completedFuture(
                null
            )
        }

        val ytProvider = YtProvider(
            null,
            YtAsyncProvider(
                mapOf(
                    YtClientId.ARNOLD_ID to mock {
                        on { transactions() } doReturn failedTransaction
                    },
                    YtClientId.HAHN_ID to mock {
                        on { transactions() } doReturn successTransaction
                        on { tables() } doReturn hahnTable
                    },
                )
            )
        )

        val yqlNamedParameterJdbcTemplate = mock<NamedParameterJdbcTemplate> {
            on { update(any(), any<Map<String, Any>>()) } doReturn 1
        }
        val reader = object : YQLPreparedYtTableReader<YtTableReaderTestData>(
            ytProvider,
            "//tmp/path",
            listOf(YtClientId.ARNOLD, YtClientId.HAHN),
            YtTableReaderTestData::class,
            yqlNamedParameterJdbcTemplate
        ) {
            override val yql = "yql"
        }

        runBlocking { reader.read().collect() }

        val queryCaptor = argumentCaptor<String>()
        verify(yqlNamedParameterJdbcTemplate).update(queryCaptor.capture(), any<Map<String, Any>>())

        // Проверяем, что был выбран hahn
        Assertions.assertThat(queryCaptor.firstValue)
            .contains(YtClientId.HAHN_ID)
    }
}

@YTreeObject
data class YtTableReaderTestData(
    val id: Long
)
