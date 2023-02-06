package ru.yandex.market.partner.mvc.controller.supplier.promo.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import Market.DataCamp.DataCampPromo;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.datacamp.DataCampUtil;
import ru.yandex.market.core.supplier.promo.dto.PromocodeApplyingTypeDto;
import ru.yandex.market.core.supplier.promo.dto.PromocodeCreationRequestDto;
import ru.yandex.market.core.supplier.promo.dto.PromocodeDiscountTypeDto;
import ru.yandex.market.core.supplier.promo.mapper.PromocodeDescriptionMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromoDescriptionMapperTest {

    @Test
    void createPartnerPromocodeDescriptionFromRequest_mainFieldsMappingTest() {
        long supplierId = 1001L;
        long businessId = 1111L;
        String promoId = "test_promo_id";
        String promocode = "AT2SG8I35SF2H47D";
        LocalDateTime startDate = LocalDateTime.of(2025, 8, 25, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 8, 26, 23, 59, 59);
        Integer discountValue = 20;
        PromocodeDiscountTypeDto discountType = PromocodeDiscountTypeDto.PERCENTAGE;
        BigDecimal budgetLimit = BigDecimal.valueOf(20000);
        PromocodeApplyingTypeDto applyingType = PromocodeApplyingTypeDto.ONE_TIME;

        PromocodeCreationRequestDto request = new PromocodeCreationRequestDto(
                promocode,
                startDate,
                endDate,
                discountValue,
                discountType,
                budgetLimit,
                applyingType,
                "validationId"
        );
        DataCampPromo.PromoDescription promoDescription =
                PromocodeDescriptionMapper.createPartnerPromocodeDescriptionFromRequest(
                        supplierId,
                        businessId,
                        promoId,
                        request
                );

        assertNotNull(promoDescription.getPrimaryKey());
        assertEquals(businessId, promoDescription.getPrimaryKey().getBusinessId());
        assertEquals(NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE, promoDescription.getPrimaryKey().getSource());
        assertEquals(promoId, promoDescription.getPrimaryKey().getPromoId());

        assertNotNull(promoDescription.getPromoGeneralInfo());
        assertEquals(DataCampPromo.PromoType.MARKET_PROMOCODE, promoDescription.getPromoGeneralInfo().getPromoType());

        assertNotNull(promoDescription.getConstraints());
        assertEquals(1, promoDescription.getConstraints().getOffersMatchingRulesCount());
        assertNotNull(promoDescription.getConstraints().getOffersMatchingRules(0).getSupplierRestriction());
        assertNotNull(promoDescription.getConstraints().getOffersMatchingRules(0).getSupplierRestriction().getSuppliers());
        assertEquals(1, promoDescription.getConstraints().getOffersMatchingRules(0).getSupplierRestriction().getSuppliers().getIdCount());
        assertEquals(supplierId, promoDescription.getConstraints().getOffersMatchingRules(0).getSupplierRestriction().getSuppliers().getId(0));
        assertTrue(promoDescription.getConstraints().getAllowMarketBonus());
        assertTrue(promoDescription.getConstraints().getAllowBluePromocode());
        assertTrue(promoDescription.getConstraints().getAllowCheapestAsGift());
        assertTrue(promoDescription.getConstraints().getAllowGenericBundle());
        assertTrue(promoDescription.getConstraints().getAllowBlueFlash());
        assertTrue(promoDescription.getConstraints().getAllowBlueSet());
        assertTrue(promoDescription.getConstraints().getEnabled());
        assertTrue(promoDescription.getConstraints().hasMoneyLimit());
        assertEquals(DataCampUtil.powToIdx(budgetLimit), promoDescription.getConstraints().getMoneyLimit().getPrice());

        assertNotNull(promoDescription.getMechanicsData());
        assertNotNull(promoDescription.getMechanicsData().getMarketPromocode());
        assertEquals(promocode, promoDescription.getMechanicsData().getMarketPromocode().getPromoCode());
        assertEquals(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.PERCENTAGE,
                promoDescription.getMechanicsData().getMarketPromocode().getDiscountType());
        assertEquals(DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType.ONE_TIME,
                promoDescription.getMechanicsData().getMarketPromocode().getApplyingType());
        assertEquals((long) discountValue, promoDescription.getMechanicsData().getMarketPromocode().getValue());
        assertFalse(promoDescription.getMechanicsData().getMarketPromocode().hasRatingRub());
    }
}
