package ru.yandex.direct.core.entity.pricepackage.service.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.pricepackage.model.PriceMarkup;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageCampaignOptions;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageClient;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFullGoals;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.defects.PricePackageDefects.clientCurrencyNotEqualsPackageCurrency;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.defects.PricePackageDefects.overlappingPriceMarkups;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoIncorrectRegions;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_ALLOWED_CREATIVE_TYPES;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_GEO;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_GEO_EXPANDED;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_GEO_TYPE;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_RETARGETING_CONDITION;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.disallowedPricePackageClient;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueCpmNotGreaterThan;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.unavailableCurrency;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_PROVINCE;
import static ru.yandex.direct.regions.Region.REGION_TYPE_TOWN;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.StringUtils.joinLongsToString;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.DateDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.DateDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.inInterval;
import static ru.yandex.direct.validation.defect.StringDefects.admissibleChars;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Gen.CANNOT_BE_EMPTY;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class PricePackageValidationServiceTest {

    @Autowired
    private PricePackageValidationService service;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private Steps steps;

    private PricePackage pricePackage;
    private PricePackage pricePackageWithFixedGeo;
    private PricePackage pricePackageWithCustomGeo;

    private ClientInfo client;

    @Before
    public void before() {
        client = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));

        var allGoals = TestFullGoals.defaultCryptaGoals();

        pricePackage = clientPricePackage(client);

        pricePackageWithFixedGeo = clientPricePackage(client);
        pricePackageWithFixedGeo.getTargetingsFixed()
                .withGeo(DEFAULT_GEO)
                .withGeoType(DEFAULT_GEO_TYPE);
        pricePackageWithFixedGeo.getTargetingsCustom()
                .withGeo(null)
                .withGeoType(null);

        pricePackageWithCustomGeo = clientPricePackage(client);
        pricePackageWithCustomGeo.getTargetingsFixed()
                .withGeo(null)
                .withGeoType(null);
        pricePackageWithCustomGeo.getTargetingsCustom()
                .withGeo(DEFAULT_GEO)
                .withGeoType(DEFAULT_GEO_TYPE);

        testCryptaSegmentRepository.addAll(allGoals);
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        var actual = validatePricePackage(pricePackage);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void pricePackagesNull() {
        var actual = validatePricePackages(null);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(), notNull()))));
    }

    @Test
    public void pricePackageNull() {
        var actual = validatePricePackage(null);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), notNull()))));
    }

    @Test
    public void titleShouldNotBeNull() {
        pricePackage.setTitle(null);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TITLE)), notNull()))));
    }

    @Test
    public void titleShouldNotBeBlank() {
        pricePackage.setTitle("");

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TITLE)), notEmptyString()))));
    }

    @Test
    public void titleLengthAndAdmissibleChars() {
        String tooLongTitle = StringUtils.repeat('A', 200);
        tooLongTitle += '@';
        pricePackage.setTitle(tooLongTitle);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TITLE)), maxStringLength(200)))));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TITLE)), admissibleChars()))));
    }

    @Test
    public void trackerUrlIsNull() {
        pricePackage.withTrackerUrl(null);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TRACKER_URL)), notNull()))));
    }

    @Test
    public void trackerUrlIsBlank() {
        pricePackage.withTrackerUrl("");

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TRACKER_URL)), notEmptyString()))));
    }

    @Test
    public void trackerUrlIsInvalidHref() {
        pricePackage.withTrackerUrl("invalid-url");

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TRACKER_URL)), invalidValue()))));
    }

    @Test
    public void dateStartIsNull() {
        pricePackage.withDateStart(null);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.DATE_START)), notNull()))));
    }

    @Test
    public void dateEndBeforeDateStart() {
        pricePackage.withDateEnd(pricePackage.getDateStart().minusDays(1));

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.DATE_END)),
                        greaterThanOrEqualTo(pricePackage.getDateStart())))));
    }

    @Test
    public void currencyAndPriceAreNull() {
        pricePackage.withPrice(null).withCurrency(null);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.CURRENCY)), notNull()))));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.PRICE)), notNull()))));
    }

    @Test
    public void currencyIsYndFixed() {
        pricePackage.withPrice(null).withCurrency(CurrencyCode.YND_FIXED);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.PRICE)), notNull()))));
        Set<CurrencyCode> availableCurrencies = EnumSet.allOf(CurrencyCode.class);
        availableCurrencies.remove(CurrencyCode.YND_FIXED);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.CURRENCY)),
                        unavailableCurrency(CurrencyCode.YND_FIXED, availableCurrencies)))));
    }

    @Test
    public void currencyIsNull() {
        pricePackage.withCurrency(null);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.CURRENCY)), notNull()))));
    }

    @Test
    public void priceGreaterThanMax() {
        Currency currency = pricePackage.getCurrency().getCurrency();
        pricePackage.withPrice(currency.getMaxCpmPrice().add(BigDecimal.ONE));

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.PRICE)),
                        invalidValueCpmNotGreaterThan(Money.valueOf(currency.getMaxCpmPrice(), currency.getCode()))))));
    }

    @Test
    public void priceMarkupsValid() {
        pricePackage.setDateStart(LocalDate.of(2020, 11, 1));
        pricePackage.withDateEnd(LocalDate.of(2021, 2, 28));

        PriceMarkup priceMarkup1 = new PriceMarkup()
                .withDateStart(LocalDate.of(2020, 12, 1))
                .withDateEnd(LocalDate.of(2020, 12, 30))
                .withPercent(-25);
        PriceMarkup priceMarkup2 = new PriceMarkup()
                .withDateStart(LocalDate.of(2020, 12, 31))
                .withDateEnd(LocalDate.of(2020, 12, 31))
                .withPercent(5);
        PriceMarkup priceMarkup3 = new PriceMarkup()
                .withDateStart(LocalDate.of(2021, 1, 1))
                .withDateEnd(LocalDate.of(2021, 1, 31))
                .withPercent(75);
        pricePackage.setPriceMarkups(List.of(priceMarkup1, priceMarkup2, priceMarkup3));

        var actual = validatePricePackage(pricePackage);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void priceMarkupsDateStartAfterPackageDateEnd() {
        PriceMarkup priceMarkup = new PriceMarkup()
                .withDateStart(LocalDate.of(2020, 12, 1))
                .withDateEnd(LocalDate.of(2020, 12, 31))
                .withPercent(-25);
        pricePackage.setPriceMarkups(List.of(priceMarkup));

        var actual = validatePricePackage(pricePackage);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.PRICE_MARKUPS), index(0),
                        field(PriceMarkup.DATE_END)),
                        lessThanOrEqualTo(pricePackage.getDateEnd())))));
    }

    @Test
    public void priceMarkupsDateEndBeforeDateStart() {
        PriceMarkup priceMarkup = new PriceMarkup()
                .withDateStart(LocalDate.of(2020, 12, 31))
                .withDateEnd(LocalDate.of(2020, 12, 1))
                .withPercent(-25);
        pricePackage.setPriceMarkups(List.of(priceMarkup));

        var actual = validatePricePackage(pricePackage);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.PRICE_MARKUPS), index(0),
                        field(PriceMarkup.DATE_END)),
                        greaterThanOrEqualTo(priceMarkup.getDateStart())))));
    }

    @Test
    public void priceMarkupsIllegalPercent() {
        PriceMarkup priceMarkup = new PriceMarkup()
                .withDateStart(LocalDate.of(2020, 12, 14))
                .withDateEnd(LocalDate.of(2020, 12, 14))
                .withPercent(-250);
        pricePackage.setPriceMarkups(List.of(priceMarkup));

        var actual = validatePricePackage(pricePackage);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.PRICE_MARKUPS), index(0),
                        field(PriceMarkup.PERCENT)),
                        inInterval(-100, 100)))));
    }

    @Test
    public void priceMarkupsOverlapping() {
        PriceMarkup priceMarkup1 = new PriceMarkup()
                .withDateStart(LocalDate.of(2020, 12, 1))
                .withDateEnd(LocalDate.of(2020, 12, 31))
                .withPercent(-25);
        PriceMarkup priceMarkup2 = new PriceMarkup()
                .withDateStart(LocalDate.of(2020, 11, 1))
                .withDateEnd(LocalDate.of(2020, 12, 31))
                .withPercent(25);
        pricePackage.setPriceMarkups(List.of(priceMarkup1, priceMarkup2));

        var actual = validatePricePackage(pricePackage);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.PRICE_MARKUPS)),
                        overlappingPriceMarkups()))));
    }

    @Test
    public void priceMarkupsOverlappingSameDate() {
        PriceMarkup priceMarkup1 = new PriceMarkup()
                .withDateStart(LocalDate.of(2020, 12, 14))
                .withDateEnd(LocalDate.of(2020, 12, 15))
                .withPercent(-25);
        PriceMarkup priceMarkup2 = new PriceMarkup()
                .withDateStart(LocalDate.of(2020, 12, 15))
                .withDateEnd(LocalDate.of(2020, 12, 16))
                .withPercent(25);
        pricePackage.setPriceMarkups(List.of(priceMarkup1, priceMarkup2));

        var actual = validatePricePackage(pricePackage);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.PRICE_MARKUPS)),
                        overlappingPriceMarkups()))));
    }

    @Test
    public void isPublicNull() {
        pricePackage.withIsPublic(null);

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.IS_PUBLIC)), notNull()))));
    }

    @Test
    public void geoTypeFixedNullWhenGeoFixedNotNull() {
        pricePackageWithFixedGeo.getTargetingsFixed()
                .withGeo(DEFAULT_GEO)
                .withGeoType(null);

        var actual = validatePricePackage(pricePackageWithFixedGeo);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.GEO_TYPE)),
                        notNull()))));
    }

    @Test
    public void geoTypeFixedNotNullWhenGeoFixedNull() {
        pricePackageWithFixedGeo.getTargetingsFixed()
                .withGeo(null)
                .withGeoType(DEFAULT_GEO_TYPE);

        var actual = validatePricePackage(pricePackageWithFixedGeo);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.GEO_TYPE)),
                        isNull()))));
    }

    @Test
    public void geoTypeFixedWrong() {
        pricePackageWithFixedGeo.getTargetingsFixed()
                .withGeo(DEFAULT_GEO)
                .withGeoType(REGION_TYPE_TOWN);

        var actual = validatePricePackage(pricePackageWithFixedGeo);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.GEO_TYPE)),
                        inCollection()))));
    }

    @Test
    public void geoTypeCustomNullWhenGeoCustomNotNull() {
        pricePackageWithCustomGeo.getTargetingsCustom()
                .withGeo(DEFAULT_GEO)
                .withGeoType(null);

        var actual = validatePricePackage(pricePackageWithCustomGeo);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_CUSTOM),
                        field(TargetingsFixed.GEO_TYPE)),
                        notNull()))));
    }

    @Test
    public void geoTypeCustomNotNullWhenGeoCustomNull() {
        pricePackageWithCustomGeo.getTargetingsCustom()
                .withGeo(null)
                .withGeoType(DEFAULT_GEO_TYPE);

        var actual = validatePricePackage(pricePackageWithCustomGeo);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_CUSTOM),
                        field(TargetingsFixed.GEO_TYPE)),
                        isNull()))));
    }

    @Test
    public void geoTypeCustomWrong() {
        pricePackageWithCustomGeo.getTargetingsCustom()
                .withGeo(DEFAULT_GEO)
                .withGeoType(REGION_TYPE_TOWN);

        var actual = validatePricePackage(pricePackageWithCustomGeo);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_CUSTOM),
                        field(TargetingsFixed.GEO_TYPE)),
                        inCollection()))));
    }

    @Test
    public void geoBothNull() {
        pricePackage.getTargetingsFixed().setGeo(null);
        pricePackage.getTargetingsCustom().setGeo(null);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_CUSTOM),
                        field(TargetingsFixed.GEO)),
                        notNull()))));
    }

    @Test
    public void geoBothNotNull() {
        pricePackage.getTargetingsFixed()
                .withGeo(DEFAULT_GEO)
                .withGeoType(DEFAULT_GEO_TYPE);
        pricePackage.getTargetingsCustom()
                .withGeo(DEFAULT_GEO)
                .withGeoType(DEFAULT_GEO_TYPE);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_CUSTOM),
                        field(TargetingsFixed.GEO)),
                        isNull()))));
    }

    @Test
    public void geoFixedRegionTypeWrong() {
        var wrongRegions = List.of(SAINT_PETERSBURG_REGION_ID, -MOSCOW_REGION_ID);
        var packageRegions = new ImmutableList.Builder<Long>()
                .addAll(wrongRegions)
                .add(RUSSIA_REGION_ID, MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)
                .build();

        pricePackageWithFixedGeo.getTargetingsFixed()
                .withGeo(packageRegions)
                .withGeoType(REGION_TYPE_PROVINCE);

        var actual = validatePricePackage(pricePackageWithFixedGeo);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.GEO)),
                        geoIncorrectRegions(joinLongsToString(wrongRegions))))));
    }

    @Test
    public void geoCustomRegionTypeWrong() {
        var wrongRegions = List.of(SAINT_PETERSBURG_REGION_ID, -MOSCOW_REGION_ID);
        var packageRegions = new ImmutableList.Builder<Long>()
                .addAll(wrongRegions)
                .add(RUSSIA_REGION_ID, MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)
                .build();

        pricePackageWithCustomGeo.getTargetingsCustom()
                .withGeo(packageRegions)
                .withGeoType(REGION_TYPE_PROVINCE);

        var actual = validatePricePackage(pricePackageWithCustomGeo);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_CUSTOM),
                        field(TargetingsFixed.GEO)),
                        geoIncorrectRegions(joinLongsToString(wrongRegions))))));
    }

    @Test
    public void targetingsCustomNull() {
        pricePackage.withTargetingsCustom(null);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_CUSTOM)), notNull()))));
    }

    @Test
    public void targetingsFixedNull() {
        pricePackage.withTargetingsFixed(null);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED)), notNull()))));
    }

    @Test
    public void viewTypesNull() {
        pricePackage.getTargetingsFixed().withViewTypes(null);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.VIEW_TYPES)),
                        notNull()))));
    }

    @Test
    public void viewTypesTypeNull() {
        List<ViewType> viewTypes = new ArrayList<>();
        viewTypes.add(null);
        pricePackage.getTargetingsFixed().withViewTypes(viewTypes).withAllowPremiumDesktopCreative(true);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.VIEW_TYPES), index(0)),
                        notNull()))));
    }

    @Test
    public void allowExpandedDesktopCreativeNull() {
        pricePackage.getTargetingsFixed().withAllowExpandedDesktopCreative(null);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.ALLOW_EXPANDED_DESKTOP_CREATIVE)),
                        notNull()))));
    }

    @Test
    public void allowPremiumDesktopCreativeNull() {
        pricePackage.getTargetingsFixed().withAllowPremiumDesktopCreative(null);
        pricePackage.getTargetingsFixed().withAllowExpandedDesktopCreative(false);
        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void allowPremiumDesktopCreative() {
        pricePackage.getTargetingsFixed().withAllowPremiumDesktopCreative(true);
        pricePackage.getTargetingsFixed().withAllowExpandedDesktopCreative(false);
        pricePackage.getTargetingsFixed().withViewTypes(List.of(ViewType.DESKTOP));
        pricePackage.withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE));
        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void allowPremiumDesktopCreativeMobileViewType() {
        pricePackage.getTargetingsFixed().withAllowPremiumDesktopCreative(true);
        pricePackage.getTargetingsFixed().withAllowExpandedDesktopCreative(false);
        pricePackage.getTargetingsFixed().withViewTypes(List.of(ViewType.MOBILE));
        pricePackage.withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE));
        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void allowPremiumDesktopCreativeMultipleViewTypes() {
        pricePackage.getTargetingsFixed().withAllowPremiumDesktopCreative(true);
        pricePackage.getTargetingsFixed().withAllowExpandedDesktopCreative(false);
        pricePackage.getTargetingsFixed().withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE));
        pricePackage.withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE));
        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                                field(TargetingsFixed.ALLOW_PREMIUM_DESKTOP_CREATIVE)),
                        invalidValue()))));
    }

    @Test
    public void notAllowPremiumDesktopCreativeWhenAllowExpandedDesktopCreative() {
        pricePackage.getTargetingsFixed().withAllowExpandedDesktopCreative(true);
        pricePackage.getTargetingsFixed().withAllowPremiumDesktopCreative(true);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.ALLOW_PREMIUM_DESKTOP_CREATIVE)),
                        invalidValue()))));
    }

    @Test
    public void notAllowPremiumDesktopCreativeWhenNotOnlyDesktop() {
        pricePackage.getTargetingsFixed().withAllowPremiumDesktopCreative(true);
        pricePackage.getTargetingsFixed().withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE));

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.ALLOW_PREMIUM_DESKTOP_CREATIVE)),
                        invalidValue()))));
    }

    @Test
    public void NotAllowHideIncomeSegmentWithoutFixedIncomeSegments() {
        pricePackage.getTargetingsFixed().withHideIncomeSegment(true);

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.HIDE_INCOME_SEGMENT)),
                        invalidValue()))));
    }


    @Test
    public void NotAllowHideIncomeSegmentWithoutAllCustomIncomeInFixed() {
        pricePackage.getTargetingsFixed().withHideIncomeSegment(true);
        pricePackage.getTargetingsCustom().getRetargetingCondition().withCryptaSegments(List.of(2499000009L));

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                        field(TargetingsFixed.HIDE_INCOME_SEGMENT)),
                        invalidValue()))));
    }


    @Test
    public void allowHideIncomeSegment() {
        pricePackage.getTargetingsFixed().withHideIncomeSegment(true);
        pricePackage.getTargetingsCustom().getRetargetingCondition().withCryptaSegments(List.of(2499000009L, 1L));
        pricePackage.getTargetingsFixed().withCryptaSegments(List.of(2499000009L));

        var actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void clientsNull() {
        pricePackage.withClients(null);

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.CLIENTS)), notNull()))));
    }

    @Test
    public void clientsItemNull() {
        pricePackage.withClients(singletonList(null));

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.CLIENTS), index(0)), notNull()))));
    }

    @Test
    public void clientIdNull() {
        pricePackage.withClients(List.of(allowedPricePackageClient((Long) null)));

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(PricePackage.CLIENTS), index(0), field(PricePackageClient.CLIENT_ID)),
                notNull()))));
    }

    @Test
    public void clientNotFound() {
        pricePackage.withClients(List.of(allowedPricePackageClient(-1L)));

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(PricePackage.CLIENTS), index(0), field(PricePackageClient.CLIENT_ID)),
                objectNotFound()))));
    }

    @Test
    public void clientCurrencyNotEqualPackageCurrency() {
        ClientInfo client = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.USD));
        pricePackage.withClients(List.of(allowedPricePackageClient(client)));

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(PricePackage.CLIENTS), index(0), field(PricePackageClient.CLIENT_ID)),
                clientCurrencyNotEqualsPackageCurrency()))));
    }

    @Test
    public void clientIsAllowedNull() {
        pricePackage.withClients(List.of(
                allowedPricePackageClient(client).withIsAllowed(null)
        ));

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(PricePackage.CLIENTS), index(0), field(PricePackageClient.IS_ALLOWED)),
                notNull()))));
    }

    @Test
    public void clientIsAllowedInPublicPackage() {
        pricePackage
                .withIsPublic(true)
                .withClients(List.of(allowedPricePackageClient(client)));

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(PricePackage.CLIENTS), index(0), field(PricePackageClient.IS_ALLOWED)),
                invalidValue()))));
    }

    @Test
    public void clientIsAllowedInNotPublicPackage() {
        pricePackage
                .withIsPublic(false)
                .withClients(List.of(allowedPricePackageClient(client)));

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void clientIsNotAllowedInPublicPackage() {
        pricePackage
                .withIsPublic(true)
                .withClients(List.of(disallowedPricePackageClient(client)));

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void clientIsNotAllowedInNotPublicPackage() {
        pricePackage
                .withIsPublic(false)
                .withClients(List.of(disallowedPricePackageClient(client)));

        ValidationResult<List<PricePackage>, Defect> actual = validatePricePackage(pricePackage);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(PricePackage.CLIENTS), index(0), field(PricePackageClient.IS_ALLOWED)),
                invalidValue()))));
    }

    @Test
    public void listOfTwo() {
        var pricePackage2 = clientPricePackage(client);
        var pricePackages2 = List.of(pricePackage, pricePackage2);

        var actual = validatePricePackages(pricePackages2);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void priceIgnoredCpd() {
        var pricePackageCPD = clientPricePackage(client);
        Currency currency = pricePackageCPD.getCurrency().getCurrency();
        pricePackageCPD.withPrice(currency.getMaxCpmPrice().add(BigDecimal.ONE));
        pricePackageCPD.withIsCpd(true);
        pricePackageCPD.getTargetingsCustom().getRetargetingCondition().withCryptaSegments(emptyList());
        pricePackageCPD.getTargetingsFixed().withCryptaSegments(emptyList());

        var actual = validatePricePackage(pricePackageCPD);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }


    @Test
    public void videoFrontpage_valid() {
        var actual = validatePricePackage(videoFrontpage(client));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void videoFrontpage_invalidViewTypes() {
        var pack = videoFrontpage(client);
        pack.getTargetingsFixed().setViewTypes(emptyList());
        var actual = validatePricePackage(pack);
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(PricePackage.TARGETINGS_FIXED),
                                field(TargetingsFixed.VIEW_TYPES)),
                        CANNOT_BE_EMPTY))));
    }

    private PricePackage videoFrontpage(ClientInfo client) {
        PricePackage pricePackage = clientPricePackage(client)
                .withIsFrontpage(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withAllowedCreativeTemplates(null);
        pricePackage.getTargetingsFixed().withAllowExpandedDesktopCreative(false);
        return pricePackage;
    }

    private PricePackage clientPricePackage(ClientInfo client) {
        return new PricePackage()
                .withTitle("Title_1")
                .withTrackerUrl("http://ya.ru")
                .withPrice(BigDecimal.valueOf(2999))
                .withCurrency(CurrencyCode.RUB)
                .withOrderVolumeMin(1L)
                .withOrderVolumeMax(1L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(DEFAULT_GEO)
                        .withGeoExpanded(DEFAULT_GEO_EXPANDED)
                        .withGeoType(DEFAULT_GEO_TYPE)
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(new TargetingsCustom()
                        .withRetargetingCondition(DEFAULT_RETARGETING_CONDITION))
                .withDateStart(LocalDate.of(2020, 1, 1))
                .withDateEnd(LocalDate.of(2020, 1, 1))
                .withIsPublic(false)
                .withIsSpecial(false)
                .withIsCpd(false)
                .withCampaignOptions(new PricePackageCampaignOptions())
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE))
                .withAllowedCreativeTemplates(DEFAULT_ALLOWED_CREATIVE_TYPES)
                .withClients(List.of(allowedPricePackageClient(client)))
                .withCategoryId(1L);
    }

    private ValidationResult<List<PricePackage>, Defect> validatePricePackage(PricePackage pricePackage) {
        return validatePricePackages(singletonList(pricePackage));
    }

    private ValidationResult<List<PricePackage>, Defect> validatePricePackages(List<PricePackage> pricePackages) {
        return service.validatePricePackages(pricePackages, geoTreeFactory.getGlobalGeoTree(), new User().withUid(1L));
    }
}
