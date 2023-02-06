package ru.yandex.market.logistics.front.library.json_unwrapped;

import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.annotation.Editable;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.detail.DetailField;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

class EditableJsonUnwrappedTest {

    private static final Dto DTO = new Dto()
        .setId(100500L)
        .setInner1(
            new Inner()
                .setInner(new InnerInner())
        )
        .setInner2(
            new Inner()
                .setInner(new InnerInner())
        );

    @Test
    @org.junit.jupiter.api.DisplayName("Проверка работы аннотации @Editable при использовании @JsonUnwrapped")
    void test() {
        DetailData detailData = ViewUtils.getDetail(DTO, Mode.EDIT, false);
        Map<String, Boolean> displayNames = detailData.getMeta().getFields().stream()
            .collect(Collectors.toMap(
                DetailField::getName,
                DetailField::isEditable
            ));

        // Проверяем, что свойство editable = true, если оно true для всех родительских элементов в иерархии
        assertThat(displayNames).isEqualTo(
            ImmutableMap.builder()
                .put("field1", true)
                .put("field2", false)
                .put("inner1_field1", true)
                .put("inner1_field2", false)
                .put("inner1_innerinner_field1", true)
                .put("inner1_innerinner_field2", false)
                .put("inner2_field1", false)
                .put("inner2_field2", false)
                .put("inner2_innerinner_field1", false)
                .put("inner2_innerinner_field2", false)
                .build()
        );
    }

    @Data
    @Accessors(chain = true)
    static class Dto {
        Long id;

        @Editable
        String field1;

        @Editable(isEditable = false)
        String field2;

        @Editable
        @JsonUnwrapped(prefix = "inner1_")
        Inner inner1;

        @Editable(isEditable = false)
        @JsonUnwrapped(prefix = "inner2_")
        Inner inner2;
    }

    @Data
    @Accessors(chain = true)
    static class Inner {
        @Editable
        String field1;

        @Editable(isEditable = false)
        String field2;

        @Editable
        @JsonUnwrapped(prefix = "innerinner_")
        InnerInner inner;
    }

    @Data
    @Accessors(chain = true)
    static class InnerInner {
        @Editable
        String field1;

        @Editable(isEditable = false)
        String field2;
    }
}
