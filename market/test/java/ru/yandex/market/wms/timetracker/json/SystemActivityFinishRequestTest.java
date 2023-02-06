package ru.yandex.market.wms.timetracker.json;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.response.SystemActivityFinishRequest;

@JsonTest
@ActiveProfiles("test")
class SystemActivityFinishRequestTest {

    @Autowired
    private ObjectMapper mapper;

    @Test
    void canDeserialize() throws JsonProcessingException {
        String content  = "{" +
                " \"users\" : [\"first\", \"second\", \"other\"] " +
                "}";

        final SystemActivityFinishRequest expected = mapper.readValue(content, SystemActivityFinishRequest.class);

        Assertions.assertAll(
                () -> Assertions.assertNotNull(expected),
                () -> Assertions.assertEquals(3, expected.getUsers().size()),
                () -> Assertions.assertTrue(expected.getUsers().containsAll(List.of("first", "second", "other")))
        );
    }
}
