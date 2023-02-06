package ru.yandex.market.wms.common.spring.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.FillingStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;

public class EmptyToteServiceTest extends IntegrationTest {

    @Autowired
    private EmptyToteService emptyToteService;

    @Test
    @DatabaseSetup("/db/service/empty-tote/before.xml")
    @ExpectedDatabase(value = "/db/service/empty-tote/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void updateToteOnProcessDoesntUpdateItTwice() {
        final String containerId = "TM12345";
        emptyToteService.updateToteOnProcess(FillingStatus.PICKING, containerId);
        emptyToteService.updateToteOnProcess(FillingStatus.PICKING, containerId);
        emptyToteService.updateToteOnProcess(FillingStatus.PICKING, containerId);
    }
}
