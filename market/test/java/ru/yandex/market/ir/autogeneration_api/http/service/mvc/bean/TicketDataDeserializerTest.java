package ru.yandex.market.ir.autogeneration_api.http.service.mvc.bean;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.Test;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.MVCConfig;

import java.io.IOException;

public class TicketDataDeserializerTest {
    //language=JSON
    private static final String JSON = "{\"categoryId\": 13491683,\n" +
        "\"parameters\": {\"7893318\": [\n\"BABY DAM\"\n],\n\"7893318\": [\n\"еще один вендор\"\n]}}";


    @Test(expected = JsonMappingException.class)
    public void testDeserialize() throws IOException {
        MVCConfig.getObjectMapper().readValue(JSON, TicketData.class);
    }
}