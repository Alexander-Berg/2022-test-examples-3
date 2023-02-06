package ru.yandex.market.wms.receiving.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.exception.TableNotConfiguredException;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

public class LocServiceTest extends ReceivingIntegrationTest {

    @Autowired
    private LocService locService;

    @Test
    @DatabaseSetup("/service/inbound/db/loc-unconfigured-table.xml")
    void resolveVghLocWithUnconfiguredTable() {
        Assertions.assertThrows(TableNotConfiguredException.class, () ->
                locService.resolveVghLoc("STAGE22")
        );
    }
}
