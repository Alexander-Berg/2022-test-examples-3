package ru.yandex.market.vendors.analytics.platform.controller.external.unfulfilled_demand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.util.UriComponentsBuilder;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.model.common.language.Language;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

@DbUnitDataSet(before = "unfulfilledDemand.before.csv")
public class UnfulfilledDemandControllerTest extends FunctionalTest {
    private static final String UNFULFILLED_DEMAND_BASE_PATH = "/data/unfulfilledDemand";

    protected MockRestServiceServer mockRestServiceServer;

    @ClickhouseDbUnitDataSet(before = "unfulfilledDemand.ch.before.csv")
    @Test
    @DisplayName("Проверка получения данных отчета")
    public void getUnfulfilledDemand() {
        String body = "{\n" +
                        "  \"filters\": {\n" +
                        "    \"categories\": [\n" +
                        "    ],\n" +
                        "    \"departments\": [\n" +
                        "    ],\n" +
                        "    \"name\": \"\"\n" +
                        "  },\n" +
                        "  \"paging\": {\n" +
                        "    \"pageCount\": 0,\n" +
                        "    \"pageNumber\": 0,\n" +
                        "    \"rowsPerPage\": 10\n" +
                        "  },\n" +
                        "  \"sortOrder\": {\n" +
                        "    \"column\": \"model_name\",\n" +
                        "    \"order\": \"ASC\"\n" +
                        "  }\n" +
                        "}";

        String actual = getDemand(body);
        String expected = loadFromFile("unfulfilledDemand.response.json");
        JsonTestUtil.assertEquals(expected, actual);
    }

    private String getDemand(String body) {
        return FunctionalTestHelper.postForJson(getFullUrl("").toUriString(), body);
    }

    private UriComponentsBuilder getFullUrl(String urlSuffix) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path(UNFULFILLED_DEMAND_BASE_PATH)
                .path(urlSuffix)
                .queryParam("language", Language.RU);
    }
}
