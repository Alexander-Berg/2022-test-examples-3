package ru.yandex.direct.core.entity.bids.validation;

import java.math.BigDecimal;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.CpmYndxFrontpageAdGroupPriceRestrictions;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyChf;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoFrontpageNoDesktopImmersionsInRegions;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoFrontpageNoMobileImmersionsInRegions;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueCpmNotLessThan;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PriceValidatorForCpmYndxFrontpageTest {
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    private GeoTree geoTree;

    private static Map<FrontpageCampaignShowType, Map<Long, BigDecimal>> minPricesByRegionAndShowType =
            EntryStream
                    .of(CpmYndxFrontpagePriceConstants.getDefaultRestrictions())
                    .mapValues(priceByRegion -> EntryStream.of(priceByRegion)
                            .mapValues(priceInfo -> priceInfo.getMinFrontpagePrice(CurrencyChf.getInstance()))
                            .toMap())
                    .toMap();

    private static Map<FrontpageCampaignShowType, Map<Long, BigDecimal>> maxPricesByRegionAndShowType =
            EntryStream
                    .of(CpmYndxFrontpagePriceConstants.getDefaultRestrictions())
                    .mapValues(priceByRegion -> EntryStream.of(priceByRegion)
                            .mapValues(priceInfo -> priceInfo.getMaxFrontpagePrice(CurrencyChf.getInstance()))
                            .toMap())
                    .toMap();

    @Before
    public void setUp() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
    }

    @Test
    public void validate_HighPrice_NoErrors() {
        BigDecimal price = BigDecimal.valueOf(10);
        CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions =
                buildCpmYndxFrontpageCurrencyValidationData(1., 1500.);
        PriceValidator priceValidator = new PriceValidator(CurrencyChf.getInstance(),
                AdGroupType.CPM_YNDX_FRONTPAGE,
                cpmYndxFrontpageAdGroupPriceRestrictions);
        ValidationResult<BigDecimal, Defect> vr = priceValidator.apply(price);
        assertThat(vr.hasAnyErrors()).isFalse();
        assertThat(vr.hasAnyWarnings()).isFalse();
    }

    @Test
    public void validate_LowPrice_Error() {
        BigDecimal price = BigDecimal.valueOf(6);
        CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions =
                buildCpmYndxFrontpageCurrencyValidationData(10., 50.);
        PriceValidator priceValidator = new PriceValidator(CurrencyChf.getInstance(),
                AdGroupType.CPM_YNDX_FRONTPAGE,
                cpmYndxFrontpageAdGroupPriceRestrictions);
        ValidationResult<BigDecimal, Defect> vr = priceValidator.apply(price);
        assertThat(vr.flattenErrors()).is(matchedBy(contains(validationError(path(), invalidValueCpmNotLessThan(
                Money.valueOf(10., CurrencyCode.CHF))))));
        assertThat(vr.hasAnyWarnings()).isFalse();
    }

    @Test
    public void validate_LowerThanMinCpmPrice_Error() {
        BigDecimal price = BigDecimal.valueOf(0.01);
        CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions =
                buildCpmYndxFrontpageCurrencyValidationData(0.001, 100.);
        PriceValidator priceValidator =
                new PriceValidator(CurrencyChf.getInstance(), AdGroupType.CPM_YNDX_FRONTPAGE,
                        cpmYndxFrontpageAdGroupPriceRestrictions);
        ValidationResult<BigDecimal, Defect> vr = priceValidator.apply(price);
        assertThat(vr.flattenErrors()).is(matchedBy(contains(validationError(path(), invalidValueCpmNotLessThan(
                Money.valueOf(CurrencyChf.getInstance().getMinCpmPrice(), CurrencyCode.CHF))))));
    }

    @Test
    public void validate_MoscowLowPrice_Warning() {
        BigDecimal price = BigDecimal.valueOf(2.);
        CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions =
                buildCpmYndxFrontpageCurrencyValidationData(0.0001, 10000.);
        PriceValidator priceValidator =
                new PriceValidator(CurrencyChf.getInstance(), AdGroupType.CPM_YNDX_FRONTPAGE,
                        cpmYndxFrontpageAdGroupPriceRestrictions);
        ValidationResult<BigDecimal, Defect> vr = priceValidator.apply(price);
        assertThat(vr.flattenWarnings().size()).is(matchedBy(equalTo(1)));
        assertThat(vr.flattenWarnings()).is(matchedBy(contains(validationError(path(),
                geoFrontpageNoDesktopImmersionsInRegions(singletonList(geoTree.getRegion(MOSCOW_REGION_ID)))))));
        assertThat(vr.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_MoscowDesktopRegionLowPrice_WarningOnlyMoscowRegionDesktop() {
        BigDecimal price = BigDecimal.valueOf(1.3);
        CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions =
                buildCpmYndxFrontpageCurrencyValidationData(0.0001, 10000.);
        PriceValidator priceValidator =
                new PriceValidator(CurrencyChf.getInstance(), AdGroupType.CPM_YNDX_FRONTPAGE,
                        cpmYndxFrontpageAdGroupPriceRestrictions);
        ValidationResult<BigDecimal, Defect> vr = priceValidator.apply(price);
        assertThat(vr.flattenWarnings().size()).is(matchedBy(equalTo(1)));
        assertThat(vr.flattenWarnings()).is(matchedBy(contains(validationError(path(),
                geoFrontpageNoDesktopImmersionsInRegions(
                        singletonList(geoTree.getRegion(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)))))));
        assertThat(vr.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_MoscowDesktopRegionMobileLowPrice_WarningsMoscowRegionMobileAndDesktop() {
        BigDecimal price = BigDecimal.valueOf(1.2);
        CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions =
                buildCpmYndxFrontpageCurrencyValidationData(0.0001, 10000.);
        PriceValidator priceValidator =
                new PriceValidator(CurrencyChf.getInstance(), AdGroupType.CPM_YNDX_FRONTPAGE,
                        cpmYndxFrontpageAdGroupPriceRestrictions);
        ValidationResult<BigDecimal, Defect> vr = priceValidator.apply(price);
        assertThat(vr.flattenWarnings().size()).is(matchedBy(equalTo(2)));
        assertThat(vr.flattenWarnings()).is(matchedBy(containsInAnyOrder(validationError(path(),
                geoFrontpageNoMobileImmersionsInRegions(
                        singletonList(geoTree.getRegion(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)))),
                validationError(path(), geoFrontpageNoDesktopImmersionsInRegions(
                        singletonList(geoTree.getRegion(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)))))));
        assertThat(vr.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_MoscowMobileAndStPetersburgAllRegionLowPrice_WarningMoscowStPetersburg() {
        BigDecimal price = BigDecimal.valueOf(1.1);
        CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions =
                buildCpmYndxFrontpageCurrencyValidationData(0.0001, 10000.);
        PriceValidator priceValidator =
                new PriceValidator(CurrencyChf.getInstance(), AdGroupType.CPM_YNDX_FRONTPAGE,
                        cpmYndxFrontpageAdGroupPriceRestrictions);
        ValidationResult<BigDecimal, Defect> vr = priceValidator.apply(price);
        assertThat(vr.flattenWarnings().size()).is(matchedBy(equalTo(2)));
        assertThat(vr.flattenWarnings()).is(matchedBy(containsInAnyOrder(validationError(
                path(), geoFrontpageNoMobileImmersionsInRegions(
                        ImmutableList.of(geoTree.getRegion(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)))),
                validationError(path(), geoFrontpageNoDesktopImmersionsInRegions(
                        ImmutableList.of(geoTree.getRegion(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                                geoTree.getRegion(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)))))));
        assertThat(vr.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_MoscowRegionAndRussiaMobileAndDesktopLowPrice_WarningsOnlyRussia() {
        BigDecimal price = BigDecimal.valueOf(.5);
        CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions =
                buildCpmYndxFrontpageCurrencyValidationData(0.0001, 10000.);
        PriceValidator priceValidator =
                new PriceValidator(CurrencyChf.getInstance(), AdGroupType.CPM_YNDX_FRONTPAGE,
                        cpmYndxFrontpageAdGroupPriceRestrictions);
        ValidationResult<BigDecimal, Defect> vr = priceValidator.apply(price);
        assertThat(vr.flattenWarnings().size()).is(matchedBy(equalTo(2)));
        assertThat(vr.flattenWarnings()).is(matchedBy(containsInAnyOrder(validationError(
                path(), geoFrontpageNoMobileImmersionsInRegions(
                        ImmutableList.of(geoTree.getRegion(RUSSIA_REGION_ID), geoTree.getRegion(UKRAINE_REGION_ID)))),
                validationError(path(), geoFrontpageNoDesktopImmersionsInRegions(ImmutableList
                        .of(geoTree.getRegion(RUSSIA_REGION_ID), geoTree.getRegion(UKRAINE_REGION_ID)))))));
        assertThat(vr.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_MoscowRegionMobileLowAndRussiaDesktopLowPrice_WarningsRussiaDesktopMoscowRegionMobile() {
        BigDecimal price = BigDecimal.valueOf(.9);
        CpmYndxFrontpageAdGroupPriceRestrictions cpmYndxFrontpageAdGroupPriceRestrictions =
                buildCpmYndxFrontpageCurrencyValidationData(0.0001, 10000.);
        PriceValidator priceValidator =
                new PriceValidator(CurrencyChf.getInstance(), AdGroupType.CPM_YNDX_FRONTPAGE,
                        cpmYndxFrontpageAdGroupPriceRestrictions);
        ValidationResult<BigDecimal, Defect> vr = priceValidator.apply(price);
        assertThat(vr.flattenWarnings().size()).is(matchedBy(equalTo(2)));
        assertThat(vr.flattenWarnings()).is(matchedBy(containsInAnyOrder(validationError(
                path(), geoFrontpageNoMobileImmersionsInRegions(ImmutableList.of(
                        geoTree.getRegion(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                        geoTree.getRegion(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID)))),
                validationError(path(), geoFrontpageNoDesktopImmersionsInRegions(ImmutableList.of(
                        geoTree.getRegion(RUSSIA_REGION_ID)))))));
        assertThat(vr.hasAnyErrors()).isFalse();
    }

    private CpmYndxFrontpageAdGroupPriceRestrictions buildCpmYndxFrontpageCurrencyValidationData(Double minPrice,
                                                                                                 Double maxPrice) {
        return new CpmYndxFrontpageAdGroupPriceRestrictions(BigDecimal.valueOf(minPrice), BigDecimal.valueOf(maxPrice))
                .withClientCurrency(CurrencyChf.getInstance())
                .withMinPriceByRegion(minPricesByRegionAndShowType)
                .withMaxPriceByRegion(maxPricesByRegionAndShowType)
                .withRegionsById(geoTree.getRegions());
    }
}
