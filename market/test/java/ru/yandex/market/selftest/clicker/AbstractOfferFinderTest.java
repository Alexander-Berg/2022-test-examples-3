package ru.yandex.market.selftest.clicker;

import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.json.common.Titles;
import ru.yandex.market.common.report.model.json.common.Urls;
import ru.yandex.market.common.report.model.json.prime.Result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Vadim Lyalin
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractOfferFinderTest {
    private AbstractOfferFinder offerFinder;

    @Mock
    private AsyncMarketReportService marketReportService;

    @Before
    public void setUp() {
        offerFinder = new AbstractOfferFinder() {
            @Override
            protected String resolveSearchUrl(long shopId, String offerSearchString) {
                return shopId + offerSearchString;
            }
        };

        offerFinder.setMarketReportService(marketReportService);
        offerFinder.setMarketClickUrl("marketClickUrl");
    }

    @Test
    public void findOffer_Null() throws Exception {
        when(marketReportService.async(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        assertNull(offerFinder.findOffer(1L));
    }

    @Test
    public void findOffer_NotNull() throws Exception {
        Result foundOffer = new Result();
        foundOffer.setEntity("offer");
        Titles titles = new Titles();
        titles.setRaw("name");
        foundOffer.setTitles(titles);
        Urls urls = new Urls();
        urls.setEncrypted("url");
        foundOffer.setUrls(urls);

        when(marketReportService.async(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(foundOffer));

        Offer offer = offerFinder.findOffer(1L);
        assertEquals(offer.getYaUrl(), "marketClickUrlurl");
        assertEquals(offer.getFoundOnUrl(), "1name");
    }
}
