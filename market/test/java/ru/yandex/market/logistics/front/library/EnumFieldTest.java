package ru.yandex.market.logistics.front.library;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.front.library.annotation.EnumField;
import ru.yandex.market.logistics.front.library.dto.FrontEnum;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.front.library.dto.grid.GridColumn;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumFieldTest {

    @DisplayName("Конвертация статических опций")
    @ParameterizedTest(name = "{index} : {1}")
    @MethodSource("enumFieldArguments")
    void excludeEnumConstant(
        @SuppressWarnings("unused") String displayName,
        IdDto dto,
        List<ReferenceObject> expectedOptions
    ) {
        assertThat(convertAndGetOptions(dto))
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(expectedOptions);
    }

    @Nonnull
    private static Stream<Arguments> enumFieldArguments() {
        return Stream.of(
            Arguments.of(
                "Перечисление с пустой опцией",
                new NullableOptionDto(),
                Arrays.asList(
                    new ReferenceObject(null, "—", null),
                    new ReferenceObject("APPLE", "Яблоко", null),
                    new ReferenceObject("BANANA", "Банан", null),
                    new ReferenceObject("ORANGE", "Апельсин", null)
                )
            ),
            Arguments.of(
                "Перечисление с исключением части опций",
                new ExcludeOptionsDto(),
                Arrays.asList(
                    new ReferenceObject("APPLE", "Яблоко", null),
                    new ReferenceObject("BANANA", "Банан", null)
                )
            ),
            Arguments.of(
                "Перечисление с выбором части опций",
                new IncludeOptionsDto(),
                Arrays.asList(
                    new ReferenceObject(null, "—", null),
                    new ReferenceObject("APPLE", "Яблоко", null),
                    new ReferenceObject("ORANGE", "Апельсин", null)
                )
            )
        );
    }

    private enum Fruit implements FrontEnum {
        APPLE("Яблоко"),
        BANANA("Банан"),
        ORANGE("Апельсин");

        private final String title;

        Fruit(String title) {
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getName() {
            return name();
        }
    }

    private static class IdDto {
        private final long id = 1;

        public long getId() {
            return id;
        }
    }

    private static class NullableOptionDto extends IdDto {
        @EnumField(
            value = Fruit.class,
            nullable = true
        )
        private final Fruit fruit = Fruit.APPLE;

        public Fruit getFruit() {
            return fruit;
        }
    }

    private static class ExcludeOptionsDto extends IdDto {
        @EnumField(
            value = Fruit.class,
            names = "ORANGE",
            mode = EnumField.Mode.EXCLUDE
        )
        private final Fruit fruit = Fruit.APPLE;

        public Fruit getFruit() {
            return fruit;
        }
    }

    private static class IncludeOptionsDto extends IdDto {
        @EnumField(
            value = Fruit.class,
            names = {"APPLE", "ORANGE"},
            mode = EnumField.Mode.INCLUDE,
            nullable = true
        )
        private final Fruit fruit = Fruit.APPLE;

        public Fruit getFruit() {
            return fruit;
        }
    }

    @Test
    @DisplayName("Среди констант перечисления нет указанного имени")
    void invalidOptionName() {
        assertThatThrownBy(() -> convertAndGetOptions(new InvalidOptionNameDto()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Invalid enum constant names [KIWI, PINEAPPLE] in EnumField annotation. " +
                    "Valid names include: [APPLE, BANANA, ORANGE]"
            );
    }

    private static class InvalidOptionNameDto extends IdDto {
        @EnumField(
            value = Fruit.class,
            names = {"PINEAPPLE", "KIWI"},
            mode = EnumField.Mode.EXCLUDE
        )
        private final Fruit fruit = Fruit.APPLE;

        public Fruit getFruit() {
            return fruit;
        }
    }

    @Nonnull
    private List<ReferenceObject> convertAndGetOptions(IdDto dto) {
        return ViewUtils.getGridView(Collections.singletonList(dto), Mode.VIEW)
            .getMeta()
            .getColumns().stream()
            .filter(column -> "fruit".equals(column.getName()))
            .findFirst()
            .map(GridColumn::getOptions)
            .orElseThrow(() -> new NoSuchElementException("No value present"));
    }
}
