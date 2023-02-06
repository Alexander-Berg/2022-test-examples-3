package ru.yandex.market.forecastint.api;

import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.CompletionException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.mj.generated.client.forecast_int.api.ForecastCorrectionApiClient;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastCorrectionRequest;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastCorrectionRequestItem;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastCorrectionType;
import ru.yandex.mj.generated.client.forecast_int.model.ForecastTimeInterval;
import ru.yandex.mj.generated.client.forecast_int.model.TimeIntervalEdge;
import ru.yandex.mj.generated.client.forecast_int.model.TimeIntervalType;

public class ForecastCorrectionApiServiceTest extends AbstractFunctionalTest {

    @Autowired
    private ForecastCorrectionApiClient client;

    @Test
    @DbUnitDataSet(
            after = "ForecastCorrectionApiTest_testSave.after.csv",
            before = "ForecastCorrectionApiTest_testSave.before.csv"
    )
    public void testSave() {
        final ForecastCorrectionRequest request = new ForecastCorrectionRequest()
                .corrections(Collections.singletonList(
                        new ForecastCorrectionRequestItem()
                                .msku(2222L)
                                .supplierId(2L)
                                .warehouseId(300L)
                                .forecastCorrectionType(ForecastCorrectionType.ABSOLUTE)
                                .forecastCorrectionValue(1.12)
                                .interval(new ForecastTimeInterval()
                                        .type(TimeIntervalType.DAY)
                                        .from(new TimeIntervalEdge()
                                                .date(LocalDate.of(2022, 1, 8)))
                                        .to(new TimeIntervalEdge()
                                                .date(LocalDate.of(2022, 1, 9))))
                                .price(10.22d)
                ))
                .comment("test012")
                .dateFrom(LocalDate.of(2022, 1, 6))
                .dateTo(LocalDate.of(2022, 1, 19));
        client.apiV1ForecastCorrectPost(request).scheduleVoid().join();
    }

    @Test
    @DbUnitDataSet(
            before = "ForecastCorrectionApiTest_testSave.before.csv"
    )
    public void testSaveFailWithoutMsku() {
        final ForecastCorrectionRequest request = new ForecastCorrectionRequest()
                .corrections(Collections.singletonList(
                        new ForecastCorrectionRequestItem()
                                .supplierId(2L)
                                .warehouseId(300L)
                                .forecastCorrectionType(ForecastCorrectionType.ABSOLUTE)
                                .forecastCorrectionValue(1.)
                                .interval(new ForecastTimeInterval()
                                        .type(TimeIntervalType.DAY)
                                        .from(new TimeIntervalEdge()
                                                .date(LocalDate.of(2022, 1, 8)))
                                        .to(new TimeIntervalEdge()
                                                .date(LocalDate.of(2022, 1, 8))))
                                .price(10.)
                ))
                .comment("test012")
                .dateFrom(LocalDate.of(2022, 1, 6))
                .dateTo(LocalDate.of(2022, 1, 19));
        var call = client.apiV1ForecastCorrectPost(request).schedule();
        try {
            call.join();
            Assertions.fail("had no fail");
        } catch (CompletionException e) {
            Assertions.assertThat(e.getCause())
                    .isInstanceOf(CommonRetrofitHttpExecutionException.class)
                    .hasMessageStartingWith("HTTP 400,")
                    .hasMessageEndingWith("@ POST http://localhost:8080/api/v1/forecast/correct");
        }
    }

    @Test
    @DbUnitDataSet(
            before = "ForecastCorrectionApiTest_testSave.before.csv"
    )
    public void testSaveFailWithoutWarehouse() {
        final ForecastCorrectionRequest request = new ForecastCorrectionRequest()
                .corrections(Collections.singletonList(
                        new ForecastCorrectionRequestItem()
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
                ))
                .comment("test012")
                .dateFrom(LocalDate.of(2022, 1, 6))
                .dateTo(LocalDate.of(2022, 1, 19));
        var call = client.apiV1ForecastCorrectPost(request).schedule();
        try {
            call.join();
            Assertions.fail("had no fail");
        } catch (CompletionException e) {
            Assertions.assertThat(e.getCause())
                    .isInstanceOf(CommonRetrofitHttpExecutionException.class)
                    .hasMessageStartingWith("HTTP 400,")
                    .hasMessageEndingWith("@ POST http://localhost:8080/api/v1/forecast/correct");
        }
    }

    @Test
    @DbUnitDataSet(
            before = "ForecastCorrectionApiTest_testSave.before.csv"
    )
    public void testSaveFailWithoutSupplier() {
        final ForecastCorrectionRequest request = new ForecastCorrectionRequest()
                .corrections(Collections.singletonList(
                        new ForecastCorrectionRequestItem()
                                .msku(2L)
                                .warehouseId(300L)
                                .forecastCorrectionType(ForecastCorrectionType.ABSOLUTE)
                                .forecastCorrectionValue(1.)
                                .interval(new ForecastTimeInterval()
                                        .type(TimeIntervalType.DAY)
                                        .from(new TimeIntervalEdge()
                                                .date(LocalDate.of(2022, 1, 8)))
                                        .to(new TimeIntervalEdge()
                                                .date(LocalDate.of(2022, 1, 8))))
                                .price(10.)
                ))
                .comment("test012")
                .dateFrom(LocalDate.of(2022, 1, 6))
                .dateTo(LocalDate.of(2022, 1, 19));
        var call = client.apiV1ForecastCorrectPost(request).schedule();
        try {
            call.join();
            Assertions.fail("had no fail");
        } catch (CompletionException e) {
            Assertions.assertThat(e.getCause())
                    .isInstanceOf(CommonRetrofitHttpExecutionException.class)
                    .hasMessageStartingWith("HTTP 400,")
                    .hasMessageEndingWith("@ POST http://localhost:8080/api/v1/forecast/correct");
        }
    }
}
