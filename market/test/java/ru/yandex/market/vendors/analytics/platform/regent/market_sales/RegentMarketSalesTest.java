package ru.yandex.market.vendors.analytics.platform.regent.market_sales;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.common.paging.PageRequestDTO;
import ru.yandex.market.vendors.analytics.core.dao.category.CategoryDao;
import ru.yandex.market.vendors.analytics.core.model.categories.CategoryInfo;
import ru.yandex.market.vendors.analytics.core.regent.dto.RegentRequestDto;
import ru.yandex.market.vendors.analytics.core.utils.DateUtils;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.regent.adapter.MarketSalesRegentAdapterController;
import ru.yandex.market.vendors.analytics.platform.regent.AbstractRegentTest;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ClickhouseDbUnitDataSet(before = "marketSales.before.csv")
public class RegentMarketSalesTest extends AbstractRegentTest {
    @Autowired
    MarketSalesRegentAdapterController adapterController;

    @MockBean(name = "clock")
    private Clock clock;

    @MockBean(name = "categoryDao")
    CategoryDao categoryDao;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(DateUtils.toInstant(LocalDate.of(2022, 2, 28)));
        when(categoryDao.getCategoriesInfo(ArgumentMatchers.eq(List.of(90000L,91000L)))).thenReturn(List.of(
                new CategoryInfo(90000,0,"hid","hid", "type"),
                new CategoryInfo(91000,0,"hid","hid", "type")
        ));

        when(categoryDao.getCategoryNamesByHids(any())).thenReturn(Map.of(90000L, "hid1", 91000L,"hid2"));
    }

    @Test
    public void getData() throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>(Map.of(
                "partners", 1,
                "category", 90000
        ));
        var res = MAPPER.writeValueAsString(adapterController.plot(map));
        String expected = loadFromFile("testGetData.json");
        JsonTestUtil.assertEquals(expected, res);
    }

    @Test
    public void getDataClipped() throws JsonProcessingException {
        when(clock.instant()).thenReturn(DateUtils.toInstant(LocalDate.of(2022, 2, 5)));
        Map<String, Object> map = new HashMap<>(Map.of(
                "partners", 1,
                "category", 90000
        ));
        var res = MAPPER.writeValueAsString(adapterController.plot(map));
        String expected = loadFromFile("testGetDataClipped.json");
        JsonTestUtil.assertEquals(expected, res);
    }

    @Test
    public void getDataNotClipped() throws JsonProcessingException {
        when(clock.instant()).thenReturn(DateUtils.toInstant(LocalDate.of(2022, 2, 4)));
        Map<String, Object> map = new HashMap<>(Map.of(
                "partners", 1,
                "category", 90000
        ));
        var res = MAPPER.writeValueAsString(adapterController.plot(map));
        String expected = loadFromFile("testGetDataNotClipped.json");
        JsonTestUtil.assertEquals(expected, res);
    }


    @Test
    public void getEmpty() throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>(Map.of(
                "partners", 123,
                "category", 90000
        ));
        var res = MAPPER.writeValueAsString(adapterController.plot(map));
        String expected = loadFromFile("testGetEmpty.json");
        JsonTestUtil.assertEquals(expected, res);
    }

    @Test
    public void getCategories() throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>(Map.of("partners", 1));
        var res = MAPPER.writeValueAsString(adapterController.getCategories(map));
        String expected = loadFromFile("testGetCategories.json");
        JsonTestUtil.assertEquals(expected, res);
    }

    @Test
    public void getTableView() throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>(Map.of("partners", 1));
        var res = MAPPER.writeValueAsString(regentFacade.generate(makeRequest("MARKET_SALES_TABLE", map, false)));
        String expected = loadFromFile("testGetTable.json");
        JsonTestUtil.assertEquals(expected, res);
    }

    @Test
    public void getTableViewPaged() throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>(Map.of("partners", 1));
        var res = MAPPER.writeValueAsString(regentFacade.generate(
                new RegentRequestDto(
                        "MARKET_SALES_TABLE",
                        null,
                        new PageRequestDTO(1,1),
                        false,
                        map,
                        false,
                        false
                )
        ));
        String expected = loadFromFile("testGetTablePaged.json");
        JsonTestUtil.assertEquals(expected, res);
    }
}
