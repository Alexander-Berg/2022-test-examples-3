package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupForBannerOperation;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.CpmAudioBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.McBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.core.entity.uac.service.trackingurl.TrackingUrlParseService;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidDomain;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.trackingSystemDomainNotSupported;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.trackingUrlDoesntContainMacros;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithHrefValidatorProviderTest {

    private static final Path PATH = path(index(0), field(BannerWithHref.HREF));
    private static final String NOT_TRUSTED_HREF = "http://not-trusted.ru";
    private static final String INVALID_HREF = "invalidHref";
    private static final String VALID_HREF = "http://ya.ru";

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public BannerWithHref banner;

    @Parameterized.Parameter(2)
    public Defect<String> expectedDefect;

    @Parameterized.Parameter(3)
    public Map<Integer, AdGroupForBannerOperation> adGroupByBannerPosition;

    @Mock
    private TrustedRedirectsService trustedRedirectsService;
    private TrackingUrlParseService trackingUrlParseService;

    private BannerWithHrefValidatorProvider provider;
    private BannersAddOperationContainer validationContainer;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер: href не задан",
                        new TextBanner().withHref(null),
                        null,
                        emptyMap()
                },
                {
                        "Текстовый баннер: href пуст",
                        new TextBanner().withHref(null),
                        null,
                        emptyMap()
                },
                {
                        "ContentPromotion баннер: href не задан",
                        new ContentPromotionBanner().withHref(null),
                        null,
                        emptyMap()
                },
                {
                        "Динамический баннер: валидация по умолчанию",
                        new DynamicBanner().withHref(VALID_HREF),
                        null,
                        emptyMap()
                },
                {
                        "Динамический баннер: невалидный href",
                        new DynamicBanner().withHref(INVALID_HREF),
                        invalidHref(),
                        emptyMap()
                },
                {
                        "MobileApp баннер: корректный href",
                        new MobileAppBanner().withHref(VALID_HREF),
                        null,
                        emptyMap()
                },
                {
                        "MobileApp баннер: невалидный href",
                        new MobileAppBanner().withHref(INVALID_HREF),
                        invalidHref(),
                        emptyMap()
                },
                {
                        "MobileApp баннер: not trusted href",
                        new MobileAppBanner().withHref(NOT_TRUSTED_HREF),
                        trackingSystemDomainNotSupported(),
                        emptyMap()
                },
                {
                        "MobileApp баннер: href не содержит макрос",
                        new MobileAppBanner().withHref(VALID_HREF),
                        trackingUrlDoesntContainMacros(),
                        emptyMap()
                },
                {
                        "McBanner: href не задан",
                        new McBanner().withHref(null),
                        notNull(),
                        emptyMap()
                },
                {
                        "McBanner: невалидный href",
                        new McBanner().withHref(INVALID_HREF),
                        invalidHref(),
                        emptyMap()
                },
                {
                        "McBanner: валидный href",
                        new McBanner().withHref(VALID_HREF),
                        null,
                        emptyMap()
                },
                {
                        "CpmAudioBanner: невалидный href",
                        new CpmAudioBanner().withHref(INVALID_HREF),
                        invalidHref(),
                        emptyMap()
                },
                {
                        "ImageBanner: невалидный href",
                        new ImageBanner().withHref(INVALID_HREF),
                        invalidHref(),
                        emptyMap()
                },
                {
                        "ImageBanner: not trusted href, trackingHrefValidator не вызывается",
                        new ImageBanner().withHref(NOT_TRUSTED_HREF),
                        null,
                        emptyMap()
                },
                {
                        "ImageBanner: mobile_content, not trusted href, trackingHrefValidator вызывается",
                        new ImageBanner().withHref(NOT_TRUSTED_HREF),
                        trackingSystemDomainNotSupported(),
                        Map.of(0, adGroupWithType(AdGroupType.MOBILE_CONTENT))
                },
                {
                        "CpmBanner: валидный href",
                        new CpmBanner().withHref(VALID_HREF),
                        null,
                        emptyMap()
                },
                {
                        "CpmBanner: невалидный href",
                        new CpmBanner().withHref(INVALID_HREF),
                        invalidHref(),
                        emptyMap()
                },
                {
                        "CpmBanner: любой href запрещен при adGroupType = cpm_geoproduct",
                        new CpmBanner().withHref(VALID_HREF),
                        isNull(),
                        Map.of(0, adGroupWithType(AdGroupType.CPM_GEOPRODUCT))
                },
                {
                        "CpmBanner: href не задан при adGroupType = cpm_geoproduct",
                        new CpmBanner().withHref(null),
                        null,
                        Map.of(0, adGroupWithType(AdGroupType.CPM_GEOPRODUCT))
                },
                {
                        "CpmIndoorBanner: href состоит только из протокола и домена",
                        new CpmIndoorBanner().withHref(VALID_HREF),
                        null,
                        Map.of(0, adGroupWithType(AdGroupType.CPM_INDOOR))
                },
                {
                        "CpmIndoorBanner: href включает что-то еще, кроме протокола и домена",
                        new CpmIndoorBanner().withHref(VALID_HREF + "?param=value"),
                        invalidDomain(),
                        Map.of(0, adGroupWithType(AdGroupType.CPM_INDOOR))
                },
                {
                        "CpmOutdoorBanner: href состоит только из протокола и домена",
                        new CpmOutdoorBanner().withHref(VALID_HREF),
                        null,
                        Map.of(0, adGroupWithType(AdGroupType.CPM_OUTDOOR))
                },
                {
                        "CpmOutdoorBanner: href включает что-то еще, кроме протокола и домена",
                        new CpmOutdoorBanner().withHref(VALID_HREF + "?param=value"),
                        invalidDomain(),
                        Map.of(0, adGroupWithType(AdGroupType.CPM_OUTDOOR))
                },
        });
    }

    @Before
    public void before() {
        initMocks(this);
        when(trustedRedirectsService.checkTrackingHref(eq(NOT_TRUSTED_HREF)))
                .thenReturn(TrustedRedirectsService.Result.NOT_TRUSTED);
        when(trustedRedirectsService.checkTrackingHref(not(eq(NOT_TRUSTED_HREF))))
                .thenReturn(TrustedRedirectsService.Result.TRUSTED);

        provider = new BannerWithHrefValidatorProvider(trustedRedirectsService, trackingUrlParseService);

        validationContainer = newBannerValidationContainer()
                .withIndexToAdGroupForOperationMap(adGroupByBannerPosition)
                .withBannerToIndexMap(Map.of(banner, 0))
                .build();
    }

    @Test
    public void testValidationProvider() {
        ValidationResult<List<BannerWithHref>, Defect> vr = validate(banner);
        if (expectedDefect != null) {
            assertThat(vr, hasDefectDefinitionWith(validationError(PATH, expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private ValidationResult<List<BannerWithHref>, Defect> validate(BannerWithHref banner) {
        return ListValidationBuilder.<BannerWithHref, Defect>of(singletonList(banner))
                .checkEachBy(provider.bannerWithHrefValidator(validationContainer))
                .getResult();
    }

    private static AdGroupForBannerOperation adGroupWithType(AdGroupType adGroupType) {
        return new AdGroup().withType(adGroupType);
    }
}
