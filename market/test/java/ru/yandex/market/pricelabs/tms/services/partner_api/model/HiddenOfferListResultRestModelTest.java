package ru.yandex.market.pricelabs.tms.services.partner_api.model;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class HiddenOfferListResultRestModelTest {

    @Test
    void parseHiddenOffers() {
        var model = Utils.fromJsonResource("tms/services/partner_api/model/hidden-offers.json",
                HiddenOfferListResultRestModel.class);
        assertNotNull(model);
        log.info("Model: {}", model);

        var expect = new HiddenOfferListResultRestModel();
        expect.setPaging(new PagingDirectionsDTO());
        expect.setHiddenOffers(List.of());

        assertEquals(expect, model);

    }

}
