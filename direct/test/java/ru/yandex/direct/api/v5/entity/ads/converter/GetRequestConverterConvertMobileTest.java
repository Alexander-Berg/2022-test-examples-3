package ru.yandex.direct.api.v5.entity.ads.converter;

import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Test;

import ru.yandex.direct.core.entity.YesNo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetRequestConverter.convertMobile;

public class GetRequestConverterConvertMobileTest {
    @Test
    public void test_yes() {
        assertThat(convertMobile(YesNoEnum.YES)).isEqualByComparingTo(YesNo.YES);
    }

    @Test
    public void test_no() {
        assertThat(convertMobile(YesNoEnum.NO)).isEqualByComparingTo(YesNo.NO);
    }

    @Test
    public void test_null() {
        assertThat(convertMobile(null)).isNull();
    }
}
