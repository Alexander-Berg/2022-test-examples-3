package ru.yandex.market.checkout.checkouter.shop;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class ShopControllerUpdateMetaErrorTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 123456789L;
    @Autowired
    private TestSerializationService testSerializationService;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                builder().withCampaignId(null).build(),
                builder().withClientId(null).build(),
                builder().withSandboxClass("ILLEGAL").build(),
                builder().withProdClass("ILLEGAL").build(),
                builder().withArticles(Collections.singletonList(null)).build(),
                builder().withArticles(Collections.singletonList(Collections.singletonMap("articleId", null))).build(),
                builder().withArticles(Collections.singletonList(Collections.singletonMap("articleId", null))).build(),
                builder().withArticles(Collections.singletonList(Collections.singletonMap("articleId", "0123456"))
                ).build()
        )
                .map(json -> new Object[]{json})
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    private static Builder builder() {
        return new Builder();
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldReturnBadRequestOnInvalidRequest(Map<String, Object> json) throws Exception {
        mockMvc.perform(put("/shops/{shopId}", SHOP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(testSerializationService.serializeCheckouterObject(json)))
                .andExpect(status().isBadRequest());

    }

    private static class Builder {

        private final Map<String, Object> request = new HashMap<>();

        Builder() {
            request.put("campaignId", 123456);
            request.put("clientId", 345678);
            request.put("sandboxClass", "SHOP");
            request.put("prodClass", "SHOP");
            request.put("articles", null);
        }

        public Builder withCampaignId(Long campaignId) {
            request.put("campaignId", campaignId);
            return this;
        }

        public Builder withClientId(Long clientId) {
            request.put("clientId", clientId);
            return this;
        }

        public Builder withSandboxClass(String sandboxClass) {
            request.put("sandboxClass", sandboxClass);
            return this;
        }

        public Builder withProdClass(String prodClass) {
            request.put("prodClass", prodClass);
            return this;
        }

        public Builder withArticles(List<Map<String, Object>> articles) {
            request.put("articles", articles);
            return this;
        }

        public Map<String, Object> build() {
            return request;
        }
    }
}
