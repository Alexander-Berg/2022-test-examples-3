package ru.yandex.market.wms.common.spring;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.intellij.lang.annotations.Language;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONParser;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;


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
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {"wmwhseConnection", "enterpriseConnection", "scprdd1DboConnection"})
public abstract class IntegrationTest extends BaseIntegrationTest {

    protected static final boolean STRICT = true;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ServicebusClient servicebusClient;

    protected ListAppender<ILoggingEvent> attachLogListAppender(Class type) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);
        return listAppender;
    }

    protected static String json(@Language("json5") String json5) throws JSONException {
        return JSONParser.parseJSON(json5).toString();
    }

    protected void callTwice(Callable action) throws Exception {
        action.call();
        action.call();
    }

    protected ResultActions assertHttpCall(MockHttpServletRequestBuilder requestBuilder,
                                  ResultMatcher status,
                                  String requestFile) throws Exception {
        return assertHttpCall(requestBuilder, status, requestFile, null);
    }

    protected ResultActions assertHttpCall(MockHttpServletRequestBuilder requestBuilder,
                                  ResultMatcher status,
                                  String requestFile,
                                  String responseFile) throws Exception {
        ResultActions result = mockMvc.perform(requestBuilder
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andExpect(status);
        if (responseFile != null) {
            String response = getFileContent(responseFile);
            result.andExpect(content().json(response, false));
        }
        return result;
    }

    protected ResultActions assertHttpCall(MockHttpServletRequestBuilder requestBuilder,
                                  ResultMatcher status) throws Exception {
        return mockMvc.perform(requestBuilder)
                .andExpect(status);
    }

    protected ResultActions assertHttpCall(MockHttpServletRequestBuilder requestBuilder,
                                  ResultMatcher status,
                                  Map<String, String> params,
                                  String responseFile) throws Exception {
        ResultActions result = mockMvc.perform(requestBuilder
                .contentType(MediaType.APPLICATION_JSON)
                .params(CollectionUtils.toMultiValueMap(
                        params.entrySet().stream().collect(
                                Collectors.toMap(Map.Entry::getKey, e -> List.of(e.getValue()))
                        ))))
                .andExpect(status);
        if (responseFile != null) {
            String response = getFileContent(responseFile);
            result.andExpect(content().json(response, false));
        }
        return result;
    }
}
