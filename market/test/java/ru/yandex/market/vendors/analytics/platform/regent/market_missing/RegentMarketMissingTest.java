package ru.yandex.market.vendors.analytics.platform.regent.market_missing;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.dao.category.CategoryDao;
import ru.yandex.market.vendors.analytics.core.model.categories.CategoryInfo;
import ru.yandex.market.vendors.analytics.core.utils.DateUtils;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.regent.adapter.MarketSalesRegentAdapterController;
import ru.yandex.market.vendors.analytics.platform.regent.AbstractRegentTest;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;


@ClickhouseDbUnitDataSet(before = "marketMissing.before.csv")
public class RegentMarketMissingTest extends AbstractRegentTest {
    private static final String MARKET_MISSING_PRESET = "MARKET_MISSING";

    @Autowired
    MarketSalesRegentAdapterController adapterController;

    @MockBean(name = "clock")
    private Clock clock;

    @MockBean(name = "categoryDao")
    CategoryDao categoryDao;

    @BeforeEach
    void setUp() {
    }

    @Test
    public void getData() throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>(Map.of());
        var res = MAPPER.writeValueAsString(regentFacade.generate(makeRequest(MARKET_MISSING_PRESET, map, false)));
        String expected = loadFromFile("testGetData.json");
        JsonTestUtil.assertEquals(expected, res);
    }

    @Test
    public void getEmpty() throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>(Map.of("categories",123));
        var res = MAPPER.writeValueAsString(regentFacade.generate(makeRequest(MARKET_MISSING_PRESET, map, false)));
        String expected = loadFromFile("testGetEmpty.json");
        JsonTestUtil.assertEquals(expected, res);
    }

    @Test
    public void getWithFilters() throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>(Map.of(
                "categories",90000,
                "name","stna"
        ));
        var res = MAPPER.writeValueAsString(regentFacade.generate(makeRequest(MARKET_MISSING_PRESET, map, false)));
        String expected = loadFromFile("testGetForFilters.json");
        JsonTestUtil.assertEquals(expected, res);
    }
}
