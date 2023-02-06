package ru.yandex.chemodan.app.telemost.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import org.junit.Test;

import ru.yandex.chemodan.app.telemost.appmessages.AppMessage;
import ru.yandex.chemodan.app.telemost.appmessages.model.UserSource;
import ru.yandex.chemodan.app.telemost.appmessages.model.commands.MuteMicrophoneCommand;
import ru.yandex.chemodan.app.telemost.appmessages.model.commands.SendCommandRequest;
import ru.yandex.chemodan.app.telemost.appmessages.model.events.EventMessage;
import ru.yandex.chemodan.app.telemost.appmessages.model.events.RoleChangedEvent;
import ru.yandex.chemodan.app.telemost.repository.model.UserRole;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;

public class GenerateSchemaTest {
    private final UserSource defaultSource = new UserSource(PassportOrYaTeamUid.parseUid("11111"));

    @Test
    public void generateEventDoc() throws Exception {
        JsonSchema schema = GenerateSchemas.generateSchemaFromJavaClass(EventMessage.class);
        EventMessage eventMessage = new EventMessage(defaultSource, new RoleChangedEvent("1111", UserRole.ADMIN));
        generateDoc(schema, eventMessage);

    }

    @Test
    public void generateCommandDoc() throws Exception {
        JsonSchema schema = GenerateSchemas.generateSchemaFromJavaClass(SendCommandRequest.class);
        SendCommandRequest sendCommandRequest = new SendCommandRequest(defaultSource, new MuteMicrophoneCommand());
        generateDoc(schema, sendCommandRequest);

    }

    private void generateDoc(JsonSchema schema, AppMessage eventMessage) throws JsonProcessingException {
        System.out.println("<{схема");
        System.out.println("%%(json)");
        System.out.println(toJsonString(schema));
        System.out.println("%%}>");
        System.out.println("<{пример");
        System.out.println("%%(json)");
        System.out.println(toJsonString(eventMessage));
        System.out.println("%%}>");
    }

    private String toJsonString(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(object);
    }
}
