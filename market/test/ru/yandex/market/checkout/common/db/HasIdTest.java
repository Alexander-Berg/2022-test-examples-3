package ru.yandex.market.checkout.common.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author sergeykoles
 * Created on: 11.04.18
 */
public class HasIdTest {

    @Test
    public void valueByStringIdWithUnknown() {
        Assertions.assertSame(MyTestEnum.VALUE1, HasId.valueById("Value1", MyTestEnum.class));
        Assertions.assertSame(MyTestEnum.VALUE2, HasId.valueById("vAlUe2", MyTestEnum.class));
        Assertions.assertSame(MyTestEnum.UNKNOWN, HasId.valueById("value1", MyTestEnum.class));
        Assertions.assertNull(HasId.valueById(null, MyTestEnum.class));
    }

    @Test
    public void valueByLongIdWithoutUnknown() {
        Assertions.assertSame(MySecondTestEnum.VALUE1, HasId.valueById(1L, MySecondTestEnum.class));
        Assertions.assertSame(MySecondTestEnum.VALUE2, HasId.valueById(2L, MySecondTestEnum.class));
        Assertions.assertNull(HasId.valueById(3L, MySecondTestEnum.class));
    }


    public enum MyTestEnum implements HasId<String> {
        VALUE1("Value1"),
        VALUE2("vAlUe2"),
        UNKNOWN("unknown");

        private final String id;

        MyTestEnum(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isUnknown() {
            return this == UNKNOWN;
        }
    }

    public enum MySecondTestEnum implements HasId<Long> {
        VALUE1(1L),
        VALUE2(2L);

        private final long id;

        MySecondTestEnum(long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public boolean isUnknown() {
            return false;
        }
    }
}
