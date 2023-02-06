package ru.yandex.market.tpl.core.domain.promo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.util.JacksonUtil;
import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PromoMapperTest {

    private final PromoMapper promoMapper;

    @Test
    void map() throws IOException {
        JsonNode rawPromoContent = JacksonUtil.toJsonNode(
                IOUtils.toString(
                        this.getClass().getResourceAsStream("/promo/test_promo.json"),
                        StandardCharsets.UTF_8
                )
        );

        Promo promo = promoMapper.map(rawPromoContent, "NEW");

        assertThat(promo.getId()).isEqualTo("123466");
        assertThat(promo.getStatus()).isEqualTo(PromoStatus.NEW);
        assertThat(promo.getPriority()).isEqualTo(123L);
        assertThat(promo.getScreens()).containsExactlyInAnyOrderElementsOf(List.of("WELCOME", "TEST"));
    }
}
