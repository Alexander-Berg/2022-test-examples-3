package ru.yandex.market.forecastint.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.forecastint.repository.postgres.SalesRepository;
import ru.yandex.market.forecastint.service.forecast.sales.SalesService;
import ru.yandex.market.forecastint.utils.TestUtils;
import ru.yandex.mj.generated.client.forecast_int.api.ForecastTreeSskuApiClient;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastTimeInterval;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastTreeSskuData;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastTreeSskuRequest;
import ru.yandex.mj.generated.client.forecast_int.model.TimeIntervalEdge;
import ru.yandex.mj.generated.client.forecast_int.model.TimeIntervalType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ForecastTreeSskuApiServiceTest extends AbstractFunctionalTest {

    @Autowired
    private ForecastTreeSskuApiClient client;

    @Autowired
    private SalesService salesService;

    @Autowired
    private SalesRepository salesRepository;

    @Test
    @DbUnitDataSet(before = "ForecastTreeSskuApiServiceTest.before.csv")
    void test() {
        TestUtils.setMockedTimeServiceWithNowDateTime(salesService,
                LocalDateTime.of(2022, 1, 12, 0, 0, 0));
        salesRepository.refreshSalesMonthsView();
        final ForecastTreeSskuRequest request = new ForecastTreeSskuRequest()
                .pageSize(10)
                .pageNumber(2)
                .interval(new ForecastTimeInterval()
                        .from(new TimeIntervalEdge().date(LocalDate.of(2021, 12, 28)))
                        .to(new TimeIntervalEdge().date(LocalDate.of(2021, 12, 28)))
                        .type(TimeIntervalType.DAY))
                .categoryIds(List.of(1L));
        final List<ForecastTreeSskuData> result = client.apiV1ForecastTreeSskuPost(request).schedule().join();
        assertEquals(2, result.size());
        final ForecastTreeSskuData data = result.get(0);
        assertEquals(1L, data.getSupplierId());
        assertEquals("1.1", data.getSsku());
        assertEquals("title1", data.getTitle());
        assertEquals(1L, data.getMsku());
        assertEquals("title1", data.getSskuTitle());
        assertEquals("REGULAR", data.getStatus());
        assertEquals("1", data.getSupplierName());
        assertEquals(1., data.getBaselineForecast(), 1E-6);
        assertEquals(2., data.getCorrectedForecast(), 1E-6);
        assertEquals(3., data.getBaselineGrossForecast(), 1E-6);
        assertEquals(4., data.getCorrectedGrossForecast(), 1E-6);
        assertEquals(2.5, data.getPriceOnSite(), 1E-6);
        assertEquals(2., data.getSales(), 1E-6);
        assertEquals(2., data.getAvgSales(), 1E-6);
        assertEquals(2., data.getSalesPrice(), 1E-6);
        assertEquals(2., data.getSalesAvgPrice(), 1E-6);
    }
}
