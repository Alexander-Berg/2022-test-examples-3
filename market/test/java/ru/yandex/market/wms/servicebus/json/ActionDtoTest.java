package ru.yandex.market.wms.servicebus.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.AssigmentType;
import ru.yandex.market.wms.common.model.enums.ProcessType;
import ru.yandex.market.wms.common.spring.dto.ActionDto;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.servicebus.IntegrationTest;

public class ActionDtoTest extends IntegrationTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void canSerialize() throws IOException {
        final ActionDto expected = ActionDto.builder()
                .user("AT113")
                .processType(ProcessType.PLACEMENT)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("AT113").build();

        final String result = objectMapper.writeValueAsString(expected);

        Assertions.assertEquals(
                JsonParser.parseString(FileContentUtils.getFileContent(
                        "json/model.json")),
                JsonParser.parseString(result));

    }
}
