package ru.yandex.market.api.integration;

import java.util.Collections;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.longs.LongLists;
import org.junit.Test;

import ru.yandex.market.api.controller.v2.SearchControllerV2;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.GeoCoordinatesV2;
import ru.yandex.market.api.domain.v2.redirect.parameters.SearchQuery;
import ru.yandex.market.api.internal.report.CommonReportOptions;
import ru.yandex.market.api.model.UniversalModelSort;
import ru.yandex.market.api.search.SearchType;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static ru.yandex.market.api.internal.report.CommonReportOptions.Param.HIDE_OFFERS_WITHOUT_CPC_LINK;


/**
 * @author dimkarp93
 * @see <a href="https://st.yandex-team.ru/MARKETAPI-4105">MARKETAPI-4105</a>
 * @see <a href="https://st.yandex-team.ru/MARKETAPI-3859">MARKETAPI-3859</a>
 */
public class CpaOnlyTest extends BaseTest {

    private static final String TEST_PLACE = "prime";
    private static final String SEARCH_TEXT = "iphone";
    private static final String REPORT_PRIME_RESPONSE_PATH = "report_prime.json";

    @Inject
    private SearchControllerV2 searchController;

    @Inject
    private ReportTestClient reportTestClient;

    @Test
    public void notHideOnlyOffers_partnerWithoutShowShopUrl() {
        setClient(Client.Type.EXTERNAL, false);

        reportTestClient.doRequest(TEST_PLACE,
                x -> x.withoutParam(HIDE_OFFERS_WITHOUT_CPC_LINK)
        ).ok().body(REPORT_PRIME_RESPONSE_PATH);

        doRequest();
    }

    @Test
    public void hideOnlyOffers_partnerShowShopUrl_notInternal() {
        setClient(Client.Type.EXTERNAL, true);

        reportTestClient.doRequest(TEST_PLACE,
                x -> x.param(HIDE_OFFERS_WITHOUT_CPC_LINK, "1")
        ).ok().body(REPORT_PRIME_RESPONSE_PATH);

        doRequest();
    }

    @Test
    public void notHideOnlyOffers_partnerShowShopUrl_internal() {
        setClient(Client.Type.INTERNAL, true);

        reportTestClient.doRequest(TEST_PLACE,
                x -> x.withoutParam(HIDE_OFFERS_WITHOUT_CPC_LINK)
        ).ok().body(REPORT_PRIME_RESPONSE_PATH);

        doRequest();
    }

    private void doRequest() {
        searchController.search(
                LongLists.EMPTY_LIST,
                false,
                new GeoCoordinatesV2(0.0, 0.0),
                false,
                false,
                Collections.emptyList(),
                new SearchQuery(SEARCH_TEXT, SearchType.TEXT, null),
                Collections.emptyList(),
                0L,
                10000L,
                PageInfo.DEFAULT,
                UniversalModelSort.DISCOUNT_SORT,
                false,
                CommonReportOptions.ResultType.OFFERS,
                Collections.emptyMap(),
                genericParams,
                0L,
                true,
                null
        ).waitResult();
    }

    private static void setClient(Client.Type type, boolean isShowShopUrl) {
        ContextHolder.update(ctx -> {
            Client client = new Client();
            client.setType(type);
            client.setShowShopUrl(isShowShopUrl);
            ctx.setClient(client);
        });
    }

}
