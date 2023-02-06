package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.general.AdGroupTypesEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetRequestConverter.convertTypes;

@RunWith(Parameterized.class)
public class GetRequestConverterConvertTypesTest {

    @Parameterized.Parameter
    public List<AdGroupTypesEnum> types;

    @Parameterized.Parameter(1)
    public Set<AdGroupType> expectedTypes;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {singletonList(AdGroupTypesEnum.TEXT_AD_GROUP), EnumSet.of(AdGroupType.BASE)},
                {singletonList(AdGroupTypesEnum.MOBILE_APP_AD_GROUP), EnumSet.of(AdGroupType.MOBILE_CONTENT)},
                {singletonList(AdGroupTypesEnum.DYNAMIC_TEXT_AD_GROUP), EnumSet.of(AdGroupType.DYNAMIC)},
                {singletonList(AdGroupTypesEnum.SMART_AD_GROUP), EnumSet.of(AdGroupType.PERFORMANCE)},
                {singletonList(AdGroupTypesEnum.CONTENT_PROMOTION_AD_GROUP), EnumSet.of(AdGroupType.CONTENT_PROMOTION)},
                {Arrays.asList(AdGroupTypesEnum.TEXT_AD_GROUP,
                        AdGroupTypesEnum.MOBILE_APP_AD_GROUP,
                        AdGroupTypesEnum.DYNAMIC_TEXT_AD_GROUP),
                        EnumSet.of(AdGroupType.BASE, AdGroupType.MOBILE_CONTENT, AdGroupType.DYNAMIC)},
                {Arrays.asList(AdGroupTypesEnum.TEXT_AD_GROUP, AdGroupTypesEnum.MOBILE_APP_AD_GROUP,
                        AdGroupTypesEnum.DYNAMIC_TEXT_AD_GROUP, AdGroupTypesEnum.TEXT_AD_GROUP,
                        AdGroupTypesEnum.MOBILE_APP_AD_GROUP, AdGroupTypesEnum.DYNAMIC_TEXT_AD_GROUP,
                        AdGroupTypesEnum.SMART_AD_GROUP, AdGroupTypesEnum.CONTENT_PROMOTION_AD_GROUP),
                        EnumSet.of(AdGroupType.BASE,
                                AdGroupType.MOBILE_CONTENT,
                                AdGroupType.DYNAMIC,
                                AdGroupType.PERFORMANCE,
                                AdGroupType.CONTENT_PROMOTION)},
        };
    }

    @Test
    public void test() {
        assertThat(convertTypes(types)).containsExactlyInAnyOrder(expectedTypes.toArray(new AdGroupType[0]));
    }
}
