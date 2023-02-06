package ru.yandex.market.forecastint.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.forecastint.repository.postgres.SalesRepository;
import ru.yandex.market.forecastint.service.forecast.sales.SalesServiceImpl;
import ru.yandex.market.forecastint.utils.TestUtils;
import ru.yandex.mj.generated.client.forecast_int.api.ForecastPlotApiClient;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastPlotRequest;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastPlotRequestYAxe;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastTimeInterval;
import ru.yandex.mj.generated.client.forecast_int.model.ItemsType;
import ru.yandex.mj.generated.client.forecast_int.model.Plot;
import ru.yandex.mj.generated.client.forecast_int.model.PlotType;
import ru.yandex.mj.generated.client.forecast_int.model.TimeIntervalEdge;
import ru.yandex.mj.generated.client.forecast_int.model.TimeIntervalType;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ForecastPlotApiServiceTest extends AbstractFunctionalTest {

    @Autowired
    private ForecastPlotApiClient forecastPlotApiClient;

    @Autowired
    private SalesServiceImpl salesService;

    @Autowired
    private SalesRepository salesRepository;

    @Test
    @DbUnitDataSet(before = "ForecastPlotApiServiceTest.before.csv")
    void test() {
        TestUtils.setMockedTimeServiceWithNowDateTime(salesService,
                LocalDateTime.of(2022, 1, 12, 0, 0, 0));
        salesRepository.refreshSalesMonthsView();
        final ForecastPlotRequest request = new ForecastPlotRequest()
                .plotTypes(Arrays
                        .asList(PlotType.CORRECTED_FORECAST,
                                PlotType.BASELINE_FORECAST,
                                PlotType.AVG_SALES,
                                PlotType.FACT_SALES))
                .supplierId(1L)
                .xAxe(new ForecastTimeInterval()
                        .from(new TimeIntervalEdge().date(LocalDate.of(2021, 12, 18)))
                        .to(new TimeIntervalEdge().date(LocalDate.of(2022, 2, 2)))
                        .type(TimeIntervalType.DAY))
                .yAxe(new ForecastPlotRequestYAxe().type(ItemsType.ITEMS));
        final List<Plot> result = forecastPlotApiClient.apiV1ForecastPlotPost(request).schedule().join();
        assertEquals(4, result.size());
        assertEquals(47, result.get(0).getData().size());
        assertEquals(47, result.get(1).getData().size());
        assertEquals(47, result.get(2).getData().size());
        assertEquals(47, result.get(3).getData().size());
    }

    @Test
    @DbUnitDataSet(before = "ForecastPlotApiServiceTest.before.csv")
    void testWeeks() {
        TestUtils.setMockedTimeServiceWithNowDateTime(salesService,
                LocalDateTime.of(2022, 1, 12, 0, 0, 0));
        salesRepository.refreshSalesMonthsView();
        final ForecastPlotRequest request = new ForecastPlotRequest()
                .plotTypes(Arrays
                        .asList(PlotType.CORRECTED_FORECAST,
                                PlotType.BASELINE_FORECAST,
                                PlotType.AVG_SALES,
                                PlotType.FACT_SALES))
                .supplierId(1L)
                .xAxe(new ForecastTimeInterval()
                        .from(new TimeIntervalEdge().date(LocalDate.of(2021, 12, 27)))
                        .to(new TimeIntervalEdge().date(LocalDate.of(2022, 1, 10)))
                        .type(TimeIntervalType.WEEK))
                .yAxe(new ForecastPlotRequestYAxe().type(ItemsType.ITEMS));
        final List<Plot> result = forecastPlotApiClient.apiV1ForecastPlotPost(request).schedule().join();
        assertEquals(4, result.size());
        assertEquals(3, result.get(0).getData().size());
        assertEquals(3, result.get(1).getData().size());
        assertEquals(3, result.get(2).getData().size());
        assertEquals(3, result.get(3).getData().size());
    }
}
