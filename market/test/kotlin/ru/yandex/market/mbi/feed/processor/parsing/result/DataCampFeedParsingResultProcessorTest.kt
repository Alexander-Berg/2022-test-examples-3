package ru.yandex.market.mbi.feed.processor.parsing.result

import Market.DataCamp.API.GeneralizedMessageOuterClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.core.indexer.model.ReturnCode
import ru.yandex.market.logbroker.LogbrokerEventPublisher
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.model.FeedType
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.DatacampParsingHistoryRecord
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.FeedParsingResult
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.ParsingType
import ru.yandex.market.mbi.feed.processor.test.capture
import ru.yandex.market.mbi.feed.processor.test.getProto
import ru.yandex.market.yt.binding.YTBinder
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource
import java.time.Instant

/**
 * Тесты для [DataCampFeedParsingResultProcessor].
 */
@DbUnitDataSet(before = ["DataCampFeedParsingResultProcessorTest.before.csv"])
internal class DataCampFeedParsingResultProcessorTest : FunctionalTest() {

    @Autowired
    private lateinit var dataCampFeedParsingResultProcessor: DataCampFeedParsingResultProcessor

    @Autowired
    private lateinit var mbiFeedUpdateNotificationLogbrokerService: LogbrokerEventPublisher<FeedParsingResultEventNotification>

    @Autowired
    private lateinit var ytDatacampParsingHistoryClientProxySource: YtClientProxySource

    @Autowired
    private lateinit var ytDatacampParsingHistoryClientProxy: YtClientProxy

    private var ytDatacampParsingHistoryClientProxySourceClient: YtClientProxy = mock { }

    @BeforeEach
    internal fun setUp() {
        doReturn(ytDatacampParsingHistoryClientProxySourceClient).`when`(ytDatacampParsingHistoryClientProxySource).currentClient
    }

    @Test
    fun `proxy parsing result to lb topic feed-parsing-result-events`() {
        mockDatacampParsingHistory()
        process("parseResult.json")
        val values = mbiFeedUpdateNotificationLogbrokerService.capture()
        assertThat(values).hasSize(1).first().extracting { it.payload.partnerId }.isEqualTo(1002L)
    }

