package ru.yandex.market.oms.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;

public class TestWriteSubstitutions extends TestBase {

    @Test
    public void testWriteDuplicate() {
        Assertions.assertThrows(
                RuntimeException.class,
                () -> {
                    ObjectMapper objMapper = Mockito.mock(ObjectMapper.class);
                    Mockito.when(objMapper.readValue((File) any(), (Class) any())
                    ).then(invocationOnMock -> mapper.readValue(invocationOnMock.getArgument(0, File.class),
                            invocationOnMock.getArgument(1, Class.class)));

                    ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
                    Mockito.doNothing().when(objMapper).writeValue((File) any(), argument.capture());

                    ProcaasYamlPatcher patcher = new ProcaasYamlPatcher(null,
                            objMapper,
                            null);

                    patcher.writeSubstitution(Path.of(
                            ClassLoader.getSystemResource("substitutions/production.yaml").toURI()),
                            "order-event-procaas-bus", "topic-name");

                }
        );
    }
}
