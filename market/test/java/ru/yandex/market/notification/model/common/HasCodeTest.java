package ru.yandex.market.notification.model.common;

import javax.annotation.Nonnull;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.market.notification.model.common.HasCode.findByCode;
import static ru.yandex.market.notification.model.common.HasCode.getByCode;
import static ru.yandex.market.notification.model.common.HasCode.getCode;

/**
 * Unit-тесты для {@link HasCode}.
 *
 * @author Vladislav Bauer
 */
public class HasCodeTest {

    @Test
    public void testFindByCode() {
        assertThat(findByCode(TestEnum.class, 200).orElse(null), equalTo(TestEnum.OK));
        assertThat(findByCode(TestEnum.class, 500).orElse(null), equalTo(TestEnum.ERROR));
        assertThat(findByCode(TestEnum.class, 0).orElse(null), nullValue());
    }

    @Test
    public void testGetByCodePositive() {
        assertThat(getByCode(TestEnum.class, 200), equalTo(TestEnum.OK));
        assertThat(getByCode(TestEnum.class, 500), equalTo(TestEnum.ERROR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByCodeNegative() {
        fail(getByCode(TestEnum.class, 0).toString());
    }

    @Test
    public void testGetCode() {
        assertThat(getCode(TestEnum.OK), equalTo(200));
        assertThat(getCode(null), nullValue());
    }


    private enum TestEnum implements HasCode<Integer> {
        OK(200),
        ERROR(500);

        private final int code;

        TestEnum(final int code) {
            this.code = code;
        }

        @Nonnull
        @Override
        public Integer getCode() {
            return code;
        }
    }

}
