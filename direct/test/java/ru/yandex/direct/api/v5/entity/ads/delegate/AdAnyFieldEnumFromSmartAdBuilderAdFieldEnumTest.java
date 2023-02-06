package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.SmartAdBuilderAdFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum.fromSmartAdBuilderAdFieldEnum;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdAnyFieldEnumFromSmartAdBuilderAdFieldEnumTest {

    @Parameterized.Parameter
    public SmartAdBuilderAdFieldEnum smartAdBuilderAdFieldEnum;

    @Parameterized.Parameter(1)
    public AdAnyFieldEnum adAnyFieldEnum;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {SmartAdBuilderAdFieldEnum.CREATIVE, AdAnyFieldEnum.SMART_AD_BUILDER_AD_CREATIVE}
        };
    }

    @Test
    public void test() {
        assertThat(fromSmartAdBuilderAdFieldEnum(smartAdBuilderAdFieldEnum)).isSameAs(adAnyFieldEnum);
    }
}
