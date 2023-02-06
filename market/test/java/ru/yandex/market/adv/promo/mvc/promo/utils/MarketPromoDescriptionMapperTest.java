package ru.yandex.market.adv.promo.mvc.promo.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampPromo;
import NMarket.Common.Promo.Promo;
import NMarketIndexer.Common.Common;
import com.google.protobuf.Timestamp;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.mvc.promo.common.dto.PromocodeApplyingTypeDto;
import ru.yandex.market.adv.promo.mvc.promo.common.dto.PromocodeDiscountTypeDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_id.dto.PiPromoMechanicDto;
import ru.yandex.market.adv.promo.mvc.promo.promo_import.dto.CategoryRestrictionDto;
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

class MarketPromoDescriptionMapperTest extends FunctionalTest {
    @Test
    public void testCreateDirectDiscountPromo() {
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

        long priority = 666L;
        DataCampPromo.PromoDescription actualDescription =
                MarketPromoDescriptionMapper.convertPromoDescriptionFromRequest(requestDto, priority, null);
        Timestamp timestamp = actualDescription.getUpdateInfo().getMeta().getTimestamp();
        DataCampOfferMeta.DataSource source = DataCampOfferMeta.DataSource.B2B_PROMO;

        DataCampPromo.PromoDescription expectedDescription = DataCampPromo.PromoDescription.newBuilder()
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                        .setMeta(createMeta(timestamp, source))
                )
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(0)
                        .setSource(Promo.ESourceType.ANAPLAN)
                        .setPromoId(promoId)
                )
                .setPromotion(DataCampPromo.PromoPromotion.newBuilder()
                        .addAllChannel(List.of(
                                DataCampPromo.PromoPromotion.Channel.MAIN_HERO_BANNER,
                                DataCampPromo.PromoPromotion.Channel.BANNER_IN_CATALOG,
                                DataCampPromo.PromoPromotion.Channel.MAIN_HERO_BANNER_MARKET
                        ))
                        .setMeta(createMeta(timestamp, source))
                )
                .setResponsible(DataCampPromo.PromoResponsible.newBuilder()
                        .setAuthor(tradeLogin)
                        .setMeta(createMeta(timestamp, source))
                )
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setMeta(createMeta(timestamp, source))
                        .setName(promoName)
                        .setPublishDatePi(piPublishDate)
                        .setSendPromoPi(true)
                        .setPriority(priority)
                        .setParentPromoId(parentPromoId)
                        .setLendingUrl(landingUrl)
                        .setRulesUrl(rulesUrl)
                        .setStatus(DataCampPromo.PromoAdditionalInfo.PromoStatus.NEW)
                        .setCreatedAt(createdAtDate)
                        .setUpdatedAt(timestamp.getSeconds())
                )
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setEnabled(true)
                        .setEndDate(endDateTime)
                        .setStartDate(startDateTime)
                        .setMeta(createMeta(timestamp, source))
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setOrigionalCategoryRestriction(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                                .addIncludeCategegoryRestriction(
                                                        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                                .setId(1)
                                                                .setMinDiscount(5)
                                                )
                                                .addIncludeCategegoryRestriction(
                                                        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                                .setId(3)
                                                                .setMinDiscount(15)
                                                )
                                                .setIncludeCategoryRestrictionsCount(2)
                                )
                                .setCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                        .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(1)
                                                .setMinDiscount(5)
                                        )
                                        .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(2)
                                                .setMinDiscount(15)
                                        )
                                )
                                .setWarehouseRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                                        .setWarehouse(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                .addAllId(warehouseRestrictionList)
                                        )
                                        .setWarehouseRestrictionCount(2)
                                )
                                .setSupplierRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.SupplierRestriction.newBuilder()
                                        .setSuppliers(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                .addAllId(supplierRestrictionList)
                                        )
                                        .setSupplierRestrictionCount(3)
                                )
                                .setOriginalBrandRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalBrandRestriction.newBuilder()
                                        .setIncludeBrands(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoBrands.newBuilder()
                                                .addBrands(DataCampPromo.PromoBrand.newBuilder()
                                                        .setId(11)
                                                )
                                                .addBrands(DataCampPromo.PromoBrand.newBuilder()
                                                        .setId(22)
                                                )
                                                .addBrands(DataCampPromo.PromoBrand.newBuilder()
                                                        .setId(33)
                                                )
                                        )
                                        .setBrandRestrictionCount(3)
                                )
                                .setMskuRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.MskuRestriction.newBuilder()
                                        .setMsku(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                .addAllId(mskuRestrictionList)
                                        )
                                        .setMskuRestrictionCount(3)
                                )
                        )
                )
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMeta(createMeta(timestamp, source))
                )
                .setUpdateInfo(DataCampPromo.UpdateInfo.newBuilder()
                        .setMeta(createMeta(timestamp, source))
                        .setUpdatedAt(timestamp.getSeconds())
                        .setCreatedAt(timestamp.getSeconds())
                )
                .build();

        Assertions.assertThat(actualDescription).isEqualTo(expectedDescription);
    }

    @Test
    public void testCreateMarketPromocodePromo() {
        String promoId = "#1337";
        PiPromoMechanicDto promoMechanic = PiPromoMechanicDto.MARKET_PROMOCODE;
        long startDateTime = 1651611600L;
        long endDateTime = 1652129999L;
        OriginalCategoryRestrictionDto originalCategoryRestrictionDto = new OriginalCategoryRestrictionDto(
            List.of(new PromoCategoryDto(1L, null), new PromoCategoryDto(3L, null))
        );

        PromoConstraintsDto constraintsDto = new PromoConstraintsDto(startDateTime, endDateTime, true,
                null, originalCategoryRestrictionDto, null,
                null, null, null);

        String promoName = "promoName";
        String landingUrl = "https://greed.is.good";
        String rulesUrl = "https://aezak.mi";
        long piPublishDate = 1640984400L;
        long createdAtDate = 1640984400L;
        PromoStatusDto promoStatusDto = PromoStatusDto.RUNNING;

        PromoAdditionalInfoDto promoAdditionalInfoDto = new PromoAdditionalInfoDto(promoName, landingUrl, rulesUrl,
                piPublishDate, createdAtDate, promoStatusDto);

        String promocode = "PROMOCODE";
        PromocodeDiscountTypeDto promocodeDiscountTypeDto = PromocodeDiscountTypeDto.VALUE;
        long discountValue = 5000;
        long cartMinPrice = 100000;
        long orderMaxPrice = 1000000;
        PromocodeApplyingTypeDto applyingTypeDto = PromocodeApplyingTypeDto.ONE_TIME;

        MarketPromocodeMechanicDto marketPromocodeMechanicDto =
                new MarketPromocodeMechanicDto(promocode, promocodeDiscountTypeDto, discountValue, cartMinPrice,
                        orderMaxPrice, null, applyingTypeDto);
        PromoMechanicsDataDto promoMechanicsDataDto = new PromoMechanicsDataDto(null, marketPromocodeMechanicDto);

        PromoDescriptionRequestDto requestDto = new PromoDescriptionRequestDto(promoId, promoMechanic, null,
                constraintsDto, promoAdditionalInfoDto, null, null, promoMechanicsDataDto);

        long priority = 666L;
        DataCampPromo.PromoDescription actualDescription =
                MarketPromoDescriptionMapper.convertPromoDescriptionFromRequest(requestDto, priority, null);
        Timestamp timestamp = actualDescription.getUpdateInfo().getMeta().getTimestamp();
        DataCampOfferMeta.DataSource source = DataCampOfferMeta.DataSource.B2B_PROMO;

        DataCampPromo.PromoDescription expectedDescription = DataCampPromo.PromoDescription.newBuilder()
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                        .setMeta(createMeta(timestamp, source))
                )
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(0)
                        .setSource(Promo.ESourceType.ANAPLAN)
                        .setPromoId(promoId)
                )
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setMeta(createMeta(timestamp, source))
                        .setName(promoName)
                        .setPublishDatePi(piPublishDate)
                        .setSendPromoPi(true)
                        .setPriority(priority)
                        .setLendingUrl(landingUrl)
                        .setRulesUrl(rulesUrl)
                        .setStatus(DataCampPromo.PromoAdditionalInfo.PromoStatus.RUNNING)
                        .setCreatedAt(createdAtDate)
                        .setUpdatedAt(timestamp.getSeconds())
                )
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setEnabled(true)
                        .setEndDate(endDateTime)
                        .setStartDate(startDateTime)
                        .setMeta(createMeta(timestamp, source))
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setOrigionalCategoryRestriction(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                                .addIncludeCategegoryRestriction(
                                                        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                                .setId(1)
                                                                .setMinDiscount(0)
                                                )
                                                .addIncludeCategegoryRestriction(
                                                        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                                .setId(3)
                                                                .setMinDiscount(0)
                                                )
                                                .setIncludeCategoryRestrictionsCount(2)
                                )
                        )
                        .setMoneyLimit(Common.PriceExpression.newBuilder()
                                .setPrice(10000000)
                        )
                )
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.VALUE)
                                .setRatingRub(discountValue)
                                .setApplyingType(DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType.ONE_TIME)
                                .setPromoCode(promocode)
                                .setBucketMinPrice(cartMinPrice)
                                .setOrderMaxPrice(orderMaxPrice)
                        )
                        .setMeta(createMeta(timestamp, source))
                )
                .setUpdateInfo(DataCampPromo.UpdateInfo.newBuilder()
                        .setMeta(createMeta(timestamp, source))
                        .setUpdatedAt(timestamp.getSeconds())
                        .setCreatedAt(timestamp.getSeconds())
                )
                .build();

        Assertions.assertThat(actualDescription).isEqualTo(expectedDescription);
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

    private Market.DataCamp.DataCampOfferMeta.UpdateMeta createMeta(
            Timestamp timestamp,
            DataCampOfferMeta.DataSource source
    ) {
        return DataCampOfferMeta.UpdateMeta.newBuilder()
                .setSource(source)
                .setTimestamp(timestamp)
                .build();
    }
}
