package ru.yandex.market.logistics.front.library.json_unwrapped;

import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.annotation.HiddenField;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.detail.DetailField;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

class HiddenJsonUnwrappedTest {

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
    @org.junit.jupiter.api.DisplayName("Проверка работы аннотации @HiddenField при использовании @JsonUnwrapped")
    void test() {
        DetailData detailData = ViewUtils.getDetail(DTO, Mode.EDIT, false);
        Map<String, Boolean> displayNames = detailData.getMeta().getFields().stream()
            .collect(Collectors.toMap(
                DetailField::getName,
                DetailField::isHidden
            ));

        // Проверяем, что свойство hidden = true, если оно true для хотя бы одного родительского элемента в иерархии
        assertThat(displayNames).isEqualTo(
            ImmutableMap.builder()
                .put("field1", true)
                .put("field2", false)
                .put("inner1_field1", true)
                .put("inner1_field2", true)
                .put("inner1_innerinner_field1", true)
                .put("inner1_innerinner_field2", true)
                .put("inner2_field1", true)
                .put("inner2_field2", false)
                .put("inner2_innerinner_field1", true)
                .put("inner2_innerinner_field2", true)
                .build()
        );
    }

    @Data
    @Accessors(chain = true)
    static class Dto {
        Long id;

        @HiddenField
        String field1;

        @HiddenField(isHidden = false)
        String field2;

        @HiddenField
        @JsonUnwrapped(prefix = "inner1_")
        Inner inner1;

        @HiddenField(isHidden = false)
        @JsonUnwrapped(prefix = "inner2_")
        Inner inner2;
    }

    @Data
    @Accessors(chain = true)
    static class Inner {
        @HiddenField
        String field1;

        @HiddenField(isHidden = false)
        String field2;

        @HiddenField
        @JsonUnwrapped(prefix = "innerinner_")
        InnerInner inner;
    }

    @Data
    @Accessors(chain = true)
    static class InnerInner {
        @HiddenField
        String field1;

        @HiddenField(isHidden = false)
        String field2;
    }
}
