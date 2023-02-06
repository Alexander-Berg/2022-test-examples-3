package ru.yandex.market.fulfillment.wrap.marschroute.api.response.services;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.client.MarschrouteClientConfiguration;

import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MarschrouteClientConfiguration.class)
class OrderServiceParsingTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Проверяем, что при отсутствии обязательных полей не удастся создать экземпляр OrderService.
     */
    @Test
    void missingMandatoryFields() throws Exception {
        String json = Resources.toString(
            getResource("orders_service/missing_mandatory_field.json"),
            Charsets.UTF_8
        );

        assertThatThrownBy(() -> objectMapper.readValue(json, OrderService.class))
                .isInstanceOf(JsonMappingException.class);
    }
}
