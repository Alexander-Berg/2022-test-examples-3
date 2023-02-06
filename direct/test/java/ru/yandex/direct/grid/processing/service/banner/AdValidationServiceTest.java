package ru.yandex.direct.grid.processing.service.banner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.banner.GdAdPrice;
import ru.yandex.direct.grid.processing.model.banner.GdAdPriceCurrency;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.GdTurboGalleryParams;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateCpmAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateCpmAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateSmartCentersItem;
import ru.yandex.direct.grid.processing.model.cliententity.GdPixel;
import ru.yandex.direct.grid.processing.model.cliententity.GdPixelKind;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageSmartCenter;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageSizeNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.priceGreaterThanOld;
import static ru.yandex.direct.core.entity.banner.type.price.BannerWithPriceConstants.MAX_BANNER_PRICE;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_BANNER;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.TEXT;
import static ru.yandex.direct.grid.processing.model.cliententity.GdPixelKind.AUDIENCE;
import static ru.yandex.direct.grid.processing.model.cliententity.GdPixelKind.AUDIT;
import static ru.yandex.direct.grid.processing.service.banner.AdValidationService.invalidAudiencePixelFormat;
import static ru.yandex.direct.grid.processing.service.banner.AdValidationService.invalidAuditPixelFormat;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdValidationServiceTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    public AdValidationService adValidationService;

    private static final String ONE = BigDecimal.ONE.setScale(2, HALF_UP).toString();
    private static final String TEN = BigDecimal.TEN.setScale(2, HALF_UP).toString();
    private static final String VALID_TURBO_GALLERY_HREF = "https://yandex.ru/turbo?text=any_text";

    @Test
    public void validateUpdateAds_EmptyAdItems_GotNoExceptions() {
        GdUpdateAds gdUpdateAds = new GdUpdateAds()
                .withAdUpdateItems(Collections.emptyList())
                .withSaveDraft(false);
        adValidationService.validateUpdateAdsRequest(gdUpdateAds);
    }

    @Test
    public void validateUpdateAds_InvalidId_GotGridException() {
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(path(field(GdUpdateAds.AD_UPDATE_ITEMS), index(0),
                        field(GdUpdateAd.ID)),
                        CommonDefects.validId()))));
        List<GdUpdateAd> adUpdateItems =
                singletonList(new GdUpdateAd().withId(-1L));
        GdUpdateAds gdUpdateAds = new GdUpdateAds()
                .withAdUpdateItems(adUpdateItems)
                .withSaveDraft(false);
        adValidationService.validateUpdateAdsRequest(gdUpdateAds);
    }

    @Test
    public void validateUpdateAds_NegativePrice_GotGridException() {
        checkPrice("-1", ONE, field(GdAdPrice.PRICE), greaterThan(ZERO));
    }

    @Test
    public void validateUpdateAds_PriceTooHigh_GotGridException() {
        checkPrice(BigDecimal.TEN.pow(100).toString(), ONE,
                field(GdAdPrice.PRICE), lessThanOrEqualTo(MAX_BANNER_PRICE));
    }

    @Test
    public void validateUpdateAds_NegativeOldPrice_GotGridException() {
        checkPrice(ONE, "-1", field(GdAdPrice.PRICE_OLD), greaterThan(ZERO));
    }

    @Test
    public void validateUpdateAds_OldPriceTooHigh_GotGridException() {
        checkPrice(ONE, BigDecimal.TEN.pow(100).toString(),
                field(GdAdPrice.PRICE_OLD), lessThanOrEqualTo(MAX_BANNER_PRICE));
    }

    @Test
    public void validateUpdateAds_OldPriceLowerThanNew_GotGridException() {
        checkPrice(TEN, ONE, field(GdAdPrice.PRICE_OLD), priceGreaterThanOld());
    }

    @Test
    public void validateUpdateAds_PriceInvalid_GotGridException() {
        checkPrice("test", ONE, field(GdAdPrice.PRICE), invalidValue());
    }

    @Test
    public void validateUpdateAds_OldPriceInvalid_GotGridException() {
        checkPrice(TEN, "test", field(GdAdPrice.PRICE_OLD), invalidValue());
    }

    private void checkPrice(String price, String oldPrice, PathNode.Field field, Defect defect) {
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(path(field(GdUpdateAds.AD_UPDATE_ITEMS), index(0),
                        field(GdUpdateAd.AD_PRICE), field), defect))));
        List<GdUpdateAd> adUpdateItems =
                singletonList(new GdUpdateAd()
                        .withId(1L)
                        .withAdPrice(new GdAdPrice()
                                .withPrice(price)
                                .withPriceOld(oldPrice)
                                .withCurrency(GdAdPriceCurrency.RUB)));
        GdUpdateAds gdUpdateAds = new GdUpdateAds()
                .withAdUpdateItems(adUpdateItems)
                .withSaveDraft(false);
        adValidationService.validateUpdateAdsRequest(gdUpdateAds);
    }

    @Test
    public void validateUpdateAds_ValidUpdate_GotNoExceptions() {
        List<GdUpdateAd> adUpdateItems = singletonList(new GdUpdateAd().withId(1L));
        GdUpdateAds gdUpdateAds = new GdUpdateAds().withAdUpdateItems(
                adUpdateItems)
                .withSaveDraft(false);
        adValidationService.validateUpdateAdsRequest(gdUpdateAds);
    }

    @Test
    public void validateUpdateSmartCenters_noExceptions() {
        GdUpdateSmartCentersItem updateSmartCentersItem = new GdUpdateSmartCentersItem()
                .withSize("x300")
                .withSmartCenter(new GdImageSmartCenter().withRatio("1:1").withX(2).withY(10));
        adValidationService.validateUpdateSmartCenters(singletonList(updateSmartCentersItem));
    }

    @Test
    public void validateUpdateSmartCenters_imageSizeNotFound() {
        GdUpdateSmartCentersItem updateSmartCentersItem = new GdUpdateSmartCentersItem()
                .withSize("xqwe300")
                .withSmartCenter(new GdImageSmartCenter().withRatio("1:1").withX(2).withY(10));
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(path(index(0), field(GdUpdateSmartCentersItem.SIZE)), imageSizeNotFound()))));
        adValidationService.validateUpdateSmartCenters(singletonList(updateSmartCentersItem));
    }

    @Test
    public void validateUpdateSmartCenters_paramsAreNull() {
        GdUpdateSmartCentersItem updateSmartCentersItem = new GdUpdateSmartCentersItem()
                .withSize("x300")
                .withSmartCenter(new GdImageSmartCenter());
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(path(index(0),
                        field(GdUpdateSmartCentersItem.SMART_CENTER),
                        field(GdImageSmartCenter.X)),
                        notNull()))));
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(path(index(0),
                        field(GdUpdateSmartCentersItem.SMART_CENTER),
                        field(GdImageSmartCenter.Y)),
                        notNull()))));
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(path(index(0),
                        field(GdUpdateSmartCentersItem.SMART_CENTER),
                        field(GdImageSmartCenter.RATIO)),
                        notNull()))));
        adValidationService.validateUpdateSmartCenters(singletonList(updateSmartCentersItem));
    }

    @Test
    @Parameters(method = "typesWithoutTurboGalleryHref")
    public void validateTurboGalleryHref_updateAd_invalidBannerType(GdAdType adType) {
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(path(field(GdUpdateAds.AD_UPDATE_ITEMS), index(0),
                        field(GdUpdateAd.TURBO_GALLERY_PARAMS)),
                        BannerDefects.bannerCannotHaveTurboGalleryHref()))));
        GdUpdateAd gdUpdateAd = new GdUpdateAd()
                .withAdType(adType)
                .withId(1L)
                .withTurboGalleryParams(new GdTurboGalleryParams().withTurboGalleryHref(VALID_TURBO_GALLERY_HREF));
        GdUpdateAds input = new GdUpdateAds()
                .withSaveDraft(false)
                .withAdUpdateItems(singletonList(gdUpdateAd));
        adValidationService.validateUpdateAdsRequest(input);
    }

    public Set<GdAdType> typesWithoutTurboGalleryHref() {
        EnumSet<GdAdType> gdAdTypes = EnumSet.allOf(GdAdType.class);
        gdAdTypes.remove(TEXT);
        return gdAdTypes;
    }

    @Test
    public void validateTurboGalleryHref_updateAd_success() {
        GdUpdateAd gdUpdateAd = new GdUpdateAd()
                .withAdType(TEXT)
                .withId(1L)
                .withTurboGalleryParams(new GdTurboGalleryParams().withTurboGalleryHref(VALID_TURBO_GALLERY_HREF));
        GdUpdateAds input = new GdUpdateAds()
                .withSaveDraft(false)
                .withAdUpdateItems(singletonList(gdUpdateAd));
        adValidationService.validateUpdateAdsRequest(input);
    }

    @Test
    public void validateTurboGalleryHref_addAd_success() {
        GdAddAd gdAddAd = new GdAddAd()
                .withAdType(TEXT)
                .withAdGroupId(1L)
                .withTurboGalleryHref(VALID_TURBO_GALLERY_HREF);
        GdAddAds input = new GdAddAds()
                .withSaveDraft(false)
                .withAdAddItems(singletonList(gdAddAd));
        adValidationService.validateAddAdsRequest(input);
    }

    @Test
    @Parameters(method = "urlParams")
    public void validateUpdateCpmAdsRequest_invalidPixelFormat(String url, GdPixelKind kind,
                                                               Defect<Void> expectedDefect) {
        GdUpdateCpmAd item = new GdUpdateCpmAd()
                .withAdType(CPM_BANNER)
                .withId(1L)
                .withPixels(singletonList(
                        new GdPixel()
                                .withUrl(url)
                                .withKind(kind)));
        GdUpdateCpmAds input = new GdUpdateCpmAds()
                .withSaveDraft(false)
                .withAdUpdateItems(singletonList(item));

        ValidationResult<GdUpdateCpmAds, Defect> vr = adValidationService.validateUpdateCpmAdsRequest(input,
                emptySet());

        if (expectedDefect == null) {
            assertThat(vr, hasNoDefectsDefinitions());
        } else {
            Path errPath = path(field(GdUpdateCpmAds.AD_UPDATE_ITEMS), index(0), field(GdUpdateCpmAd.PIXELS), index(0));
            assertThat(vr, hasDefectDefinitionWith(validationError(errPath, expectedDefect)));
        }
    }

    public Object[][] urlParams() {
        return new Object[][]{
                {yaAudiencePixelUrl(), AUDIENCE, null},
                {adfoxPixelUrl(), AUDIT, null},
                {yaAudiencePixelUrl(), AUDIT, invalidAuditPixelFormat()},
                {adfoxPixelUrl(), AUDIENCE, invalidAudiencePixelFormat()},
        };
    }
}
