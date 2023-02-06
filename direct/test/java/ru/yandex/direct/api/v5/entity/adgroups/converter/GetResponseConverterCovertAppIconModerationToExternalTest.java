package ru.yandex.direct.api.v5.entity.adgroups.converter;

import com.yandex.direct.api.v5.adgroups.ObjectFactory;
import com.yandex.direct.api.v5.general.ExtensionModeration;
import com.yandex.direct.api.v5.general.StatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.covertAppIconModerationToExternal;

@RunWith(Parameterized.class)
public class GetResponseConverterCovertAppIconModerationToExternalTest {
    private static final ObjectFactory FACTORY = new ObjectFactory();

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public MobileContent mobileContent;

    @Parameterized.Parameter(2)
    public ExtensionModeration expectedStatus;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"mobileContent's iconHash is null", new MobileContent().withIconHash(null), null},
                {"mobileContent has iconHash and moderationStatus",
                        new MobileContent().withIconHash("iconHash").withStatusIconModerate(
                                StatusIconModerate.YES),
                        new ExtensionModeration().withStatus(StatusEnum.ACCEPTED).withStatusClarification("")},
        };
    }

    @Test
    public void test() {
        assertThat(covertAppIconModerationToExternal(mobileContent)).isEqualToComparingFieldByFieldRecursively(
                FACTORY.createMobileAppAdGroupGetAppIconModeration(expectedStatus));
    }
}
