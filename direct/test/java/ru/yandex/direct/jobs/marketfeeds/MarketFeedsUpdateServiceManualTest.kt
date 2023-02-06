package ru.yandex.direct.jobs.marketfeeds

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.feed.repository.DataCampFeedYtRepository
import ru.yandex.direct.jobs.configuration.ManualTestingWithTvm

@Disabled("Ходит в реальные YT-таблицы")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [ManualTestingWithTvm::class])
class MarketFeedsUpdateServiceManualTest {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MarketFeedsUpdateService::class.java)
    }

    @Autowired
    private lateinit var marketFeedsUpdateService: MarketFeedsUpdateService

    @Autowired
    private lateinit var dataCampFeedYtRepository: DataCampFeedYtRepository

    @Test
    fun getTableModificationTime() {
        val tableUploadTime = marketFeedsUpdateService.getTableModificationTime()
        assertThat(tableUploadTime).isNotNull
    }

    @Test
    fun getAllClientIdsFromYt() {
        val clientIds = marketFeedsUpdateService.getAllClientIdsFromYt()
        val msg = clientIds.joinToString(", ", "clientIds = [", "]", 10)
        LOGGER.info(msg)
        assertThat(clientIds).isNotEmpty
    }

    @Test
    fun getFeedsFromYt() {
        val clientIds = marketFeedsUpdateService.getAllClientIdsFromYt().take(3).toSet()
        val ytFeedsRows = dataCampFeedYtRepository.getYtFeedsRows(clientIds)
        val feeds = ytFeedsRows.map { FeedConverter.ytRowToFeed(it, false) }
        val msg = feeds.joinToString(", ", "feeds = [", "]", 10)
        LOGGER.info(msg)
        assertThat(feeds).isNotEmpty
    }

}
