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
import ru.yandex.mj.generated.client.forecast_int.api.ForecastTreeCategoriesApiClient;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastTimeInterval;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastTreeCategoriesRequest;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastTreeCategoryData;
import ru.yandex.mj.generated.client.forecast_int.model.TimeIntervalEdge;
import ru.yandex.mj.generated.client.forecast_int.model.TimeIntervalType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ForecastTreeCategoriesApiServiceTest extends AbstractFunctionalTest {

    @Autowired
    private ForecastTreeCategoriesApiClient client;

    @Autowired
    private SalesService salesService;

    @Autowired
    private SalesRepository salesRepository;

    @Test
    @DbUnitDataSet(before = "ForecastTreeCategoriesApiServiceTest.before.csv")
    void test() {
        TestUtils.setMockedTimeServiceWithNowDateTime(salesService,
                LocalDateTime.of(2022, 1, 12, 0, 0, 0));
        salesRepository.refreshSalesMonthsView();
        final ForecastTreeCategoriesRequest request = new ForecastTreeCategoriesRequest()
                .categoryIds(List.of(3L, 2L))
                .interval(new ForecastTimeInterval()
                        .from(new TimeIntervalEdge().date(LocalDate.of(2021, 12, 28)))
                        .to(new TimeIntervalEdge().date(LocalDate.of(2021, 12, 28)))
                        .type(TimeIntervalType.DAY));
        final List<ForecastTreeCategoryData> result =
                client.apiV1ForecastTreeCategoriesPost(request).schedule().join();
        assertEquals(1, result.size());
        final ForecastTreeCategoryData data = result.get(0);
        assertEquals(3L, data.getCategoryId());
        assertEquals(2., data.getBaselineForecast(), 1E-6);
        assertEquals(4., data.getCorrectedForecast(), 1E-6);
        assertEquals(6., data.getBaselineGrossForecast(), 1E-6);
        assertEquals(8., data.getCorrectedGrossForecast(), 1E-6);
        assertEquals(4., data.getSales(), 1E-6);
        assertEquals(4., data.getAvgSales(), 1E-6);
        assertEquals(4., data.getSalesPrice(), 1E-6);
        assertEquals(4., data.getAvgSalesPrice(), 1E-6);
    }

}
