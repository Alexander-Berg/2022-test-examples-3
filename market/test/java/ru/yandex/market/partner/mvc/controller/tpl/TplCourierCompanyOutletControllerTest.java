package ru.yandex.market.partner.mvc.controller.tpl;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональный тест для {@link TplOutletController}.
 */
public class TplCourierCompanyOutletControllerTest extends FunctionalTest {

    public final static long UUID_1 = 1248L;
    public final static long UUID_2 = 2248L;

    /**
     * Корректность получения списка ПВЗ.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-outlets.csv")
    public void testPickupPointList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-outlets.json");
    }

    /**
     * Корректность получения списка ПВЗ при наличии нескольких партнеров.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-outlets-many.csv")
    public void testPickupPointListWithMultipleElements() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-outlets-many.json");
    }

    /**
     * Корректность получения списка только своих ПВЗ конкретным пользователем.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-outlets-other-user.csv")
    public void testPickupPointListForOtherUser() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_2));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-outlets-other-user.json");
    }

    /**
     * Корректность получения пустого списка ПВЗ.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-outlets-empty-list.csv")
    public void testPickupPointEmptyList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-outlets-empty-list.json");
    }

    /**
     * Корректность фильтрации магазинов другого типа.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-outlets-filtering.csv")
    public void testPickupPointListFiltering() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-outlets-empty-list.json");
    }

    private String partnersUrl(final long effectiveUid) {
        return String.format("%s/tpl/outlets?euid=%s", baseUrl, effectiveUid);
    }
}
