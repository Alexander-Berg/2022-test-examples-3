package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.MobileAppAdBuilderAdFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum.fromMobileAppAdBuilderAdFieldEnum;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdAnyFieldEnumFromMobileAppAdBuilderAdFieldEnumTest {

    @Parameterized.Parameter
    public MobileAppAdBuilderAdFieldEnum mobileAppAdBuilderAdFieldEnum;

    @Parameterized.Parameter(1)
    public AdAnyFieldEnum adAnyFieldEnum;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {MobileAppAdBuilderAdFieldEnum.CREATIVE, AdAnyFieldEnum.MOBILE_APP_AD_BUILDER_AD_CREATIVE},
                {MobileAppAdBuilderAdFieldEnum.TRACKING_URL, AdAnyFieldEnum.MOBILE_APP_AD_BUILDER_AD_TRACKING_URL},
        };
    }

    @Test
    public void test() {
        assertThat(fromMobileAppAdBuilderAdFieldEnum(mobileAppAdBuilderAdFieldEnum)).isSameAs(adAnyFieldEnum);
    }
}
