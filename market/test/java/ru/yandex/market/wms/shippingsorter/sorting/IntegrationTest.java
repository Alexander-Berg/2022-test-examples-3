package ru.yandex.market.wms.shippingsorter.sorting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.assertj.core.api.SoftAssertions;
import org.intellij.lang.annotations.Language;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.skyscreamer.jsonassert.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;
import ru.yandex.market.wms.shippingsorter.configuration.BaseTestConfig;
import ru.yandex.market.wms.shippingsorter.configuration.IntegrationTestConfig;
import ru.yandex.market.wms.shippingsorter.sorting.utils.NullableColumnsDataSetLoader;

@SpringBootTest(classes = {BaseTestConfig.class, IntegrationTestConfig.class})
@ActiveProfiles(Profiles.TEST)
@Transactional
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@AutoConfigureMockMvc
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class)
public abstract class IntegrationTest {

    protected static final boolean STRICT = true;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ServicebusClient servicebusClient;

    protected SoftAssertions assertions;

    @BeforeEach
    public void setup() {
        assertions = new SoftAssertions();
    }

    protected static String json(@Language("json5") String json5) throws JSONException {
        return JSONParser.parseJSON(json5).toString();
    }

    protected static <T> T parseJson(String json, Class<T> type) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new KotlinModule());
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return mapper.readValue(json, type);
    }
}
