package ru.yandex.market.clab.api;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.common.util.IOUtils;
import ru.yandex.market.clab.api.config.ApiAppConfig;
import ru.yandex.market.clab.db.config.ControlledClockConfiguration;
import ru.yandex.market.clab.db.config.MainLiquibaseConfig;
import ru.yandex.market.clab.test.IntegrationTestContextInitializer;
import ru.yandex.market.clab.test.config.TestRemoteServicesConfiguration;
import ru.yandex.market.common.postgres.embedded.PGSpringInitializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 05.12.2018
 */
@ContextConfiguration(initializers = {PGSpringInitializer.class, IntegrationTestContextInitializer.class})
@SpringBootTest(
    properties = {
        "spring.main.allow-bean-definition-overriding=true"
    },
    classes = {
        ApiAppConfig.class,
        ControlledClockConfiguration.class,
        MainLiquibaseConfig.class,
        TestRemoteServicesConfiguration.class
    })
@AutoConfigureMockMvc
@Transactional
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public abstract class BaseApiIntegrationTest {

    protected static final String QUERY_GATEWAY = "/api/fulfillment";

    @Autowired
    private MockMvc mockMvc;

    protected ResultActions send(String resourcePath) throws Exception {
        return send(resourcePath, Collections.emptyMap());
    }

    protected ResultActions send(String resourcePath, Map<String, Object> values) throws Exception {
        String content = read(resourcePath);
        content = replacePlaceholders(values, content);
        return mockMvc.perform(
            post(QUERY_GATEWAY)
                .content(content)
                .contentType(MediaType.TEXT_XML)
                .characterEncoding(StandardCharsets.UTF_8.name())
        );
    }

    /*
     * Please, don't add any new functionality around templating, patterns and so on
     * If you need some features more, you'r doing something wrong.
     */
    private String placeholder(String placeholderName) {
        return "~~~" + placeholderName + "~~~";
    }

    private String replacePlaceholders(Map<String, Object> values, String content) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            content = content.replace(placeholder(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return content;
    }

    protected static String read(String resourcePath) throws IOException {
        try (InputStream resource = BaseApiIntegrationTest.class.getResourceAsStream(resourcePath)) {
            return IOUtils.readInputStream(resource);
        }
    }

    /**
     * Use placeholder {@link SkipExpectedDifferences} to mark values in xml
     * that you don't want to verify.
     */
    protected static ResultMatcher xml(String xmlTemplate, Map<String, Object> responseValues) {
        return new TemplateXmlMatcher(xmlTemplate, responseValues);
    }

    /**
     * Use placeholder {@link SkipExpectedDifferences} to mark values in xml
     * that you don't want to verify.
     */
    protected static ResultMatcher xml(String xmlTemplate) {
        return new TemplateXmlMatcher(xmlTemplate, new HashMap<>());
    }

    protected String response(MvcResult result) throws UnsupportedEncodingException {
        return result.getResponse().getContentAsString();
    }
}
