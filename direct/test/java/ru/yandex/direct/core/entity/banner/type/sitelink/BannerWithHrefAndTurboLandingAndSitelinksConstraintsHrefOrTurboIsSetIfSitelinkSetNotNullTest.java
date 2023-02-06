package ru.yandex.direct.core.entity.banner.type.sitelink;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.BannerWithHrefAndTurboLandingAndSitelinks;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidSitelinkSetIdUsage;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithHrefAndTurboLandingAndSitelinksConstraintsHrefOrTurboIsSetIfSitelinkSetNotNullTest {

    private static final Path PATH = path(index(0));

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public BannerWithHrefAndTurboLandingAndSitelinks banner;

    @Parameterized.Parameter(2)
    public boolean enableFeatureDesktopLanding;

    @Parameterized.Parameter(3)
    public Defect<String> expectedDefect;

    private BannerWithHrefAndTurboLandingAndSitelinksValidatorProvider provider =
            new BannerWithHrefAndTurboLandingAndSitelinksValidatorProvider();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер: sitelinksSetId=null",
                        new TextBanner(),
                        false,
                        null
                },
                {
                        "Текстовый баннер: href=null, turboLandingId!=null",
                        new TextBanner()
                                .withSitelinksSetId(1L)
                                .withTurboLandingId(2L),
                        false,
                        invalidSitelinkSetIdUsage()
                },
                {
                        "Текстовый баннер: href=null, turboLandingId!=null, фича DESKTOP_LANDING включена",
                        new TextBanner()
                                .withSitelinksSetId(1L)
                                .withTurboLandingId(2L),
                        true,
                        null
                },
                {
                        "Текстовый баннер: href!=null, turboLandingId=null",
                        new TextBanner()
                                .withSitelinksSetId(1L)
                                .withHref("http://ya.ru"),
                        false,
                        null
                },
        });
    }

    @Test
    public void testValidationProvider() {
        ValidationResult<List<BannerWithHrefAndTurboLandingAndSitelinks>, Defect> vr =
                validate(banner, createValidationContainer());
        if (expectedDefect != null) {
            assertThat(vr, hasDefectWithDefinition(validationError(PATH, expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private BannersAddOperationContainer createValidationContainer() {
        Set<String> clientEnabledFeatures = enableFeatureDesktopLanding
                ? Set.of(FeatureName.DESKTOP_LANDING.getName())
                : emptySet();
        return new BannersAddOperationContainerImpl(1, 2L, RbacRole.CLIENT, ClientId.fromLong(3L),
                4L, null, null, clientEnabledFeatures, ModerationMode.FORCE_MODERATE, false, false, true);
    }

    private ValidationResult<List<BannerWithHrefAndTurboLandingAndSitelinks>, Defect> validate(
            BannerWithHrefAndTurboLandingAndSitelinks banner, BannersAddOperationContainer validationContainer) {
        return ListValidationBuilder.<BannerWithHrefAndTurboLandingAndSitelinks, Defect>of(singletonList(banner))
                .checkEachBy(provider.bannerWithHrefAndTurboLandingAndSitelinksValidator(validationContainer))
                .getResult();
    }
}
