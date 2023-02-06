package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import com.yandex.direct.api.v5.adgroups.AdGroupFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.delegate.AdGroupAnyFieldEnum.fromAdGroupFieldEnum;

@RunWith(Parameterized.class)
public class AdGroupAnyFieldEnumFromAdGroupFieldEnumTest {

    @Parameterized.Parameter
    public AdGroupFieldEnum adGroupField;

    @Parameterized.Parameter(1)
    public AdGroupAnyFieldEnum adGroupAnyField;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {AdGroupFieldEnum.ID, AdGroupAnyFieldEnum.AD_GROUP_ID},
                {AdGroupFieldEnum.CAMPAIGN_ID, AdGroupAnyFieldEnum.AD_GROUP_CAMPAIGN_ID},
                {AdGroupFieldEnum.NAME, AdGroupAnyFieldEnum.AD_GROUP_NAME},
                {AdGroupFieldEnum.NEGATIVE_KEYWORDS, AdGroupAnyFieldEnum.AD_GROUP_NEGATIVE_KEYWORDS},
                {AdGroupFieldEnum.NEGATIVE_KEYWORD_SHARED_SET_IDS,
                        AdGroupAnyFieldEnum.AD_GROUP_NEGATIVE_KEYWORD_SHARED_SET_IDS},
                {AdGroupFieldEnum.REGION_IDS, AdGroupAnyFieldEnum.AD_GROUP_REGION_IDS},
                {AdGroupFieldEnum.RESTRICTED_REGION_IDS, AdGroupAnyFieldEnum.AD_GROUP_RESTRICTED_REGION_IDS},
                {AdGroupFieldEnum.SERVING_STATUS, AdGroupAnyFieldEnum.AD_GROUP_SERVING_STATUS},
                {AdGroupFieldEnum.STATUS, AdGroupAnyFieldEnum.AD_GROUP_STATUS},
                {AdGroupFieldEnum.SUBTYPE, AdGroupAnyFieldEnum.AD_GROUP_SUBTYPE},
                {AdGroupFieldEnum.TRACKING_PARAMS, AdGroupAnyFieldEnum.AD_GROUP_TRACKING_PARAMS},
                {AdGroupFieldEnum.TYPE, AdGroupAnyFieldEnum.AD_GROUP_TYPE},
        };
    }

    @Test
    public void test() {
        assertThat(fromAdGroupFieldEnum(adGroupField)).isSameAs(adGroupAnyField);
    }
}
