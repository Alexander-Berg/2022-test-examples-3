package ru.yandex.market.logistics.front.library.json_unwrapped;

import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.annotation.DisplayName;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.detail.DetailField;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayNameJsonUnwrappedTest {

    private static final Dto DTO = new Dto()
        .setId(100500L)
        .setInner1(
            new Inner()
                .setInner(
                    new InnerInner()
                )
        )
        .setInner2(
            new Inner()
                .setInner(
                    new InnerInner()
                )
        );

    @Test
    @org.junit.jupiter.api.DisplayName("Проверка работы аннотации @DisplayName при использовании @JsonUnwrapped")
    void test() {
        DetailData detailData = ViewUtils.getDetail(DTO, Mode.EDIT, false);
        Map<String, String> displayNames = detailData.getMeta().getFields().stream()
            .collect(Collectors.toMap(
                DetailField::getName,
                DetailField::getTitle
            ));

        // Проверяем, что оторбажается заголовки, указанные в @DisplayName, сцепляются по иерархии объектов
        assertThat(displayNames).isEqualTo(
            ImmutableMap.builder()
                .put("field", "Field")
                .put("inner1_field", "Inner: Field")
                .put("inner1_innerinner_field", "Inner: Inner: Field")
                .put("inner2_field", "Field")
                .put("inner2_innerinner_field", "Inner: Field")
                .build()
        );
    }

    @Data
    @Accessors(chain = true)
    static class Dto {
        Long id;

        @DisplayName("Field")
        String field;

        @DisplayName("Inner: ")
        @JsonUnwrapped(prefix = "inner1_")
        Inner inner1;

        @JsonUnwrapped(prefix = "inner2_")
        Inner inner2;
    }

    @Data
    @Accessors(chain = true)
    static class Inner {
        @DisplayName("Field")
        String field;

        @DisplayName("Inner: ")
        @JsonUnwrapped(prefix = "innerinner_")
        InnerInner inner;
    }

    @Data
    @Accessors(chain = true)
    static class InnerInner {
        @DisplayName("Field")
        String field;
    }
}
