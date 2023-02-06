package ru.yandex.market.logistics.front.library;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.front.library.annotation.EnumField;
import ru.yandex.market.logistics.front.library.annotation.QueryParam;
import ru.yandex.market.logistics.front.library.annotation.WithAutocomplete;
import ru.yandex.market.logistics.front.library.dto.Autocomplete;
import ru.yandex.market.logistics.front.library.dto.FrontEnum;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.QueryParamObject;
import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.detail.DetailField;
import ru.yandex.market.logistics.front.library.dto.grid.GridColumn;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WithAutocompleteTest {
    @MethodSource("autocompleteArguments")
    @ParameterizedTest(name = "{index} : {1}")
    void autocompleteGridColumn(
        @SuppressWarnings("unused") String displayName,
        TestDto dto,
        Autocomplete expectedAutocomplete
    ) {
        GridData gridView = ViewUtils.getGridView(Collections.singletonList(dto), Mode.VIEW);
        Autocomplete actualAutocomplete = gridView.getMeta().getColumns().stream()
            .filter(column -> "partner".equals(column.getName()))
            .findFirst().map(GridColumn::getAutocomplete)
            .orElseThrow(() -> new NoSuchElementException("No value present"));
        assertThat(actualAutocomplete).isEqualToComparingFieldByFieldRecursively(expectedAutocomplete);
    }

    @MethodSource("autocompleteArguments")
    @ParameterizedTest(name = "{index} : {1}")
    void autocompleteDetailColumn(
        @SuppressWarnings("unused") String displayName,
        TestDto dto,
        Autocomplete expectedAutocomplete
    ) {
        DetailData detailData = ViewUtils.getDetail(dto, Mode.VIEW, false);
        Autocomplete actualAutocomplete = detailData.getMeta().getFields().stream()
            .filter(column -> "partner".equals(column.getName()))
            .findFirst().map(DetailField::getAutocomplete)
            .orElseThrow(() -> new NoSuchElementException("No value present"));
        assertThat(actualAutocomplete).isEqualToComparingFieldByFieldRecursively(expectedAutocomplete);
    }

    @MethodSource("autocompleteWithIllegalStateArguments")
    @ParameterizedTest(name = "{index} : {1}")
    void autocompleteWithIllegalState(
        @SuppressWarnings("unused") String displayName,
        TestDto dto
    ) {
        assertThatThrownBy(() -> ViewUtils.getDetail(dto, Mode.VIEW, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Должно быть заполнено ровно одно из полей options и optionsSlug");
    }

    private static Stream<Arguments> autocompleteWithIllegalStateArguments() {
        return Stream.of(
            Arguments.of(
                "Автодополнение с пустыми полями options и optionSlug",
                new TestDtoEnumBothFieldsEmpty()
            ),
            Arguments.of(
                "Автодополнение с заполненным полями options и optionSlug",
                new TestDtoEnumBothFieldsFilled()
            )
        );
    }

    private static Stream<Arguments> autocompleteArguments() {
        return Stream.of(
            Arguments.of(
                "Автодополнение со значениями по умолчанию",
                new TestDtoDefaultValues(),
                Autocomplete.builder()
                    .optionsSlug("lms/partner")
                    .multiple(false)
                    .idFieldName("id")
                    .titleFieldName("title")
                    .hint("")
                    .pageSize(20)
                    .queryParams(Collections.emptySet())
                    .queryParamName("")
                    .options(Collections.emptyList())
                    .build()
            ),
            Arguments.of(
                "Автодополнение со всеми кастомными значениями",
                new TestDtoCustomValues(),
                Autocomplete.builder()
                    .optionsSlug("lms/partner")
                    .multiple(true)
                    .authorities(Collections.singletonList("PARTNER_AUTHORITY"))
                    .idFieldName("partnerId")
                    .titleFieldName("name")
                    .hint("Введите название партнера")
                    .pageSize(5)
                    .queryParams(ImmutableSet.of(new QueryParamObject("key", ImmutableSet.of("v1", "v2"))))
                    .queryParamName("term")
                    .options(Collections.emptyList())
                    .build()
            ),
            Arguments.of(
                "Автодополнение со статическими опциями",
                new TestDtoEnum(),
                Autocomplete.builder()
                    .optionsSlug("")
                    .multiple(false)
                    .idFieldName("id")
                    .titleFieldName("title")
                    .hint("")
                    .pageSize(20)
                    .queryParams(Collections.emptySet())
                    .queryParamName("")
                    .options(ImmutableList.of(new ReferenceObject("TEST", "Тест", null)))
                    .build()
            )
        );
    }

    private static class TestDto {
        private final long id = 1;

        public long getId() {
            return id;
        }
    }

    private static class TestDtoDefaultValues extends TestDto {
        @WithAutocomplete(optionsSlug = "lms/partner")
        private final ReferenceObject partner = new ReferenceObject()
            .setId("1")
            .setDisplayName("DPD")
            .setSlug("lms/partner");

        public ReferenceObject getPartner() {
            return partner;
        }
    }

    private static class TestDtoCustomValues extends TestDto {
        @WithAutocomplete(
            optionsSlug = "lms/partner",
            authorities = {"PARTNER_AUTHORITY"},
            titleFieldName = "name",
            idFieldName = "partnerId",
            hint = "Введите название партнера",
            multiple = true,
            pageSize = 5,
            queryParams = {
                @QueryParam(key = "key", values = {"v1", "v2"})
            },
            queryParamName = "term"
        )
        private final ReferenceObject partner = new ReferenceObject()
            .setId("1")
            .setDisplayName("DPD")
            .setSlug("lms/partner");

        public ReferenceObject getPartner() {
            return partner;
        }
    }

    private static class TestDtoEnum extends TestDto {
        @WithAutocomplete(
            options = @EnumField(value = FrontEnumTest.class)
        )
        private final ReferenceObject partner = new ReferenceObject()
            .setId("1")
            .setDisplayName("DPD")
            .setSlug("lms/partner");

        public ReferenceObject getPartner() {
            return partner;
        }
    }

    private static class TestDtoEnumBothFieldsEmpty extends TestDto {
        @WithAutocomplete()
        private final ReferenceObject partner = new ReferenceObject()
            .setId("1")
            .setDisplayName("DPD")
            .setSlug("lms/partner");

        public ReferenceObject getPartner() {
            return partner;
        }
    }

    private static class TestDtoEnumBothFieldsFilled extends TestDto {
        @WithAutocomplete(
            optionsSlug = "test",
            options = @EnumField(value = FrontEnumTest.class)
        )
        private final ReferenceObject partner = new ReferenceObject()
            .setId("1")
            .setDisplayName("DPD")
            .setSlug("lms/partner");

        public ReferenceObject getPartner() {
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
