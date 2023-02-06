package ru.yandex.direct.core.entity.banner.type.mobilecontent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithMobileContent;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
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
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.trackingSystemDomainNotSupported;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.trackingUrlDoesntContainMacros;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithMobileContentValidatorProviderTest {

    private static final Path PATH = path(index(0), field(BannerWithMobileContent.IMPRESSION_URL));
    private static final String NOT_TRUSTED_HREF = "http://not-trusted.ru";
    private static final String INVALID_HREF = "invalidHref";
    private static final String VALID_HREF = "http://ya.ru";

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public BannerWithMobileContent banner;

    @Parameterized.Parameter(2)
    public Defect<String> expectedDefect;

    @Mock
    private TrustedRedirectsService trustedRedirectsService;
    @Mock
    private TrackingUrlParseService trackingUrlParseService;
    private BannerWithMobileContentValidatorProvider provider;
    private BannersAddOperationContainer validationContainer;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "impressionUrl не задан",
                        new MobileAppBanner().withImpressionUrl(null),
                        null
                },
                {
                        "Валидация по умолчанию",
                        new MobileAppBanner().withImpressionUrl(VALID_HREF),
                        null
                },
                {
                        "Невалидный impressionUrl",
                        new MobileAppBanner().withImpressionUrl(INVALID_HREF),
                        invalidHref()
                },
                {
                        "not trusted impressionUrl",
                        new MobileAppBanner().withImpressionUrl(NOT_TRUSTED_HREF),
                        trackingSystemDomainNotSupported()
                },
                {
                        "impressionUrl не содержит макрос",
                        new MobileAppBanner().withImpressionUrl(VALID_HREF),
                        trackingUrlDoesntContainMacros()
                },
        });
    }

    @Before
    public void before() {
        initMocks(this);
        when(trustedRedirectsService.checkImpressionUrl(eq(NOT_TRUSTED_HREF)))
                .thenReturn(TrustedRedirectsService.Result.NOT_TRUSTED);
        when(trustedRedirectsService.checkImpressionUrl(not(eq(NOT_TRUSTED_HREF))))
                .thenReturn(TrustedRedirectsService.Result.TRUSTED);

        provider = new BannerWithMobileContentValidatorProvider(trustedRedirectsService, trackingUrlParseService);

        validationContainer = newBannerValidationContainer()
                .withIndexToAdGroupForOperationMap(emptyMap())
                .withBannerToIndexMap(Map.of(banner, 0))
                .build();
    }

    @Test
    public void testValidationProvider() {
        ValidationResult<List<BannerWithMobileContent>, Defect> vr = validate(banner);
        if (expectedDefect != null) {
            assertThat(vr, hasDefectDefinitionWith(validationError(PATH, expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private ValidationResult<List<BannerWithMobileContent>, Defect> validate(BannerWithMobileContent banner) {
        return ListValidationBuilder.<BannerWithMobileContent, Defect>of(singletonList(banner))
                .checkEachBy(provider.bannerWithMobileContentValidator(validationContainer))
                .getResult();
    }
}
