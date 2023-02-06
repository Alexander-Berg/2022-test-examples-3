package ru.yandex.market.partner.mvc.controller.supplier.info;

import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link ru.yandex.market.partner.mvc.controller.supplier.SupplierController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "SupplierControllerInfoFunctionalTest.csv")
class SupplierControllerInfoFunctionalTest extends FunctionalTest {

    @Autowired
    BalanceContactService balanceContactService;

    @Test
    @DisplayName("Не найден clientId по переданному uid")
    void testSupplierStateWhenClientIdNotFound() {
        ResponseEntity<String> response = getSuppliersFullInfo(100555L, null);
        JsonTestUtil.assertEquals(
                response,
                "{\"suppliers\":[]}"
        );
    }

    @Test
    @DisplayName("Переданный uid не имеет доступа к компании поставщика")
    void testSupplierWhenUidHasNotAccess() {
        ResponseEntity<String> response = getSuppliersFullInfo(100502L, null);
        JsonTestUtil.assertEquals(
                response,
                "{\"suppliers\":[]}"
        );
    }

    @Test
    @DisplayName("Положительный сценарий")
    void testSupplierWhenUidIsSuperAdminHasAccess() {
        ResponseEntity<String> responseEntity = getSuppliersFullInfo(100505L, null);
        JsonTestUtil.assertEquals(
                responseEntity,
                //language=json
                "{  \n" +
                        "   \"suppliers\":[  \n" +
                        "      {  \n" +
                        "         \"campaignId\":10776,\n" +
                        "         \"datasourceId\":6,\n" +
                        "         \"programStatus\": \"filled\",\n" +
                        "         \"domain\":\"my.shop.ru\",\n" +
                        "         \"name\":\"my shop\",\n" +
                        "         \"placementTypes\":[\"DROPSHIP\"]" +
                        "      }\n" +
                        "   ]\n" +
                        "}"
        );
    }

    @Test
    @DisplayName("Положительный сценарий")
    void testSupplierStateWhenSupplierFoundWithoutDraft() {
        ResponseEntity<String> response = getSuppliersFullInfo(100503L, null);
        JsonTestUtil.assertEquals(
                response,
                //language=json
                "{\n" +
                        "  \"suppliers\": [\n" +
                        "    {\n" +
                        "      \"campaignId\": 10776,\n" +
                        "      \"datasourceId\": 6,\n" +
                        "      \"programStatus\": \"filled\",\n" +
                        "      \"domain\": \"my.shop.ru\",\n" +
                        "      \"name\": \"my shop\",\n" +
                        "      \"placementTypes\": [\n" +
                        "        \"DROPSHIP\"\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
        );
    }

    @Test
    @DisplayName("Поиск всех поставщиков клиента - ни один не найден")
    void getSuppliersEmpty() {
        ResponseEntity<String> suppliersFullInfo = getSuppliersFullInfo(1005001L, null);
        JsonTestUtil.assertEquals(
                suppliersFullInfo,
                //language=json
                "{\n" +
                        "  \"suppliers\": [\n" +
                        "  ]\n" +
                        "}"
        );
    }

    @Test
    @DisplayName("Поиск всех поставщиков клиента (найден 1 поставщик)")
    void getSuppliersOneFound() {
        ResponseEntity<String> responseEntity = getSuppliersFullInfo(100505L, null);
        JsonTestUtil.assertEquals(
                responseEntity,
                //language=json
                "{\n" +
                        "  \"suppliers\":[\n" +
                        "    {\n" +
                        "      \"campaignId\":10776,\n" +
                        "      \"datasourceId\":6,\n" +
                        "      \"programStatus\": \"filled\",\n" +
                        "      \"domain\":\"my.shop.ru\",\n" +
                        "      \"name\":\"my shop\",\n" +
                        "      \"placementTypes\":[\"DROPSHIP\"]" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
        );
    }

