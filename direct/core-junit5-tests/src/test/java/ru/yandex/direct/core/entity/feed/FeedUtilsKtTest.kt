package ru.yandex.direct.core.entity.feed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.model.FeedSimple
import ru.yandex.direct.core.entity.feed.model.FeedUsageType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.EnumSet

internal class FeedUtilsKtTest {

    @Test
    fun test_feedWithUsageTypeToAppliedChanges_changedLastUsedToNull() {
        val actualDateTime = getActualDateTime()
        val map = mapOf(
            Feed().withUsageTypes(EnumSet.of(FeedUsageType.SEARCH_SNIPPET_GALLERY))
                .withLastUsed(actualDateTime) as FeedSimple
                to EnumSet.of(FeedUsageType.GOODS_ADS)
        )
        val appliedChanges = feedWithUsageTypeToAppliedChanges(map)
        assertThat(appliedChanges[0].getNewValue(Feed.LAST_USED)).isNull()
    }

    @Test
    fun test_feedWithUsageTypeToAppliedChanges_notChanged() {
        val map = mapOf(
            Feed().withUsageTypes(EnumSet.of(FeedUsageType.SEARCH_SNIPPET_GALLERY)) as FeedSimple
                to EnumSet.of(FeedUsageType.SEARCH_SNIPPET_GALLERY)
        )
        val appliedChanges = feedWithUsageTypeToAppliedChanges(map)
        assertThat(appliedChanges).isEmpty()
    }

    @Test
    fun test_feedWithUsageTypeToAppliedChanges_changedLastUsedToActualTime() {
        val actualDateTime = getActualDateTime()
        val map = mapOf(
            Feed().withUsageTypes(EnumSet.of(FeedUsageType.GOODS_ADS)) as FeedSimple
                to EnumSet.noneOf(FeedUsageType::class.java)
        )
        val appliedChanges = feedWithUsageTypeToAppliedChanges(map)
        assertThat(appliedChanges[0].getNewValue(Feed.LAST_USED))!!.isAfter(actualDateTime)
    }

    private fun getActualDateTime(): LocalDateTime {
        return Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

}
