package ru.yandex.market.partner.mvc.controller.shipping;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Unit тесты для {@link ShowAllDeliveryServicesController}.
 */
@DbUnitDataSet(before = "database/ShowAllDeliveryServicesControllerTest.before.csv")
class ShowAllDeliveryServicesControllerTest extends FunctionalTest {

    @Test
    void testDeliveryServiceAllShop() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/delivery-services/all?id={campaignId}&from_outlets=true&format=json", 101L);
        JsonTestUtil.assertEquals(response, getClass(), "json/delivery_services_all_by_shop.json");
    }

    @Test
    void testDeliveryServiceAllSupplier() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/delivery-services/all?id={campaignId}&format=json", 201L);
        JsonTestUtil.assertEquals(response, getClass(), "json/delivery_services_all_by_supplier.json");
    }

    @Test
    void testDeliveryServiceAllFilterByIds() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/delivery-services/all?format=json&ids=123", 101L);
        JsonTestUtil.assertEquals(response, getClass(), "json/delivery_services_all_by_ids.json");
    }

    @Test
    void testDeliveryServiceAllFilterByFavorites() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/delivery-services/all?format=json&from_favorites=true", 101L);
        JsonTestUtil.assertEquals(response, getClass(), "json/delivery_services_only_favorites.json");
    }

}
