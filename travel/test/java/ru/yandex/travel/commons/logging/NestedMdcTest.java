package ru.yandex.travel.commons.logging;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.commons.logging.CommonMdcParams.MDC_ENTITY_ID;
import static ru.yandex.travel.commons.logging.CommonMdcParams.MDC_ENTITY_TYPE;

@Slf4j
public class NestedMdcTest {
    @Test
    public void testNestedContexts() {
        UUID id1 = UUID.fromString("0-0-0-0-1");
        UUID id2 = UUID.fromString("0-0-0-0-2");

        assertThat(MDC.get(MDC_ENTITY_ID)).isNull();
        assertThat(MDC.get(MDC_ENTITY_TYPE)).isNull();
        try (var ignored = NestedMdc.forEntity(id1, "type1")) {
            assertThat(MDC.get(MDC_ENTITY_ID)).isEqualTo(id1.toString());
            assertThat(MDC.get(MDC_ENTITY_TYPE)).isEqualTo("type1");
            try (var ignored2 = NestedMdc.forEntity(id2, "type2")) {
                assertThat(MDC.get(MDC_ENTITY_ID)).isEqualTo(id2.toString());
                assertThat(MDC.get(MDC_ENTITY_TYPE)).isEqualTo("type2");
                try (var ignored3 = NestedMdc.forEntityId("id3")) {
                    assertThat(MDC.get(MDC_ENTITY_ID)).isEqualTo("id3");
                    assertThat(MDC.get(MDC_ENTITY_TYPE)).isNull();
                }
            }
            assertThat(MDC.get(MDC_ENTITY_ID)).isEqualTo(id1.toString());
            assertThat(MDC.get(MDC_ENTITY_TYPE)).isEqualTo("type1");
        }
        assertThat(MDC.get(MDC_ENTITY_ID)).isNull();
        assertThat(MDC.get(MDC_ENTITY_TYPE)).isNull();
    }
}
