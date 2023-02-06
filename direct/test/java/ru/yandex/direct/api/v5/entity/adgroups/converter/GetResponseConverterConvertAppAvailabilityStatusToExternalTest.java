package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.time.LocalDateTime;

import com.yandex.direct.api.v5.adgroups.AppAvailabilityStatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertAppAvailabilityStatusToExternal;

@RunWith(Parameterized.class)
public class GetResponseConverterConvertAppAvailabilityStatusToExternalTest {

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public MobileContent mobileContent;

    @Parameterized.Parameter(2)
    public AppAvailabilityStatusEnum expectedStatus;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"modifyTime is null", new MobileContent().withModifyTime(null), AppAvailabilityStatusEnum.UNPROCESSED},
                {"modifyTime is not null and isAvailable is true",
                        new MobileContent().withModifyTime(LocalDateTime.now()).withIsAvailable(Boolean.TRUE),
                        AppAvailabilityStatusEnum.AVAILABLE},
                {"modifyTime is not null and isAvailable is false",
                        new MobileContent().withModifyTime(LocalDateTime.now()).withIsAvailable(Boolean.FALSE),
                        AppAvailabilityStatusEnum.NOT_AVAILABLE},
        };
    }

    @Test
    public void test() {
        assertThat(convertAppAvailabilityStatusToExternal(mobileContent)).isEqualTo(expectedStatus);
    }
}
