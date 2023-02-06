package ru.yandex.market.adv.promo.mvc.promo.common.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampPromo;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.adv.promo.mvc.promo.promo_id.dto.PiPromoMechanicDto;
import ru.yandex.market.adv.promo.mvc.promo.common.dto.CategoryDiscountInfoDto;
import ru.yandex.market.adv.promo.mvc.promo.common.dto.PromoDescriptionDto;
import ru.yandex.market.adv.promo.mvc.promo.common.dto.PromocodeApplyingTypeDto;
import ru.yandex.market.adv.promo.mvc.promo.common.dto.PromocodeCoverageTypeDto;
import ru.yandex.market.adv.promo.mvc.promo.common.dto.PromocodeDiscountTypeDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.adv.promo.datacamp.utils.DateTimeUtils.getDateTimeInSeconds;
import static ru.yandex.market.adv.promo.utils.BlueFlashMechanicTestUtils.createAnaplanBlueFlashDescription;
import static ru.yandex.market.adv.promo.utils.CheapestAsGiftMechanicTestUtils.createAnaplanCheapestAsGiftDescription;
import static ru.yandex.market.adv.promo.utils.DirectDiscountMechanicTestUtils.createAnaplanDirectDiscountDescription;
import static ru.yandex.market.adv.promo.utils.PromoMechanicTestUtils.addUpdateTimeToPromoDescription;
import static ru.yandex.market.adv.promo.utils.PromocodeMechanicTestUtils.createAnaplanPromocodeDescription;
import static ru.yandex.market.adv.promo.utils.PromocodeMechanicTestUtils.createPartnerPromocodeDescription;

class PromoDescriptionMapperTest {

    @Test
    @DisplayName("Проверка маппинга партнерского промокода")
    void convertPartnerPromocodeFromPromosStorageDescriptionTest() {
        long partnerId = 1001L;
        long businessId = 1111L;
        LocalDateTime creationTime = LocalDateTime.of(2020, 1, 1, 12, 0);
        String promoId = "1001_AT2SG847D";
        String promocode = "AT2SG847D";
        String promoName = "Promocode AT2SG847D";
        long discountValue = 20000;
        DataCampPromo.PromoMechanics.MarketPromocode.DiscountType discountType =
                DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.VALUE;
        DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType applyingType =
                DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType.REUSABLE;
        BigDecimal budgetLimit = BigDecimal.valueOf(20000);

        DataCampPromo.PromoDescription promocodeDescription = createPartnerPromocodeDescription(
                businessId,
                promoId,
                promocode,
                promoName,
                discountValue,
                discountType,
                applyingType,
                budgetLimit
        );
        promocodeDescription = addUpdateTimeToPromoDescription(
                promocodeDescription,
                getDateTimeInSeconds(creationTime),
                0
        );
        PromoDescriptionDto promoDescriptionDto =
                PromoDescriptionMapper.convertFromPromosStorageDescription(partnerId, promocodeDescription, false);

        assertNotNull(promoDescriptionDto);
        assertEquals(promoId, promoDescriptionDto.getPromoId());
        assertEquals(promoName, promoDescriptionDto.getName());
        assertEquals(PiPromoMechanicDto.MARKET_PROMOCODE, promoDescriptionDto.getMechanic());
        assertEquals(creationTime, promoDescriptionDto.getCreationTime());
        assertTrue(promoDescriptionDto.isPartnerPromo());
        assertTrue(promoDescriptionDto.isEnabled());
        assertNull(promoDescriptionDto.getAnaplanPromoInfo());
        assertNull(promoDescriptionDto.getCheapestAsGiftInfo());
        assertNull(promoDescriptionDto.getBlueFlashInfo());
        assertNotNull(promoDescriptionDto.getPromocodeInfo());
        assertEquals(promocode, promoDescriptionDto.getPromocodeInfo().getPromocode());
        assertEquals(budgetLimit.toBigInteger(), promoDescriptionDto.getPromocodeInfo().getBudgetLimit().toBigInteger());
        assertEquals(PromocodeApplyingTypeDto.REUSABLE, promoDescriptionDto.getPromocodeInfo().getApplyingType());
        assertEquals(PromocodeDiscountTypeDto.VALUE, promoDescriptionDto.getPromocodeInfo().getDiscountType());
        assertEquals(discountValue, promoDescriptionDto.getPromocodeInfo().getDiscountValue());
        assertEquals(PromocodeCoverageTypeDto.PRODUCT, promoDescriptionDto.getPromocodeInfo().getCoverageType());
        assertNull(promoDescriptionDto.getPromocodeInfo().getCartMinPrice());
        assertNull(promoDescriptionDto.getPromocodeInfo().getOrderMaxPrice());
    }

