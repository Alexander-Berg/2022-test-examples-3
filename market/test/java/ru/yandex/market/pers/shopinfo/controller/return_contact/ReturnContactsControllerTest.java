package ru.yandex.market.pers.shopinfo.controller.return_contact;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.pers.shopinfo.test.context.FunctionalTest;

@DbUnitDataSet(before = "returnContacts.csv")
class ReturnContactsControllerTest extends FunctionalTest {

    private static ResponseEntity<String> returnContactsInfo(String urlBasePrefix, String supplierId) {
        return FunctionalTestHelper.get(urlBasePrefix + "/returnContacts?supplier-id" + "=" + supplierId);
    }

    @Test
    @DisplayName("GET /returnContacts")
    void testSupplierInfoSuccess() throws JSONException {
        final ResponseEntity<String> response = returnContactsInfo(urlBasePrefix, "123");
        JSONAssert.assertEquals("[\n" +
                "  {\n" +
                "    \"supplierId\": \"123\",\n" +
                "    \"email\": \"'info@company.ru'\",\n" +
                "    \"firstName\": \"'Иван'\",\n" +
                "    \"middleName\": \"'Иванович'\",\n" +
                "    \"phoneNumber\": \"'+79010010101'\",\n" +
                "    \"lastName\": \"'Иванов'\",\n" +
                "    \"comments\": \"\",\n" +
                "    \"jobPosition\": \"\",\n" +
                "    \"companyName\": \"\",\n" +
                "    \"address\": \"\",\n" +
                "    \"type\": \"'PERSON'\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"supplierId\": \"123\",\n" +
                "    \"email\": \"'info@company.ru'\",\n" +
                "    \"firstName\": \"\",\n" +
                "    \"middleName\": \"\",\n" +
                "    \"phoneNumber\": \"\",\n" +
                "    \"lastName\": \"\",\n" +
                "    \"comments\": \"\",\n" +
                "    \"jobPosition\": \"\",\n" +
                "    \"companyName\": \"Иванов Иван Иванович\",\n" +
                "    \"address\": \"Москва, ул. Радио, д.4\",\n" +
                "    \"type\": \"'POST'\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"supplierId\": \"123\",\n" +
                "    \"email\": \"\",\n" +
                "    \"firstName\": \"'Рулон'\",\n" +
                "    \"middleName\": \"'Иванович'\",\n" +
                "    \"phoneNumber\": \"'+74950010102'\",\n" +
                "    \"lastName\": \"'Обоев'\",\n" +
                "    \"comments\": \"\",\n" +
                "    \"jobPosition\": \"Курьер\",\n" +
                "    \"companyName\": \"\",\n" +
                "    \"address\": \"Москва, ул. Радио, д.4\",\n" +
                "    \"type\": \"'CARRIER'\",\n" +
                "  },\n" +
                "  {\n" +
                "    \"supplierId\": \"123\",\n" +
                "    \"email\": \"\",\n" +
                "    \"firstName\": \"'Билл'\",\n" +
                "    \"middleName\": \"\",\n" +
                "    \"phoneNumber\": \"'+79010010103'\",\n" +
                "    \"lastName\": \"'Гейтс'\",\n" +
                "    \"comments\": \"Привозить с часу до двух, строго после обеда и до сончаса\",\n" +
                "    \"jobPosition\": \"Приемщик\",\n" +
                "    \"companyName\": \"\",\n" +
                "    \"address\": \"Москва, ул. Радио, д.4\",\n" +
                "    \"type\": \"'SELF'\",\n" +
                "  }\n" +
                "]", response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("GET /returnContacts. Контактов возврата не существует.")
    void testSupplierInfoNotFound() throws JSONException {
        final ResponseEntity<String> response = returnContactsInfo(urlBasePrefix, "404");
        JSONAssert.assertEquals("[]", response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("GET /returnContacts. В кэш попадают живые DBS.")
    void testAliveDbs() {
        final ResponseEntity<String> response = returnContactsInfo(urlBasePrefix, "201");
        JSONAssert.assertEquals("[\n" +
                "  {\n" +
                "    \"supplierId\": \"201\",\n" +
                "    \"email\": \"dbs@yandex.ru\",\n" +
                "    \"firstName\": \"Дбс\",\n" +
                "    \"middleName\": \"\",\n" +
                "    \"phoneNumber\": \"+79161002031\",\n" +
                "    \"lastName\": \"Живой\",\n" +
                "    \"comments\": \"\",\n" +
                "    \"jobPosition\": \"\",\n" +
                "    \"companyName\": \"ДБС ПЛЮС\",\n" +
                "    \"address\": \"Москва, дома\",\n" +
                "    \"type\": \"POST\"\n" +
                "  }" +
                "]", response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("GET /returnContacts. В кэш не попадают мертвые не активированные DBS.")
    void testDeadDbs() {
        final ResponseEntity<String> response = returnContactsInfo(urlBasePrefix, "202");
        JSONAssert.assertEquals("[]", response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }
}

