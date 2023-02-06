package ru.yandex.market.logistics.front.library;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import ru.yandex.market.logistics.front.library.annotation.FieldOrder;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.detail.DetailField;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

class FieldOrderTest {

    @ParameterizedTest(name = "{index}: {2}")
    @ArgumentsSource(SortArgumentsProviderImpl.class)
    void testFieldsSorted(BaseMock mockDto, List<String> expectedFieldOrder, String description) {

        DetailData detailView = ViewUtils.getDetail(mockDto, Mode.VIEW, false);

        List<String> actualFieldOrder = detailView.getMeta()
            .getFields()
            .stream()
            .map(DetailField::getName)
            .collect(Collectors.toList());


        Assertions.assertEquals(expectedFieldOrder, actualFieldOrder, "field order must be equal");
    }

    private static class SortArgumentsProviderImpl implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                Arguments.of(new MockCommon(), Arrays.asList("name", "length", "active"), "all fields sorted"),
                Arguments.of(new MockPartial(), Arrays.asList("active", "name", "length"), "part of fields sorted"),
                Arguments.of(new MockOne(), Arrays.asList("name", "active", "length"), "one set, other alphabetical"),
                Arguments.of(new MockSameFieldRepeated(), Arrays.asList("name", "active", "length"), "same field " +
                    "repeated"),
                Arguments.of(new MockUnknown(), Arrays.asList("active", "length", "name"), "unknown fields"),
                Arguments.of(new MockSomeUnknown(), Arrays.asList("name", "length", "active"), "all fields and some " +
                    "unknown"),
                Arguments.of(new MockNoOrder(), Arrays.asList("active", "length", "name"), "no order")
            );
        }
    }

    @FieldOrder({"name", "id", "length", "active"})
    private static class MockCommon extends BaseMock {
    }

    @FieldOrder({"active", "name"})
    private static class MockPartial extends BaseMock {
    }

    @FieldOrder({"name"})
    private static class MockOne extends BaseMock {
    }

    @FieldOrder({"name", "name", "name", "name"})
    private static class MockSameFieldRepeated extends BaseMock {
    }

    @FieldOrder({"aa", "aa", "bb", "namee"})
    private static class MockUnknown extends BaseMock {
    }

    @FieldOrder({"a", "b", "c", "d", "e", "f", "name", "id", "length", "active"})
    private static class MockSomeUnknown extends BaseMock {
    }

    private static class MockNoOrder extends BaseMock {
    }

    private static class BaseMock {
        private Long id = 1L;
        private String name = "name";
        private boolean active = true;
        private int length = 4;

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isActive() {
            return active;
        }

        public int getLength() {
            return length;
        }
    }
}
