package ru.yandex.direct.api.v5.entity.adgroups.converter;

import com.yandex.direct.api.v5.general.MobileOperatingSystemTypeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertAppOperatingSystemTypeToExternal;

@RunWith(Parameterized.class)
public class GetResponseConverterConvertAppOperatingSystemTypeToExternalTest {

    @Parameterized.Parameter()
    public OsType actualParam;

    @Parameterized.Parameter(1)
    public MobileOperatingSystemTypeEnum expectedResult;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {OsType.ANDROID, MobileOperatingSystemTypeEnum.ANDROID},
                {OsType.IOS, MobileOperatingSystemTypeEnum.IOS},
                {null, MobileOperatingSystemTypeEnum.OS_TYPE_UNKNOWN},
        };
    }

    @Test
    public void test() {
        assertThat(convertAppOperatingSystemTypeToExternal(new MobileContent().withOsType(actualParam)))
                .isEqualTo(expectedResult);
    }
}
