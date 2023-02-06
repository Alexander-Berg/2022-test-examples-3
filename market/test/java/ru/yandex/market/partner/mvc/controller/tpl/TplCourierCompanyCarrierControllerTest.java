package ru.yandex.market.partner.mvc.controller.tpl;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональный тест для {@link TplCarrierController}.
 */
public class TplCourierCompanyCarrierControllerTest extends FunctionalTest {

    public final static long UUID_1 = 1248L;
    public final static long UUID_2 = 2248L;

    /**
     * Корректность получения списка магистральных перевозчиков.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-carriers.csv")
    public void testCarrierList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(carriersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-carriers.json");
    }

    /**
     * Корректность получения списка магистральных перевозчиков при наличии нескольких партнеров.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-carriers-many.csv")
    public void testCarrierListWithMultipleElements() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(carriersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-carriers-many.json");
    }

    /**
     * Корректность получения списка только своих магистральных перевозчиков конкретным пользователем.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-carriers-other-user.csv")
    public void testCarrierListForOtherUser() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(carriersUrl(UUID_2));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-carriers-other-user.json");
    }

    /**
     * Корректность получения пустого списка магистральных перевозчиков.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-carriers-empty-list.csv")
    public void testCarrierEmptyList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(carriersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-carriers-empty-list.json");
    }

    /**
     * Корректность фильтрации магазинов другого типа.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-carriers-filtering.csv")
    public void testCarrierListFiltering() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(carriersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-carriers-empty-list.json");
    }

    private String carriersUrl(final long effectiveUid) {
        return String.format("%s/tpl/carriers?euid=%s", baseUrl, effectiveUid);
    }
}
