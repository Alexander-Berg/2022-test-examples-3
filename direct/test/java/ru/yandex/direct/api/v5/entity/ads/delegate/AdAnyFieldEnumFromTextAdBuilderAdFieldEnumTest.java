package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.TextAdBuilderAdFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum.fromTextAdBuilderAdFieldEnum;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdAnyFieldEnumFromTextAdBuilderAdFieldEnumTest {

    @Parameterized.Parameter
    public TextAdBuilderAdFieldEnum textAdBuilderAdFieldEnum;

    @Parameterized.Parameter(1)
    public AdAnyFieldEnum adAnyFieldEnum;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {TextAdBuilderAdFieldEnum.CREATIVE, AdAnyFieldEnum.TEXT_AD_BUILDER_AD_CREATIVE},
                {TextAdBuilderAdFieldEnum.HREF, AdAnyFieldEnum.TEXT_AD_BUILDER_AD_HREF},
        };
    }

    @Test
    public void test() {
        assertThat(fromTextAdBuilderAdFieldEnum(textAdBuilderAdFieldEnum)).isSameAs(adAnyFieldEnum);
    }
}
