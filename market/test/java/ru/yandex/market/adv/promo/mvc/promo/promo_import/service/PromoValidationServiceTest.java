package ru.yandex.market.adv.promo.mvc.promo.promo_import.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampPromo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.mvc.promo.promo_id.dto.PiPromoMechanicDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.CategoryRestrictionDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.CheapestAsGiftMechanicDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.MarketPromocodeMechanicDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.MskuRestrictionDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.OriginalBrandRestrictionDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.OriginalCategoryRestrictionDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.PromoAdditionalInfoDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.PromoBrandDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.PromoCategoryDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.PromoChannelsDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.PromoConstraintsDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.PromoDescriptionRequestDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.PromoMechanicsDataDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.PromoResponsibleDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.PromoStatusDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.SupplierRestrictionDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.WarehouseRestrictionDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.model.PromoValidationErrorType;

class PromoValidationServiceTest extends FunctionalTest {
    @Autowired
    private PromoValidationService promoValidationService;

    @Test
    public void testNewDiscountPromoCorrectValidation() {
        String promoId = "#1337";
        PiPromoMechanicDto promoMechanic = PiPromoMechanicDto.DIRECT_DISCOUNT;
        long startDateTime = 1651611600L;
        long endDateTime = 1652129999L;
        CategoryRestrictionDto categoryRestrictionDto = createCategoryRestrictionDto(Map.of(1L, 5, 2L, 15));
        OriginalCategoryRestrictionDto originalCategoryRestrictionDto = createOriginalCategoryRestrictionDto(Map.of(1L, 5, 3L, 15));

        List<Long> originalBrandRestrictionList = List.of(11L, 22L, 33L);
        List<Long> mskuRestrictionList = List.of(1111111L, 2222222L, 3333333L);
        List<Long> supplierRestrictionList = List.of(111L, 222L, 333L);
        List<Long> warehouseRestrictionList = List.of(1111L, 2222L);
        OriginalBrandRestrictionDto originalBrandRestrictionDto = createOriginalBrandRestrictionDto(originalBrandRestrictionList);
        MskuRestrictionDto mskuRestrictionDto = new MskuRestrictionDto(mskuRestrictionList);
        SupplierRestrictionDto supplierRestrictionDto = new SupplierRestrictionDto(supplierRestrictionList);
        WarehouseRestrictionDto warehouseRestrictionDto = new WarehouseRestrictionDto(warehouseRestrictionList);
        PromoConstraintsDto constraintsDto = new PromoConstraintsDto(startDateTime, endDateTime, true,
                categoryRestrictionDto, originalCategoryRestrictionDto, originalBrandRestrictionDto,
                mskuRestrictionDto, supplierRestrictionDto, warehouseRestrictionDto);

        String promoName = "promoName";
        String landingUrl = "https://greed.is.good";
        String rulesUrl = "https://aezak.mi";
        long piPublishDate = 1640984400L;
        long createdAtDate = 1640984400L;
        PromoStatusDto promoStatusDto = PromoStatusDto.NEW;

        PromoAdditionalInfoDto promoAdditionalInfoDto = new PromoAdditionalInfoDto(promoName, landingUrl, rulesUrl,
                piPublishDate, createdAtDate, promoStatusDto);

        String tradeLogin = "tigran";
        PromoResponsibleDto promoResponsibleDto = new PromoResponsibleDto(tradeLogin);

        PromoChannelsDto promoChannelsDto = new PromoChannelsDto(List.of(1, 42, 50));

        String parentPromoId = "Parent";
        PromoDescriptionRequestDto requestDto = new PromoDescriptionRequestDto(promoId, promoMechanic, parentPromoId,
                constraintsDto, promoAdditionalInfoDto, promoResponsibleDto, promoChannelsDto, null);
        List<String> promoValidationErrors = promoValidationService.validateRequest(requestDto);

        Assertions.assertThat(promoValidationErrors).isEmpty();
    }

