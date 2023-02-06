package ru.yandex.market.mbi.feed.processor.samovar.result

import Market.DataCamp.API.UpdateTask
import NZoraPb.Statuscodes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.common.util.application.EnvironmentType
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.util.ProtoTestUtil
import ru.yandex.market.core.campaign.model.CampaignType
import ru.yandex.market.logbroker.LogbrokerEventPublisher
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.environment.UnitedEnvironmentService
import ru.yandex.market.mbi.feed.processor.model.FeedProcessingStatus
import ru.yandex.market.mbi.feed.processor.model.FeedType
import ru.yandex.market.mbi.feed.processor.parsing.update.event.DataCampFeedUpdateLogbrokerEvent
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.DatacampParsingHistoryRecord
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.DatacampParsingHistoryRecord.Id
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.SamovarDownloadFeedStatus
import ru.yandex.market.mbi.feed.processor.samovar.feedInfoBuilder
import ru.yandex.market.mbi.feed.processor.samovar.itemBuilder
import ru.yandex.market.mbi.feed.processor.samovar.messageBatchBuilder
import ru.yandex.market.mbi.feed.processor.samovar.proxy.SamovarResultDataProxyEvent
import ru.yandex.market.mbi.feed.processor.samovar.status.SamovarFeedDownloadStatusRepository
import ru.yandex.market.mbi.feed.processor.samovar.status.SamovarReturnCode
import ru.yandex.market.mbi.feed.processor.samovar.validationFeedInfoBuilder
import ru.yandex.market.mbi.feed.processor.test.capture
import ru.yandex.market.yt.binding.YTBinder
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource
import ru.yandex.market.yt.samovar.SamovarContextOuterClass.FeedInfo
import ru.yandex.market.yt.samovar.SamovarContextOuterClass.SamovarContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Тесты для [SamovarResultDataProcessor].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = ["SamovarResultDataProcessorTest.before.csv"])
internal class SamovarResultDataProcessorTest : FunctionalTest() {

    @Autowired
    private lateinit var samovarResultDataProcessor: SamovarResultDataProcessor

    @Autowired
    private lateinit var samovarFeedDownloadStatusRepository: SamovarFeedDownloadStatusRepository

    @Autowired
    @Qualifier("dataCampMarketFeedUpdateLogbrokerService")
    private lateinit var dataCampFeedUpdateLogbrokerService: LogbrokerEventPublisher<DataCampFeedUpdateLogbrokerEvent>

    @Autowired
    private lateinit var samovarResultDataProxyLogbrokerService: LogbrokerEventPublisher<SamovarResultDataProxyEvent>

    @Autowired
    private lateinit var pushProcessor: SamovarPushProcessor

    @Autowired
    private lateinit var ytDatacampParsingHistoryClientProxySource: YtClientProxySource

    @Autowired
    private lateinit var ytDatacampParsingHistoryClientProxy: YtClientProxy

    @Autowired
    private lateinit var unitedEnvironmentService: UnitedEnvironmentService

    @BeforeEach
    fun mockTerminal() {
        doReturn(ytDatacampParsingHistoryClientProxy).`when`(ytDatacampParsingHistoryClientProxySource).currentClient
        doReturn(
            listOf<DatacampParsingHistoryRecord>()
        ).`when`(
            ytDatacampParsingHistoryClientProxy
        ).lookupRows(
            any(),
            eq(YTBinder.getBinder(Id::class.java)),
            any(),
            eq(YTBinder.getBinder(DatacampParsingHistoryRecord::class.java)),
        )
    }

