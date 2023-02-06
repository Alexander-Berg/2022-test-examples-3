package ru.yandex.market.oms.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;

public class TestWriteSendEvent extends TestBase {

    @Test
    public void testWrite() throws URISyntaxException, IOException {
        ObjectMapper objMapper = Mockito.mock(ObjectMapper.class);
        Mockito.when(objMapper.readValue((File) any(), (Class) any())
        ).then(invocationOnMock -> mapper.readValue(invocationOnMock.getArgument(0, File.class),
                invocationOnMock.getArgument(1, Class.class)));

        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        Mockito.doNothing().when(objMapper).writeValue((File) any(), argument.capture());

        Configuration configuration = new Configuration();
        configuration.setProject("testProject");
        configuration.setStageId("send-event-lb");
        configuration.setEvent(Map.of("args#xget", "/event/payload"));
        ProcaasYamlPatcher patcher = new ProcaasYamlPatcher(configuration,
                objMapper,
                null);

        patcher.writeSendEvent(Path.of(
                ClassLoader.getSystemResource("example1/oevents/piplines/oevent-pipeline.yaml").toURI()));

        Map map = argument.getValue();

        List stages = (List) ((Map) ((Map) ((List) map.get("operators")).get(0)).get("value#processing-pipeline")).get(
                "stages");
        assertEquals(3, stages.size());
        map = (Map) stages.get(1);
        assertEquals("testProject_send_event", map.get("id"));
        map = (Map) ((List) ((Map) ((List) map.get("handlers")).get(0)).get("logbrokers")).get(0);
        assertEquals("testProject_send_event", map.get("id"));
        assertEquals("testProject-topic", map.get("alias"));
        assertEquals("/event/payload", map.get("args#xget"));
    }
}
