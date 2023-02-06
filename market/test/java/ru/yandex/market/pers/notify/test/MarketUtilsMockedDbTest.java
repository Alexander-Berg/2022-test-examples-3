package ru.yandex.market.pers.notify.test;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.pers.area.db.PersAreaEmbeddedDbUtil;
import ru.yandex.market.pers.notify.exceptions.Mediator4xxResponseException;

import java.io.IOException;

@WebAppConfiguration
@ContextConfiguration(value = "classpath:market-utils-test-bean.xml", inheritLocations = false)
public abstract class MarketUtilsMockedDbTest extends MockedDbTest {
    @Autowired
    private MarketUtilsTestEnvironment marketUtilsTestEnvironment;

    private ObjectMapper objectMapper = new ObjectMapper();

    private PersAreaEmbeddedDbUtil persAreaEmbeddedDbUtil = PersAreaEmbeddedDbUtil.INSTANCE;

    @BeforeEach
    public void cleanPersAreaDatabase() {
        persAreaEmbeddedDbUtil.truncatePersAreaTables();
    }

    @BeforeEach
    public void setUpMarketUtils() throws Mediator4xxResponseException {
        marketUtilsTestEnvironment.setUp();
    }

    @AfterEach
    public void tearDownMarketUtils() {
        marketUtilsTestEnvironment.tearDown();
    }

    protected String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    protected <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    @SuppressWarnings("WeakerAccess")
    protected static class Error {
        public Error(String code, String message, int status) {
            this.code = code;
            this.message = message;
            this.status = status;
        }

        public String code;
        public String message;
        public int status;
    }
}
