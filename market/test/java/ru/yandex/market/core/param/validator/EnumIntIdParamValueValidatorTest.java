package ru.yandex.market.core.param.validator;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.ds.info.PhoneVisibility;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.StringParamValue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit тесты для {@link EnumIntIdParamValueValidator}.
 *
 * @author avetokhin 03.09.18.
 */
class EnumIntIdParamValueValidatorTest {

    @Test
    void validate() {
        final EnumIntIdParamValueValidator<PhoneVisibility> validator =
                new EnumIntIdParamValueValidator<>(ParamType.PHONE_VISIBILITY, PhoneVisibility.class);

        // Все доступные ID перечисления.
        for (PhoneVisibility pv : PhoneVisibility.values()) {
            assertThat(validator.isValid(new StringParamValue(ParamType.PHONE_VISIBILITY, 0, String.valueOf(pv.getId()))), equalTo(true));
        }

        // Заведомо большое число, которого нет в ID перечисления.
        assertThat(validator.isValid(new StringParamValue(ParamType.PHONE_VISIBILITY, 0, "100500")), equalTo(false));
    }
}
