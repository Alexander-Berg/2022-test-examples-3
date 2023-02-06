package ru.yandex.direct.core.entity.banner.type.vcard;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithOrganizationAndVcard;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.type.vcard.BannerWithVcardConstraints.preferVcardOverPermalinkConsistentWithBannerSettings;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.invalidPreferVCardOverPermalink;

@RunWith(Parameterized.class)
public class BannerWithVcardConstraintsPreferVcardOverPermalinkConsistentWithBannerSettingsTest {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public BannerWithOrganizationAndVcard banner;

    @Parameterized.Parameter(2)
    public Defect<?> expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "preferVCardOverPermalink == true; permalinkId == null",
                        new TextBanner()
                                .withPreferVCardOverPermalink(true)
                                .withVcardId(1L),
                        invalidPreferVCardOverPermalink()
                },
                {
                        "preferVCardOverPermalink == true; vcardId == null",
                        new TextBanner()
                                .withPreferVCardOverPermalink(true)
                                .withPermalinkId(2L),
                        invalidPreferVCardOverPermalink()
                },
                {
                        "preferVCardOverPermalink == true; vcardId != null; permalinkId != null",
                        new TextBanner()
                                .withPreferVCardOverPermalink(true)
                                .withVcardId(1L)
                                .withPermalinkId(2L),
                        null
                },
                {
                        "preferVCardOverPermalink == true",
                        new TextBanner()
                                .withPreferVCardOverPermalink(false),
                        null
                },
                {
                        "preferVCardOverPermalink == null",
                        new TextBanner(),
                        null
                },
        });
    }

    @Test
    public void testConstraint() {
        Constraint<Boolean, Defect> constraint = preferVcardOverPermalinkConsistentWithBannerSettings(banner);
        assertThat(constraint.apply(banner.getPreferVCardOverPermalink())).isEqualTo(expectedDefect);
    }
}