    companion object {

        @JvmStatic
        private val NOT_EXISTED_CODE = 999999

        @JvmStatic
        fun provideHttpCodes() = listOf(
            Arguments.of(200, true, true), // на 2xx чистим базу и отправляем сообщение
            Arguments.of(302, false, true), // на 3xx чистим базу, но не отправляем сообщение
            Arguments.of(404, false, false), // на 4xx повышаем счетчик ошибок и не отправляем сообщение
            Arguments.of(501, false, false) // на 5xx повышаем счетчик ошибок и не отправляем сообщение
        )

        @JvmStatic
        fun provideZoraCodes() = listOf(
            Arguments.of(
                Statuscodes.EZoraStatus.ZS_OK_VALUE, Statuscodes.EFetchStatus.FS_MESSAGE_EOF_VALUE,
                true, false
            ),
            Arguments.of(
                Statuscodes.EZoraStatus.ZS_OK_VALUE, Statuscodes.EFetchStatus.FS_FETCHER_MB_ERROR_VALUE,
                false, true
            ), // проверяем что нормально обрабатывается фетч код, неизвестный для нас
            Arguments.of(Statuscodes.EZoraStatus.ZS_OK_VALUE, NOT_EXISTED_CODE, false, true),
            Arguments.of(
                Statuscodes.EZoraStatus.ZS_QUOTA_INTERNAL_ERROR_VALUE,
                Statuscodes.EFetchStatus.FS_OK_VALUE, false, true
            ),
            Arguments.of(
                Statuscodes.EZoraStatus.ZS_TIMED_OUT_VALUE,
                Statuscodes.EFetchStatus.FS_OK_VALUE, false, true
            ), // проверяем что нормально обрабатывается фетч код, неизвестный для нас
            Arguments.of(NOT_EXISTED_CODE, Statuscodes.EFetchStatus.FS_OK_VALUE, false, true)
        )
    }

    @Test
    fun `supplier with old price feed`() {
        val url = "http://ya.ru/utility_feed"
        val feedId: Long = 1002
        val shopId: Long = 2
        val feedInfo = feedInfoBuilder(
            url = url,
            feedId = feedId,
            shopId = shopId,
            businessId = 100,
            campaignType = CampaignType.SUPPLIER,
            feedType = FeedInfo.FeedType.PRICES
        )
        val original = itemBuilder(
            url = url,
            feedInfos = listOf(feedInfo),
            numberOfParts = 3,
            mdsKeys = "123/asdf|456/q|789/zxcvb"
        )
        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        val event = dataCampFeedUpdateLogbrokerService.capture()

        assertThat(event).hasSize(1)
        val task = event[0].payload
        val expected = ProtoTestUtil.getProtoMessageByJson(
            UpdateTask.FeedParsingTask::class.java,
            "proto/testOwnEnvBlue.json",
            javaClass
        )
        ProtoTestUtil.assertThat(task)
            .ignoringFieldsMatchingRegexes(".*timestamp.*", ".*memoizedSize.*")
            .isEqualTo(expected)

        Mockito.verify(ytDatacampParsingHistoryClientProxy, times(2))
            .insertRows(any(), any<YTBinder<DatacampParsingHistoryRecord>>(), any<List<DatacampParsingHistoryRecord>>())
    }

    @Test
    fun `merge service + business`() {
        unitedEnvironmentService.isGroupBusinessAndServiceInSamovarFeed = true
        val url = "http://ya.ru/utility_feed"
        val feedId: Long = 1002
        val businessFeedId: Long = 1003
        val shopId: Long = 2
        val original = itemBuilder(
            url = url,
            feedInfos = listOf(
                feedInfoBuilder(
                    url = url,
                    feedId = feedId,
                    shopId = shopId,
                    businessId = 100,
                    campaignType = CampaignType.SUPPLIER,
                    feedType = FeedInfo.FeedType.ASSORTMENT
                ),
                feedInfoBuilder(
                    url = url,
                    feedId = businessFeedId,
                    businessId = 100,
                    campaignType = CampaignType.BUSINESS,
                    feedType = FeedInfo.FeedType.ASSORTMENT
                )
            ),
            numberOfParts = 3,
            mdsKeys = "123/asdf|456/q|789/zxcvb"
        )
        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        val event = dataCampFeedUpdateLogbrokerService.capture()

        assertThat(event).hasSize(1)
        val task = event[0].payload
        val expected = ProtoTestUtil.getProtoMessageByJson(
            UpdateTask.FeedParsingTask::class.java,
            "proto/testMergeBusinessService.json",
            javaClass
        )
        ProtoTestUtil.assertThat(task)
            .ignoringFieldsMatchingRegexes(".*timestamp.*", ".*memoizedSize.*")
            .isEqualTo(expected)

        Mockito.verify(ytDatacampParsingHistoryClientProxy, times(2))
            .insertRows(any(), any<YTBinder<DatacampParsingHistoryRecord>>(), any<List<DatacampParsingHistoryRecord>>())
    }

