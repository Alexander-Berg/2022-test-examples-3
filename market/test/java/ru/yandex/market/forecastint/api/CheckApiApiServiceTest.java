package ru.yandex.market.forecastint.api;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.forecastint.utils.TestUtils;
import ru.yandex.mj.generated.client.forecast_int.api.CheckApiApiClient;

public class CheckApiApiServiceTest extends AbstractFunctionalTest {

    @Autowired
    private CheckApiApiClient checkApiApiClient;

    @Autowired
    private CheckApiApiService checkApiApiService;

    @DbUnitDataSet(before = "checkApiApiServiceTest.before.csv")
    @Test
    void test() {
        TestUtils.setMockedTimeServiceWithNowDateTime(checkApiApiService,
                LocalDateTime.of(2021, 12, 14, 0, 0, 0));
        Assertions.assertEquals(
                "test",
                checkApiApiClient.apiCheckApiGet().schedule().join()
        );
    }

}