    @Test
    public void testIncorrectNewCheapestAsGift() {
        String promoId = "#1337";
        PiPromoMechanicDto promoMechanic = PiPromoMechanicDto.CHEAPEST_AS_GIFT;
        long startDateTime = 1651611600L;
        long endDateTime = 1L;
        OriginalCategoryRestrictionDto originalCategoryRestrictionDto = new OriginalCategoryRestrictionDto(
                List.of(new PromoCategoryDto(1L, null), new PromoCategoryDto(3L, null))
        );

        PromoConstraintsDto constraintsDto = new PromoConstraintsDto(startDateTime, endDateTime, true,
                null, originalCategoryRestrictionDto, null, null, null, null);

        String promoName = "promoName";
        String landingUrl = "https://greed.is.good";
        String rulesUrl = "https://aezak.mi";
        long piPublishDate = 1640984400L;
        long createdAtDate = 1640984400L;
        PromoStatusDto promoStatusDto = PromoStatusDto.NEW;

        PromoAdditionalInfoDto promoAdditionalInfoDto = new PromoAdditionalInfoDto(promoName, landingUrl, rulesUrl,
                piPublishDate, createdAtDate, promoStatusDto);

        String tradeLogin = "tigran";
        PromoResponsibleDto promoResponsibleDto = new PromoResponsibleDto(tradeLogin);

        PromoChannelsDto promoChannelsDto = new PromoChannelsDto(List.of(1, 42, 50));
        CheapestAsGiftMechanicDto cheapestAsGiftMechanicDto = new CheapestAsGiftMechanicDto();
        PromoMechanicsDataDto promoMechanicsDataDto = new PromoMechanicsDataDto(cheapestAsGiftMechanicDto, null);
        String parentPromoId = "Parent";
        PromoDescriptionRequestDto requestDto = new PromoDescriptionRequestDto(promoId, promoMechanic, parentPromoId,
                constraintsDto, promoAdditionalInfoDto, promoResponsibleDto, promoChannelsDto, promoMechanicsDataDto);
        List<String> promoValidationErrors = promoValidationService.validateRequest(requestDto);
        Assertions.assertThat(promoValidationErrors)
                .containsExactlyInAnyOrder(
                        PromoValidationErrorType.EMPTY_CHEAPEST_AS_GIFT_WAREHOUSE.getErrorText(),
                        String.format(PromoValidationErrorType.INCORRECT_DATES.getErrorText(), startDateTime, endDateTime)
                );
    }

    @Test
    public void testIncorrectNewPromocode() {
        String promoId = "#1337";
        PiPromoMechanicDto promoMechanic = PiPromoMechanicDto.MARKET_PROMOCODE;
        long startDateTime = 1651611600L;
        long endDateTime = 1651611800L;
        OriginalCategoryRestrictionDto originalCategoryRestrictionDto = new OriginalCategoryRestrictionDto(
                List.of(new PromoCategoryDto(1L, null), new PromoCategoryDto(3L, null))
        );

        PromoConstraintsDto constraintsDto = new PromoConstraintsDto(startDateTime, endDateTime, true,
                null, originalCategoryRestrictionDto, null, null, null, null);

        String promoName = "promoName";
        long piPublishDate = 1651611800L;
        long createdAtDate = 1640984400L;

        PromoAdditionalInfoDto promoAdditionalInfoDto = new PromoAdditionalInfoDto(promoName, null, null,
                piPublishDate, createdAtDate, null);

        int channedId = 969;
        PromoChannelsDto promoChannelsDto = new PromoChannelsDto(List.of(channedId));
        MarketPromocodeMechanicDto marketPromocodeMechanicDto = new MarketPromocodeMechanicDto();
        PromoMechanicsDataDto promoMechanicsDataDto = new PromoMechanicsDataDto(null, marketPromocodeMechanicDto);
        PromoDescriptionRequestDto requestDto = new PromoDescriptionRequestDto(promoId, promoMechanic, null,
                constraintsDto, promoAdditionalInfoDto, null, promoChannelsDto, promoMechanicsDataDto);
        List<String> promoValidationErrors = promoValidationService.validateRequest(requestDto);
        Assertions.assertThat(promoValidationErrors)
                .containsExactlyInAnyOrder(
                        String.format(PromoValidationErrorType.INCORRECT_PI_PUBLISH_DATES.getErrorText(), piPublishDate, startDateTime),
                        String.format(PromoValidationErrorType.INCORRECT_CHANNEL.getErrorText(), channedId)
                );
    }

