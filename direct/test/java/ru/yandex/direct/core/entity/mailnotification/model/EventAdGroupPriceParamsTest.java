package ru.yandex.direct.core.entity.mailnotification.model;

import java.math.BigDecimal;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SuppressWarnings("ConstantConditions")
public class EventAdGroupPriceParamsTest {

    @Test
    public void jsonValue_ResultStringIsEqualToExpectedString() throws Exception {
        EventAdGroupPriceParams adGroupParams =
                new EventAdGroupPriceParams(1L, "SuperGroup", BigDecimal.ZERO, BigDecimal.ONE);
        String expectedJsonValue = "{\"pid\":1,\"group_name\":\"SuperGroup\",\"old_text\":0,\"new_text\":1}";

        assertThat(adGroupParams.jsonValue()).isEqualTo(expectedJsonValue);
    }

    @Test
    public void fromJson_ResultObjectIsEqualToExpectedObject() throws Exception {
        String jsonValue = "{\"old_text\":0,\"new_text\":1,\"pid\":1,\"group_name\":\"SuperGroup\"}";
        //noinspection unchecked
        EventAdGroupPriceParams actual = EventParams.fromJson(jsonValue, EventAdGroupPriceParams.class);
        EventAdGroupPriceParams expected =
                new EventAdGroupPriceParams(1L, "SuperGroup", BigDecimal.ZERO, BigDecimal.ONE);

        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void constructor_NewValueEqualsOld_ThrowsException() throws Exception {
        Throwable throwable = catchThrowable(() -> {
            BigDecimal scaledZero = BigDecimal.valueOf(0, 6);
            new EventAdGroupPriceParams(1L, "SuperGroup", BigDecimal.ZERO, scaledZero);
        });

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class);
    }
}
