package ru.yandex.market.mbi.feed.processor.parsing.update

import Market.DataCamp.API.UpdateTask
import Market.DataCamp.API.UpdateTask.FeedParsingTask
import Market.DataCamp.DataCampOfferMeta
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.common.test.util.ProtoTestUtil
import ru.yandex.market.core.delivery.DeliveryServiceType
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType
import ru.yandex.market.logbroker.LogbrokerEventPublisher
import ru.yandex.market.logbroker.LogbrokerInteractionException
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.environment.EnvironmentService
import ru.yandex.market.mbi.feed.processor.model.FeedType
import ru.yandex.market.mbi.feed.processor.parsing.result.SendingFeedEventToLogbrokerException
import ru.yandex.market.mbi.feed.processor.parsing.update.event.DataCampFeedUpdateLogbrokerEvent
import ru.yandex.market.mbi.feed.processor.parsing.update.event.PartnerParsingFeedInternalEvent
import ru.yandex.market.mbi.feed.processor.parsing.update.event.PartnerWarehouse
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.DatacampParsingHistoryRecord
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.DatacampParsingHistoryRecord.Id
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.ParsingType
import ru.yandex.market.mbi.feed.processor.test.capture
import ru.yandex.market.mbi.feed.processor.test.getProto
import ru.yandex.market.mbi.feed.processor.test.toInstant
import ru.yandex.market.yt.binding.YTBinder
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource

