package ru.yandex.market.mbi.feed.processor.yt.reader

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.yandex.inside.yt.kosher.impl.ytree.`object`.annotation.YTreeObject
import ru.yandex.market.mbi.feed.processor.yt.YtFactory
import ru.yandex.market.mbi.feed.processor.yt.YtProperties

/**
 * Ручной тест для [AbstractYtTableReader].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@Disabled
internal class YtTableReaderManualTest {

    private val ytFactory: YtFactory

    init {
        val ytProperties = YtProperties(
            username = "robot-feed-proc-test",
            token = "token"
        )
        ytFactory = YtFactory(ytProperties)
    }

    @Test
    fun `manual test`() {
        val reader = TestYtReader(ytFactory)

        runBlocking {
            val read: Flow<TestYtModel> = reader.read()
            read.collect {
                println("Data from static yt: $it")
            }
        }
    }
}

class TestYtReader(ytFactory: YtFactory) :
    AbstractYtTableReader<TestYtModel>(ytFactory, "//tmp/batalin/fp_test", TestYtModel::class)

@YTreeObject
data class TestYtModel(
    val requestId: String
)
