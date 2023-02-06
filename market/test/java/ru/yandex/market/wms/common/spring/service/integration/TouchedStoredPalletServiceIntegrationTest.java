package ru.yandex.market.wms.common.spring.service.integration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.service.TouchedStoredPalletService;

public class TouchedStoredPalletServiceIntegrationTest extends IntegrationTest {

    private static final Instant EDIT_DATE_FROM =
            LocalDate.of(2021, Month.JANUARY, 1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();

    @Autowired
    private TouchedStoredPalletService touchedStoredPalletService;

    @Test
    @DatabaseSetup("/db/service/touched-stored-pallets/update-after-edit-date/before.xml")
    @ExpectedDatabase(value = "/db/service/touched-stored-pallets/update-after-edit-date/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void updateFromItrnOnlyAfterEditDate() {
        touchedStoredPalletService.updateFromItrn(EDIT_DATE_FROM);
    }

    @Test
    @DatabaseSetup("/db/service/touched-stored-pallets/update-allowed-zones/before.xml")
    @ExpectedDatabase(value = "/db/service/touched-stored-pallets/update-allowed-zones/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void updateFromItrnOnlyAllowedPalletZones() {
        touchedStoredPalletService.updateFromItrn(EDIT_DATE_FROM);
    }

    @Test
    @DatabaseSetup("/db/service/touched-stored-pallets/not-update-allowed-zones-received-qty/before.xml")
    @ExpectedDatabase(value = "/db/service/touched-stored-pallets/not-update-allowed-zones-received-qty/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void notUpdateWhenMovedSameQtyAsReceived() {
        touchedStoredPalletService.updateFromItrn(EDIT_DATE_FROM);
    }

    @Test
    @DatabaseSetup("/db/service/touched-stored-pallets/update-with-existing-touched-pallets/before.xml")
    @ExpectedDatabase(value = "/db/service/touched-stored-pallets/update-with-existing-touched-pallets/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void updateFromItrnWithExistingTouchedPallets() {
        touchedStoredPalletService.updateFromItrn(EDIT_DATE_FROM);
    }

    @Test
    @DatabaseSetup("/db/service/touched-stored-pallets/consider-only-zone-config/before.xml")
    @ExpectedDatabase(value = "/db/service/touched-stored-pallets/consider-only-zone-config/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void updateFromItrnOnlyConsideringZoneConfig() {
        touchedStoredPalletService.updateFromItrn(EDIT_DATE_FROM);
    }
}
