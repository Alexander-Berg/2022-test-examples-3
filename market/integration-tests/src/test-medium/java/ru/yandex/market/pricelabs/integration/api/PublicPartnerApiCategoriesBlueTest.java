package ru.yandex.market.pricelabs.integration.api;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.pricelabs.generated.server.pub.model.SearchCategoriesResponse;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID_N;

public class PublicPartnerApiCategoriesBlueTest extends AbstractPartnerApiCategoriesTest {

    private static final SearchCategoriesResponse C1 = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 1)
            .categoryName("name 1")
            .parentCategoryId(ShopCategory.NO_CATEGORY)
            .offersCount(1 + 3 + 4)
            .childrenCount(0)
            .categoryCount(1)
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
            .categoryCount(1)
            .level(1)
            .path(List.of());
    private static final SearchCategoriesResponse C4 = new SearchCategoriesResponse()
            .categoryId((long) SHOP_ID_N + 4)
            .categoryName("name 4") // одно название (уникальное)
            .parentCategoryId((long) SHOP_ID_N + 3)
            .offersCount(7 + 8 + (9))
            .childrenCount(1)
            .categoryCount(1)
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

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.blue, C1, C2, C3, C4, C5);
    }
}
