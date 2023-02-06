package ru.yandex.mail.hackathon.calendar;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeserializeTest {
    private static final String RAW_ANSWER = "{\n" +
            "   \"todo-lists\" : [\n" +
            "      {\n" +
            "         \"title\" : \"Miss me\",\n" +
            "         \"timestamp\" : \"1568974667\",\n" +
            "         \"color\" : \"#b9b9b9\",\n" +
            "         \"external-id\" : \"Hackyandex.ru\",\n" +
            "         \"todo-items-count\" : \"0\",\n" +
            "         \"id\" : \"13\",\n" +
            "         \"creation-ts\" : \"1568974667\",\n" +
            "         \"default\" : \"true\",\n" +
            "         \"description\" : \"\"\n" +
            "      }\n" +
            "   ],\n" +
            "   \"invocation-info\" : {\n" +
            "      \"host-id\" : \"\",\n" +
            "      \"exec-duration-millis\" : \"25\",\n" +
            "      \"req-id\" : \"Magic\",\n" +
            "      \"app-name\" : \"web\",\n" +
            "      \"app-version\" : \"5688118\",\n" +
            "      \"hostname\" : \"hack.team.ru\",\n" +
            "      \"action\" : \"getTodoLists\"\n" +
            "   }\n" +
            "}";

    @Test
    public void parseTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        GetTodoListsResponse lists = mapper.readValue(RAW_ANSWER, GetTodoListsResponse.class);
        assertThat(lists.getItems()).hasSize(1).first()
                .hasFieldOrPropertyWithValue("externalId", "Hackyandex.ru")
                .hasFieldOrPropertyWithValue("title", "Miss me");
    }
}
