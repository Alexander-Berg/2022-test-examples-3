package ru.yandex.market.logistics.front.library.json_unwrapped;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ValuesJsonUnwrappedTest {

    private static final Dto DTO = new Dto()
        .setId(100500L)
        .setField("value")
        .setInner1(
            new Inner()
                .setField("inner value")
                .setInner(
                    new InnerInner()
                        .setField("inner inner value")
                )
        )
        .setInner2(null);

    @Test
    @org.junit.jupiter.api.DisplayName("Проверка значений полей при использовании @JsonUnwrapped")
    void test() {
        DetailData detailData = ViewUtils.getDetail(DTO, Mode.EDIT, false);

        assertThat(detailData.getItem().getValues())
            .isEqualTo(
                ImmutableMap.builder()
                    .put("field", "value")
                    .put("inner1_field_suf", "inner value")
                    .put("inner1_innerinner_field_sufsuf_suf", "inner inner value")
                    .build()
            );
    }

    @Data
    @Accessors(chain = true)
    static class Dto {
        Long id;

        String field;

        @JsonUnwrapped(prefix = "inner1_", suffix = "_suf")
        Inner inner1;

        @JsonUnwrapped(prefix = "inner2_", suffix = "_suf")
        Inner inner2;
    }

    @Data
    @Accessors(chain = true)
    static class Inner {
        String field;

        @JsonUnwrapped(prefix = "innerinner_", suffix = "_sufsuf")
        InnerInner inner;
    }

    @Data
    @Accessors(chain = true)
    static class InnerInner {
        String field;
    }
}
