package ru.yandex.market.forecastint.config.yql

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.market.forecastint.config.yt.YtFactory
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest
import ru.yandex.yt.ytclient.proxy.YtClient
import ru.yandex.yt.ytclient.proxy.YtCluster
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import java.util.concurrent.CompletableFuture

@Configuration
open class YtConfig {
    @Bean
    open fun ytFactory(): YtFactory {
        return object : YtFactory {
            override fun getYt(cluster: YtCluster): Yt {
                return Mockito.mock(Yt::class.java)
            }

            private fun makeNode(msku: Long, categoryId: Long): YTreeMapNode {
                return YTree.mapBuilder()
                    .key("msku").value(msku)
                    .key("supplier_id").value(1L)
                    .key("category_id").value(categoryId)
                    .key("demand_shipped_items").value(1.0)
                    .key("demand_corrected_shipped_items").value(2.0)
                    .key("demand_gross_items").value(3.0)
                    .key("demand_corrected_gross_items").value(4.0)
                    .buildMap()
            }

            override fun <T> runWithClient(action: (YtClient) -> T): T {
                val mockedClient = Mockito.mock(YtClient::class.java)
                val rowset = Mockito.mock(UnversionedRowset::class.java)
                Mockito.`when`(rowset.yTreeRows)
                    .thenReturn(listOf(makeNode(1L, 1L), makeNode(2L, 4L)))
                Mockito.`when`(
                    mockedClient.selectRows(
                        ArgumentMatchers.any(
                            SelectRowsRequest::class.java
                        )
                    )
                )
                    .thenReturn(CompletableFuture.completedFuture(rowset))
                Mockito.`when`(mockedClient.existsNode(ArgumentMatchers.anyString()))
                    .thenReturn(CompletableFuture.completedFuture(true))
                Mockito.`when`(mockedClient.removeNode(ArgumentMatchers.anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null))
                Mockito.`when`(
                    mockedClient.createNode(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.anyMap()
                    )
                ).thenReturn(
                    CompletableFuture.completedFuture(GUID.create())
                )
                Mockito.`when`(mockedClient.startOperation(ArgumentMatchers.any())).thenReturn(
                    CompletableFuture.completedFuture(
                        GUID.create()
                    )
                )
                Mockito.`when`(mockedClient.getOperation(ArgumentMatchers.any()))
                    .thenReturn(
                        CompletableFuture.completedFuture(
                            YTree.mapBuilder()
                                .key("state")
                                .value("completed").buildMap()
                        )
                    )
                Mockito.`when`(mockedClient.alterTable(ArgumentMatchers.any()))
                    .thenReturn(CompletableFuture.completedFuture(null))
                Mockito.`when`(mockedClient.mountTable(ArgumentMatchers.anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null))
                Mockito.`when`(mockedClient.unmountTable(ArgumentMatchers.anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null))
                return action(mockedClient)
            }

            override fun <T> runWithClientChunked(
                joiner: (List<T>) -> T,
                actions: List<(YtClient) -> T>
            ): T {
                return runBlocking {
                    return@runBlocking joiner(actions.asFlow()
                        .map { action -> runWithClient(action) }
                        .toList())
                }
            }
        }
    }
}
