package ru.yandex.market.delivery.transport_manager.service.health.product;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.controller.health.RegularXdocDCHealthController;

class RegularXdocDCHealthControllerTest extends AbstractContextualTest {

    private static final String OK = "0;OK";

    @Autowired
    private RegularXdocDCHealthController controller;

    @Test
    @DatabaseSetup("/repository/distribution_unit_center/with_creation_timestamp.xml")
    void testOutdatedPallets() {
        clock.setFixed(Instant.parse("2021-06-06T14:00:00.00Z"), ZoneOffset.UTC);
        String result = controller.checkForOutdatedPallets();
        softly.assertThat(result).isEqualTo(
            "2;Found 2 pallets that are stored in DC for more than 7 days. DC ids: [1, 3]"
        );
    }

    @Test
    @DatabaseSetup("/repository/distribution_unit_center/with_creation_timestamp.xml")
    void testNoOutdatedPallets() {
        clock.setFixed(Instant.parse("2021-05-30T14:00:00.00Z"), ZoneOffset.UTC);
        softly.assertThat(controller.checkForOutdatedPallets()).isEqualTo(OK);
    }

    @Test
    void testEmptyOutdatedPalletsOk() {
        softly.assertThat(controller.checkForOutdatedPallets()).isEqualTo(OK);
    }

    @Test
    @DatabaseSetup("/repository/distribution_unit_center/with_frozen_timestamp.xml")
    void testPalletsFrozenForTooLong() {
        clock.setFixed(Instant.parse("2021-05-31T01:00:00.00Z"), ZoneOffset.UTC);
        String result = controller.checkForNotSentPallets();
        softly.assertThat(result).isEqualTo(
            "2;Found 1 pallets that are frozen for more than 12 hours. DC ids: [1]"
        );
    }

    @Test
    @DatabaseSetup("/repository/distribution_unit_center/with_frozen_timestamp.xml")
    void testNoPalletsFrozenForTooLong() {
        clock.setFixed(Instant.parse("2021-05-29T18:00:00.00Z"), ZoneOffset.UTC);
        String result = controller.checkForNotSentPallets();
        softly.assertThat(result).isEqualTo(OK);
    }
}
