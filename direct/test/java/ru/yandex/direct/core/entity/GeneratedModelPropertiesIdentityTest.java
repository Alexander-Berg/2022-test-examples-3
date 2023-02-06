package ru.yandex.direct.core.entity;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.autobudget.model.CpaAutobudgetAlert;
import ru.yandex.direct.core.entity.autobudget.model.HourlyAutobudgetAlert;
import ru.yandex.direct.core.entity.banner.model.BannerAddition;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerSimple;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithVcard;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.model.ModelProperty;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class GeneratedModelPropertiesIdentityTest {

    @Parameter
    public ModelProperty<?, ?> propA;

    @Parameter(1)
    public ModelProperty<?, ?> propB;

    @Parameter(2)
    public boolean expected;

    @Parameters(name = "assert {2}: {0} == {1}")
    public static Iterable<Object[]> params() {
        return asList(

                positiveCase(OldBanner.ID, OldTextBanner.ID),
                positiveCase(OldBannerSimple.AD_GROUP_ID, OldTextBanner.AD_GROUP_ID),
                positiveCase(OldBannerWithVcard.VCARD_ID, OldTextBanner.VCARD_ID),
                positiveCase(OldBannerWithVcard.AD_GROUP_ID, OldBanner.AD_GROUP_ID),
                positiveCase(OldBanner.REVERSE_DOMAIN, OldTextBanner.REVERSE_DOMAIN),
                positiveCase(CpaAutobudgetAlert.CID, HourlyAutobudgetAlert.CID),

                negativeCase(OldBanner.ID, AdGroup.ID),
                negativeCase(AdGroup.CAMPAIGN_ID, Campaign.ID),
                negativeCase(OldBanner.ID, BannerAddition.ID)
        );
    }

    @Test
    public void testModelPropertiesIdentity() throws Exception {
        assertThat(propA == propB).isEqualTo(expected);
    }

    private static Object[] positiveCase(ModelProperty<?, ?> propA, ModelProperty<?, ?> propB) {
        return new Object[]{propA, propB, true};
    }

    private static Object[] negativeCase(ModelProperty<?, ?> propA, ModelProperty<?, ?> propB) {
        return new Object[]{propA, propB, false};
    }

}
