package ru.yandex.market.partner.auction;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.core.auction.model.AuctionOfferId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;

/**
 * Тест для {@link OfferTitleClarificationService}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class OfferTitleClarificationServiceTest {
    private static final long SHOP_ID = 774l;
    private static final long SOME_FEED = 10005L;
    private static final long SOME_MODEL = 9966L;
    private static final String SOME_TITLE = "humanReadableTitle";
    private static final AuctionOfferId SOME_FEED_OFFER_ID = new AuctionOfferId(SOME_FEED, "someOfferId");
    private static final LightOfferExistenceChecker.LightOfferInfo SOME_OFFER_INFO
            = new LightOfferExistenceChecker.LightOfferInfo(SOME_TITLE, SOME_MODEL);
    @Mock
    private LightOfferExistenceChecker lightOfferExistenceChecker;

    @DisplayName("Маппинг, при успешном запросе")
    @Test
    void test_loadHumanReadableTitles() {
        final OfferTitleClarificationService offerTitleClarificationService = new OfferTitleClarificationService(
                lightOfferExistenceChecker
        );

        Mockito.when(lightOfferExistenceChecker.getOfferInfo(any()))
                .thenReturn(CompletableFuture.completedFuture(SOME_OFFER_INFO));

        final Map<AuctionOfferId, String> titleByOfferId
                = offerTitleClarificationService.loadHumanReadableTitles(SHOP_ID, ImmutableList.of(SOME_FEED_OFFER_ID));

        assertThat(titleByOfferId.size(), is(1));
        assertThat(titleByOfferId, hasEntry(SOME_FEED_OFFER_ID, SOME_TITLE));
    }

    @DisplayName("Маппинг, при ошибке")
    @Test
    void test_loadHumanReadableTitlesError() {
        final OfferTitleClarificationService offerTitleClarificationService = new OfferTitleClarificationService(
                lightOfferExistenceChecker
        );

        final CompletableFuture<LightOfferExistenceChecker.LightOfferInfo> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("SomeException"));

        Mockito.when(lightOfferExistenceChecker.getOfferInfo(any()))
                .thenReturn(failedFuture);

        final Map<AuctionOfferId, String> titleByOfferId
                = offerTitleClarificationService.loadHumanReadableTitles(SHOP_ID, ImmutableList.of(SOME_FEED_OFFER_ID));

        assertThat(titleByOfferId.size(), is(0));
    }

}