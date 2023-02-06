package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.MobileAppImageAdFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum.fromMobileAppImageAdFieldEnum;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdAnyFieldEnumFromMobileAppImageAdFieldEnumTest {

    @Parameterized.Parameter
    public MobileAppImageAdFieldEnum mobileAppImageAdFieldEnum;

    @Parameterized.Parameter(1)
    public AdAnyFieldEnum adAnyFieldEnum;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {MobileAppImageAdFieldEnum.AD_IMAGE_HASH, AdAnyFieldEnum.MOBILE_APP_IMAGE_AD_IMAGE_HASH},
                {MobileAppImageAdFieldEnum.TRACKING_URL, AdAnyFieldEnum.MOBILE_APP_IMAGE_AD_TRACKING_URL},
        };
    }

    @Test
    public void test() {
        assertThat(fromMobileAppImageAdFieldEnum(mobileAppImageAdFieldEnum)).isSameAs(adAnyFieldEnum);
    }
}
