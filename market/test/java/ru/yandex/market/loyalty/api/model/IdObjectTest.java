package ru.yandex.market.loyalty.api.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.client.test.MarketLoyaltyMockedServerTest;
import ru.yandex.market.loyalty.client.utils.BeanHolder;

import java.io.IOException;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class IdObjectTest extends MarketLoyaltyMockedServerTest {
    private static final Logger logger = LogManager.getLogger(IdObjectTest.class);

    @Autowired
    private BeanHolder<ObjectMapper> objectMapperHolder;

    @Test(expected = JsonMappingException.class)
    public void shouldFailOnNullSerialization() throws IOException {
        logger.error("Object read: {}",
                objectMapperHolder.get().readValue("{\"id\":null}", IdObject.class)
        );
    }

    @Test(expected = JsonMappingException.class)
    public void shouldFailOnAbsentId() throws IOException {
        logger.error("Object read: {}",
                objectMapperHolder.get().readValue("{}", IdObject.class)
        );
    }

    @Test
    public void shouldSerializeJsonListWithDuplicatesToSet() throws IOException {
        String jsonArray = "[{\"id\":\"11\"}, {\"id\":\"12\"}, {\"id\":\"11\"}]";

        Set<IdObject> idObjectSet = objectMapperHolder.get().readValue(jsonArray, new TypeReference<Set<IdObject>>() {
        });

        assertThat(idObjectSet, hasSize(2));
    }
}
