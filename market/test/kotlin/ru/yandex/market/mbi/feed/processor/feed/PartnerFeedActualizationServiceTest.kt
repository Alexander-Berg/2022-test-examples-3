package ru.yandex.market.mbi.feed.processor.feed

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.model.FeedType
import ru.yandex.market.mbi.feed.processor.test.toInstant
import java.time.Instant

/**
 * Тесты для [PartnerFeedActualizationService].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class PartnerFeedActualizationServiceTest : FunctionalTest() {

    @Autowired
    private lateinit var partnerFeedActualizationService: PartnerFeedActualizationService

    @Test
    fun `isActualUrl, new feed returns true`() {
        val actual = partnerFeedActualizationService.isActualUrl(getPartnerFeed())
        Assertions.assertThat(actual).isTrue
    }

    @Test
    @DbUnitDataSet(after = ["PartnerFeedActualizationServiceTest.actual1.after.csv"])
    fun `updateIfActualUrl, new feed returns true and saves feed`() {
        val actual = partnerFeedActualizationService.updateIfActualUrl(getPartnerFeed())
        Assertions.assertThat(actual).isTrue
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerFeedActualizationServiceTest.deleted.before.csv"],
        after = ["PartnerFeedActualizationServiceTest.deleted.before.csv"]
    )
    fun `isActualUrl, feed was deleted`() {
        val actual = partnerFeedActualizationService.isActualUrl(getPartnerFeed())
        Assertions.assertThat(actual).isFalse
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerFeedActualizationServiceTest.outdated.before.csv"],
        after = ["PartnerFeedActualizationServiceTest.outdated.before.csv"]
    )
    fun `isActualUrl, feed is outdated`() {
        val actual = partnerFeedActualizationService.isActualUrl(getPartnerFeed())
        Assertions.assertThat(actual).isFalse
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerFeedActualizationServiceTest.actual_same_url.before.csv"],
        after = ["PartnerFeedActualizationServiceTest.actual_same_url.before.csv"]
    )
    fun `isActualUrl, feed is actual (same url)`() {
        val actual = partnerFeedActualizationService.isActualUrl(getPartnerFeed())
        Assertions.assertThat(actual).isTrue
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerFeedActualizationServiceTest.actual_new_url.before.csv"],
    )
    fun `isActualUrl, feed is actual (updated_at is newer)`() {
        val actual = partnerFeedActualizationService.isActualUrl(getPartnerFeed())
        Assertions.assertThat(actual).isTrue
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerFeedActualizationServiceTest.actual_new_url.before.csv"],
        after = ["PartnerFeedActualizationServiceTest.actual_new_url.after.csv"]
    )
    fun `updateIfActualUrl, feed is actual (updated_at is newer)`() {
        val actual = partnerFeedActualizationService.updateIfActualUrl(getPartnerFeed())
        Assertions.assertThat(actual).isTrue
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerFeedActualizationServiceTest.actual_new_url.before.csv"],
        after = ["PartnerFeedActualizationServiceTest.actual_new_url.after.csv"]
    )
    fun `update, updated_at is newer`() {
        val actual = partnerFeedActualizationService.update(getPartnerFeed())
        Assertions.assertThat(actual).isTrue
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerFeedActualizationServiceTest.actual_new_url.before.csv"],
        after = ["PartnerFeedActualizationServiceTest.actual_new_url.before.csv"]
    )
    fun `update, updated_at is older`() {
        val outdatedFeed = getPartnerFeed().copy(updatedAt = toInstant(1900, 1, 1))
        val actual = partnerFeedActualizationService.update(outdatedFeed)
        Assertions.assertThat(actual).isFalse
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerFeedActualizationServiceTest.actual_default.before.csv"],
        after = ["PartnerFeedActualizationServiceTest.actual_default.before.csv"]
    )
    fun `default feed in db, event with older updated_at, will be false`() {
        val outdatedFeed = getPartnerFeed().copy(updatedAt = toInstant(1900, 1, 1))
        val actual = partnerFeedActualizationService.updateIfActualUrl(outdatedFeed)
        Assertions.assertThat(actual).isFalse
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerFeedActualizationServiceTest.actual_new_url.before.csv"],
        after = ["PartnerFeedActualizationServiceTest.actual_default.before.csv"]
    )
    fun `make feed default`() {
        val defaultFeed = PartnerFeed(
            innerId = 0,
            feedId = 100,
            feedType = FeedType.ASSORTMENT_FEED,
            partnerId = 200,
            businessId = null,
            url = null,
            updatedAt = toInstant(2021, 1, 1),
            isDeleted = false,
            deletedUpdatedAt = toInstant(2021, 1, 1),
            login = null,
            password = null,
            isUpload = false,
            isDefault = true,
            originalName = null,
            parsingFields = null,
        )
        val actual = partnerFeedActualizationService.updateIfActualUrl(defaultFeed)
        Assertions.assertThat(actual).isTrue
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerFeedActualizationServiceTest.actual_new_url.before.csv"],
        after = ["PartnerFeedActualizationServiceTest.actual_upload.after.csv"]
    )
    fun `make feed upload`() {
        val defaultFeed = getPartnerFeed().copy(isUpload = true)
        val actual = partnerFeedActualizationService.updateIfActualUrl(defaultFeed)
        Assertions.assertThat(actual).isTrue
    }

    @Test
    fun `skip inconsistent feeds`() {
        val brokenFeed = PartnerFeed(
            innerId = 0,
            feedId = 200533400,
            feedType = FeedType.STOCK_FEED,
            partnerId = 10388304,
            businessId = null,
            url = null,
            updatedAt = Instant.parse("2045-01-09T01:00:10.481099Z"),
            deletedUpdatedAt = Instant.parse("2045-01-09T01:00:10.481099Z"),
            isDeleted = false,
            login = null,
            password = null,
            isDefault = false,
            isUpload = true,
            originalName = "orig.xml",
            parsingFields = null,
        )
        assertDoesNotThrow { partnerFeedActualizationService.updateIfActualUrl(brokenFeed) }
    }

    private fun getPartnerFeed() = PartnerFeed(
        innerId = 0,
        feedId = 100,
        feedType = FeedType.ASSORTMENT_FEED,
        partnerId = 200,
        businessId = 666,
        url = "http://url1.ru",
        updatedAt = toInstant(2020, 1, 1),
        isDeleted = false,
        deletedUpdatedAt = toInstant(2020, 1, 1),
        login = null,
        password = null,
        isUpload = false,
        isDefault = false,
        originalName = null,
        parsingFields = null,
    )
}
