package ru.yandex.market.mbi.feed.processor.samovar.result

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.environment.EnvironmentService
import ru.yandex.market.mbi.feed.processor.environment.UnitedEnvironmentService
import ru.yandex.market.mbi.feed.processor.migration.ParsingMigrationService
import ru.yandex.market.mbi.feed.processor.parsing.status.DataCampFeedParsingStatusRepository
import ru.yandex.market.mbi.feed.processor.parsing.update.DataCampParsingHistorySeqRepository
import ru.yandex.market.mbi.feed.processor.parsing.yt.YtDatacampParsingHistoryService
import ru.yandex.market.mbi.feed.processor.samovar.feedInfoBuilder
import ru.yandex.market.mbi.feed.processor.samovar.itemBuilder
import ru.yandex.market.mbi.feed.processor.samovar.messageBatchBuilder
import ru.yandex.market.mbi.feed.processor.samovar.proxy.SamovarResultDataProxy
import ru.yandex.market.mbi.feed.processor.samovar.status.SamovarFeedDownloadStatusRepository
import ru.yandex.market.yt.samovar.SamovarContextOuterClass
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = ["SamovarResultDataProcessorTest.before.csv"])
internal class SamovarResultDataProcessorMockTest(
    @Value("\${feed-processor.samovar.parsing.cache.sec:60}")
    private val samovarLogbrokerProcessorCacheSec: Long
) : FunctionalTest() {

    @Autowired
    private lateinit var samovarMessageFactory: SamovarMessageFactory

    @Autowired
    private lateinit var samovarFeedDownloadStatusRepository: SamovarFeedDownloadStatusRepository

    @Autowired
    private lateinit var parsingMigrationService: ParsingMigrationService

    @Autowired
    private lateinit var samovarResultDataProxy: SamovarResultDataProxy

    @Autowired
    private lateinit var ytDatacampParsingHistoryService: YtDatacampParsingHistoryService

    @Autowired
    private lateinit var environmentService: EnvironmentService

    @Autowired
    private lateinit var dataCampFeedParsingStatusRepository: DataCampFeedParsingStatusRepository

    @Autowired
    private lateinit var dataCampParsingHistorySeqRepository: DataCampParsingHistorySeqRepository

    @Autowired
    private lateinit var unitedEnvironmentService: UnitedEnvironmentService

    @Test
    @DisplayName("Парсинг сообщения из Самовара проходит успешно. Вытаскиваем зазипованный контекст")
    fun `message parsing (test for format)`() {
        val pushProcessorMock = mock<SamovarPushProcessor> { }
        val processor = SamovarResultDataProcessor(
            { true },
            pushProcessorMock,
            samovarMessageFactory,
            samovarFeedDownloadStatusRepository,
            parsingMigrationService,
            samovarResultDataProxy,
            ytDatacampParsingHistoryService,
            environmentService,
            dataCampFeedParsingStatusRepository,
            dataCampParsingHistorySeqRepository,
            unitedEnvironmentService,
            Clock.fixed(Instant.EPOCH, ZoneId.of("Asia/Vladivostok")),
            samovarLogbrokerProcessorCacheSec,
        )

        // Готовим сжатое событие, которое будет присылать самовар.
        // Это будет зазипованный протобуф.
        val feedInfo = feedInfoBuilder(feedId = 1001, shopId = 1)
        val original = itemBuilder(feedInfos = listOf(feedInfo))
        val batch = messageBatchBuilder(items = listOf(original))

        // Вызываем парсер сообщения
        processor.process(batch)
        val captor = ArgumentCaptor.forClass(SamovarFeedInfoSet::class.java)
        Mockito.verify(pushProcessorMock).accept(captor.capture())

        // Проверяем, что распарсилось именно то, что было передано.
        val actual = captor.value.serviceFeedInfos.first()

        val context = SamovarContextOuterClass.SamovarContext.parseFrom(original.context)
        Assertions.assertThat(actual.message.url).isEqualTo(original.url)
        Assertions.assertThat(actual.message.numberOfParts).isEqualTo(original.numberOfParts)
        Assertions.assertThat(actual.message.mdsKeys).isEqualTo(original.mdsKeys)
        Assertions.assertThat(actual.context.allFields).isEqualTo(context.allFields)
    }
}
