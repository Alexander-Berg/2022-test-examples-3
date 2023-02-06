package ru.yandex.market.partner.mvc.controller.param;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

public class LegacyDatasourceParamControllerFunctionalTest extends FunctionalTest {
    @ParameterizedTest
    @DisplayName("Получение значений всех параметров магазина. Тест ручки /getDatasourceParams" +
            "Получение значений популярных параметров магазина. Тест ручки /getPopularDatasourceParams")
    @DbUnitDataSet(before = "LegacyDatasourceParamController_getDatasourceParams.before.csv")
    @CsvSource({"getPopularDatasourceParams", "getDatasourceParams"})
    void testGetDatasourceParams(String method) {
        long campaignId = 10666;
        ResponseEntity<String> response = FunctionalTestHelper.get(urlGetDatasourceParams(method, campaignId));

        JSONAssert.assertEquals(StringTestUtil.getString(this.getClass(),
                "LegacyDatasourceParamController_" + method + ".json"),
                JsonTestUtil.parseJson(response.getBody()).getAsJsonObject()
                        .get("result")
                        .toString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    private String urlGetDatasourceParams(String method, long campaignId) {
        return baseUrl + "/" + method + "?_user_id=123&format=json&id=" + campaignId;
    }
}
