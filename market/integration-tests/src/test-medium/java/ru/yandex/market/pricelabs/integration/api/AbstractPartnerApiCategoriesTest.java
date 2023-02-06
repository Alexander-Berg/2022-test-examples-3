package ru.yandex.market.pricelabs.integration.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.api.api.PublicPartnerApi;
import ru.yandex.market.pricelabs.generated.server.pub.api.PartnerApi;
import ru.yandex.market.pricelabs.generated.server.pub.model.SearchCategoriesResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.SearchCategoriesResponseCount;
import ru.yandex.market.pricelabs.integration.AbstractIntegrationSpringConfiguration;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTest.checkResponse;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID_N;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.UNKNOWN_SHOP;

public abstract class AbstractPartnerApiCategoriesTest extends AbstractIntegrationSpringConfiguration {

    PartnerApi partnerApi;
    @Autowired
    private PublicApiTestInitializer initializer;
    @Autowired
    private PublicPartnerApi partnerApiBean;
    private String target;
    private SearchCategoriesResponse c1;
    private SearchCategoriesResponse c2;
    private SearchCategoriesResponse c3;
    private SearchCategoriesResponse c4;
    private SearchCategoriesResponse c5;

    void init(AutostrategyTarget target,
              SearchCategoriesResponse c1, SearchCategoriesResponse c2, SearchCategoriesResponse c3,
              SearchCategoriesResponse c4, SearchCategoriesResponse c5) {
        partnerApi = MockMvcProxy.buildProxy(PartnerApi.class, partnerApiBean);
        this.target = target.name();
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
        this.c4 = c4;
        this.c5 = c5;
        testControls.initOnce(this.getClass(), () -> initializer.init());
    }

    @Test
    void partnerCategoriesByNameGet() {
        var ret = checkResponse(partnerApi.partnerCategoriesByNameGet(SHOP_ID_N, "Тест", null, null, target, null));
        assertEquals(List.of(), ret);
    }

    @Test
    void partnerCategoriesByNameGetNull() {
        var ret = checkResponse(partnerApi.partnerCategoriesByNameGet(SHOP_ID_N, null, null, null, target, null));
        assertEquals(List.of(), ret);
    }

    @Test
    void partnerCategoriesByNameGetOne() {
        var ret = checkResponse(partnerApi.partnerCategoriesByNameGet(SHOP_ID_N, "NAME 1", null, null, target, null));
        assertEquals(List.of(c1), ret);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void partnerCategoriesByNameGetAll(boolean withOffersOnly) {
        var ret = checkResponse(partnerApi.partnerCategoriesByNameGet(SHOP_ID_N, "NAME", null, null, target,
                withOffersOnly));
        assertEquals(List.of(c1, c2, c3, c4, c5), ret);
    }

    @Test
    void partnerCategoriesByNameGetLimit2() {
        var ret = checkResponse(partnerApi.partnerCategoriesByNameGet(SHOP_ID_N, "NAME", 2, null, target, null));
        assertEquals(List.of(c1, c2), ret);
    }

    @Test
    void partnerCategoriesByNameGetAll12() {
        var ret = checkResponse(partnerApi.partnerCategoriesByNameGet(SHOP_ID_N, "NAME", 1, 2, target, null));
        assertEquals(List.of(c3), ret);
    }

    @Test
    void partnerCategoriesByNameUnknownShop() {
        var ret = checkResponse(partnerApi.partnerCategoriesByNameGet(UNKNOWN_SHOP, "NAME", null, null, target, null));
        assertEquals(List.of(), ret);
    }

    @Test
    void partnerCategoriesByParentGet() {
        var ret = checkResponse(partnerApi.partnerCategoriesByParentGet(SHOP_ID_N,
                (long) SHOP_ID_N + 111222333, null, null, target, null));
        assertEquals(List.of(), ret);
    }

    @Test
    void partnerCategoriesByParentGetTop() {
        var ret = checkResponse(partnerApi.partnerCategoriesByParentGet(SHOP_ID_N, null, null, null, target, null));
        assertEquals(List.of(), ret);
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void partnerCategoriesByParentGetOne(boolean withOffersOnly) {
        var ret = checkResponse(partnerApi.partnerCategoriesByParentGet(SHOP_ID_N,
                (long) SHOP_ID_N + 3, null, null, target, withOffersOnly));
        assertEquals(List.of(c4), ret);
    }

    @Test
    void partnerCategoriesByParentGet2() {
        var ret = checkResponse(partnerApi.partnerCategoriesByParentGet(SHOP_ID_N,
                (long) SHOP_ID_N + 3, 2, null, target, null));
        assertEquals(List.of(c4), ret);
    }

    @Test
    void partnerCategoriesByParentGet12() {
        var ret = checkResponse(partnerApi.partnerCategoriesByParentGet(SHOP_ID_N,
                (long) SHOP_ID_N + 3, 1, 2, target, null));
        assertEquals(List.of(), ret);
    }

    @Test
    void partnerCategoriesByParentUnknownShop() {
        var ret = checkResponse(partnerApi.partnerCategoriesByParentGet(UNKNOWN_SHOP, null, null, null, target, null));
        assertEquals(List.of(), ret);
    }

    @Test
    void partnerCategoriesByIdGet() {
        var ret = checkResponse(partnerApi.partnerCategoriesByIdGet(SHOP_ID_N,
                List.of(1L, 2L), null, null, target, null));
        assertEquals(List.of(), ret);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void partnerCategoriesByIdGetOne(boolean withOffersOnly) {
        var ret = checkResponse(partnerApi.partnerCategoriesByIdGet(SHOP_ID_N,
                List.of((long) SHOP_ID_N + 4), null, null, target, withOffersOnly));
        assertEquals(List.of(c4), ret);
    }

    @Test
    void partnerCategoriesByIdGetAnotherOne() {
        var ret = checkResponse(partnerApi.partnerCategoriesByIdGet(SHOP_ID_N,
                List.of((long) SHOP_ID_N + 1), null, null, target, null));
        assertEquals(List.of(c1), ret);
    }

    @Test
    void partnerCategoriesByIdUnknownShop() {
        var ret = checkResponse(partnerApi.partnerCategoriesByIdGet(UNKNOWN_SHOP, List.of((long) SHOP_ID_N + 4),
                null, null, target, null));
        assertEquals(List.of(), ret);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void partnerCategoriesCountGet(boolean withOffersOnly) {
        var ret = checkResponse(partnerApi.partnerCategoriesCountGet(SHOP_ID_N, target, withOffersOnly));
        // Суммируется только количество потомков на верхнем уровне!
        assertEquals(new SearchCategoriesResponseCount()
                .childrenCount(1) //  + 2 + 4
                .offersCount((1 + 3 + 4) + (2) + (5 + 6 + (7 + 8 + (9)))) //  + 1 + 3
                .categoryCount(5), ret);
    }

    @Test
    void partnerCategoriesCountGetUnknown() {
        var ret = checkResponse(partnerApi.partnerCategoriesCountGet(UNKNOWN_SHOP, target, null));
        assertEquals(new SearchCategoriesResponseCount()
                .childrenCount(0)
                .offersCount(0)
                .categoryCount(0), ret);
    }

}
