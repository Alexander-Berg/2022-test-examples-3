package ru.yandex.market.api.util;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.api.controller.jackson.CapiObjectMapperFactory;
import ru.yandex.market.api.controller.serialization.StringCodecStorage;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.model.Block;
import ru.yandex.market.api.model.Param;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

import static org.junit.Assert.assertEquals;

/**
 * @author Denis Chernyshov
 */
@WithContext
public class BlockSerializerTest extends UnitTestBase {

    private CapiObjectMapperFactory objectMapperFactory;

    @Before
    public void setUp() {
        StringCodecStorage storage = Mockito.mock(StringCodecStorage.class);
        objectMapperFactory = new CapiObjectMapperFactory(storage);
    }

    @Test
    public void shouldWorkWell() throws IOException {
        ObjectMapper mapper = objectMapperFactory.getOldJsonObjectMapper();

        Block block = new Block();
        block.setName("my_block_name");
        block.getParams().add(new Param("param_name1", "param_value1"));
        block.getParams().add(new Param("param_name2", "param_value2"));
        String actual = mapper.writeValueAsString(block);
        System.out.println(actual);
        String expected = "{\"name\":\"my_block_name\",\"params\":[{\"name\":\"param_name1\",\"value\":\"param_value1\"},{\"name\":\"param_name2\",\"value\":\"param_value2\"}]}";
        assertEquals(expected, actual);
    }
}
