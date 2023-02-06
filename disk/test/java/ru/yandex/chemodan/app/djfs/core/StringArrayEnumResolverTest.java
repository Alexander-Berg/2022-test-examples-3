package ru.yandex.chemodan.app.djfs.core;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class StringArrayEnumResolverTest {
    public enum TestEnum implements StringArrayEnum {
        A("a", "aa"),
        B("b", "bb"),
        ;
        private String value;
        private ListF<String> representations;

        TestEnum(String value, String... alternativeRepresentations) {
            this.value = value;
            this.representations = Cf.arrayList(value).plus(Cf.arrayList(alternativeRepresentations));
        }
        @Override
        public String value() {
            return value;
        }

        @Override
        public ListF<String> representations() {
            return representations;
        }
    }

    public static StringArrayEnumResolver<TestEnum> sut = StringArrayEnumResolver.r(TestEnum.class);

    @Test
    public void valueOf() {
        Assert.equals(TestEnum.A, sut.valueOf("a"));
        Assert.equals(TestEnum.A, sut.valueOf("aa"));
        Assert.equals(TestEnum.B, sut.valueOf("b"));
        Assert.equals(TestEnum.B, sut.valueOf("bb"));
    }

    @Test
    public void valueOfO() {
        Assert.equals(TestEnum.A, sut.valueOfO("a").get());
        Assert.equals(TestEnum.A, sut.valueOfO("aa").get());
        Assert.equals(TestEnum.B, sut.valueOfO("b").get());
        Assert.equals(TestEnum.B, sut.valueOfO("bb").get());

        Assert.none(sut.valueOfO("c"));
        Assert.none(sut.valueOfO(""));
        Assert.none(sut.valueOfO(null));
    }
}