    @Test
    @DisplayName("Проверка маппинга маркетплейсного промокода")
    void convertAnaplanPromocodeFromPromosStorageDescriptionTest() {
        long partnerId = 1001L;
        long businessId = 1111L;
        String promoId = "#12345";
        String promocode = "ANAPLANP";
        String promoName = "Promocode ANAPLANP";
        long discountValue = 15;
        DataCampPromo.PromoMechanics.MarketPromocode.DiscountType discountType =
                DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.PERCENTAGE;
        DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType applyingType =
                DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType.ONE_TIME;
        BigDecimal cartMinPrice = BigDecimal.valueOf(2000);
        BigDecimal orderMaxPrice = BigDecimal.valueOf(15000);

        DataCampPromo.PromoDescription promocodeDescription = createAnaplanPromocodeDescription(
                businessId,
                promoId,
                promocode,
                promoName,
                discountValue,
                discountType,
                applyingType,
                true,
                cartMinPrice,
                orderMaxPrice
        );
        PromoDescriptionDto promoDescriptionDto =
                PromoDescriptionMapper.convertFromPromosStorageDescription(partnerId, promocodeDescription, false);

        assertNotNull(promoDescriptionDto);
        assertEquals(promoId, promoDescriptionDto.getPromoId());
        assertEquals(promoName, promoDescriptionDto.getName());
        assertEquals(PiPromoMechanicDto.MARKET_PROMOCODE, promoDescriptionDto.getMechanic());
        assertFalse(promoDescriptionDto.isPartnerPromo());
        assertTrue(promoDescriptionDto.isEnabled());
        assertNull(promoDescriptionDto.getCheapestAsGiftInfo());
        assertNull(promoDescriptionDto.getBlueFlashInfo());
        assertNotNull(promoDescriptionDto.getAnaplanPromoInfo());
        assertNotNull(promoDescriptionDto.getPromocodeInfo());
        assertEquals(promocode, promoDescriptionDto.getPromocodeInfo().getPromocode());
        assertEquals(PromocodeApplyingTypeDto.ONE_TIME, promoDescriptionDto.getPromocodeInfo().getApplyingType());
        assertEquals(PromocodeDiscountTypeDto.PERCENTAGE, promoDescriptionDto.getPromocodeInfo().getDiscountType());
        assertEquals(discountValue, promoDescriptionDto.getPromocodeInfo().getDiscountValue());
        assertEquals(PromocodeCoverageTypeDto.CART, promoDescriptionDto.getPromocodeInfo().getCoverageType());
        assertEquals(cartMinPrice, promoDescriptionDto.getPromocodeInfo().getCartMinPrice());
        assertEquals(orderMaxPrice, promoDescriptionDto.getPromocodeInfo().getOrderMaxPrice());
    }

