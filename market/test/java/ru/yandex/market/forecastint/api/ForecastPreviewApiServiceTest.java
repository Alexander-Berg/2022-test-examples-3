package ru.yandex.market.forecastint.api;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.mj.generated.client.forecast_int.api.ForecastPreviewApiClient;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastCorrectionType;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastPreviewRequestItem;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastPreviewResponseItem;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastTimeInterval;
import ru.yandex.mj.generated.client.forecast_int.model.TimeIntervalEdge;
import ru.yandex.mj.generated.client.forecast_int.model.TimeIntervalType;

public class ForecastPreviewApiServiceTest extends AbstractFunctionalTest {

    @Autowired
    private ForecastPreviewApiClient client;

    @Test
    void test() {
        final List<ForecastPreviewResponseItem> result = client.apiV1ForecastPreviewPost(
                Collections.singletonList(
                        new ForecastPreviewRequestItem()
                                .msku(2L)
                                .supplierId(2L)
                                .forecastCorrectionType(ForecastCorrectionType.ABSOLUTE)
                                .forecastCorrectionValue(1.)
                                .interval(new ForecastTimeInterval()
                                        .type(TimeIntervalType.DAY)
                                        .from(new TimeIntervalEdge()
                                                .date(LocalDate.of(2022, 1, 8)))
                                        .to(new TimeIntervalEdge()
                                                .date(LocalDate.of(2022, 1, 8))))
                                .price(10.)
                )
        ).schedule().join();
        Assertions.assertNotNull(result);
    }

}
