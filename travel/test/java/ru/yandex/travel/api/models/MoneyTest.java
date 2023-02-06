package ru.yandex.travel.api.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.commons.jackson.MoneySerializersModule;

import static org.assertj.core.api.Assertions.assertThat;

public class MoneyTest {

    @Data
    private static class MoneyKeeper {
        Money money;
    }

    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SnakeCaseStrategy.SNAKE_CASE)
            .registerModule(new MoneySerializersModule())
            .registerModule(new JavaTimeModule());

    @Test
    public void testAmountDeserialization() throws Exception {

        String json = "{\"money\": {\"amount\": \"123.45\", \"currency\": \"RUB\"}}";
        JsonNode parsedJsonTree = mapper.readTree(json);
        MoneyKeeper deserialized = mapper.treeToValue(parsedJsonTree, MoneyKeeper.class);

        assertThat(deserialized.money).isEqualTo(Money.of(123.45, "RUB"));
    }

    @Test
    public void testValueDeserialization() throws Exception {

        String json = "{\"money\": {\"value\": 123.45, \"currency\": \"RUB\"}}";
        JsonNode parsedJsonTree = mapper.readTree(json);
        MoneyKeeper deserialized = mapper.treeToValue(parsedJsonTree, MoneyKeeper.class);

        assertThat(deserialized.money).isEqualTo(Money.of(123.45, "RUB"));
    }
}
