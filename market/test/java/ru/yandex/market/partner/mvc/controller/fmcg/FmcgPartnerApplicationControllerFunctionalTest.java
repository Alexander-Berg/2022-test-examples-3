package ru.yandex.market.partner.mvc.controller.fmcg;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@DbUnitDataSet(before = "data/csv/FmcgControllerFunctionalTest.before.csv")
class FmcgPartnerApplicationControllerFunctionalTest extends FunctionalTest {
    private static final long DATASOURCE_ID = 774;

    /**
     * Тест проверяет создание магазином новой заявки.
     */
    @Test
    @DbUnitDataSet(after = "data/csv/newApplicationCreationTest.after.csv")
    void newApplicationCreationTest() {
        ResponseEntity<String> response = sendRequestFromFile(
                getPostEditUrl(DATASOURCE_ID),
                "data/json/create-application-request.json"
        );
        JsonTestUtil.assertEquals(response, getClass(), "data/json/create-application-response.json");
    }

    /**
     * Тест проверяет обновление существующей заявки магазина.
     */
    @Test
    @DbUnitDataSet(before = "data/csv/applicationUpdateTest.before.csv",
            after = "data/csv/applicationUpdateTest.after.csv")
    void applicationUpdateTest() {
        ResponseEntity<String> response = sendRequestFromFile(
                getPostEditUrl(DATASOURCE_ID),
                "data/json/update-application-request.json"
        );
        JsonTestUtil.assertEquals(response, getClass(), "data/json/update-application-response.json");
    }

    /**
     * Тест проверяет, что если у магазина есть активная заявка, то она вернется по ручке {@code GET /fmcg/application}.
     */
    @Test
    @DbUnitDataSet(before = "data/csv/getApplicationTest.before.csv")
    void getApplicationTest() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getGetApplicationUrl(DATASOURCE_ID));
        JsonTestUtil.assertEquals(response, getClass(), "data/json/get-application-response.json");
    }

    /**
     * Тест проверяет, что по ручке {@code GET /fmcg/application} возвращается пустая форма,
     * если у магазина еще нет активной заявки.
     */
    @Test
    void getEmptyApplicationTest() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getGetApplicationUrl(DATASOURCE_ID));
        JsonTestUtil.assertEquals(response, getClass(), "data/json/get-empty-application-response.json");
    }

    private ResponseEntity<String> sendRequestFromFile(final String url, final String filename) {
        final HttpEntity request = JsonTestUtil.getJsonHttpEntity(getClass(), filename);
        return FunctionalTestHelper.post(url, request);
    }

    private String getPostEditUrl(long datasourceId) {
        return baseUrl + "/fmcg/application/edits?datasource_id=" + datasourceId;
    }

    private String getGetApplicationUrl(long datasourceId) {
        return baseUrl + "/fmcg/application?datasource_id=" + datasourceId;
    }
}