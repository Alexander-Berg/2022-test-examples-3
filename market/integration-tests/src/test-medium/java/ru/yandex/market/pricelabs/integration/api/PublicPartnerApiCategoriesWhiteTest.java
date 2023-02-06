package ru.yandex.market.pricelabs.integration.api;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.generated.server.pub.model.SearchCategoriesResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.SearchCategoriesResponseCount;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTest.checkResponse;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID_N;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID_N2;

public class PublicPartnerApiCategoriesWhiteTest extends AbstractPartnerApiCategoriesTest {

    private static final SearchCategoriesResponse C1 = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 1)
            .categoryName("name 1, name 1.1, name 1.2")
            .parentCategoryId(ShopCategory.NO_CATEGORY)
            .offersCount(1 + 3 + 4)
            .childrenCount(0)
            .categoryCount(3)
            .level(1)
            .path(List.of());
    private static final SearchCategoriesResponse C2 = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 2)
            .categoryName("name 2")
            .parentCategoryId(ShopCategory.NO_CATEGORY)
            .offersCount(2)
            .childrenCount(0)
            .categoryCount(1)
            .level(1)
            .path(List.of());
    private static final SearchCategoriesResponse C3 = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 3)
            .categoryName("name 3") // одно название (уникальное)
            .parentCategoryId(ShopCategory.NO_CATEGORY)
            .offersCount(5 + 6 + (7 + 8 + (9)))
            .childrenCount(1)
            .categoryCount(2)
            .level(1)
            .path(List.of());
    private static final SearchCategoriesResponse C4 = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 4)
            .categoryName("name 4") // одно название (уникальное)
            .parentCategoryId((long) SHOP_ID_N + 3)
            .offersCount(7 + 8 + (9))
            .childrenCount(1)
            .categoryCount(2)
            .level(2)
            .path(List.of((long) SHOP_ID_N + 3));
    private static final SearchCategoriesResponse C5 = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 5)
            .categoryName("name 4.1")
            .parentCategoryId((long) SHOP_ID_N + 4)
            .offersCount(9)
            .childrenCount(0)
            .categoryCount(1)
            .level(3)
            .path(List.of((long) SHOP_ID_N + 3, (long) SHOP_ID_N + 4));


    private static final SearchCategoriesResponse N2C1 = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 1)
            .categoryName("name 1")
            .parentCategoryId(ShopCategory.NO_CATEGORY)
            .offersCount(1)
            .childrenCount(1)
            .categoryCount(1)
            .level(1)
            .path(List.of());
    private static final SearchCategoriesResponse N2C2 = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 2)
            .categoryName("name 2")
            .parentCategoryId((long) SHOP_ID_N + 1)
            .offersCount(1)
            .childrenCount(1)
            .categoryCount(1)
            .level(2)
            .path(List.of((long) SHOP_ID_N + 1));
    private static final SearchCategoriesResponse N2C2_W = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 2)
            .categoryName("name 2")
            .parentCategoryId((long) SHOP_ID_N + 1)
            .offersCount(1)
            .childrenCount(0)
            .categoryCount(1)
            .level(2)
            .path(List.of((long) SHOP_ID_N + 1));
    private static final SearchCategoriesResponse N2C3 = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 3)
            .categoryName("name 3")
            .parentCategoryId((long) SHOP_ID_N + 2)
            .offersCount(0)
            .childrenCount(0)
            .categoryCount(1)
            .level(3)
            .path(List.of((long) SHOP_ID_N + 1, (long) SHOP_ID_N + 2));

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.white, C1, C2, C3, C4, C5);
    }

    @Test
    void partnerCategoriesByNameGetAllN2() {
        var ret = checkResponse(partnerApi.partnerCategoriesByNameGet(SHOP_ID_N2, "NAME", null, null, null, false));
        assertEquals(List.of(N2C1, N2C2, N2C3), ret);
    }

    @Test
    void partnerCategoriesByNameGetAllN2WithOffersOnly() {
        var ret = checkResponse(partnerApi.partnerCategoriesByNameGet(SHOP_ID_N2, "NAME", null, null, null, true));
        assertEquals(List.of(N2C1, N2C2_W), ret);
    }

    @Test
    void partnerCategoriesByParentGetOneN2() {
        var ret = checkResponse(partnerApi.partnerCategoriesByParentGet(SHOP_ID_N2,
                (long) SHOP_ID_N + 2, null, null, null, false));
        assertEquals(List.of(N2C3), ret);
    }

    @Test
    void partnerCategoriesByParentGetOneN2WithOffersOnly() {
        var ret = checkResponse(partnerApi.partnerCategoriesByParentGet(SHOP_ID_N2,
                (long) SHOP_ID_N + 2, null, null, null, true));
        assertEquals(List.of(), ret);
    }

    @Test
    void partnerCategoriesByParentGetOneN2WithOffersOnlyOK() {
        var ret = checkResponse(partnerApi.partnerCategoriesByParentGet(SHOP_ID_N2,
                (long) SHOP_ID_N + 1, null, null, null, true));
        assertEquals(List.of(N2C2_W), ret);
    }

    @Test
    void partnerCategoriesByIdGetOneN2() {
        var ret = checkResponse(partnerApi.partnerCategoriesByIdGet(SHOP_ID_N2,
                List.of((long) SHOP_ID_N + 3), null, null, null, false));
        assertEquals(List.of(N2C3), ret);
    }

    @Test
    void partnerCategoriesByIdGetOneN2WithOffersOnly() {
        var ret = checkResponse(partnerApi.partnerCategoriesByIdGet(SHOP_ID_N2,
                List.of((long) SHOP_ID_N + 3), null, null, null, true));
        assertEquals(List.of(), ret);
    }

    @Test
    void partnerCategoriesByIdGetOneN2WithOffersOnlyOK() {
        var ret = checkResponse(partnerApi.partnerCategoriesByIdGet(SHOP_ID_N2,
                List.of((long) SHOP_ID_N + 2), null, null, null, true));
        assertEquals(List.of(N2C2_W), ret);
    }

    @Test
    void partnerCategoriesCountGetN2() {
        var ret = checkResponse(partnerApi.partnerCategoriesCountGet(SHOP_ID_N2, null, false));
        assertEquals(new SearchCategoriesResponseCount()
                .childrenCount(1)
                .offersCount(1)
                .categoryCount(3), ret);
    }

    @Test
    void partnerCategoriesCountGetN2WithOffersOnly() {
        var ret = checkResponse(partnerApi.partnerCategoriesCountGet(SHOP_ID_N2, null, true));
        assertEquals(new SearchCategoriesResponseCount()
                .childrenCount(1)
                .offersCount(1)
                .categoryCount(2), ret);
    }
}
