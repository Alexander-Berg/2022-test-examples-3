package ru.yandex.market.mbi.api.controller.abo;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.config.FunctionalTest;

/**
 * @author komarovns
 */
public class AboPartnerControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(after = "AboPartnerControllerTest.testCreateSerpPartner.after.csv")
    public void testCreateSerpPartner() {
        mbiApiClient.createSerpPartner();
        mbiApiClient.createTurboPartner();
    }
}