    @Test
    fun `not merge service + service`() {
        unitedEnvironmentService.isGroupBusinessAndServiceInSamovarFeed = true
        val url = "http://ya.ru/utility_feed"
        val batch = messageBatchBuilder(
            items = listOf(
                itemBuilder(
                    url = url,
                    feedInfos = listOf(
                        feedInfoBuilder(
                            url = url,
                            feedId = 1002,
                            shopId = 2,
                            businessId = 100,
                            campaignType = CampaignType.SUPPLIER,
                            feedType = FeedInfo.FeedType.ASSORTMENT
                        ),
                        feedInfoBuilder(
                            url = url,
                            feedId = 1003,
                            shopId = 3,
                            businessId = 100,
                            campaignType = CampaignType.SUPPLIER,
                            feedType = FeedInfo.FeedType.ASSORTMENT
                        )
                    ),
                    numberOfParts = 3,
                    mdsKeys = "123/asdf|456/q|789/zxcvb"
                )
            )
        )

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        val event = dataCampFeedUpdateLogbrokerService.capture(times(2))
        assertThat(event).hasSize(2)

        Mockito.verify(ytDatacampParsingHistoryClientProxy, times(3))
            .insertRows(any(), any<YTBinder<DatacampParsingHistoryRecord>>(), any<List<DatacampParsingHistoryRecord>>())
    }

