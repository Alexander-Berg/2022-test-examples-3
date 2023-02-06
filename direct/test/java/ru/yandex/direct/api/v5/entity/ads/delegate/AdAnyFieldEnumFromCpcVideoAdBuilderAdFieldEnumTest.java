package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.CpcVideoAdBuilderAdFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum.fromCpcVideoAdBuilderAdFieldEnum;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdAnyFieldEnumFromCpcVideoAdBuilderAdFieldEnumTest {

    @Parameterized.Parameter
    public CpcVideoAdBuilderAdFieldEnum cpcVideoAdBuilderAdFieldEnum;

    @Parameterized.Parameter(1)
    public AdAnyFieldEnum adAnyFieldEnum;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {CpcVideoAdBuilderAdFieldEnum.CREATIVE, AdAnyFieldEnum.CPC_VIDEO_AD_BUILDER_AD_CREATIVE},
                {CpcVideoAdBuilderAdFieldEnum.HREF, AdAnyFieldEnum.CPC_VIDEO_AD_BUILDER_AD_HREF},
        };
    }

    @Test
    public void test() {
        assertThat(fromCpcVideoAdBuilderAdFieldEnum(cpcVideoAdBuilderAdFieldEnum)).isSameAs(adAnyFieldEnum);
    }
}
