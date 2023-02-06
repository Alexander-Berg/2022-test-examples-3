package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import com.yandex.direct.api.v5.adgroups.MobileAppAdGroupFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.delegate.AdGroupAnyFieldEnum.fromMobileAppAdGroupFieldEnum;

@RunWith(Parameterized.class)
public class AdGroupAnyFieldEnumFromMobileAppAdGroupFieldEnumTest {
    @Parameterized.Parameter
    public MobileAppAdGroupFieldEnum mobileAppAdGroupField;

    @Parameterized.Parameter(1)
    public AdGroupAnyFieldEnum adGroupAnyField;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {MobileAppAdGroupFieldEnum.APP_AVAILABILITY_STATUS,
                        AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_APP_AVAILABILITY_STATUS},
                {MobileAppAdGroupFieldEnum.APP_ICON_MODERATION,
                        AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_APP_ICON_MODERATION},
                {MobileAppAdGroupFieldEnum.APP_OPERATING_SYSTEM_TYPE,
                        AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_APP_OPERATING_SYSTEM_TYPE},
                {MobileAppAdGroupFieldEnum.STORE_URL, AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_STORE_URL},
                {MobileAppAdGroupFieldEnum.TARGET_CARRIER, AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_TARGET_CARRIER},
                {MobileAppAdGroupFieldEnum.TARGET_DEVICE_TYPE,
                        AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_TARGET_DEVICE_TYPE},
                {MobileAppAdGroupFieldEnum.TARGET_OPERATING_SYSTEM_VERSION,
                        AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_TARGET_OPERATING_SYSTEM_VERSION},
        };
    }

    @Test
    public void test() {
        assertThat(fromMobileAppAdGroupFieldEnum(mobileAppAdGroupField)).isSameAs(adGroupAnyField);
    }
}
