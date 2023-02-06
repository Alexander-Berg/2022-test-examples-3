package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.TextImageAdFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum.fromTextImageAdFieldEnum;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdAnyFieldEnumFromTextImageAdFieldEnumTest {

    @Parameterized.Parameter
    public TextImageAdFieldEnum textImageAdFieldEnum;

    @Parameterized.Parameter(1)
    public AdAnyFieldEnum adAnyFieldEnum;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {TextImageAdFieldEnum.AD_IMAGE_HASH, AdAnyFieldEnum.TEXT_IMAGE_AD_IMAGE_HASH},
                {TextImageAdFieldEnum.HREF, AdAnyFieldEnum.TEXT_IMAGE_AD_HREF},
        };
    }

    @Test
    public void test() {
        assertThat(fromTextImageAdFieldEnum(textImageAdFieldEnum)).isSameAs(adAnyFieldEnum);
    }
}
