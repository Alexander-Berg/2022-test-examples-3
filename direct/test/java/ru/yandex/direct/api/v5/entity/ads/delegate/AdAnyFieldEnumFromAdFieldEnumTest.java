package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum.fromAdFieldEnum;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdAnyFieldEnumFromAdFieldEnumTest {

    @Parameterized.Parameter
    public AdFieldEnum adFieldEnum;

    @Parameterized.Parameter(1)
    public AdAnyFieldEnum adAnyFieldEnum;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {AdFieldEnum.AD_CATEGORIES, AdAnyFieldEnum.AD_CATEGORIES},
                {AdFieldEnum.AGE_LABEL, AdAnyFieldEnum.AD_AGE_LABEL},
                {AdFieldEnum.AD_GROUP_ID, AdAnyFieldEnum.AD_ADGROUP_ID},
                {AdFieldEnum.CAMPAIGN_ID, AdAnyFieldEnum.AD_CAMPAIGN_ID},
                {AdFieldEnum.ID, AdAnyFieldEnum.AD_ID},
                {AdFieldEnum.STATE, AdAnyFieldEnum.AD_STATE},
                {AdFieldEnum.STATUS, AdAnyFieldEnum.AD_STATUS},
                {AdFieldEnum.STATUS_CLARIFICATION, AdAnyFieldEnum.AD_STATUS_CLARIFICATION},
                {AdFieldEnum.TYPE, AdAnyFieldEnum.AD_TYPE},
                {AdFieldEnum.SUBTYPE, AdAnyFieldEnum.AD_SUBTYPE},
        };
    }

    @Test
    public void test() {
        assertThat(fromAdFieldEnum(adFieldEnum)).isSameAs(adAnyFieldEnum);
    }
}
