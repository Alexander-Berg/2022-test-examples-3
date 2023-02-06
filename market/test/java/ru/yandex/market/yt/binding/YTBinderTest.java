package ru.yandex.market.yt.binding;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.common.YtSerializationException;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.impl.ytree.object.NullSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.OptionSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeKeyField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeSerializerClass;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class YTBinderTest {

    @Test
    void testBindersReuse() {
        var binder1 = YTBinder.getBinder(Class1.class);
        var binder2 = YTBinder.getBinder(Class2.class);
        assertThat(binder1).isNotEqualTo(binder2);
        assertThat(YTBinder.getBinder(Class1.class)).isSameAs(binder1);
        assertThat(YTBinder.getBinder(Class2.class)).isSameAs(binder2);

        var sbinder1 = YTBinder.getStaticBinder(Class1.class);
        var sbinder2 = YTBinder.getStaticBinder(Class2.class);
        assertThat(sbinder1)
                .isNotEqualTo(sbinder2)
                .isNotEqualTo(binder1);

        assertThat(YTBinder.getStaticBinder(Class1.class)).isSameAs(sbinder1);
        assertThat(YTBinder.getStaticBinder(Class2.class)).isSameAs(sbinder2);
    }

    @Test
    void testBindersExtends() {
        var binder1 = YTBinder.getBinder(Class1.class);
        var binder2 = YTBinder.getBinder(Class2.class);
        var binder3 = YTBinder.getBinder(Class3.class);

        var s1 = binder1.getCypressSchema();
        var s2 = binder2.getCypressSchema();
        var s3 = binder3.getCypressSchema();

        assertThat(s1).hasToString("<\"unique_keys\"=%true;\"strict\"=%true>[{\"name\"=\"id1\";\"type\"=\"int32\";" +
                "\"sort_order\"=\"ascending\";\"required\"=%true}]");
        assertThat(s2).hasToString("<\"unique_keys\"=%true;\"strict\"=%true>[{\"name\"=\"id1\";\"type\"=\"int32\";" +
                "\"sort_order\"=\"ascending\";\"required\"=%true};{\"name\"=\"id2\";\"type\"=\"int32\";" +
                "\"sort_order\"=\"ascending\";\"required\"=%true};{\"name\"=\"value3\";\"type\"=\"int32\";" +
                "\"required\"=%false}]");

        // Сначала ключи, потом поля
        assertThat(s3).hasToString("<\"unique_keys\"=%true;\"strict\"=%true>[{\"name\"=\"id1\";\"type\"=\"int32\";" +
                "\"sort_order\"=\"ascending\";\"required\"=%true};{\"name\"=\"id2\";\"type\"=\"int32\";" +
                "\"sort_order\"=\"ascending\";\"required\"=%true};{\"name\"=\"id3\";\"type\"=\"int32\";" +
                "\"sort_order\"=\"ascending\";\"required\"=%true};{\"name\"=\"value3\";\"type\"=\"int32\";" +
                "\"required\"=%false}]");
    }

    @Test
    void supportsOptionals() {
        var binder = YTBinder.getBinder(Nullability.class);
        assertThat(binder.getCypressSchema())
                .hasToString("<\"unique_keys\"=%true;\"strict\"=%true>[" +
                "{\"name\"=\"nonnullStr\";\"type\"=\"string\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                "{\"name\"=\"nonnullEnum\";\"type\"=\"string\";\"required\"=%true};" + // nonnull
                "{\"name\"=\"nullableStr\";\"type\"=\"string\";\"required\"=%false};" +
                "{\"name\"=\"nullableTime\";\"type\"=\"int64\";\"required\"=%false}" +
                "]");

        assertThat(checkSerialization(binder, new Nullability(
                "x",
                YTAttributes.InMemoryMode.none,
                null,
                null
        ))).hasToString("{\"nonnullEnum\"=\"none\";\"nonnullStr\"=\"x\"}");

        assertThat(checkSerialization(binder, new Nullability(
                "x",
                YTAttributes.InMemoryMode.none,
                "y",
                Instant.ofEpochSecond(123)
        ))).hasToString("{\"nullableTime\"=123000;\"nonnullEnum\"=\"none\";\"nonnullStr\"=\"x\";\"nullableStr\"=\"y\"}");

        for (var o : List.of(
                new Nullability(null, null, "whatever", Instant.EPOCH),
                new Nullability(null, YTAttributes.InMemoryMode.none, "whatever", Instant.EPOCH),
                new Nullability("x", null, "whatever", Instant.EPOCH)
        )) {
            assertThatExceptionOfType(YtSerializationException.class)
                    .as(
                            NullSerializationStrategy.FAIL_ON_NULL_FIELDS + " and " +
                                    OptionSerializationStrategy.SKIP_OPTION + " should together handle " +
                                    "nulls and Optional.empty values"
                    )
                    .isThrownBy(() -> checkSerialization(binder, o));
        }
    }

    @Test
    void testSupportLockGroups() {
        var binder = YTBinder.getBinder(LockGroups.class);
        assertThat(binder.getCypressSchema())
                .hasToString("<\"unique_keys\"=%true;\"strict\"=%true>[" +
                        "{\"name\"=\"key\";\"type\"=\"int32\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                        "{\"name\"=\"data\";\"lock\"=\"data\";\"type\"=\"string\";\"required\"=%false}" +
                        "]");
    }

    @Test
    void testSupportAggregateColumns() {
        var binder = YTBinder.getBinder(Aggregates.class);
        assertThat(binder.getCypressSchema())
                .hasToString("<\"unique_keys\"=%true;\"strict\"=%true>[" +
                        "{\"name\"=\"key\";\"type\"=\"int32\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                        "{\"name\"=\"data\";\"type\"=\"string\";\"required\"=%false;\"aggregate\"=\"sum\"}" +
                        "]");
    }

    @Test
    void testPriceMapping() {
        var binder1 = YTBinder.getBinder(PriceTypeLong.class);
        var binder2 = YTBinder.getBinder(PriceTypeDouble.class);

        var typeLong = new PriceTypeLong();
        typeLong.price = 5687;

        var typeDouble = new PriceTypeDouble();
        typeDouble.price = 56.87;

        var serialized = checkSerialization(binder1, typeLong);
        assertThat(serialized).hasToString("{\"price\"=5687}");

        var expect = binder2.getSerializer().deserialize(serialized);
        assertThat(expect).isEqualTo(typeDouble);
    }

    private static <T> YTreeNode checkSerialization(YTBinder<T> binder, T object) {
        var yTreeBuilder = new YTreeBuilder();
        binder.getSerializer().serialize(object, yTreeBuilder);
        var serialized = yTreeBuilder.build();
        var deserialized = binder.getSerializer().deserialize(serialized);
        assertThat(deserialized).isEqualTo(object);
        return serialized;
    }

    @Test
    void testCompatibleLevel12() {
        var binder1 = YTBinder.getBinder(Class1.class);
        var binder2 = YTBinder.getBinder(Class2.class);
        binder1.checkCompatibleWith(binder2);
        binder2.checkCompatibleWith(binder1);
    }

    @Test
    void testCompatibleLevel13() {
        var binder1 = YTBinder.getBinder(Class1.class);
        var binder3 = YTBinder.getBinder(Class3.class);
        binder1.checkCompatibleWith(binder3);
        binder3.checkCompatibleWith(binder1);
    }

    @Test
    void testCompatibleLevel23() {
        var binder2 = YTBinder.getBinder(Class2.class);
        var binder3 = YTBinder.getBinder(Class3.class);
        binder2.checkCompatibleWith(binder3);
        binder3.checkCompatibleWith(binder2);
    }

    @Test
    void testIncompatible() {
        var binder1 = YTBinder.getBinder(PriceTypeLong.class);
        var binder2 = YTBinder.getBinder(PriceTypeDouble.class);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> binder1.checkCompatibleWith(binder2));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> binder2.checkCompatibleWith(binder1));
    }

    @YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
    static class Class1 {

        @YTreeKeyField
        private int id1;
    }


    @YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
    static class Class2 extends Class1 {

        @YTreeKeyField
        private int id2;

        private int value3;
    }

    @YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
    static class Class3 extends Class2 {

        @YTreeKeyField
        private int id3;
    }

    @YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
    static class LockGroups {
        @YTreeKeyField
        private int key;
        @YTLockGroup("data")
        private String data;
    }

    @YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
    static class Aggregates {
        @YTreeKeyField
        private int key;
        @YTreeField(key = "data", aggregate = "sum")
        private String data;
    }

    @YTreeObject // не указываем nullSerializationStrategy
    static class Nullability {
        @YTreeKeyField
        private final String nonnullStr;
        @Nonnull
        private final YTAttributes.InMemoryMode nonnullEnum;
        private final Optional<String> nullableStr;
        private final Optional<Instant> nullableTime;

        public Nullability(
                String nonnullStr,
                YTAttributes.InMemoryMode nonnullEnum,
                @Nullable String nullableStr,
                @Nullable Instant nullableTime) {
            this.nonnullStr = nonnullStr;
            this.nonnullEnum = nonnullEnum;
            this.nullableStr = Optional.ofNullable(nullableStr);
            this.nullableTime = Optional.ofNullable(nullableTime);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Nullability that = (Nullability) o;
            return nonnullStr.equals(that.nonnullStr)
                    && nonnullEnum.equals(that.nonnullEnum)
                    && nullableStr.equals(that.nullableStr)
                    && nullableTime.equals(that.nullableTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nonnullStr, nonnullEnum, nullableStr, nullableTime);
        }
    }

    @YTreeObject
    static class PriceTypeLong {
        private long price;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PriceTypeLong that = (PriceTypeLong) o;
            return price == that.price;
        }

        @Override
        public int hashCode() {
            return Objects.hash(price);
        }
    }

    @YTreeObject
    static class PriceTypeDouble {
        @YTreeSerializerClass(YTPriceFieldSerializer.class)
        private double price;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PriceTypeDouble)) {
                return false;
            }
            PriceTypeDouble that = (PriceTypeDouble) o;
            return Double.compare(that.price, price) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(price);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("price", price)
                    .toString();
        }
    }

}
