package ru.yandex.market.api.partner.controllers.feed;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.controllers.offers.searcher.ChunkedOffersSearcher;
import ru.yandex.market.common.report.model.SearchResults;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@DbUnitDataBaseConfig({@DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")})
@DbUnitDataSet(before = {
        "FeedLogControllerV2Test.csv",
        "FeedLogControllerV2LogEntriesFlavoursTest.csv"})
class FeedControllerV2PlacementTest extends AbstractFeedControllerV2Test {
    @Autowired
    private ChunkedOffersSearcher chunkedOffersSearcher;

    @Test
    void testControllerQueriesReportXmlNewSchema() throws Exception {
        SearchResults searchResults = new SearchResults();
        searchResults.setTotalOffers(543);
        when(chunkedOffersSearcher.asyncOffersRegardlessOfRegionChunked(543192, 579438L))
                .thenReturn(CompletableFuture.completedFuture(new Pair<>(searchResults, Collections.emptyList())));
        ResponseEntity<String> response = requestCampaignFeed(1543192, 579438, Format.XML);
        //language=xml
        String expected = "" +
                "<response>" +
                "    <feed id=\"579438\" url=\"http://nowhere.com/feed.xml\">" +
                "        <download status=\"OK\"/>" +
                "        <content status=\"OK\" total-offers-count=\"1668\" rejected-offers-count=\"110\"/>" +
                "        <publication status=\"OK\">" +
                "            <full" +
                "                published-time=\"2019-07-08T09:57:00+03:00\"/>" +
                "            <price-and-stock-update" +
                "                published-time=\"2019-07-08T09:57:00+03:00\"/>" +
                "        </publication>" +
                "        <placement status=\"OK\" total-offers-count=\"543\"/>" +
                "    </feed>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testControllerQueriesReportXmlWithErrorNewSchema() throws Exception {
        // эмулируем, что выполнение future завершилось ошибкой
        CompletableFuture completableFuture = Mockito.mock(CompletableFuture.class);
        when(completableFuture.get(anyLong(), any(TimeUnit.class)))
                .thenThrow(new ExecutionException(new RuntimeException()));
        when(chunkedOffersSearcher.asyncOffersRegardlessOfRegionChunked(543192, 579438L))
                .thenReturn(completableFuture);
        ResponseEntity<String> response = requestCampaignFeed(1543192, 579438, Format.XML);
        //language=xml
        String expected = "" +
                "<response>" +
                "    <feed id=\"579438\" url=\"http://nowhere.com/feed.xml\">" +
                "        <download status=\"OK\"/>" +
                "        <content status=\"OK\" total-offers-count=\"1668\" rejected-offers-count=\"110\"/>" +
                "        <publication status=\"OK\">" +
                "             <full" +
                "                published-time=\"2019-07-08T09:57:00+03:00\"/>" +
                "            <price-and-stock-update" +
                "                published-time=\"2019-07-08T09:57:00+03:00\"/>" +
                "        </publication>" +
                "        <placement status=\"NA\"/>" +
                "    </feed>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }
}
