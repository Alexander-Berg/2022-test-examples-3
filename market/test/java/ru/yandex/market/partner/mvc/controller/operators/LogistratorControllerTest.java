package ru.yandex.market.partner.mvc.controller.operators;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональный тест для {@link LogistratorController}.
 */

public class LogistratorControllerTest extends FunctionalTest {
    public static final long UUID_1 = 1248L;
    public static final long UUID_2 = 2248L;

    /**
     * Корректность получения списка кабинетов Логистратор.
     */
    @Test
    @DbUnitDataSet(before = "csv/logistrator-get-cabinets.csv")
    public void testCabinetList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(cabinetsUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/logistrator-get-cabinets.json");
    }

    /**
     * Корректность получения списка кабинетов Логистратор при наличии нескольких кабинетов.
     */
    @Test
    @DbUnitDataSet(before = "csv/logistrator-get-cabinets-many.csv")
    public void testCabinetListWithMultipleElements() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(cabinetsUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/logistrator-get-cabinets-many.json");
    }

    /**
     * Корректность получения списка только своих кабинетов Логистратор конкретным пользователем.
     */
    @Test
    @DbUnitDataSet(before = "csv/logistrator-get-cabinets-other-user.csv")
    public void testCabinetListForOtherUser() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(cabinetsUrl(UUID_2));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/logistrator-get-cabinets-other-user.json");
    }

    /**
     * Корректность получения пустого списка кабинетов Логистратор.
     */
    @Test
    @DbUnitDataSet(before = "csv/logistrator-get-cabinets-empty-list.csv")
    public void testCabinetEmptyList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(cabinetsUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/logistrator-get-cabinets-empty-list.json");
    }

    /**
     * Корректность фильтрации кабинетов другого типа.
     */
    @Test
    @DbUnitDataSet(before = "csv/logistrator-get-cabinets-filtering.csv")
    public void testCabinetListFiltering() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(cabinetsUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/logistrator-get-cabinets-empty-list.json");
    }

    private String cabinetsUrl(final long effectiveUid) {
        return String.format("%s/logistrator/cabinets?euid=%s", baseUrl, effectiveUid);
    }
}