    @Test
    public void testValidatePromoUpdate() {
        String promoId = "#1337";
        PiPromoMechanicDto promoMechanic = PiPromoMechanicDto.DIRECT_DISCOUNT;
        long startDateTime = 1651611600L;
        long endDateTime = 1652129999L;
        CategoryRestrictionDto categoryRestrictionDto = createCategoryRestrictionDto(Map.of(1L, 5, 2L, 15));
        OriginalCategoryRestrictionDto originalCategoryRestrictionDto = createOriginalCategoryRestrictionDto(Map.of(3L, 15));

        List<Long> mskuRestrictionList = List.of(1111111L, 3333333L);
        List<Long> supplierRestrictionList = List.of(111L);
        List<Long> warehouseRestrictionList = List.of(1111L, 2222L);
        MskuRestrictionDto mskuRestrictionDto = new MskuRestrictionDto(mskuRestrictionList);
        SupplierRestrictionDto supplierRestrictionDto = new SupplierRestrictionDto(supplierRestrictionList);
        WarehouseRestrictionDto warehouseRestrictionDto = new WarehouseRestrictionDto(warehouseRestrictionList);
        PromoConstraintsDto constraintsDto = new PromoConstraintsDto(startDateTime, endDateTime, true,
                categoryRestrictionDto, originalCategoryRestrictionDto, null,
                mskuRestrictionDto, supplierRestrictionDto, warehouseRestrictionDto);

        String promoName = "promoName";
        String landingUrl = "https://greed.is.good";
        String rulesUrl = "https://aezak.mi";
        long piPublishDate = 1L;
        long createdAtDate = 1640984400L;
        PromoStatusDto promoStatusDto = PromoStatusDto.NEW;

        PromoAdditionalInfoDto promoAdditionalInfoDto = new PromoAdditionalInfoDto(promoName, landingUrl, rulesUrl,
                piPublishDate, createdAtDate, promoStatusDto);

        PromoDescriptionRequestDto requestDto = new PromoDescriptionRequestDto(promoId, promoMechanic, null,
                constraintsDto, promoAdditionalInfoDto, null, null, null);

        DataCampPromo.PromoDescription previousDescription =
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                        )
                        .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                        )
                        .setAdditionalInfo(
                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                        .setPublishDatePi(piPublishDate)
                                        .setSendPromoPi(true)
                        )
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                        .setCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                                .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                        .setId(1)
                                                        .setMinDiscount(15)
                                                )
                                        )
                                        .setOrigionalCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                                .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                        .setId(1)
                                                        .setMinDiscount(5)
                                                )
                                                .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                        .setId(3)
                                                        .setMinDiscount(5)
                                                )
                                        )
                                        .setMskuRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.MskuRestriction.newBuilder()
                                                .setMsku(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                        .addId(1111111L)
                                                        .addId(2222222L)
                                                        .addId(3333333L)
                                                )
                                        )
                                        .setSupplierRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.SupplierRestriction.newBuilder()
                                                .setSuppliers(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                        .addId(111)
                                                        .addId(222)
                                                        .addId(333)
                                                )
                                        )
                                        .setWarehouseRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                                                .setWarehouse(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                        .addId(3333L)
                                                        .addId(4444L)
                                                )
                                        )
                                )
                                .setStartDate(startDateTime + 1)
                                .setEndDate(endDateTime - 1)
                        )
                        .build();
        List<String> promoValidationErrors = promoValidationService.validateRequest(requestDto, previousDescription);
        Assertions.assertThat(promoValidationErrors)
                .containsExactlyInAnyOrder(
                        PromoValidationErrorType.CAN_NOT_REDUCE_START_DATE_FOR_CHANGES.getErrorText(),
                        PromoValidationErrorType.CAN_NOT_INCREASE_END_DATE_FOR_CHANGES.getErrorText(),
                        String.format(PromoValidationErrorType.CAN_NOT_DELETE_CATEGORY_FROM_ORIGINAL_RESTRICTIONS.getErrorText(), 1),
                        String.format(PromoValidationErrorType.CAN_NOT_CHANGE_MIN_DISCOUNT.getErrorText(), 1, 15, 5),
                        String.format(PromoValidationErrorType.CAN_NOT_DELETE_MSKU_FROM_RESTRICTIONS.getErrorText(), 2222222),
                        String.format(PromoValidationErrorType.CAN_NOT_DELETE_SUPPLIERS_FROM_RESTRICTIONS.getErrorText(), "333, 222"),
                        String.format(PromoValidationErrorType.CAN_NOT_DELETE_WAREHOUSES_FROM_RESTRICTIONS.getErrorText(), "3333, 4444"),
                        String.format(PromoValidationErrorType.CAN_NOT_CHANGE_ORIGINAL_MIN_DISCOUNT.getErrorText(), 3, 5, 15)
                );
    }

    @Test
    public void testValidateAddingNewRestrictions() {
        String promoId = "#1337";
        PiPromoMechanicDto promoMechanic = PiPromoMechanicDto.DIRECT_DISCOUNT;
        long startDateTime = 1651611600L;
        long endDateTime = 1652129999L;
        CategoryRestrictionDto categoryRestrictionDto = createCategoryRestrictionDto(Map.of(1L, 5, 2L, 15));
        OriginalCategoryRestrictionDto originalCategoryRestrictionDto = createOriginalCategoryRestrictionDto(Map.of(3L, 15));

        List<Long> brandRestrictionList = List.of(11L, 22L);
        List<Long> mskuRestrictionList = List.of(1111111L, 3333333L);
        List<Long> supplierRestrictionList = List.of(111L);
        List<Long> warehouseRestrictionList = List.of(1111L, 2222L);
        OriginalBrandRestrictionDto originalBrandRestrictionDto = createOriginalBrandRestrictionDto(brandRestrictionList);
        MskuRestrictionDto mskuRestrictionDto = new MskuRestrictionDto(mskuRestrictionList);
        SupplierRestrictionDto supplierRestrictionDto = new SupplierRestrictionDto(supplierRestrictionList);
        WarehouseRestrictionDto warehouseRestrictionDto = new WarehouseRestrictionDto(warehouseRestrictionList);
        PromoConstraintsDto constraintsDto = new PromoConstraintsDto(startDateTime, endDateTime, true,
                categoryRestrictionDto, originalCategoryRestrictionDto, originalBrandRestrictionDto,
                mskuRestrictionDto, supplierRestrictionDto, warehouseRestrictionDto);

        String promoName = "promoName";
        String landingUrl = "https://greed.is.good";
        String rulesUrl = "https://aezak.mi";
        long piPublishDate = 1L;
        long createdAtDate = 1640984400L;
        PromoStatusDto promoStatusDto = PromoStatusDto.NEW;

        PromoAdditionalInfoDto promoAdditionalInfoDto = new PromoAdditionalInfoDto(promoName, landingUrl, rulesUrl,
                piPublishDate, createdAtDate, promoStatusDto);

        PromoDescriptionRequestDto requestDto = new PromoDescriptionRequestDto(promoId, promoMechanic, null,
                constraintsDto, promoAdditionalInfoDto, null, null, null);

        DataCampPromo.PromoDescription previousDescription =
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                        )
                        .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                        )
                        .setAdditionalInfo(
                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                        .setPublishDatePi(piPublishDate)
                                        .setSendPromoPi(true)
                        )
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder())
                                .setStartDate(startDateTime)
                                .setEndDate(endDateTime)
                        )
                        .build();
        List<String> promoValidationErrors = promoValidationService.validateRequest(requestDto, previousDescription);
        Assertions.assertThat(promoValidationErrors)
                .containsExactlyInAnyOrder(
                        PromoValidationErrorType.CAN_NOT_ADD_NEW_MSKU_RESTRICTIONS.getErrorText(),
                        PromoValidationErrorType.CAN_NOT_ADD_NEW_BRAND_RESTRICTIONS.getErrorText(),
                        PromoValidationErrorType.CAN_NOT_ADD_NEW_SUPPLIER_RESTRICTIONS.getErrorText(),
                        PromoValidationErrorType.CAN_NOT_ADD_NEW_ORIGINAL_CATEGORIES_RESTRICTIONS.getErrorText(),
                        PromoValidationErrorType.CAN_NOT_ADD_NEW_WAREHOUSES_RESTRICTIONS.getErrorText()
                );
    }

    private CategoryRestrictionDto createCategoryRestrictionDto(Map<Long, Integer> discountsForCategories) {
        List<PromoCategoryDto> promoCategories = discountsForCategories.entrySet().stream()
                .map(entry -> new PromoCategoryDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return new CategoryRestrictionDto(promoCategories);
    }

    private OriginalCategoryRestrictionDto createOriginalCategoryRestrictionDto(Map<Long, Integer> discountsForCategories) {
        List<PromoCategoryDto> promoCategories = discountsForCategories.entrySet().stream()
                .map(entry -> new PromoCategoryDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return new OriginalCategoryRestrictionDto(promoCategories);
    }

    private OriginalBrandRestrictionDto createOriginalBrandRestrictionDto(Collection<Long> brandIds) {
        List<PromoBrandDto> promoBrands = brandIds.stream()
                .map(PromoBrandDto::new)
                .collect(Collectors.toList());
        return new OriginalBrandRestrictionDto(promoBrands);
    }
}
