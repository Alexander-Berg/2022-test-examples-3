package ru.yandex.market.vendors.analytics.platform.regent.veil_price_index;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.regent.AbstractRegentTest;

import java.util.Map;

public class RegentVeilPriceIndexTest extends AbstractRegentTest {
    private static final String preset = "VEIL_PRICE_INDEX";

    @Test
    @ClickhouseDbUnitDataSet(before = "veil_price_index.before.csv")
    public void getData() throws JsonProcessingException {
        Map<String, Object> map = Map.of(
                "partnerId", 0,
                "partnerType", "BUSINESS"
        );
        var res = MAPPER.writeValueAsString(regentFacade.generate(makeRequest(preset, map, false)));
        String expected = loadFromFile("testGetData.json");
        JsonTestUtil.assertEquals(expected, res);
    }

    @Test
    @ClickhouseDbUnitDataSet(before = "veil_price_index.before.csv")
    public void getForEmpty() throws JsonProcessingException {
        Map<String, Object> map = Map.of(
                "partnerId", 123,
                "partnerType", "BUSINESS"
        );
        var res = MAPPER.writeValueAsString(regentFacade.generate(makeRequest(preset, map, false)));
        String expected = loadFromFile("testGetEmpty.json");
        JsonTestUtil.assertEquals(expected, res);
    }
}
