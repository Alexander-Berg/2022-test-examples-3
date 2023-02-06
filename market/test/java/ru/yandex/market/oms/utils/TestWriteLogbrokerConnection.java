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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;

public class TestWriteLogbrokerConnection extends TestBase {

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
        configuration.setLogbrokerEndPoint("testEndPoint");
        ProcaasYamlPatcher patcher = new ProcaasYamlPatcher(configuration,
                objMapper,
                null);

        patcher.writeLogbrokerConnection(Path.of(ClassLoader.getSystemResource("example1/oevents/main.yaml").toURI()));

        Map map = argument.getValue();

        Map producers = (Map) ((Map) ((Map) ((List) map.get("operators")).get(0)).get("value#processing-queue")).get(
                "logbroker-producers");
        assertEquals(3, producers.size());
        assertTrue(producers.containsKey("testProject-topic"));
        map = (Map) producers.get("testProject-topic");

        assertEquals("logbroker", map.get("tvm-service-name"));
        assertEquals("$" + configuration.getTopicId(), map.get("topic"));
        assertEquals("processing", map.get("source-id"));
        assertEquals(configuration.getLogbrokerEndPoint(), map.get("global-endpoint"));
    }
}
