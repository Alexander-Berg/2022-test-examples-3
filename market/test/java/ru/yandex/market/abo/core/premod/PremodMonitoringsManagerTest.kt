package ru.yandex.market.abo.core.premod

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import ru.yandex.EmptyTestWithTransactionTemplate
import ru.yandex.market.abo.core.premod.model.PremodItem
import ru.yandex.market.abo.core.premod.model.PremodItemStatus
import ru.yandex.market.abo.core.premod.model.PremodItemStatus.NEWBORN
import ru.yandex.market.abo.core.premod.model.PremodItemType.MONITORINGS
import ru.yandex.market.abo.core.premod.model.PremodTicket
import ru.yandex.market.abo.core.quality_monitoring.startrek.model.MonitoringType
import ru.yandex.market.abo.core.quality_monitoring.startrek.model.MonitoringType.ANTI_FAKE
import ru.yandex.market.abo.core.quality_monitoring.startrek.model.MonitoringType.BROKEN_FEED
import ru.yandex.market.abo.core.quality_monitoring.startrek.model.MonitoringType.UNIQUE
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.YtIdxMonitoringManager
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.index.ShopIndexStateManager
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.model.MonitoringIndexType.WHITE
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.model.MonitoringValue
import ru.yandex.market.abo.core.storage.json.model.JsonEntityType
import ru.yandex.market.abo.core.storage.json.premod.monitoring.JsonMonitoringResultService
import ru.yandex.market.abo.core.yt.YtCluster.HAHN

/**
 * @author komarovns
 * @date 21.10.2019
 */
open class PremodMonitoringsManagerTest : EmptyTestWithTransactionTemplate() {
    private val premodManager: PremodManager = mock()
    private val jsonMonitoringResultService: JsonMonitoringResultService = mock()
    private val ytIdxMonitoringManager: YtIdxMonitoringManager = mock()
    private val ytIdxMonitoringPlaneshiftManager: YtIdxMonitoringManager = mock()
    private val shopIndexStateManager: ShopIndexStateManager = mock()
    private val premodTicketService: PremodTicketService = mock()
    private val premodItemService: PremodItemService = mock()

    private val premodMonitoringsManager = PremodMonitoringsManager(
        premodManager, jsonMonitoringResultService, ytIdxMonitoringManager, ytIdxMonitoringPlaneshiftManager,
        shopIndexStateManager, premodTicketService, premodItemService, transactionTemplate, mock()
    )

    @ParameterizedTest(name = "processPremodMonitoringsTest_{index}")
    @MethodSource("processPremodMonitoringsTestMethodSource")
    fun processPremodMonitoringsTest(status: PremodItemStatus,
                                     expectedMonitorings: Map<MonitoringType, MonitoringValue>,
                                     allMonitorings: Map<Long, Map<MonitoringType, MonitoringValue>>
    ) {
        val monitoringItem = mock<PremodItem> {
            on { id } doReturn MONITORING_ITEM_ID
            on { ticketId } doReturn TICKET_ID
            on { type } doReturn MONITORINGS
        }
        val ticket = mock<PremodTicket> {
            on { id } doReturn TICKET_ID
            on { shopId } doReturn SHOP_ID
        }
        ytIdxMonitoringPlaneshiftManager.stub {
            on { loadLastGenerationForAllMonitorings(any(), eq(WHITE)) } doReturn LAST_MIN_IDX_GENERATION
            on { loadMonitoringResultsByShop(any(), eq(WHITE), eq(LAST_MIN_IDX_GENERATION)) } doReturn allMonitorings
        }
        premodItemService.stub {
            on { loadPremodItemsByStatusAndType(NEWBORN, MONITORINGS) }.doReturn(listOf(monitoringItem), listOf())
            on { loadPremodItemByTicketIdAndType(TICKET_ID, MONITORINGS) } doReturn monitoringItem
        }
        premodTicketService.stub {
            on { loadTicketsByIds(listOf(TICKET_ID)) } doReturn listOf(ticket)
        }
        shopIndexStateManager.stub {
            on { loadIndexedShops(any(), any(), any()) } doReturn setOf()
            on { loadIndexedShops(eq(LAST_MIN_IDX_GENERATION), eq(HAHN), eq(true)) } doReturn setOf(SHOP_ID)
        }

        premodMonitoringsManager.processPremodMonitorings()

        verify(monitoringItem).status = status
        verify(premodManager).updatePremodItem(monitoringItem)
        verify(jsonMonitoringResultService).saveIfNeeded(
            eq(TICKET_ID),
            eq(JsonEntityType.PREMOD_MONITORING_RESULT),
            eq(expectedMonitorings)
        )
    }

    @Test
    fun processPremodMonitoringsTest_nullIdxGeneration() {
        ytIdxMonitoringPlaneshiftManager.stub {
            on { loadLastMonitoringGeneration(any(), eq(WHITE)) } doReturn null
        }
        premodMonitoringsManager.processPremodMonitorings()
        verify(premodItemService, never()).loadPremodItemsByStatusAndType(any(), any())
    }

    companion object {
        private const val LAST_MIN_IDX_GENERATION = "20191018_1752"
        private const val SHOP_ID: Long = 774
        private const val TICKET_ID: Long = 1
        private const val MONITORING_ITEM_ID: Long = 2

        @JvmStatic
        fun processPremodMonitoringsTestMethodSource() = listOf(
            Arguments.of(
                PremodItemStatus.NEW,
                hashMapOf(
                    ANTI_FAKE to MonitoringValue(SHOP_ID, 1, listOf(1L)),
                    UNIQUE to MonitoringValue(SHOP_ID, 2, listOf(1L, 2L))
                ),
                hashMapOf(
                    SHOP_ID to hashMapOf(
                        ANTI_FAKE to MonitoringValue(SHOP_ID, 1, listOf(1L)),
                        UNIQUE to MonitoringValue(SHOP_ID, 2, listOf(1L, 2L))
                    ),
                    SHOP_ID + 1 to hashMapOf(
                        ANTI_FAKE to MonitoringValue(SHOP_ID + 1, 1, listOf(1L)),
                        BROKEN_FEED to MonitoringValue(SHOP_ID + 1, 2, listOf(1L, 2L))
                    )
                )
            ),
            Arguments.of(
                PremodItemStatus.PASS, emptyMap<MonitoringType, MonitoringValue>(),
                hashMapOf(SHOP_ID + 1 to hashMapOf(
                    ANTI_FAKE to MonitoringValue(SHOP_ID + 1, 1, listOf(1L)))
                )
            )
        )
    }
}
