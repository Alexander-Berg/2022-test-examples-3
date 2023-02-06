package ru.yandex.market.partner.mvc.controller.sortingcenters;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональный тест для {@link SortingCentersController}.
 */
public class SortingCentersControllerTest extends FunctionalTest {

    public final static long UUID_1 = 1248L;
    public final static long UUID_2 = 2248L;

    /**
     * Корректность получения списка партнеров.
     */
    @Test
    @DbUnitDataSet(before = "csv/sorting-centers-get.csv")
    public void testPartnerList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/sorting-centers-get.json");
    }

    /**
     * Корректность получения списка партнеров при наличии нескольких партнеров.
     */
    @Test
    @DbUnitDataSet(before = "csv/sorting-centers-get-many.csv")
    public void testPartnerListManyPartners() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/sorting-centers-get-many.json");
    }

    /**
     * Корректность получения списка только своих партнеров конкретным пользователем.
     */
    @Test
    @DbUnitDataSet(before = "csv/sorting-centers-get-for-other-user.csv")
    public void testPartnerListOtherUser() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_2));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/sorting-centers-get-for-other-user.json");
    }

    /**
     * Корректность получения пустого списка партнеров.
     */
    @Test
    @DbUnitDataSet(before = "csv/sorting-centers-get-empty-list.csv")
    public void testPartnerEmptyList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/sorting-centers-get-empty-list.json");
    }

    /**
     * Корректность фильтрации партнеров другого типа.
     */
    @Test
    @DbUnitDataSet(before = "csv/sorting-centers-filtering.csv")
    public void testPartnerFiltering() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/sorting-centers-get-empty-list.json");
    }

    private String partnersUrl(final long effectiveUid) {
        return String.format("%s/sorting-centers?euid=%s", baseUrl, effectiveUid);
    }
}
