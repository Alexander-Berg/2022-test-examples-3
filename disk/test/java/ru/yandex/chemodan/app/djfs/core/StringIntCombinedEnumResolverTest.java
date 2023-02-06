package ru.yandex.chemodan.app.djfs.core;

import org.junit.Test;

import ru.yandex.misc.enums.OrdinalIntEnum;
import ru.yandex.misc.test.Assert;

public class StringIntCombinedEnumResolverTest {
    public enum TestEnum implements OrdinalIntEnum, StringIntCombinedEnum {
        A(0),
        B(1),
        ;

        private final int value;

        TestEnum(int value) {
            this.value = value;
        }

        @Override
        public int value() {
            return value;
        }

        @Override
        public int getIntRepresentation() {
            return value();
        }

        @Override
        public String getStringRepresentation() {
            return name().toLowerCase();
        }
    }

    private static StringIntCombinedEnumResolver<TestEnum> sut = StringIntCombinedEnumResolver.r(TestEnum.class);

    @Test
    public void valueOfOForString() {
        Assert.equals(StringIntCombinedEnumResolverTest.TestEnum.A, sut.valueOfO("a").get());
        Assert.equals(StringIntCombinedEnumResolverTest.TestEnum.B, sut.valueOfO("b").get());

        Assert.none(sut.valueOfO("A"));
        Assert.none(sut.valueOfO("c"));
        Assert.none(sut.valueOfO(""));
        Assert.none(sut.valueOfO(null));
    }

    @Test
    public void valueOfOForInt() {
        Assert.equals(StringIntCombinedEnumResolverTest.TestEnum.A, sut.valueOfO(0).get());
        Assert.equals(StringIntCombinedEnumResolverTest.TestEnum.B, sut.valueOfO(1).get());

        Assert.none(sut.valueOfO(2));
    }
}
