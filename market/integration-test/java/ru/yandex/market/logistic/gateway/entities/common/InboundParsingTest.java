package ru.yandex.market.logistic.gateway.entities.common;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Inbound;

import static org.assertj.core.api.Assertions.assertThat;

public class InboundParsingTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void offsetDateTimeTest() throws IOException {
        String fileContent = getFileContent("fixtures/entities/common/inbound.json");
        Inbound inboundClass = objectMapper.readValue(fileContent, Inbound.class);

        String actualContent = objectMapper.writeValueAsString(inboundClass);

        Inbound reverseInboundClass = objectMapper.readValue(actualContent, Inbound.class);

        assertThat(inboundClass)
                .as("Asserting that serialized and deserialized objects are the same")
                .isEqualToComparingFieldByFieldRecursively(reverseInboundClass);
    }
}
