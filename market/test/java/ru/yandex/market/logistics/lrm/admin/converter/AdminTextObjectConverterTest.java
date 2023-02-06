package ru.yandex.market.logistics.lrm.admin.converter;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lrm.LrmTest;

@ParametersAreNonnullByDefault
@DisplayName("Конвертация в текстовое поле")
class AdminTextObjectConverterTest extends LrmTest {

    private final AdminTextObjectConverter textObjectConverter = new AdminTextObjectConverter();

    @Test
    @DisplayName("Конвертация без аргументов")
    void convertToStringNoArgs() {
        softly.assertThat(textObjectConverter.convertToStringRepresentation()).isNotNull();
    }

    @Test
    @DisplayName("Конвертация строковых аргументов")
    void convertStringPair() {
        softly.assertThat(
            textObjectConverter.convertToStringRepresentation(
                Pair.of("a", "b"),
                Pair.of("b", "c"),
                //blank or null skipped
                Pair.of("c", "   "),
                Pair.of("    ", "d"),
                Pair.of("", ""),
                Pair.of("d", null),
                Pair.of(null, "e"),
                Pair.of(null, null)
            )
        )
            .isEqualTo(
                """
                    a : b
                    b : c\
                    """
            );
    }

    @Test
    @DisplayName("Если элементы пары - объекты, то берется значение их метода toString()")
    void convertObjectPairs() {
        softly.assertThat(textObjectConverter.convertToStringRepresentation(
            Pair.of(SomeObject.of("a"), AnotherObject.of("b")),
            Pair.of(SomeObject.of("c"), AnotherObject.of("d"))
        ))
            .isEqualTo(
                """
                    AdminTextObjectConverterTest.SomeObject(text=a) : data is b
                    AdminTextObjectConverterTest.SomeObject(text=c) : data is d\
                    """
            );
    }

    @Value
    private static class SomeObject {
        String text;

        static SomeObject of(String text) {
            return new SomeObject(text);
        }
    }

    @Value
    private static class AnotherObject {
        String data;

        static AnotherObject of(String data) {
            return new AnotherObject(data);
        }

        @Nonnull
        @Override
        public String toString() {
            return "data is " + data;
        }
    }

}
