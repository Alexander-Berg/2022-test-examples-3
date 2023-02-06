package ru.yandex.market.logistics.front.library;

import java.util.Collections;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import ru.yandex.market.logistics.front.library.annotation.WithFullTextSearchString;
import ru.yandex.market.logistics.front.library.dto.FullTextSearchString;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

class WithFullTextSearchTest {
    @ParameterizedTest(name = "[{index}] {2}")
    @ArgumentsSource(WithFullTextSearchArgumentsProvider.class)
    void dtoWithSearchString(TestDto dto, String expectedName, String caseName) {
        GridData gridView = ViewUtils.getGridView(Collections.singletonList(dto), Mode.VIEW);
        Assertions.assertThat(gridView.getMeta().getSearchString())
            .as("search string has to be equal")
            .isEqualToComparingFieldByField(new FullTextSearchString(expectedName));
    }

    private static class WithFullTextSearchArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(defaultName(), specifiedName());
        }

        private Arguments defaultName() {
            return Arguments.of(new TestDtoWithSearchStringDefaultName(), "fullTextSearch", "default name");
        }

        private Arguments specifiedName() {
            return Arguments.of(new TestDtoWithSearchString(), "test-name", "specified name");
        }
    }

    @Test
    @DisplayName("without search string")
    void dtoWithoutSearchString() {
        GridData gridView = ViewUtils.getGridView(Collections.singletonList(new TestDto()), Mode.VIEW);
        Assertions.assertThat(gridView.getMeta().getSearchString())
            .as("search string has to be null")
            .isNull();
    }

    private static class TestDto {
        private final long id = 1L;

        public long getId() {
            return id;
        }
    }

    @WithFullTextSearchString(name = "test-name")
    private static class TestDtoWithSearchString extends TestDto {
    }

    @WithFullTextSearchString
    private static class TestDtoWithSearchStringDefaultName extends TestDto {
    }
}
