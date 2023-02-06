package ru.yandex.direct.core.entity.banner.type.turbolanding;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithHrefAndTurboLandingAndVcardAndOrganization;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredButEmptyHrefOrTurboOrVcardIdOrPermalink;
import static ru.yandex.direct.core.entity.banner.type.turbolanding.BannerWithTurboLandingConstraints.hrefOrTurboLandingOrVcardOrOrganizationIsSet;

@RunWith(Parameterized.class)
public class BannerWithTurboLandingConstraintsHrefOrTurboLandingOrVcardOrOrganizationIsSetTest {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public BannerWithHrefAndTurboLandingAndVcardAndOrganization banner;

    @Parameterized.Parameter(2)
    public boolean enableFeatureDesktopLanding;

    @Parameterized.Parameter(3)
    public Defect<?> expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "href != null",
                        new TextBanner().withHref("http://ya.ru"),
                        false,
                        null
                },
                {
                        "vcardId != null",
                        new TextBanner().withVcardId(1L),
                        false,
                        null
                },
                {
                        "permalinkId != null",
                        new TextBanner().withPermalinkId(2L),
                        false,
                        null
                },
                {
                        "turboLandingId != null; feature disabled",
                        new TextBanner().withTurboLandingId(3L),
                        false,
                        requiredButEmptyHrefOrTurboOrVcardIdOrPermalink()
                },
                {
                        "turboLandingId != null; feature enabled",
                        new TextBanner().withTurboLandingId(3L),
                        true,
                        null
                },
                {
                        "all fields = null",
                        new TextBanner(),
                        false,
                        requiredButEmptyHrefOrTurboOrVcardIdOrPermalink()
                },
        });
    }

    @Test
    public void testConstraint() {
        Constraint<BannerWithHrefAndTurboLandingAndVcardAndOrganization, Defect> constraint =
                hrefOrTurboLandingOrVcardOrOrganizationIsSet(enableFeatureDesktopLanding);
        assertThat(constraint.apply(banner)).isEqualTo(expectedDefect);
    }
}
