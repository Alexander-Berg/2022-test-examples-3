package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.yandex.direct.api.v5.adgroups.TargetDeviceTypeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertTargetDeviceTypeToExternal;

@RunWith(Parameterized.class)
public class GetResponseConverterConvertTargetDeviceTypeToExternalTest {

    @Parameterized.Parameter
    public Set<MobileContentAdGroupDeviceTypeTargeting> actualParam;

    @Parameterized.Parameter(1)
    public Set<TargetDeviceTypeEnum> expectedResult;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {new LinkedHashSet<>(Collections.singleton(MobileContentAdGroupDeviceTypeTargeting.PHONE)),
                        new LinkedHashSet<>(Collections.singleton(TargetDeviceTypeEnum.DEVICE_TYPE_MOBILE))},
                {new LinkedHashSet<>(Collections.singleton(MobileContentAdGroupDeviceTypeTargeting.TABLET)),
                        new LinkedHashSet<>(Collections.singleton(TargetDeviceTypeEnum.DEVICE_TYPE_TABLET))},
                {new LinkedHashSet<>(Arrays.asList(MobileContentAdGroupDeviceTypeTargeting.PHONE,
                        MobileContentAdGroupDeviceTypeTargeting.TABLET)), new LinkedHashSet<>(
                        Arrays.asList(TargetDeviceTypeEnum.DEVICE_TYPE_MOBILE,
                                TargetDeviceTypeEnum.DEVICE_TYPE_TABLET))},
        };
    }

    @Test
    public void test() {
        assertThat(convertTargetDeviceTypeToExternal(actualParam)).isEqualTo(expectedResult);
    }
}
