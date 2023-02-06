package ru.yandex.travel.commons.jackson;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MoneySerializersModuleTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(
                new MoneySerializersModule()
        );
    }


    @Test
    public void testMoneySerialization() throws IOException {
        Money money = Money.of(
                10, "RUB"
        );
        String json = objectMapper.writeValueAsString(money);
        Object currency = JsonPath.read(json, "@.currency");
        assertThat(currency).isInstanceOf(String.class);
        assertThat(currency).isEqualTo("RUB");
        Object value = JsonPath.read(json, "@.value");
        assertThat(value).isInstanceOf(Integer.class);
        assertThat(value).isEqualTo(10);

        Money parsedMoney = objectMapper.readValue(json, Money.class);
        assertThat(parsedMoney).isEqualTo(money);
    }

}
