package ru.yandex.market.adv.promo.mvc.promo.promo_id.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.mvc.promo.promo_id.dto.PartnerPromoIdDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_id.dto.PiPromoMechanicDto;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

class PromoIdGeneratorControllerTest extends FunctionalTest {

    @Test
    public void testCreatePromoId() {
        long partnerId = 1;
        PiPromoMechanicDto piPromoMechanicDto = PiPromoMechanicDto.CHEAPEST_AS_GIFT;

        ResponseEntity<PartnerPromoIdDto> response = generatePromoId(partnerId, piPromoMechanicDto);

        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assertions.assertNotNull(response.getBody());
        String actual = response.getBody().getPromoId();
        Assertions.assertTrue(actual.startsWith(partnerId + "_" + piPromoMechanicDto.getAbbreviation() + "_"));
    }

    private ResponseEntity<PartnerPromoIdDto> generatePromoId(long partnerId, PiPromoMechanicDto piPromoMechanicDto) {
        String url = "/partner/promo/generate-promo-id?partnerId={partnerId}&promoMechanic={mechanic}";
        return FunctionalTestHelper.get(baseUrl() + url, PartnerPromoIdDto.class, partnerId, piPromoMechanicDto);
    }
}
