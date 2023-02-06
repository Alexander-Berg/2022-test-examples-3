package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.DynamicTextAdFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum.fromDynamicTextAdFieldEnum;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdAnyFieldEnumFromDynamicTextAdFieldEnumTest {

    @Parameterized.Parameter
    public DynamicTextAdFieldEnum dynamicTextAdFieldEnum;

    @Parameterized.Parameter(1)
    public AdAnyFieldEnum adAnyFieldEnum;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {DynamicTextAdFieldEnum.AD_IMAGE_HASH, AdAnyFieldEnum.DYNAMIC_TEXT_AD_IMAGE_HASH},
                {DynamicTextAdFieldEnum.SITELINK_SET_ID, AdAnyFieldEnum.DYNAMIC_TEXT_AD_SITELINK_SET_ID},
                {DynamicTextAdFieldEnum.TEXT, AdAnyFieldEnum.DYNAMIC_TEXT_AD_TEXT},
                {DynamicTextAdFieldEnum.V_CARD_ID, AdAnyFieldEnum.DYNAMIC_TEXT_AD_V_CARD_ID},
                {DynamicTextAdFieldEnum.AD_IMAGE_MODERATION, AdAnyFieldEnum.DYNAMIC_TEXT_AD_IMAGE_MODERATION},
                {DynamicTextAdFieldEnum.SITELINKS_MODERATION, AdAnyFieldEnum.DYNAMIC_TEXT_AD_SITELINKS_MODERATION},
                {DynamicTextAdFieldEnum.V_CARD_MODERATION, AdAnyFieldEnum.DYNAMIC_TEXT_AD_V_CARD_MODERATION},
                {DynamicTextAdFieldEnum.AD_EXTENSIONS, AdAnyFieldEnum.DYNAMIC_TEXT_AD_EXTENSIONS},
        };
    }

    @Test
    public void test() {
        assertThat(fromDynamicTextAdFieldEnum(dynamicTextAdFieldEnum)).isSameAs(adAnyFieldEnum);
    }
}