    @Test
    @DisplayName("Проверка маппинга маркетплейсного флеша")
    void convertAnaplanBlueFlashFromPromosStorageDescriptionTest() {
        long businessId = 1111L;
        long partnerId = 101010;
        String promoId = "#12345";
        String promoName = "Test promo";

        DataCampPromo.PromoDescription blueFlashDescription =
                createAnaplanBlueFlashDescription(businessId, promoId, null, promoName);
        PromoDescriptionDto promoDescriptionDto =
                PromoDescriptionMapper.convertFromPromosStorageDescription(partnerId, blueFlashDescription, false);

        assertNotNull(promoDescriptionDto);
        assertEquals(promoId, promoDescriptionDto.getPromoId());
        assertEquals(promoName, promoDescriptionDto.getName());
        assertEquals(PiPromoMechanicDto.BLUE_FLASH, promoDescriptionDto.getMechanic());
        assertFalse(promoDescriptionDto.isPartnerPromo());
        assertTrue(promoDescriptionDto.isEnabled());
        assertNotNull(promoDescriptionDto.getAnaplanPromoInfo());
        assertNotNull(promoDescriptionDto.getBlueFlashInfo());
        assertNull(promoDescriptionDto.getBlueFlashInfo().getWarehouseId());
    }

    @Test
    @DisplayName("Проверка маппинга маркетплейсной N+1=N")
    void convertAnaplanCheapestAsGiftFromPromosStorageDescriptionTest() {
        long businessId = 1111L;
        long partnerId = 101010;
        String promoId = "#12345";
        String promoName = "Test promo";
        int warehouseId = 145;
        int count = 5;

        DataCampPromo.PromoDescription cheapestAsGiftDescription =
                createAnaplanCheapestAsGiftDescription(businessId, promoId, warehouseId, count, promoName);
        PromoDescriptionDto promoDescriptionDto =
                PromoDescriptionMapper.convertFromPromosStorageDescription(partnerId, cheapestAsGiftDescription, false);

        assertNotNull(promoDescriptionDto);
        assertEquals(promoId, promoDescriptionDto.getPromoId());
        assertEquals(promoName, promoDescriptionDto.getName());
        assertEquals(PiPromoMechanicDto.CHEAPEST_AS_GIFT, promoDescriptionDto.getMechanic());
        assertFalse(promoDescriptionDto.isPartnerPromo());
        assertTrue(promoDescriptionDto.isEnabled());
        assertNotNull(promoDescriptionDto.getAnaplanPromoInfo());
        assertNotNull(promoDescriptionDto.getCheapestAsGiftInfo());
        assertEquals(warehouseId, promoDescriptionDto.getCheapestAsGiftInfo().getWarehouseId());
        assertEquals(count, promoDescriptionDto.getCheapestAsGiftInfo().getCount());
    }

    @Test
    @DisplayName("Проверка маппинга категорий в скидочной маркетплейсной акции")
    void convertAnaplanDiscountPromoCategoriesTest() {
        long partnerId = 101010;
        String promoId = "#12345";
        Set<Pair<Long, Integer>> categoriesInfo =
                Set.of(
                        Pair.of(111L, 13),
                        Pair.of(222L, 6),
                        Pair.of(333L, 45)
                );

        DataCampPromo.PromoDescription directDiscountDescription =
                createAnaplanDirectDiscountDescription(promoId, categoriesInfo);
        PromoDescriptionDto promoDescriptionDto =
                PromoDescriptionMapper.convertFromPromosStorageDescription(partnerId, directDiscountDescription, true, false);

        assertNotNull(promoDescriptionDto);
        assertNotNull(promoDescriptionDto.getDiscountInfoDto());

        List<CategoryDiscountInfoDto> categoriesInfoDto = promoDescriptionDto.getDiscountInfoDto().getCategoriesInfo();
        assertNotNull(categoriesInfoDto);
        assertEquals(3, categoriesInfoDto.size());
        assertThat(categoriesInfoDto).containsExactlyInAnyOrderElementsOf(
                categoriesInfo.stream()
                        .map(categoryInfo ->
                                new CategoryDiscountInfoDto(categoryInfo.getKey(), categoryInfo.getValue())
                        )
                        .collect(Collectors.toList())
        );

        PromoDescriptionDto promoDescriptionDto_withoutCategories =
                PromoDescriptionMapper.convertFromPromosStorageDescription(partnerId, directDiscountDescription, false, false);

        assertNotNull(promoDescriptionDto_withoutCategories);
        assertNull(promoDescriptionDto_withoutCategories.getDiscountInfoDto());
    }
}
