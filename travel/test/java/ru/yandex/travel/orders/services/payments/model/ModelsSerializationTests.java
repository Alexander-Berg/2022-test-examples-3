package ru.yandex.travel.orders.services.payments.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.junit.Test;

import ru.yandex.travel.orders.services.payments.DefaultTrustClient;
import ru.yandex.travel.orders.services.payments.model.plus.TrustTopupPayload;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelsSerializationTests {
    private final ObjectMapper mapper = DefaultTrustClient.createJsonMapper();

    @Test
    public void testBooleanAsString() throws Exception {
        TrustTopupPayload source = TrustTopupPayload.builder()
                .hasPlus(true)
                .build();

        String jsonString = mapper.writeValueAsString(source);
        JsonNode jsonTree = mapper.readTree(jsonString);
        JsonNode valueJson = jsonTree.get("has_plus");
        assertThat(valueJson.getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(valueJson.textValue()).isEqualTo("true");

        TrustTopupPayload restored = mapper.readValue(jsonString, TrustTopupPayload.class);
        assertThat(restored).isEqualTo(source);
    }

    // works for other Number types as well
    @Test
    public void testBigDecimalAsString() throws Exception {
        TrustBoundPaymentMethod method = TrustBoundPaymentMethod.builder()
                .balance(BigDecimal.valueOf(12_345.67))
                .build();

        String jsonString = mapper.writeValueAsString(method);
        JsonNode jsonTree = mapper.readTree(jsonString);
        JsonNode balanceValueJson = jsonTree.get("balance");
        assertThat(balanceValueJson.getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(balanceValueJson.textValue()).isEqualTo("12345.67");

        TrustBoundPaymentMethod restored = mapper.readValue(jsonString, TrustBoundPaymentMethod.class);
        assertThat(restored).isEqualTo(method);
    }

    @Test
    public void testBigDecimalWithoutExponent() throws Exception {
        TrustTopupPayload source = TrustTopupPayload.builder()
                .baseAmount(new BigDecimal("1E+3"))
                .build();

        String jsonString = mapper.writeValueAsString(source);
        JsonNode jsonTree = mapper.readTree(jsonString);
        JsonNode balanceValueJson = jsonTree.get("base_amount");
        assertThat(balanceValueJson.getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(balanceValueJson.textValue()).isEqualTo("1000");

        TrustTopupPayload restored = mapper.readValue(jsonString, TrustTopupPayload.class);
        assertThat(restored.getBaseAmount()).isEqualByComparingTo(source.getBaseAmount());
    }
}
