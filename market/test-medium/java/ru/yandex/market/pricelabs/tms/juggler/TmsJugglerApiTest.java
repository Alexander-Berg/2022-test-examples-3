package ru.yandex.market.pricelabs.tms.juggler;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.NewOfferGen;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.pricelabs.tms.juggler.CoreJugglerApiTest.OK;

class TmsJugglerApiTest extends AbstractTmsSpringConfiguration {

    @Autowired
    private TmsJugglerApi tmsJugglerApi;

    private YtSourceTargetScenarioExecutor<NewOfferGen, Offer> offersExecutor;

    @BeforeEach
    void init() {
        this.offersExecutor = executors.offersGen();
    }

    @Test
    void testCheckYtSourceFail() {
        offersExecutor.removeSourcePrefix();
        assertThrows(CompletionException.class, () -> tmsJugglerApi.checkYtSource());
    }

    @Test
    void testCheckYtSource() {
        offersExecutor.removeSourceTables();

        executors.setSourceOffersTable("recent");
        offersExecutor.createSourceTable();
        assertEquals(OK, tmsJugglerApi.checkYtSource());
    }

}
