package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.yandex.direct.api.v5.adgroups.TargetCarrierEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertTargetCarrierToExternal;

@RunWith(Parameterized.class)
public class GetResponseConverterConvertTargetCarrierToExternalTest {

    @Parameterized.Parameter
    public Set<MobileContentAdGroupNetworkTargeting> actualParam;

    @Parameterized.Parameter(1)
    public TargetCarrierEnum expectedResult;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {new LinkedHashSet<>(Collections.singleton(MobileContentAdGroupNetworkTargeting.WI_FI)),
                        TargetCarrierEnum.WI_FI_ONLY},
                {new LinkedHashSet<>(Collections.singleton(MobileContentAdGroupNetworkTargeting.CELLULAR)),
                        TargetCarrierEnum.WI_FI_AND_CELLULAR},
                {new LinkedHashSet<>(Arrays.asList(MobileContentAdGroupNetworkTargeting.WI_FI,
                        MobileContentAdGroupNetworkTargeting.CELLULAR)), TargetCarrierEnum.WI_FI_AND_CELLULAR},
        };
    }

    @Test
    public void test() {
        assertThat(convertTargetCarrierToExternal(actualParam)).isEqualTo(expectedResult);
    }
}
