package ru.market.tpl.common.communication;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.common.communication.crm.config.CrmCommunicationConfig;
import ru.yandex.market.tpl.common.communication.crm.model.CommunicationAbstractDto;
import ru.yandex.market.tpl.common.communication.crm.model.CommunicationEventType;
import ru.yandex.market.tpl.common.communication.crm.service.CommunicationJsonByteConverter;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@Import(value = CrmCommunicationConfig.class)
public class CommunicationJsonByteConverterTest {

    @Autowired
    private CommunicationJsonByteConverter communicationJsonByteConverter;



    @Test
    @SneakyThrows
    void formattedTest() {
        var objectMapper = new ObjectMapper();

        TestCommunicationDto communication =
                TestCommunicationDto.builder()
                        .someTime(LocalTime.of(6, 30))
                        .someDate(LocalDate.of(2021, 7, 1))
                        .someDuration(Duration.ofHours(25))
                        .build();

        byte[] convertedJson = communicationJsonByteConverter.convert(communication);
        JsonNode jsonNode = objectMapper.readValue(convertedJson, JsonNode.class);

        assertThat(jsonNode.get("someTime").asText()).isEqualTo("06:30:00");
        assertThat(jsonNode.get("someDate").asText()).isEqualTo("2021-07-01");
        assertThat(jsonNode.get("someDuration").asText()).isEqualTo("PT25H");
        assertThat(jsonNode.get("timestamp").isNull()).isFalse();

        assertThat(jsonNode.size()).isEqualTo(6);
    }

    @SuperBuilder
    @Getter
    private static class TestCommunicationDto extends CommunicationAbstractDto {
        final CommunicationEventType eventType = CommunicationEventType.FOR_UNIT_TEST;

        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        @JsonProperty("someTime")
        LocalTime someTime;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @JsonProperty("someDate")
        LocalDate someDate;

        Integer someInteger;

        Duration someDuration;

    }

}