    @Test
    @DisplayName("Поиск всех поставщиков (найдено более 1 поставщика)")
    void getSuppliersManyFound() {
        ResponseEntity<String> responseEntity = getSuppliersFullInfo(100510L, null);
        JsonTestUtil.assertEquals(
                "SupplierControllerInfoFunctionalTest.filterById.json", this.getClass(),
                responseEntity
        );
    }

    @Test
    @DisplayName("Поиск поставщиков с фильтрацией по имени")
    void filterByName() {
        ResponseEntity<String> responseEntity = getSuppliersFullInfo(100510L, "app");
        JsonTestUtil.assertEquals(
                responseEntity,
                //language=json
                "{\n" +
                        "  \"suppliers\":[\n" +
                        "    {\n" +
                        "      \"campaignId\":20777,\n" +
                        "      \"datasourceId\":8,\n" +
                        "      \"programStatus\": \"filled\",\n" +
                        "      \"name\":\"apple\",\n" +
                        "      \"placementTypes\":[\"DROPSHIP\",\"CLICK_AND_COLLECT\"]" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
        );
    }

    @Test
    @DisplayName("Поиск поставщиков с фильтрацией по campaignId")
    void filterById() {
        ResponseEntity<String> responseEntity = getSuppliersFullInfo(100510L, "11-777");
        JsonTestUtil.assertEquals(
                "SupplierControllerInfoFunctionalTest.filterById.json", this.getClass(),
                responseEntity
        );
    }

    @Test
    @DisplayName("Доступ агентства")
    void agency() {
        when(balanceContactService.getClientIdByUid(eq(100000L))).thenReturn(10000L);
        ResponseEntity<String> responseEntity = getSuppliersFullInfo(100000L, null);
        JsonTestUtil.assertEquals(
                responseEntity,
                //language=json
                "{  \n" +
                        "   \"suppliers\":[  \n" +
                        "      {  \n" +
                        "         \"campaignId\":30011,\n" +
                        "         \"datasourceId\":11,\n" +
                        "         \"programStatus\": \"full\",\n" +
                        "         \"domain\":\"under_agency_allowed.ru\",\n" +
                        "         \"name\":\"under_agency_allowed\",\n" +
                        "         \"placementTypes\":[\"FULFILLMENT\",\"CROSSDOCK\"]" +
                        "      }\n" +
                        "   ]\n" +
                        "}"
        );
    }

    @Test
    @DisplayName("Доступ агентства. Поиск")
    void agencySearch() {
        when(balanceContactService.getClientIdByUid(eq(100000L))).thenReturn(10000L);
        ResponseEntity<String> responseEntity = getSuppliersFullInfo(100000L, "allowed");
        JsonTestUtil.assertEquals(
                responseEntity,
                //language=json
                "{  \n" +
                        "   \"suppliers\":[  \n" +
                        "      {  \n" +
                        "         \"campaignId\":30011,\n" +
                        "         \"datasourceId\":11,\n" +
                        "         \"programStatus\": \"full\",\n" +
                        "         \"domain\":\"under_agency_allowed.ru\",\n" +
                        "         \"name\":\"under_agency_allowed\",\n" +
                        "         \"placementTypes\":[\"FULFILLMENT\",\"CROSSDOCK\"]" +
                        "      }\n" +
                        "   ]\n" +
                        "}"
        );
    }

    @Test
    @DisplayName("Доступ агентства. Поиск неудачный")
    void agencySearchFailed() {
        when(balanceContactService.getClientIdByUid(eq(100000L))).thenReturn(10000L);
        ResponseEntity<String> responseEntity = getSuppliersFullInfo(100000L, "not_allowed");
        JsonTestUtil.assertEquals(
                responseEntity,
                //language=json
                "{\"suppliers\": []}"
        );
    }

    private ResponseEntity<String> getSuppliersFullInfo(long uid, @Nullable String query) {
        String suppliersFullInfoUrl = SupplierUrlHelper.getSuppliersFullInfoURI(baseUrl, uid, query);
        return FunctionalTestHelper.get(suppliersFullInfoUrl);
    }
}
