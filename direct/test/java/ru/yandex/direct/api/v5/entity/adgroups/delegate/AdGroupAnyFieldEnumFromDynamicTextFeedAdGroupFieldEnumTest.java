package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import com.yandex.direct.api.v5.adgroups.DynamicTextFeedAdGroupFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.delegate.AdGroupAnyFieldEnum.fromDynamicTextFeedAdGroupFieldEnum;

@RunWith(Parameterized.class)
public class AdGroupAnyFieldEnumFromDynamicTextFeedAdGroupFieldEnumTest {
    @Parameterized.Parameter
    public DynamicTextFeedAdGroupFieldEnum dynamicTextFeedAdGroupField;

    @Parameterized.Parameter(1)
    public AdGroupAnyFieldEnum adGroupAnyField;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {DynamicTextFeedAdGroupFieldEnum.SOURCE, AdGroupAnyFieldEnum.DYNAMIC_TEXT_FEED_AD_GROUP_SOURCE},
                {DynamicTextFeedAdGroupFieldEnum.SOURCE_PROCESSING_STATUS,
                        AdGroupAnyFieldEnum.DYNAMIC_TEXT_FEED_AD_GROUP_SOURCE_PROCESSING_STATUS},
                {DynamicTextFeedAdGroupFieldEnum.SOURCE_TYPE,
                        AdGroupAnyFieldEnum.DYNAMIC_TEXT_FEED_AD_GROUP_SOURCE_TYPE},
        };
    }

    @Test
    public void test() {
        assertThat(fromDynamicTextFeedAdGroupFieldEnum(dynamicTextFeedAdGroupField))
                .isSameAs(adGroupAnyField);
    }
}
