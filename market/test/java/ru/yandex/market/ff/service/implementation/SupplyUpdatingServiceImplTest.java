package ru.yandex.market.ff.service.implementation;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.service.RequestAcceptanceService;

public class SupplyUpdatingServiceImplTest extends IntegrationTest {

    @Autowired
    private RequestAcceptanceService requestAcceptanceService;

    @Test
    @DatabaseSetup("classpath:service/supply-updating-service/1/before.xml")
    @ExpectedDatabase(value = "classpath:service/supply-updating-service/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void successAcceptByService() {
        requestAcceptanceService.acceptByService(1L, "123");
    }
}
