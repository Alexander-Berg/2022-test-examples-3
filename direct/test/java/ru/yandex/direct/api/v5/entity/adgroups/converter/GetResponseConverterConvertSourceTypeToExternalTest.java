package ru.yandex.direct.api.v5.entity.adgroups.converter;

import com.yandex.direct.api.v5.adgroups.SourceTypeGetEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertFeedTypeToExternal;

@RunWith(Parameterized.class)
public class GetResponseConverterConvertSourceTypeToExternalTest {

    @Parameterized.Parameter
    public Long source;

    @Parameterized.Parameter(1)
    public SourceTypeGetEnum expectedSourceType;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {null, SourceTypeGetEnum.UNKNOWN},
                {111L, SourceTypeGetEnum.RETAIL_FEED},
        };
    }

    @Test
    public void test() {
        assertThat(convertFeedTypeToExternal(source)).isEqualTo(expectedSourceType);
    }
}
