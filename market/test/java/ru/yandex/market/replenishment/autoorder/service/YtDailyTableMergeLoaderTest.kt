package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.cypress.Cypress
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl
import ru.yandex.inside.yt.kosher.transactions.Transaction
import ru.yandex.inside.yt.kosher.transactions.YtTransactions
import ru.yandex.market.replenishment.autoorder.service.yt.YtCluster
import ru.yandex.market.replenishment.autoorder.service.yt.YtDailyTableMergeService
import ru.yandex.market.replenishment.autoorder.service.yt.YtFactory
import ru.yandex.market.replenishment.autoorder.service.yt.loader.YtDailyTableMergeLoader
import ru.yandex.yt.ytclient.proxy.request.CreateNode
import java.time.Duration
import java.util.*

@RunWith(SpringRunner::class)
open class YtDailyTableMergeLoaderTest {

    companion object {
        const val PATH = "//home/market/testing/replenishment/autoorder/dictionaries"
    }

    @Test
    open fun testLoadError() {
        val ytFactory = MockedYtFactory("Error")
        val loader = YtDailyTableMergeLoader(YtDailyTableMergeService(ytFactory), PATH, YtCluster.HAHN)
        Assertions.assertThrows(IllegalStateException::class.java) { loader.load() }
    }

    @Test
    open fun testLoad() {
        val ytFactory = MockedYtFactory(null)
        val loader = YtDailyTableMergeLoader(YtDailyTableMergeService(ytFactory), PATH, YtCluster.HAHN)
        Assertions.assertDoesNotThrow { loader.load() }
    }

    private class MockedYtFactory(val error: String?) : YtFactory {

        override fun getYt(cluster: YtCluster): Yt {
            val yt = mock(Yt::class.java)

            val (transactions, guid) = mockTransactions()
            `when`(yt.transactions()).thenReturn(transactions)
            val cypress = mockCypress(guid)
            `when`(yt.cypress()).thenReturn(cypress)

            return yt
        }

        private fun mockTransactions(): Pair<YtTransactions, GUID> {
            val transactions = mock(YtTransactions::class.java)
            val transaction = mock(Transaction::class.java)
            val guid = GUID.create()
            `when`(transaction.id).thenReturn(guid)

            `when`(transactions.startAndGet(any(), anyBoolean(), any(Duration::class.java)))
                .thenReturn(transaction)

            doNothing().`when`(transactions).ping(any())
            doNothing().`when`(transactions).abort(any())

            return Pair(transactions, guid);
        }

        private fun mockCypress(guid: GUID): Cypress {
            val cypress = mock(Cypress::class.java)

            if (error == null) {
                `when`(cypress.list(any(YPath::class.java)))
                        .thenReturn(listOf(
                                YTreeStringNodeImpl("2022-01-20", null),
                                YTreeStringNodeImpl("2022-01-21", null)
                        ))
            } else {
                `when`(cypress.list(any(YPath::class.java))).thenThrow(NullPointerException("error"))
            }

            `when`(cypress.exists(eq(Optional.of(guid)), anyBoolean(), any())).thenReturn(true)

            val rowCount0 = YTreeEntityNodeImpl(
                    mapOf("row_count" to YTreeIntegerNodeImpl(false, 0L, null)))
            `when`(cypress.get(any(), anyBoolean(), argThat { n ->
                n?.name()?.endsWith("2022-01-20") ?: false
            }, eq(listOf("row_count")))).thenReturn(rowCount0)

            val rowCount1 = YTreeEntityNodeImpl(
                    mapOf("row_count" to YTreeIntegerNodeImpl(false, 1L, null)))
            `when`(cypress.get(any(), anyBoolean(), argThat { n ->
                n?.name()?.endsWith("2022-01-21") ?: false
            }, eq(listOf("row_count")))).thenReturn(rowCount1)

            val schemaAttr0 = YTreeListNodeImpl(null)
            schemaAttr0.add(YTreeStringNodeImpl("0", null))
            val schema0 = YTreeEntityNodeImpl(mapOf("schema" to schemaAttr0))
            `when`(cypress.get(any(), anyBoolean(), argThat { n ->
                n?.name()?.endsWith("2022-01-20") ?: false
            }, eq(listOf("schema")))).thenReturn(schema0)

            val schemaAttr1 = YTreeListNodeImpl(null)
            schemaAttr1.add(YTreeStringNodeImpl("1", null))
            val schema1 = YTreeEntityNodeImpl(mapOf("schema" to schemaAttr1))
            `when`(cypress.get(any(), anyBoolean(), argThat { n ->
                n?.name()?.endsWith("2022-01-21") ?: false
            }, eq(listOf("schema")))).thenReturn(schema1)

            doAnswer { Answer {
                invocation ->
                    val createNode = invocation.getArgument<CreateNode>(0)
                    if (createNode.path.name().endsWith("2022-01")) {
                        return@Answer
                    }
                    throw IllegalArgumentException("Wrong path")
            } }.`when`(cypress).create(any(CreateNode::class.java))
            doNothing().`when`(cypress).concatenate(any(), anyBoolean(), anyList(), any())
            doNothing().`when`(cypress).remove(eq(Optional.of(guid)), anyBoolean(), any())

            return cypress
        }
    }
}