/**
 * Тесты для [DataCampFeedUpdateEventService].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class DataCampFeedUpdateEventServiceTest : FunctionalTest() {

    @Autowired
    private lateinit var dataCampFeedUpdateEventService: DataCampFeedUpdateEventService

    @Autowired
    @Qualifier("dataCampFeedUpdateLogbrokerService")
    private lateinit var logbrokerService: LogbrokerEventPublisher<DataCampFeedUpdateLogbrokerEvent>

    @Autowired
    @Qualifier("dataCampMarketFeedUpdateLogbrokerService")
    private lateinit var logbrokerMarketService: LogbrokerEventPublisher<DataCampFeedUpdateLogbrokerEvent>

    @Autowired
    private lateinit var ytFeedParsingClientProxy: YtClientProxy

    @Autowired
    private lateinit var ytDatacampParsingHistoryClientProxy: YtClientProxy

    @Autowired
    private lateinit var ytDatacampParsingHistoryClientProxySource: YtClientProxySource

    private var ytDatacampParsingHistoryClientProxySourceClient: YtClientProxy = mock { }

    @Autowired
    private lateinit var environmentService: EnvironmentService

    @BeforeEach
    fun mockTerminal() {
        prepareMocksD()
    }

    @Test
    fun `send Business feed`() {
        val event = getBusinessEvent()
        dataCampFeedUpdateEventService.sendEvent(event)

        val actual = logbrokerMarketService.capture()
        val actualTask: FeedParsingTask = actual.single().payload

        val expectedTask = getProto<FeedParsingTask>("businessFeed.json")
        ProtoTestUtil.assertThat(actualTask)
            .isEqualTo(expectedTask)

        val historyRecords = ytDatacampParsingHistoryClientProxy.capture<DatacampParsingHistoryRecord>()
        val expectedHistory = getCommonHistory().copy(
            id = Id(
                businessId = 33L,
                partnerId = 33L,
                feedId = 34L,
                feedType = FeedType.ASSORTMENT_FEED,
                updateTime = toInstant(2020, 1, 1),
                parsingTaskId = 1L
            ),
        )
        Assertions.assertThat(historyRecords.single())
            .isEqualTo(expectedHistory)
    }

    @Test
    fun `send Business with parsingFields`() {
        val event = getBusinessEvent()
            .copy(parsingFields = listOf("field-1", "field-2"))
        dataCampFeedUpdateEventService.sendEvent(event)

        val actual = logbrokerMarketService.capture()
        val actualTask = actual.single().payload

        val expectedTask = getProto<FeedParsingTask>("businessFeed.json")
            .toBuilder().addAllParsingFields(listOf("field-1", "field-2")).build()
        ProtoTestUtil.assertThat(actualTask)
            .isEqualTo(expectedTask)

        val historyRecords = ytDatacampParsingHistoryClientProxy.capture<DatacampParsingHistoryRecord>()
        val expectedHistory = getCommonHistory().copy(
            id = Id(
                businessId = 33L,
                partnerId = 33L,
                feedId = 34L,
                feedType = FeedType.ASSORTMENT_FEED,
                updateTime = toInstant(2020, 1, 1),
                parsingTaskId = 1L
            ),
            parsingFields = listOf("field-1", "field-2")
        )
        Assertions.assertThat(historyRecords.single())
            .isEqualTo(expectedHistory)
    }

    @Test
    fun `send Direct feed`() {
        val event = getDirectEvent()
        dataCampFeedUpdateEventService.sendEvent(event)

        val actual = logbrokerService.capture()
        val actualTask: FeedParsingTask = actual.single().payload

        val expectedTask = getProto<FeedParsingTask>("directFeed1.json")
        ProtoTestUtil.assertThat(actualTask)
            .isEqualTo(expectedTask)

        val historyRecords = ytDatacampParsingHistoryClientProxy.capture<DatacampParsingHistoryRecord>()
        val expectedHistory = getCommonHistory()
        Assertions.assertThat(historyRecords.single())
            .isEqualTo(expectedHistory)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parsingTypesData")
    fun `send parsing event with correct parsing type`(
        name: String,
        feedType: FeedType,
        parsingType: ParsingType,
        expectedParsingType: UpdateTask.FeedClass
    ) {
        val event = getCommonEvent().copy(
            feedParsingType = parsingType,
            feedType = feedType,
            placementProgram = listOf(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture()
        Assertions.assertThat(actual.single().payload.type)
            .isEqualTo(expectedParsingType)
    }

    @Test
    fun `stock feed with warehouse in event will be send without warehouse mappings for dropship partner`() {
        val event = getCommonEvent().copy(
            feedParsingType = ParsingType.COMPLETE,
            feedType = FeedType.STOCK_FEED,
            placementProgram = listOf(PartnerPlacementProgramType.DROPSHIP),
            feedWarehouseMappings = listOf(
                PartnerWarehouse(
                    feedId = 100L,
                    serviceId = 300L,
                    externalWarehouseId = "ext_id",
                    DeliveryServiceType.DROPSHIP
                )
            )
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture()

        ProtoTestUtil.assertThat(actual.single().payload)
            .isEqualTo(getProto<FeedParsingTask>("dropshipStockFeed.json"))

        actual.single().payload.type
    }

    @Test
    fun `price feed with warehouse in event will be send with warehouse mappings for dropship partner`() {
        val event = getCommonEvent().copy(
            feedParsingType = ParsingType.COMPLETE,
            feedType = FeedType.PRICES_FEED,
            placementProgram = listOf(PartnerPlacementProgramType.DROPSHIP),
            feedWarehouseMappings = listOf(
                PartnerWarehouse(
                    feedId = 100L,
                    serviceId = 300L,
                    externalWarehouseId = "ext_id",
                    DeliveryServiceType.DROPSHIP
                )
            )
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture()

        ProtoTestUtil.assertThat(actual.single().payload)
            .isEqualTo(getProto<FeedParsingTask>("dropshipPriceFeed.json"))

        actual.single().payload.type
    }

    @Test
    fun `price feed with url should save correct name`() {
        val event = getCommonEvent().copy(
            isUpload = false,
            originalUrl = "http://somewhere.ru/yandex.xml",
            originalFileName = null
        )
        dataCampFeedUpdateEventService.sendEvent(event)

        val historyRecords = ytDatacampParsingHistoryClientProxy.capture<DatacampParsingHistoryRecord>()

        Assertions.assertThat(historyRecords).hasSize(1)
        Assertions.assertThat(historyRecords.first().fileName).isEqualTo("yandex.xml")
    }

    @Test
    fun `warehouse mapping without external id`() {
        val event = getCommonEvent().copy(
            feedParsingType = ParsingType.COMPLETE,
            feedType = FeedType.PRICES_FEED,
            placementProgram = listOf(PartnerPlacementProgramType.DROPSHIP),
            feedWarehouseMappings = listOf(
                PartnerWarehouse(
                    feedId = 100L,
                    serviceId = 300L,
                    externalWarehouseId = null,
                    DeliveryServiceType.DROPSHIP
                )
            )
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture()

        ProtoTestUtil.assertThat(actual.single().payload)
            .isEqualTo(getProto<FeedParsingTask>("mappingWithoutExternalId.json"))

        actual.single().payload.type
    }

    @Test
    fun `stock feed with warehouse in event will be send without warehouse mappings for dbs partner`() {
        val event = getCommonEvent().copy(
            feedParsingType = ParsingType.COMPLETE,
            feedType = FeedType.STOCK_FEED,
            placementProgram = listOf(PartnerPlacementProgramType.DROPSHIP_BY_SELLER),
            feedWarehouseMappings = listOf(
                PartnerWarehouse(
                    feedId = 100L,
                    serviceId = 300L,
                    externalWarehouseId = "ext_id",
                    DeliveryServiceType.DROPSHIP_BY_SELLER
                )
            )
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture()

        ProtoTestUtil.assertThat(actual.single().payload)
            .isEqualTo(getProto<FeedParsingTask>("dbsStockFeed.json"))

        actual.single().payload.type
    }

    @Test
    fun `multiple warehouses expand to multiple messages`() {
        val event = getCommonEvent().copy(
            feedParsingType = ParsingType.COMPLETE,
            feedType = FeedType.PRICES_FEED,
            placementProgram = listOf(PartnerPlacementProgramType.DROPSHIP),
            feedWarehouseMappings = listOf(
                PartnerWarehouse(
                    feedId = 100L,
                    serviceId = 300L,
                    externalWarehouseId = "ext_id_1",
                    DeliveryServiceType.DROPSHIP
                ),
                PartnerWarehouse(
                    feedId = 200L,
                    serviceId = 400L,
                    externalWarehouseId = "ext_id_2",
                    DeliveryServiceType.DROPSHIP
                )
            )
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture(times(2))

        val actualEvents = actual.associate { it.payload.feedId to it.payload }

        ProtoTestUtil.assertThat(actualEvents[100])
            .isEqualTo(getProto<FeedParsingTask>("multipleWarehouses1.json"))
        ProtoTestUtil.assertThat(actualEvents[200])
            .isEqualTo(getProto<FeedParsingTask>("multipleWarehouses2.json"))

        // 3 записи в истории: 2 для складских фидов, 1 для настоящего
        val historyRecords = ytDatacampParsingHistoryClientProxy.capture<DatacampParsingHistoryRecord>(mode = times(3))
        Assertions.assertThat(historyRecords[0])
            .isEqualTo(
                getCommonHistory().copy(
                    id = getCommonHistory().id.copy(
                        feedType = FeedType.PRICES_FEED,
                        feedId = 100L
                    )
                )
            )
        Assertions.assertThat(historyRecords[1])
            .isEqualTo(
                getCommonHistory().copy(
                    id = getCommonHistory().id.copy(
                        feedType = FeedType.PRICES_FEED,
                        feedId = 200L
                    )
                )
            )
        Assertions.assertThat(historyRecords[2])
            .isEqualTo(
                getCommonHistory().copy(
                    id = getCommonHistory().id.copy(
                        feedType = FeedType.PRICES_FEED,
                        feedId = 34L
                    ),
                    lbStatusCode = null,
                    lbError = null
                )
            )
    }

    @Test
    fun `ff warehouse will be ignored in warehouses list`() {
        val event = getCommonEvent().copy(
            feedParsingType = ParsingType.COMPLETE,
            feedType = FeedType.PRICES_FEED,
            placementProgram = listOf(PartnerPlacementProgramType.CROSSDOCK),
            feedWarehouseMappings = listOf(
                PartnerWarehouse(
                    feedId = 100L,
                    serviceId = 300L,
                    externalWarehouseId = "ext_id_1",
                    DeliveryServiceType.CROSSDOCK
                ),
                PartnerWarehouse(
                    feedId = 200L,
                    serviceId = 400L,
                    externalWarehouseId = "ext_id_2",
                    DeliveryServiceType.FULFILLMENT
                )
            )
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture()

        ProtoTestUtil.assertThat(actual.single().payload)
            .isEqualTo(getProto<FeedParsingTask>("ffInWarehousesList.json"))
    }

    @Test
    fun `ff warehouse only`() {
        val event = getCommonEvent().copy(
            feedParsingType = ParsingType.COMPLETE,
            feedType = FeedType.PRICES_FEED,
            placementProgram = listOf(PartnerPlacementProgramType.CROSSDOCK),
            feedWarehouseMappings = listOf(
                PartnerWarehouse(
                    feedId = 100L,
                    serviceId = 300L,
                    externalWarehouseId = "ext_id_1",
                    DeliveryServiceType.FULFILLMENT
                ),
                PartnerWarehouse(
                    feedId = 200L,
                    serviceId = 400L,
                    externalWarehouseId = "ext_id_2",
                    DeliveryServiceType.FULFILLMENT
                )
            )
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture()

        ProtoTestUtil.assertThat(actual.single().payload)
            .isEqualTo(getProto<FeedParsingTask>("ffOnlyInWarehousesList.json"))
    }

    @Test
    fun `supplier without warehouses`() {
        val event = getCommonEvent().copy(
            feedParsingType = ParsingType.COMPLETE,
            feedType = FeedType.PRICES_FEED,
            placementProgram = listOf(PartnerPlacementProgramType.CROSSDOCK),
            feedWarehouseMappings = listOf()
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture()

        ProtoTestUtil.assertThat(actual.single().payload)
            .isEqualTo(getProto<FeedParsingTask>("supplierWithoutWarehouses.json"))
    }

    @Test
    fun `supplier without warehouses_service_mode`() {
        environmentService.set("use.service.parsing.mode", "true")
        val event = getCommonEvent().copy(
            feedParsingType = ParsingType.COMPLETE,
            feedType = FeedType.PRICES_FEED,
            placementProgram = listOf(PartnerPlacementProgramType.DROPSHIP),
            feedWarehouseMappings = listOf()
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture()

        ProtoTestUtil.assertThat(actual.single().payload)
            .isEqualTo(getProto<FeedParsingTask>("supplierWithoutWarehousesServiceMode.json"))
    }

    @Test
    fun `dsbs without warehouses_service_mode`() {
        environmentService.set("use.service.parsing.mode", "true")
        val event = getCommonEvent().copy(
            feedParsingType = ParsingType.COMPLETE,
            feedType = FeedType.PRICES_FEED,
            placementProgram = listOf(PartnerPlacementProgramType.DROPSHIP_BY_SELLER),
            feedWarehouseMappings = listOf()
        )
        dataCampFeedUpdateEventService.sendEvent(event)
        val actual = logbrokerService.capture()

        ProtoTestUtil.assertThat(actual.single().payload)
            .isEqualTo(getProto<FeedParsingTask>("dsbsServiceMode.json"))
    }

    @Test
    fun `logbroker error`() {
        whenever(logbrokerService.publishEvent(any())).doThrow(LogbrokerInteractionException("ERROR!"))
        Assertions.assertThatThrownBy { dataCampFeedUpdateEventService.sendEvent(getDirectEvent()) }
            .isExactlyInstanceOf(SendingFeedEventToLogbrokerException::class.java)
            .hasMessageContaining("Failed to send parsing task to logbroker")
    }

    private fun getDirectEvent() = getCommonEvent().copy(
        shopsDatParameters = UpdateTask.ShopsDatParameters.newBuilder()
            .setColor(DataCampOfferMeta.MarketColor.DIRECT)
            .setDirectFeedId(26666L)
            .setClientId(15555L)
            .setVerticalShare(true)
            .setDirectStandby(true)
            .setDirectSearchSnippetGallery(true)
            .setDirectGoodsAds(true)
            .build()
    )

    private fun getBusinessEvent() = getCommonEvent().copy(
        shopsDatParameters = UpdateTask.ShopsDatParameters.newBuilder()
            .setIsUpload(false)
            .build(),
        partnerId = 33L
    )

    private fun getCommonEvent() = PartnerParsingFeedInternalEvent(
        sendTime = toInstant(2020, 1, 1),
        businessId = 33L,
        partnerId = 31L,
        placementProgram = emptyList(),
        feedId = 34L,
        isUpload = true,
        originalUrl = "http://url.remote",
        uploadUrl = "http://url.local",
        parts = listOf("http://url.local"),
        feedParsingType = ParsingType.COMPLETE,
        isRegularParsing = false,
        feedType = FeedType.ASSORTMENT_FEED,
        isForceRefresh = false,
        requestId = null,
        feedWarehouseMappings = emptyList(),
        shopsDatParameters = null,
        originalFileName = "original.xlsx",
        isPartnerInterface = true,
        warehouseGroup = listOf()
    )

    private fun getCommonHistory() = DatacampParsingHistoryRecord(
        id = Id(
            businessId = 33L,
            partnerId = 31L,
            feedId = 34L,
            feedType = FeedType.ASSORTMENT_FEED,
            updateTime = toInstant(2020, 1, 1),
            parsingTaskId = 1L
        ),
        fileName = "original.xlsx",
        originalUrl = "http://url.remote",
        mdsUrl = "http://url.local",
        parsingType = ParsingType.COMPLETE,
        lbStatusCode = 200,
        isUpload = true
    )

    companion object {
        @JvmStatic
        fun parsingTypesData() = listOf(
            Arguments.of(
                "ASSORTMENT + COMPLETE = FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE",
                FeedType.ASSORTMENT_FEED,
                ParsingType.COMPLETE,
                UpdateTask.FeedClass.FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
            ),
            Arguments.of(
                "ASSORTMENT + UPDATE = FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE",
                FeedType.ASSORTMENT_FEED,
                ParsingType.UPDATE,
                UpdateTask.FeedClass.FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE
            ),
            Arguments.of(
                "PRICES_FEED + COMPLETE = FEED_CLASS_COMPLETE",
                FeedType.PRICES_FEED,
                ParsingType.COMPLETE,
                UpdateTask.FeedClass.FEED_CLASS_COMPLETE
            ),
            Arguments.of(
                "PRICES_FEED + UPDATE = FEED_CLASS_UPDATE",
                FeedType.PRICES_FEED,
                ParsingType.UPDATE,
                UpdateTask.FeedClass.FEED_CLASS_UPDATE
            ),
            Arguments.of(
                "STOCK_FEED + COMPLETE = FEED_CLASS_STOCK",
                FeedType.STOCK_FEED,
                ParsingType.COMPLETE,
                UpdateTask.FeedClass.FEED_CLASS_STOCK
            ),
            Arguments.of(
                "STOCK_FEED + UPDATE = FEED_CLASS_STOCK",
                FeedType.STOCK_FEED,
                ParsingType.UPDATE,
                UpdateTask.FeedClass.FEED_CLASS_STOCK
            ),
        )
    }

    private fun prepareMocksD() {
        doReturn(ytDatacampParsingHistoryClientProxySourceClient).`when`(ytDatacampParsingHistoryClientProxySource).currentClient
        doReturn(
            listOf<DatacampParsingHistoryRecord>()
        ).`when`(
            ytDatacampParsingHistoryClientProxySourceClient
        ).lookupRows(
            any(),
            eq(YTBinder.getBinder(Id::class.java)),
            any(),
            eq(YTBinder.getBinder(DatacampParsingHistoryRecord::class.java)),
        )
    }
}
