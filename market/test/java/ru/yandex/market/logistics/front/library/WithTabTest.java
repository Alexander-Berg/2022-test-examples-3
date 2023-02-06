package ru.yandex.market.logistics.front.library;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.front.library.annotation.FieldOrder;
import ru.yandex.market.logistics.front.library.annotation.WithTab;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.Tab;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

class WithTabTest {

    @MethodSource("singleActionArgumentsProvider")
    @ParameterizedTest(name = "{index} : {1}")
    void testSingleAction(@SuppressWarnings("unused") String displayName, TestDto dto, List<Tab> tabs) {
        DetailData detailView = ViewUtils.getDetail(dto, Mode.VIEW, false);
        assertThat(detailView.getMeta().getTabs())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyElementsOf(tabs);
    }

    private static Stream<Arguments> singleActionArgumentsProvider() {
        return Stream.of(
            Arguments.of(
                "Порядок, указанный в @FieldOrder, игнорируется",
                new TestDtoWithTabWithFieldOrder(),
                Arrays.asList(
                    Tab.builder().title("Адрес").fields(Arrays.asList("address", "comment")).build(),
                    Tab.builder().title("ФИО").fields(Arrays.asList("firstName", "lastName")).build()
                )

            ),
            Arguments.of(
                "Без @FieldOrder",
                new TestDtoWithTab(),
                Arrays.asList(
                    Tab.builder().title("Адрес").fields(Arrays.asList("address", "comment")).build(),
                    Tab.builder().title("ФИО").fields(Arrays.asList("firstName", "lastName")).build()
                )
            )
        );
    }

    private static class TestDto {
        private final long id = 1;
        private final String address = "Новосибирск, ул. Николаева, д. 11";
        private final String comment = "Встречаемся у входа в кофейню";
        private final String lastName = "Иванов";
        private final String firstName = "Иван";

        public long getId() {
            return id;
        }

        public String getAddress() {
            return address;
        }

        public String getComment() {
            return comment;
        }

        public String getLastName() {
            return lastName;
        }

        public String getFirstName() {
            return firstName;
        }
    }

    @FieldOrder({"comment", "address", "lastName", "firstName"})
    @WithTab(title = "Адрес", fields = {"address", "comment"})
    @WithTab(title = "ФИО", fields = {"firstName", "lastName"})
    private static class TestDtoWithTabWithFieldOrder extends TestDto {
    }

    @WithTab(title = "Адрес", fields = {"address", "comment"})
    @WithTab(title = "ФИО", fields = {"firstName", "lastName"})
    private static class TestDtoWithTab extends TestDto {
    }
}