    @Test
    fun `white partner in same environemt`() {
        val feedInfo = feedInfoBuilder(
            feedId = 1001,
            shopId = 1,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo))

        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)

        dataCampFeedUpdateLogbrokerService.capture()
        Mockito.verify(ytDatacampParsingHistoryClientProxy, times(2))
            .insertRows(any(), any<YTBinder<DatacampParsingHistoryRecord>>(), any<List<DatacampParsingHistoryRecord>>())
    }

    @Test
    fun `white partner with real_feed_id`() {
        val feedInfo = feedInfoBuilder(
            feedId = 1001,
            shopId = 1,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo))

        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        val value = dataCampFeedUpdateLogbrokerService.capture().first()

        Mockito.verify(ytDatacampParsingHistoryClientProxy, times(2))
            .insertRows(any(), any<YTBinder<DatacampParsingHistoryRecord>>(), any<List<DatacampParsingHistoryRecord>>())

        val realFeedId: Int = value.payload.realFeedId
        assertThat(realFeedId).isEqualTo(feedInfo.feedId)
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessor.wrongUrl.before.csv"],
        after = ["SamovarResultDataProcessor.wrongUrl.before.csv"]
    )
    fun `Feed with outdated url from Samovar will not be processed`() {
        val feedInfo = feedInfoBuilder(
            feedId = 1001,
            shopId = 1,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo))

        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)

        Mockito.verifyNoMoreInteractions(dataCampFeedUpdateLogbrokerService, ytDatacampParsingHistoryClientProxy)
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessor.wrongUrl.before.csv"],
        after = ["SamovarResultDataProcessor.wrongUrl.before.csv"]
    )
    fun `Feed with actual url from Samovar will be processed`() {
        val feedInfo = feedInfoBuilder(
            feedId = 1001,
            shopId = 1,
            businessId = 134,
            campaignType = CampaignType.SHOP,
            url = "http://old_feed.ya"
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo))

        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        val value = dataCampFeedUpdateLogbrokerService.capture().first()

        Mockito.verify(ytDatacampParsingHistoryClientProxy, times(2))
            .insertRows(any(), any<YTBinder<DatacampParsingHistoryRecord>>(), any<List<DatacampParsingHistoryRecord>>())

        val realFeedId: Int = value.payload.realFeedId
        assertThat(realFeedId).isEqualTo(feedInfo.feedId)
    }

    @Test
    fun `message from Samovar is expired, skip it`() {
        val feedInfo = feedInfoBuilder(
            feedId = 1001,
            shopId = 1,
            campaignType = CampaignType.SHOP
        )
        val original = itemBuilder(
            feedInfos = listOf(feedInfo),
            lastAccess = Instant.now().minus(10, ChronoUnit.HOURS).epochSecond
        )

        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        Mockito.verifyNoMoreInteractions(dataCampFeedUpdateLogbrokerService)
    }

    @Test
    fun `message from another environment will be ignored`() {
        val feedInfo = feedInfoBuilder(
            businessId = 111L,
            feedId = 1100,
            shopId = 100,
            campaignType = CampaignType.SHOP
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo), environment = EnvironmentType.PRODUCTION)

        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        dataCampFeedUpdateLogbrokerService.capture()

        Mockito.verify(ytDatacampParsingHistoryClientProxy, times(2))
            .insertRows(any(), any<YTBinder<DatacampParsingHistoryRecord>>(), any<List<DatacampParsingHistoryRecord>>())
    }

    @Test
    fun `partner from different environment in single message`() {
        val feedInfo1 = feedInfoBuilder(
            feedId = 1100,
            shopId = 100,
            campaignType = CampaignType.SHOP,
            businessId = 111L
        )
        val feedInfo2 = feedInfoBuilder(
            feedId = 1200,
            shopId = 200,
            campaignType = CampaignType.SUPPLIER, // запрещенный тип партнера из другого окружения
            businessId = 222L
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo1, feedInfo2), environment = EnvironmentType.PRODUCTION)

        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        val values = dataCampFeedUpdateLogbrokerService.capture()
        assertThat(values)
            .hasSize(1)
            .first()
            .extracting { it.parsingTask.feedId }
            .isEqualTo(1100)

        Mockito.verify(ytDatacampParsingHistoryClientProxy, times(2))
            .insertRows(any(), any<YTBinder<DatacampParsingHistoryRecord>>(), any<List<DatacampParsingHistoryRecord>>())
    }

    @Test
    fun `message from antoher environment`() {
        val feedInfo1 = feedInfoBuilder(
            feedId = 1200,
            shopId = 200,
            campaignType = CampaignType.SUPPLIER // запрещенный тип партнера из другого окружения
        )
        val feedInfo2 = feedInfoBuilder(
            feedId = 1100,
            shopId = 100,
            campaignType = CampaignType.SHOP
        )
        val original1 = itemBuilder(feedInfos = listOf(feedInfo1), environment = EnvironmentType.PRODUCTION)
        val original2 = itemBuilder(feedInfos = listOf(feedInfo2), environment = EnvironmentType.TESTING)
        val batch = messageBatchBuilder(items = listOf(original1, original2))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        Mockito.verifyNoMoreInteractions(dataCampFeedUpdateLogbrokerService)
        Mockito.verifyNoMoreInteractions(ytDatacampParsingHistoryClientProxy)
    }

    @ParameterizedTest
    @MethodSource("provideHttpCodes")
    @DbUnitDataSet(before = ["SamovarPushProcessorTest.httpCodeProcessing.before.csv"])
    fun `process messages with different http codes`(code: Int, send: Boolean, clear: Boolean) {
        val feedId: Long = 1002
        val shopId: Long = 2
        val feedInfo = feedInfoBuilder(
            feedId = feedId,
            shopId = shopId,
            campaignType = CampaignType.SUPPLIER,
            businessId = 111L
        )
        val original1 = itemBuilder(
            feedInfos = listOf(feedInfo),
            numberOfParts = 3,
            mdsKeys = "123/asdf|456/q|789/zxcvb",
            httpCode = code
        )
        val batch = messageBatchBuilder(items = listOf(original1))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        if (send) {
            Mockito.verify(dataCampFeedUpdateLogbrokerService).publishEvent(ArgumentMatchers.any())
        } else {
            Mockito.verifyNoMoreInteractions(dataCampFeedUpdateLogbrokerService)
        }

        val samovarFeedDownloadError = samovarFeedDownloadStatusRepository.findByFeedId(feedId)!!
        val expectedExternalErrorsCount = if (clear) 0 else 5.toLong()
        val expectedInternalErrorsCount = if (clear) 0 else 2.toLong()
        assertThat(samovarFeedDownloadError.externalErrorsNumber).isEqualTo(expectedExternalErrorsCount)
        assertThat(samovarFeedDownloadError.internalErrorsNumber).isEqualTo(expectedInternalErrorsCount)
        if (code == 404) {
            Mockito.verify(ytDatacampParsingHistoryClientProxy)
                .insertRows(
                    any(),
                    any<YTBinder<DatacampParsingHistoryRecord>>(),
                    any<List<DatacampParsingHistoryRecord>>()
                )
        }
    }

    @ParameterizedTest
    @MethodSource("provideZoraCodes")
    @DbUnitDataSet(before = ["SamovarPushProcessorTest.httpCodeProcessing.before.csv"])
    fun `process messages with different zora codes`(
        zoraCode: Int,
        fetchCode: Int,
        incrementExternal: Boolean,
        incrementInternal: Boolean
    ) {
        val feedId: Long = 1002
        val shopId: Long = 2
        val feedInfo = feedInfoBuilder(
            feedId = feedId,
            shopId = shopId,
            campaignType = CampaignType.SUPPLIER
        )
        val original1 = itemBuilder(
            feedInfos = listOf(feedInfo),
            numberOfParts = 3,
            mdsKeys = "123/asdf|456/q|789/zxcvb",
            zoraCode = zoraCode,
            fetchCode = fetchCode
        )
        val batch = messageBatchBuilder(items = listOf(original1))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        Mockito.verifyNoMoreInteractions(dataCampFeedUpdateLogbrokerService)

        val samovarFeedDownloadError = samovarFeedDownloadStatusRepository.findByFeedId(feedId)!!
        val expectedExternalErrorsCount = if (incrementExternal) 5 else 4.toLong()
        val expectedInternalErrorsCount = if (incrementInternal) 3 else 2.toLong()
        assertThat(samovarFeedDownloadError.externalErrorsNumber).isEqualTo(expectedExternalErrorsCount)
        assertThat(samovarFeedDownloadError.internalErrorsNumber).isEqualTo(expectedInternalErrorsCount)
        if (zoraCode in arrayOf(
                Statuscodes.EZoraStatus.ZS_QUOTA_INTERNAL_ERROR_VALUE,
                Statuscodes.EZoraStatus.ZS_TIMED_OUT_VALUE
            ) ||
            fetchCode in arrayOf(
                    Statuscodes.EFetchStatus.FS_MESSAGE_EOF_VALUE,
                    Statuscodes.EFetchStatus.FS_FETCHER_MB_ERROR_VALUE
                )
        ) {
            Mockito.verify(ytDatacampParsingHistoryClientProxy)
                .insertRows(
                    any(),
                    any<YTBinder<DatacampParsingHistoryRecord>>(),
                    any<List<DatacampParsingHistoryRecord>>()
                )
        }
    }

    @Test
    fun `blue message from current environment`() {
        val feedId: Long = 1003
        val shopId: Long = 3
        val feedInfo = feedInfoBuilder(
            feedId = feedId,
            shopId = shopId,
            campaignType = CampaignType.SUPPLIER,
            warehouses = listOf(
                FeedInfo.WarehouseInfo.newBuilder().setFeedId(1003).setWarehouseId(10).setType("fulfillment").build()
            ),
            businessId = 111L
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo))
        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)
        val values = dataCampFeedUpdateLogbrokerService.capture()
        assertThat(values).hasSize(1)
        val payload1 = values[0].payload
        assertThat(payload1.feedId).isEqualTo(1003)
        assertThat(payload1.warehouseId).isEqualTo(0)
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessorTest.testFeedCache.before.csv"],
        after = ["SamovarResultDataProcessorTest.testFeedCache.after.csv"]
    )
    fun `feed hash is same, don't parse again`() {
        checkCache(200, forceRefresh = false, needParse = false)
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessorTest.testFeedOldCache.before.csv"],
        after = ["SamovarResultDataProcessorTest.testFeedCache.after.csv"]
    )
    fun `feed hash is defferent, sent to parse`() {
        checkCache(200, forceRefresh = false, needParse = true)
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessorTest.testNewFeedCache.before.csv"],
        after = ["SamovarResultDataProcessorTest.testFeedCache.after.csv"]
    )
    fun `new hash for 1 feed, other with old hash`() {
        checkCache(200, forceRefresh = false, needParse = true)
    }

    @Test
    @DbUnitDataSet(after = ["SamovarResultDataProcessorTest.testFeedCache.after.csv"])
    fun `new feed without hash in db`() {
        checkCache(200, forceRefresh = false, needParse = true)
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessorTest.testFeedCacheWithoutChecksum.before.csv"],
        after = ["SamovarResultDataProcessorTest.testFeedCache.after.csv"]
    )
    fun `old feed but without hash in db`() {
        checkCache(200, forceRefresh = false, needParse = true)
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessorTest.testFeedCache.before.csv"],
        after = ["SamovarResultDataProcessorTest.testFeedCache.after.csv"]
    )
    fun `send to parse even with same hash in case force update`() {
        checkCache(200, forceRefresh = true, needParse = true)
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessorTest.testFeedCacheNotModified.before.csv"],
        after = ["SamovarResultDataProcessorTest.testFeedCacheNotModified.after.csv"]
    )
    fun `304 not modified doesn't update hash in db`() {
        checkCache(304, forceRefresh = false, needParse = false)
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessorTest.testFeedCacheNotModified.before.csv"],
        after = ["SamovarResultDataProcessorTest.testFeedCacheServerError.after.csv"]
    )
    fun `download error don't update hash in db`() {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val nowUTC = now.atOffset(ZoneOffset.UTC)
        checkCache(500, messageTime = now, forceRefresh = false, needParse = false)
        val capturedRecords = ytDatacampParsingHistoryClientProxy.capture<DatacampParsingHistoryRecord>()
        assertThat(capturedRecords[0])
            .isEqualTo(
                DatacampParsingHistoryRecord(
                    id = Id(
                        businessId = 100,
                        partnerId = 1003,
                        feedId = 3001,
                        feedType = FeedType.ASSORTMENT_FEED,
                        updateTime = now,
                        parsingTaskId = 1
                    ),
                    isUpload = false,
                    fileName = "feed1",
                    originalUrl = "http://ya.ru/feed1",
                    samovarStatus = SamovarDownloadFeedStatus.ERROR,
                    samovarReturnCode = SamovarReturnCode(500, 1, 0),
                    status = FeedProcessingStatus.COULD_NOT_DOWNLOAD,
                )
            )
        assertThat(capturedRecords[1])
            .isEqualTo(
                DatacampParsingHistoryRecord(
                    id = Id(
                        businessId = 100,
                        partnerId = 1004,
                        feedId = 4001,
                        feedType = FeedType.ASSORTMENT_FEED,
                        updateTime = now,
                        parsingTaskId = 2
                    ),
                    isUpload = false,
                    fileName = "feed1",
                    originalUrl = "http://ya.ru/feed1",
                    samovarStatus = SamovarDownloadFeedStatus.ERROR,
                    samovarReturnCode = SamovarReturnCode(500, 1, 0),
                    status = FeedProcessingStatus.COULD_NOT_DOWNLOAD,
                )
            )
    }

    private fun checkCache(
        httpCode: Int,
        messageTime: Instant = Instant.now(),
        forceRefresh: Boolean,
        needParse: Boolean,
        forcedPeriodMinutes: Long? = null,
        expectedUpdatedFeedIds: Set<Int> = setOf(3001, 4001)
    ) {
        val feed3001 = feedInfoBuilder(
            feedId = 3001,
            shopId = 1003,
            businessId = 100,
            campaignType = CampaignType.SHOP,
            url = "http://ya.ru/feed1",
            forcedPeriodMinutes = forcedPeriodMinutes
        )
        val feed4001 = feedInfoBuilder(
            feedId = 4001,
            shopId = 1004,
            businessId = 100,
            campaignType = CampaignType.SHOP,
            url = "http://ya.ru/feed1",
            forcedPeriodMinutes = forcedPeriodMinutes
        )
        val originalBuilder = itemBuilder(
            feedInfos = listOf(feed3001, feed4001),
            zoraCode = 1,
            fetchCode = 0,
            httpCode = httpCode,
            crc32 = 101010,
            forceRefresh = if (forceRefresh) 1 else null,
            lastAccess = messageTime.epochSecond
        )
        val batch = messageBatchBuilder(items = listOf(originalBuilder))

        samovarResultDataProcessor.process(batch)
        if (!needParse) {
            Mockito.verifyNoMoreInteractions(dataCampFeedUpdateLogbrokerService)
            return
        }

        val values = dataCampFeedUpdateLogbrokerService.capture(times(expectedUpdatedFeedIds.size))
        val actualFeedIds = values
            .map(DataCampFeedUpdateLogbrokerEvent::getPayload)
            .map { it.feedId }
            .toSet()
        assertThat(actualFeedIds).isEqualTo(expectedUpdatedFeedIds)
    }

    @Test
    fun `migration flag enabled, white list`() {
        checkMigrationFlag(mapOf(1L to true, 999L to false))
    }

    @Test
    @DbUnitDataSet(before = ["SamovarResultDataProcessorTest.migration.enabled.before.csv"])
    fun `migration flag enabled for all`() {
        checkMigrationFlag(mapOf(1L to true, 999L to true))
    }

    private fun checkMigrationFlag(expected: Map<Long, Boolean>) {
        val feedInfo1 = feedInfoBuilder(
            feedId = 1001,
            shopId = 1,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val feedInfo2 = feedInfoBuilder(
            feedId = 1002,
            shopId = 999,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo1, feedInfo2))
        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)

        val forParsing = expected.filter { it.value }
        val dataCampEvents = dataCampFeedUpdateLogbrokerService.capture(times(forParsing.size))
        assertThat(dataCampEvents.map { it.payload.shopId.toLong() })
            .containsExactlyInAnyOrderElementsOf(forParsing.keys.toMutableList())

        val samovarProxyEvents = samovarResultDataProxyLogbrokerService.capture()
        assertThat(samovarProxyEvents.size).isEqualTo(1)
        val proxyData = SamovarContext.parseFrom(samovarProxyEvents[0].payload.context).feedsList
            .associate { it.shopId to it.processedInFeedProcessor }
        assertThat(proxyData)
            .isEqualTo(expected)
    }

    @Test
    fun `migration flag enabled, message consist of validation and feed`() {
        val validation = validationFeedInfoBuilder(
            validationFeedId = 100L
        )
        val feedInfo = feedInfoBuilder(
            feedId = 1001,
            shopId = 1,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo), validations = listOf(validation))
        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)

        val dataCampEvents = dataCampFeedUpdateLogbrokerService.capture()
        assertThat(dataCampEvents.size).isEqualTo(1)
        assertThat(dataCampEvents[0].payload.shopId).isEqualTo(1)

        val samovarProxyEvents = samovarResultDataProxyLogbrokerService.capture()
        assertThat(samovarProxyEvents.size).isEqualTo(1)
        val actualContext = SamovarContext.parseFrom(samovarProxyEvents[0].payload.context)

        val proxyFeeds = actualContext.feedsList.associate { it.shopId to it.processedInFeedProcessor }
        assertThat(proxyFeeds).isEqualTo(mapOf(1L to true))
        val proxyValidations = actualContext.validationFeedsList.map { it.validationId }
        assertThat(proxyValidations).containsExactlyInAnyOrder(100L)
    }

    @Test
    fun `migration flag enabled, message consist of validation only`() {
        val validation = validationFeedInfoBuilder(
            validationFeedId = 100L
        )
        val original = itemBuilder(validations = listOf(validation))
        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)

        val samovarProxyEvents = samovarResultDataProxyLogbrokerService.capture()
        assertThat(samovarProxyEvents.size).isEqualTo(1)
        val actualContext = SamovarContext.parseFrom(samovarProxyEvents[0].payload.context)

        val proxyValidations = actualContext.validationFeedsList.map { it.validationId }
        assertThat(proxyValidations).containsExactlyInAnyOrder(100L)
    }

    @Test
    @DbUnitDataSet(before = ["SamovarResultDataProcessorTest.testTooOftenFilter.before.csv"])
    fun `often messages for one feed will be ignored`() {
        val original = itemBuilder(
            feedInfos = listOf(
                feedInfoBuilder(feedId = 1002, shopId = 1, businessId = 100L),
                feedInfoBuilder(feedId = 1001, shopId = 1, businessId = 100L)
            )
        )
        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        samovarResultDataProcessor.process(batch)

        // Проверяем, что на парсинг отправился только фид 1001.
        val actual = dataCampFeedUpdateLogbrokerService.capture()
        assertThat(actual)
            .hasSize(1)
            .first()
            .extracting { it.parsingTask.feedId }
            .isEqualTo(1001)
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessorTest.testForcedReparse.before.csv"],
        after = ["SamovarResultDataProcessorTest.testForcedReparse.after.csv"]
    )
    fun `feed hash is same, but feeds are reparsed because of forcedReparseInterval`() {
        checkCache(
            200,
            forceRefresh = false,
            needParse = true,
            forcedPeriodMinutes = 1440,
            expectedUpdatedFeedIds = setOf(3001)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["SamovarResultDataProcessorTest.testForcedReparseIsNotTimedOut.before.csv"],
        after = ["SamovarResultDataProcessorTest.testForcedReparseIsNotTimedOut.after.csv"]
    )
    fun `feed hash is same, forcedReparseInterval is not timed out`() {
        checkCache(200, forceRefresh = false, needParse = false, forcedPeriodMinutes = 1440)
    }
}
