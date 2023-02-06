package ru.yandex.market.sc.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.test.TestFactory;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Базовая сущность для тестов контроллеров
 * @author: dbryndin
 * @date: 7/15/21
 */
@ScApiControllerTest
public abstract class BaseApiControllerTest {

    protected static final long UID = 123L;

    public static final ObjectMapper OBJECT_MAPPER = createJsonMapper();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestFactory testFactory;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected RouteSoMigrationHelper routeSoMigrationHelper;

    protected <R> R readContentAsClass(ResultActions ra, Class<R> clazz) throws Exception {
        return OBJECT_MAPPER.readValue(ra.andReturn().getResponse().getContentAsString(), clazz);
    }

    private static ObjectMapper createJsonMapper() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setSerializationInclusion(NON_NULL);

        return objectMapper;
    }

}
