package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.MobileAppAdFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum.fromMobileAppAdFieldEnum;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdAnyFieldEnumFromMobileAppAdFieldEnumTest {

    @Parameterized.Parameter
    public MobileAppAdFieldEnum mobileAppAdFieldEnum;

    @Parameterized.Parameter(1)
    public AdAnyFieldEnum adAnyFieldEnum;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {MobileAppAdFieldEnum.AD_IMAGE_HASH, AdAnyFieldEnum.MOBILE_APP_AD_IMAGE_HASH},
                {MobileAppAdFieldEnum.TITLE, AdAnyFieldEnum.MOBILE_APP_AD_TITLE},
                {MobileAppAdFieldEnum.TEXT, AdAnyFieldEnum.MOBILE_APP_AD_TEXT},
                {MobileAppAdFieldEnum.FEATURES, AdAnyFieldEnum.MOBILE_APP_AD_FEATURES},
                {MobileAppAdFieldEnum.ACTION, AdAnyFieldEnum.MOBILE_APP_AD_ACTION},
                {MobileAppAdFieldEnum.TRACKING_URL, AdAnyFieldEnum.MOBILE_APP_AD_TRACKING_URL},
                {MobileAppAdFieldEnum.AD_IMAGE_MODERATION, AdAnyFieldEnum.MOBILE_APP_AD_IMAGE_MODERATION},
        };
    }

    @Test
    public void test() {
        assertThat(fromMobileAppAdFieldEnum(mobileAppAdFieldEnum)).isSameAs(adAnyFieldEnum);
    }
}
