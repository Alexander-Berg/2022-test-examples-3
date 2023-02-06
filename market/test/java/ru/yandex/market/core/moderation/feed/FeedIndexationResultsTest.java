package ru.yandex.market.core.moderation.feed;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.feed.model.FeedSiteType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zoom
 */
class FeedIndexationResultsTest {
    @Test
    void shouldNotBeEligibleForModerationWhenNoResults() {
        FeedIndexationResults results = new FeedIndexationResults(Collections.emptyList());
        assertThat(results.areEligibleForModeration(FeedSiteType.MARKET)).isFalse();
    }
}
