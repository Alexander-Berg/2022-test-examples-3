package ru.yandex.market.logistics.front.library;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.front.library.annotation.EnumField;
import ru.yandex.market.logistics.front.library.annotation.Filter;
import ru.yandex.market.logistics.front.library.annotation.WithAutocomplete;
import ru.yandex.market.logistics.front.library.annotation.WithFilters;
import ru.yandex.market.logistics.front.library.dto.Autocomplete;
import ru.yandex.market.logistics.front.library.dto.FilterObject;
import ru.yandex.market.logistics.front.library.dto.Filters;
import ru.yandex.market.logistics.front.library.dto.FrontEnum;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.front.library.dto.Type;
import ru.yandex.market.logistics.front.library.dto.grid.GridColumn;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

class WithFiltersTest {
    @MethodSource("filtersArguments")
    @ParameterizedTest(name = "{index} : {1}")
    void withFiltersGridColumn(
        @SuppressWarnings("unused") String displayName,
        TestDto dto,
        Filters expectedFilters
    ) {
        GridData gridView = ViewUtils.getGridView(Collections.singletonList(dto), Mode.VIEW);
        Optional<Filters> actualAutocomplete = gridView.getMeta().getColumns().stream()
            .filter(column -> "partner".equals(column.getName()))
            .findFirst()
            .map(GridColumn::getFilters);
        assertThat(actualAutocomplete).hasValueSatisfying(value -> assertThat(value)
            .isEqualToComparingFieldByFieldRecursively(expectedFilters)
        );
    }

    private static Stream<Arguments> filtersArguments() {
        return Stream.of(
            Arguments.of(
                "Столбец с фильтрами",
                new TestDtoWithFilters(),
                new Filters(Arrays.asList(
                    new FilterObject("test1", "тест1", Type.NUMBER, null, null),
                    new FilterObject("test2", "тест2", Type.DATE,
                        Autocomplete.builder()
                            .optionsSlug("test")
                            .multiple(false)
                            .idFieldName("id")
                            .titleFieldName("title")
                            .hint("")
                            .pageSize(20)
                            .queryParams(Collections.emptySet())
                            .queryParamName("")
                            .options(Collections.emptyList())
                            .build(),
                        null
                    ),
                    new FilterObject("test3", "тест3", Type.DATE, Autocomplete.builder()
                        .optionsSlug("")
                        .multiple(false)
                        .idFieldName("id")
                        .titleFieldName("title")
                        .hint("")
                        .pageSize(20)
                        .queryParams(Collections.emptySet())
                        .queryParamName("")
                        .options(ImmutableList.of(new ReferenceObject("TEST", "Тест", null)))
                        .build(),
                        null
                    ),
                    new FilterObject("test4", "тест4", Type.DATE, null, null)
                ))
            )
        );
    }

    private static class TestDto {
        private final long id = 1;

        public long getId() {
            return id;
        }
    }

    private static class TestDtoWithFilters extends TestDto {
        @WithFilters(filters = {
            @Filter(name = "test1", title = "тест1", type = Integer.class),
            @Filter(
                name = "test2",
                title = "тест2", type = LocalDate.class,
                autocomplete = @WithAutocomplete(optionsSlug = "test")
            ),
            @Filter(
                name = "test3",
                title = "тест3",
                type = LocalDate.class,
                autocomplete = @WithAutocomplete(options = @EnumField(value = FrontEnumTest.class))
            ),
            @Filter(
                name = "test4",
                title = "тест4", type = LocalDate.class
            ),
        })
        private final String partner = "partner";

        public String getPartner() {
            return partner;
        }
    }

    private enum FrontEnumTest implements FrontEnum {

        TEST("Тест");

        private final String title;

        FrontEnumTest(String title) {
            this.title = title;
        }

        @Override
        public String getTitle() {
            return this.title;
        }

        @Override
        public String getName() {
            return this.name();
        }

    }
}
