package ru.yandex.market.ocrm.module.taximl.impl;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.serialization.JsonDeserializer;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.script.ScriptService;
import ru.yandex.market.ocrm.module.taximl.ModuleTaximlTestConfiguration;
import ru.yandex.market.ocrm.module.taximl.TaximlClient;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = ModuleTaximlTestConfiguration.class)
public class TaximlScriptServiceApiTest {

    @Inject
    @Lazy
    ScriptService scriptService;
    @Inject
    TaximlClient taximlClient;
    @Inject
    JsonDeserializer jsonDeserializer;

    @Test
    public void execute() {
        String expected = Randoms.string();
        String responseJson = "{\"reply\": {\"text\": \"" + expected + "\"}}";
        JsonNode response = jsonDeserializer.readObject(JsonNode.class, responseJson);
        Mockito.when(taximlClient.support(Mockito.any())).thenReturn(response);

        String body = "api.taximl.support('123', [" +
                "api.taximl.message('user', 'first message')," +
                "api.taximl.message('user', 'second message')" +
                "]).reply.text";
        Object result = scriptService.execute(body);
        Assertions.assertEquals(expected, result);
    }
}
