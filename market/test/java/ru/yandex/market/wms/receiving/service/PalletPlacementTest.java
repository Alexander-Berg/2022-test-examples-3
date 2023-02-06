package ru.yandex.market.wms.receiving.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.exception.NotFoundException;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.service.cte.PalletPlacementService;

public class PalletPlacementTest extends ReceivingIntegrationTest {

    @Autowired
    private PalletPlacementService palletPlacementService;

    @Test
    @DatabaseSetup("/service/pallet-placement/placepallethappypath/before.xml")
    @ExpectedDatabase(value = "/service/pallet-placement/placepallethappypath/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void placePalletHappyPath() {
        palletPlacementService.placePallet("PLT0000001", "LOC01");
    }

    @Test
    @DatabaseSetup("/service/pallet-placement/regular-id/before.xml")
    @ExpectedDatabase(value = "/service/pallet-placement/regular-id/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void placePalletWithExistsRegularId() {
        palletPlacementService.placePallet("CONTAINER02", "LOC01");
    }

    @Test
    @DatabaseSetup("/service/pallet-placement/not-found-id/before.xml")
    @ExpectedDatabase(value = "/service/pallet-placement/not-found-id/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void placePalletWithNotExistsId() {
        Assertions.assertThrows(NotFoundException.class,
                () -> palletPlacementService.placePallet("NOT_FOUND", "LOC01"));
    }

    @Test
    @DatabaseSetup("/service/pallet-placement/no-serial/before.xml")
    @ExpectedDatabase(value = "/service/pallet-placement/no-serial/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void placePalletWithNoUit() {
        palletPlacementService.placePallet("CONTAINER02", "LOC01");
    }
}
