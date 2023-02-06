package ru.yandex.market.mbi.feed.processor.parsing.request

import Market.DataCamp.API.UpdateTask
import NCrawl.Feeds
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.util.ProtoTestUtil
import ru.yandex.market.logbroker.LogbrokerEventPublisher
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass
import ru.yandex.market.mbi.feed.processor.parsing.update.event.DataCampFeedUpdateLogbrokerEvent
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.DatacampParsingHistoryRecord
import ru.yandex.market.mbi.feed.processor.samovar.request.event.SamovarFeedDownloadLogbrokerEvent
import ru.yandex.market.mbi.feed.processor.test.capture
import ru.yandex.market.mbi.feed.processor.test.getProto
import ru.yandex.market.yt.binding.YTBinder
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource
import ru.yandex.market.yt.samovar.SamovarContextOuterClass

/**
 * Тесты для [FeedUpdateTaskRequestProcessor]
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = ["datacamp.lb.enabled.before.csv"])
internal class FeedUpdateTaskRequestProcessorTest : FunctionalTest() {

    @Autowired
    private lateinit var feedUpdateTaskRequestProcessor: FeedUpdateTaskRequestProcessor

    @Autowired
    @Qualifier("dataCampMarketFeedUpdateLogbrokerService")
    private lateinit var dataCampFeedUpdateLogbrokerService: LogbrokerEventPublisher<DataCampFeedUpdateLogbrokerEvent>

    @Autowired
    @Qualifier("samovarFeedDownloadRequestLogbrokerService")
    private lateinit var samovarFeedDownloadRequestLogbrokerService: LogbrokerEventPublisher<SamovarFeedDownloadLogbrokerEvent>

    @Autowired
    private lateinit var ytDatacampParsingHistoryClientProxySource: YtClientProxySource

    private var ytDatacampParsingHistoryClientProxySourceClient: YtClientProxy = mock { }

    @BeforeEach
    fun mockTerminal() {
        doReturn(ytDatacampParsingHistoryClientProxySourceClient).`when`(ytDatacampParsingHistoryClientProxySource).currentClient
        doReturn(
            listOf<DatacampParsingHistoryRecord>()
        ).`when`(
            ytDatacampParsingHistoryClientProxySourceClient
        ).lookupRows(
            any(),
            eq(YTBinder.getBinder(DatacampParsingHistoryRecord.Id::class.java)),
            any(),
            eq(YTBinder.getBinder(DatacampParsingHistoryRecord::class.java)),
        )
    }

    @AfterEach
    fun checkMocks() {
        Mockito.verifyNoMoreInteractions(dataCampFeedUpdateLogbrokerService, samovarFeedDownloadRequestLogbrokerService)
    }

    @Test
    fun `Invalid request message`() {
        process("withoutBusiness.update.request.json")
    }

    @Test
    fun `Refresh feed by url`() {
        process("byUrl.update.request.json")
        checkSamovarEvent("byUrl")
    }

    @Test
    fun `Refresh feed by url with parsing fields`() {
        process("byUrl.parsingFields.update.request.json")
        checkSamovarEvent("byUrl.parsingFields")
    }

    @Test
    @DbUnitDataSet(after = ["FeedUpdateTaskRequestProcessorTest.by_file.after.csv"])
    fun `Refresh feed by file`() {
        process("byFile.update.request.json")

        val expected = getProto<UpdateTask.FeedParsingTask>("byFile.parsing.request.json")

        val actual = dataCampFeedUpdateLogbrokerService.capture()
        ProtoTestUtil.assertThat(actual.first().payload)
            .isEqualTo(expected)
    }

    @Test
    @DbUnitDataSet(after = ["FeedUpdateTaskRequestProcessorTest.by_file.parsing_fields.after.csv"])
    fun `Refresh feed by file with parsing fields`() {
        process("byFile.update.request.parsingFields.json")

        val expected = getProto<UpdateTask.FeedParsingTask>("byFile.parsing.request.parsingFields.json")

        val actual = dataCampFeedUpdateLogbrokerService.capture()
        ProtoTestUtil.assertThat(actual.first().payload)
            .isEqualTo(expected)
    }

    @Test
    @DbUnitDataSet(before = ["FeedUpdateTaskRequestProcessorTest.outdated_url.before.csv"])
    fun `Feed with outdated url will not be parsed`() {
        process("byUrl.update.request.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["FeedUpdateTaskRequestProcessorTest.delete.before.csv"],
        after = ["FeedUpdateTaskRequestProcessorTest.delete.deleted.after.csv"]
    )
    fun `Delete feed by id`() {
        process("delete.request.json")
    }

    @Test
    @DbUnitDataSet(
        after = ["FeedUpdateTaskRequestProcessorTest.delete.deleted.after.csv"]
    )
    fun `Delete feed after update`() {
        process("delete.request.json")
        process("update.beforeDelete.request.json")
    }

    @Test
    @DbUnitDataSet(
        after = ["FeedUpdateTaskRequestProcessorTest.delete.kept.after.csv"]
    )
    fun `Delete feed before update`() {
        process("update.afterDelete.request.json")
        process("delete.request.json")
        verify(samovarFeedDownloadRequestLogbrokerService, times(1)).publishEvent(any())
    }

    @Test
    @DbUnitDataSet(before = ["FeedUpdateTaskRequestProcessorTest.actual_url.before.csv"])
    fun `Feed with actual url will be parsed`() {
        process("byUrl.update.request.json")
        checkSamovarEvent("byUrl")
    }

    @Test
    @DbUnitDataSet(after = ["FeedUpdateTaskRequestProcessorTest.actual_default.after.csv"])
    fun `default feed will be saved in db but not parsed`() {
        process("default.update.request.json")
        verifyNoMoreInteractions(samovarFeedDownloadRequestLogbrokerService, dataCampFeedUpdateLogbrokerService)
    }

    @Test
    fun `dropship with stock feed will be processed without expanding`() {
        process("dropship.stock.json")

        val expected = getProto<UpdateTask.FeedParsingTask>("dropship.stock.parsing.request.json")

        val actual = dataCampFeedUpdateLogbrokerService.capture()
        ProtoTestUtil.assertThat(actual.first().payload)
            .isEqualTo(expected)
    }

    @Test
    fun `dropship in group with stock feed will be processed without expanding`() {
        process("dropship.group.stock.json")

        val expected = getProto<UpdateTask.FeedParsingTask>("dropship.group.stock.parsing.request.json")

        val actual = dataCampFeedUpdateLogbrokerService.capture()
        ProtoTestUtil.assertThat(actual.first().payload)
            .isEqualTo(expected)
    }

    private fun process(path: String) {
        val request = getProto<FeedUpdateTaskOuterClass.FeedUpdateTask>(path)
        val batch = MessageBatch("", 1, listOf(MessageData(request.toByteArray(), 0, null)))
        feedUpdateTaskRequestProcessor.process(batch)
    }

    private fun checkSamovarEvent(filePrefix: String) {
        val expected = getProto<Feeds.TFeedExt>("$filePrefix.download.request.json")
        val actual = samovarFeedDownloadRequestLogbrokerService.capture()
        val samovarEvent: Feeds.TFeedExt = actual.first().payload
        ProtoTestUtil.assertThat(samovarEvent)
            .ignoringFieldsMatchingRegexes(".*bytesValue.*")
            .isEqualTo(expected)

        val expectedContext =
            getProto<SamovarContextOuterClass.SamovarContext>("$filePrefix.download.context.request.json")
        val actualContext = SamovarContextOuterClass.SamovarContext.parseFrom(samovarEvent.feedContext.bytesValue)
        ProtoTestUtil.assertThat(actualContext)
            .isEqualTo(expectedContext)
    }
}
