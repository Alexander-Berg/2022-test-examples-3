package ru.yandex.market.common.report.parser.prime;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.SearchResults;
import ru.yandex.market.common.report.model.json.converter.PrimeResultToFoundOffersConverter;
import ru.yandex.market.common.report.parser.json.PrimeSearchResultParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author: belmatter
 */
public class PrimeResultToFoundOffersConverterTest {

    private static final String PATH = "/files/converter/prime_search_report_ONE.json";
    private static final  String URL = "http://www.svyaznoy.ru/catalog/phone/224/1581651?" +
            "utm_source=yandexmarket&utm_medium=cpc&utm_campaign=pricelist&" +
            "utm_term=mob_Apple_iPhone516Gb&utm_content=1581651&city_id=133";

    private Pair<SearchResults, List<FoundOffer>> result;

    @Before
    public void setUp() throws IOException {
        PrimeSearchResultParser<Pair<SearchResults, List<FoundOffer>>> parser = new PrimeSearchResultParser<>(
                new PrimeResultToFoundOffersConverter(1, 1));

       result = parser.parse(PrimeReportParserTest.class.getResourceAsStream(PATH));
    }

    @Test
    public void testSearchResults() throws IOException, JSONException {

        assertThat("Incorrect currentPage", result.getFirst().getCurrentPage(), equalTo(1));
        assertThat("Incorrect fromOffer", result.getFirst().getFromOffer(), equalTo(1));
        assertThat("Incorrect offersShowedOnPage", result.getFirst().getOffersShowedOnPage(), equalTo(1));
        assertThat("Incorrect pageOffers", result.getFirst().getPageOffers(), equalTo(1));
        assertThat("Incorrect pagesCount", result.getFirst().getPagesCount(), equalTo(14233));
        assertThat("Incorrect toOffer", result.getFirst().getToOffer(), equalTo(1));
        assertThat("Incorrect totalOffers", result.getFirst().getTotalOffers(), equalTo(14233));
    }

    @Test
    public void testFoundOffer(){
        final Long feedId = 200301622L;
        final String shopOfferId = "1581651";

        FoundOffer offer = result.getSecond().stream().findFirst().orElse(new FoundOffer());
        assertThat("Incorrect delivery", offer.getDelivery(), equalTo(true));
        assertThat("Incorrect deliveryFree", offer.getDeliveryFree(), equalTo(true));
        assertThat("Incorrect homeRegionId", offer.getPriorityRegionId(), equalTo(213L));
        assertThat("Incorrect description", offer.getDescription(),
                equalTo("Apple iPhone стал еще быстрее и вместе с тем изящнее. Толщина Apple iPhone 5"));
        assertThat("Incorrect feedId", offer.getFeedId(),
                equalTo(feedId));
        assertThat("Incorrect shopOfferId", offer.getShopOfferId(), equalTo(shopOfferId));
        assertThat("Incorrect feedOfferId", offer.getFeedOfferId(),
                equalTo(new FeedOfferId(shopOfferId, feedId)));
        assertThat("Incorrect wareMd5", offer.getWareMd5(), equalTo("lBlEgG7PPcS7VM7VJZ8OwA"));
        assertThat("Incorrect offerName", offer.getName(),
                equalTo("Мобильные телефоны Apple iPhone 5 16Gb (черный)"));
        assertThat("Incorrect feedCategoryId", offer.getFeedCategoryId(), equalTo("90"));
        assertThat("Incorrect price", offer.getPrice(), equalTo(new BigDecimal(28490)));
        assertThat("Incorrect url", offer.getDirectUrl(),
                equalTo(URL));


    }

}
