package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import com.yandex.direct.api.v5.adgroups.DynamicTextAdGroupFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.delegate.AdGroupAnyFieldEnum.fromDynamicTextAdGroupFieldEnum;

@RunWith(Parameterized.class)
public class AdGroupAnyFieldEnumFromDynamicTextAdGroupFieldEnumTest {

    @Parameterized.Parameter
    public DynamicTextAdGroupFieldEnum dynamicTextAdGroupField;

    @Parameterized.Parameter(1)
    public AdGroupAnyFieldEnum adGroupAnyField;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {DynamicTextAdGroupFieldEnum.DOMAIN_URL, AdGroupAnyFieldEnum.DYNAMIC_TEXT_AD_GROUP_DOMAIN_URL},
                {DynamicTextAdGroupFieldEnum.DOMAIN_URL_PROCESSING_STATUS,
                        AdGroupAnyFieldEnum.DYNAMIC_TEXT_AD_GROUP_DOMAIN_URL_PROCESSING_STATUS},
        };
    }

    @Test
    public void test() {
        assertThat(fromDynamicTextAdGroupFieldEnum(dynamicTextAdGroupField)).isSameAs(adGroupAnyField);
    }
}