    @Test
    @DbUnitDataSet(after = ["DataCampFeedParsingResultProcessorTest.realFeed.after.csv"])
    fun `save parsing result for real feed`() {
        mockDatacampParsingHistory()
        process("parsingResultForRealFeed.proto.json")
        val ytHistoryResult = ytDatacampParsingHistoryClientProxy.capture<DatacampParsingHistoryRecord>()
        assertThat(ytHistoryResult.first().offerStats)
            .isEqualTo(
                DatacampParsingHistoryRecord.OfferStatistics(
                    totalOffers = 5,
                    errorOffers = 4,
                    warningOffers = 3,
                    unloadedOffers = 2,
                    loadedOffers = 1
                )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.realFeed.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.realFeed.after.csv"]
    )
    fun `save parsing result for virtual feed`() {
        // receivedTime = Instant.EPOCH, а не null,
        // потому что: https://st.yandex-team.ru/MBI-73970#61b8bf5e3bfd4c7fe5c4e3bd
        val ids = listOf(2005L, 1005L)
        mockDatacampParsingHistory(recordsNumber = 2) {
            copy(
                id = id.copy(feedId = ids[it]),
                receivedTime = Instant.EPOCH
            )
        }
        process("parsingResultForVirtualFeed.proto.json")

        val ytHistoryResult = ytDatacampParsingHistoryClientProxy.capture<DatacampParsingHistoryRecord>()
        val updatedFeedIds = ytHistoryResult.map { it.id.feedId }.toList()
        assertThat(updatedFeedIds)
            .containsExactlyInAnyOrder(1005L)
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.unifiedServiceBusiness.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.unifiedServiceBusiness.after.csv"]
    )
    fun `save parsing result for unified feed`() {
        val businessRecord = defaultRecord.copy(
            id = defaultRecord.id.copy(
                partnerId = defaultRecord.id.businessId,
                feedId = 1006L
            )
        )
        mockDatacampParsingHistory(
            mapOf(
                listOf(defaultRecord.id) to listOf(defaultRecord),
                listOf(businessRecord.id) to listOf(businessRecord),
            )
        )
        process("parsingResultForUnifiedServiceBusiness.proto.json")

        val ytHistoryResult = ytDatacampParsingHistoryClientProxy.capture<DatacampParsingHistoryRecord>(mode = times(2))
        assertThat(ytHistoryResult.map { it.id.feedId })
            .containsExactlyInAnyOrder(1005L, 1006L)
        assertThat(ytHistoryResult.map { it.offerStats })
            .contains(
                DatacampParsingHistoryRecord.OfferStatistics(
                    totalOffers = 5,
                    errorOffers = 4,
                    warningOffers = 3,
                    unloadedOffers = 2,
                    loadedOffers = 1
                ),
                DatacampParsingHistoryRecord.OfferStatistics(
                    totalOffers = 5,
                    errorOffers = 4,
                    warningOffers = 4,
                    unloadedOffers = 3,
                    loadedOffers = 2
                )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.outdated.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.outdated.after.csv"]
    )
    fun `repeated read of parsing result from datacamp with outdated db status`() {
        mockDatacampParsingHistory()
        process("parsingResultForRealFeed.proto.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.outdated.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.outdated.after.csv"]
    )
    fun `repeated read of parsing result from datacamp with actual data in yt`() {
        mockDatacampParsingHistory {
            copy(
                receivedTime = Instant.now(),
                feedParserStatusCode = FeedParsingResult.SUCCESS,
                parseStatusCode = ReturnCode.OK
            )
        }
        process("parsingResultForRealFeed.proto.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.actual.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.actual.before.csv"]
    )
    fun `repeated read of parsing result from datacamp with actual db status`() {
        mockDatacampParsingHistory()
        process("parsingResultForRealFeed.proto.json")
    }

    @Test
    @DbUnitDataSet(after = ["DataCampFeedParsingResultProcessorTest.fatalParsing.after.csv"])
    fun `first fatal result`() {
        mockDatacampParsingHistory()
        process("fatalParsingResult.proto.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.outdated.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.fatalAfterSuccess.after.csv"]
    )
    fun `fatal result after not fatal`() {
        mockDatacampParsingHistory()
        process("fatalParsingResult.proto.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.successAfterFatal.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.successAfterFatal.after.csv"]
    )
    fun `not fatal result after fatal`() {
        mockDatacampParsingHistory()
        process("parsingResultForRealFeed.proto.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.successAfterFirstFatal.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.successAfterFirstFatal.after.csv"]
    )
    fun `not fatal result after first fatal`() {
        mockDatacampParsingHistory()
        process("parsingResultForRealFeed.proto.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.outdated.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.errorResult.after.csv"]
    )
    fun `error result (json)`() {
        mockDatacampParsingHistory()
        process("errorParsingResult.proto.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.outdated.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.errorResult.after.csv"]
    )
    fun `error result (verdicts)`() {
        mockDatacampParsingHistory()
        process("errorVerdictParsingResult.proto.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.outdated.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.warningResult.after.csv"]
    )
    fun `warning result`() {
        mockDatacampParsingHistory()
        process("warningParsingResult.proto.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.outdated.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.outdated.after.csv"]
    )
    fun `same code will increase total amount`() {
        mockDatacampParsingHistory()
        process("parsingResultForRealFeed.proto.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["DataCampFeedParsingResultProcessorTest.outdatedWithErrorCode.before.csv"],
        after = ["DataCampFeedParsingResultProcessorTest.realFeed.after.csv"]
    )
    fun `successful parsing after error will remove error_code`() {
        mockDatacampParsingHistory()
        process("parsingResultForRealFeed.proto.json")
    }

    private fun mockDatacampParsingHistory(
        recordsNumber: Int = 1,
        prepareRecord: DatacampParsingHistoryRecord.(i: Int) -> DatacampParsingHistoryRecord = { this }
    ) {
        val list = List(recordsNumber) { defaultRecord.prepareRecord(it) }
        mockDatacampParsingHistory(mapOf(list.map { it.id } to list))
    }

    private fun mockDatacampParsingHistory(
        map: Map<List<DatacampParsingHistoryRecord.Id>, List<DatacampParsingHistoryRecord>>,
    ) {
        map.forEach {
            doReturn(
                it.value
            ).`when`(
                ytDatacampParsingHistoryClientProxySourceClient
            ).lookupRows(
                any(),
                eq(YTBinder.getBinder(DatacampParsingHistoryRecord.Id::class.java)),
                eq(it.key),
                eq(YTBinder.getBinder(DatacampParsingHistoryRecord::class.java)),
            )
        }
    }

    private fun process(path: String) {
        val request = getProto<GeneralizedMessageOuterClass.GeneralizedMessage>(path)
        val batch = MessageBatch("", 1, listOf(MessageData(request.toByteArray(), 0, null)))
        dataCampFeedParsingResultProcessor.process(batch)
    }

    companion object {
        val defaultRecord = DatacampParsingHistoryRecord(
            id = DatacampParsingHistoryRecord.Id(
                businessId = 1004,
                partnerId = 1002,
                feedId = 1005L,
                feedType = FeedType.ASSORTMENT_FEED,
                updateTime = Instant.ofEpochSecond(1637486983),
                parsingTaskId = 1006,
            ),
            originalUrl = "https://website.ru/yandex.xml",
            mdsUrl = "http://storage-int.mds.yandex.net/get-turbo-commodity-feed/4479803/abc",
            parsingType = ParsingType.COMPLETE,
            lbStatusCode = 200,
            lbError = null
        )
    }
}
