package ru.yandex.market.adv.b2bmonetization;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.adv.b2bmonetization.config.TestConfig;
import ru.yandex.market.adv.config.EmbeddedPostgresAutoconfiguration;
import ru.yandex.market.adv.test.AbstractTest;
import ru.yandex.market.adv.yt.test.configuration.YtTestConfiguration;
import ru.yandex.market.adv.yt.test.extension.YtExtension;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(YtExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                EmbeddedPostgresAutoconfiguration.class,
                SpringApplicationConfig.class,
                JacksonAutoConfiguration.class,
                YtTestConfiguration.class,
                TestConfig.class
        }
)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class)
@TestPropertySource(locations = "/99_functional_application.properties")
public abstract class AbstractMonetizationTest extends AbstractTest {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    private BiConsumer<String, Runnable> ytRunner;

    protected JobExecutionContext mockContext() {
        return Mockito.mock(JobExecutionContext.class);
    }

    protected void run(String newPrefix, Runnable runnable) {
        ytRunner.accept(newPrefix, runnable);
    }

    /**
     * Мок запрос в mvc контроллер
     *
     * @param method       http метод
     * @param urlTemplate  ручка
     * @param status       проверяемый http код ответа
     * @param responseFile путь к файлу с проверяемым телом ответа
     * @param requestFile  путь к файлу с телом запроса, передаваемым в ручку
     * @throws RuntimeException в случае непредвиденных обстоятельств
     */
    protected ResultActions mvcPerform(HttpMethod method,
                                       String urlTemplate,
                                       int status,
                                       @Nullable String responseFile,
                                       @Nullable String requestFile,
                                       boolean withContent) {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.request(method, urlTemplate)
                .contentType(MediaType.APPLICATION_JSON);

        if (requestFile != null) {
            requestBuilder.content(loadFile(requestFile));
        }

        try {
            ResultActions resultActions = mvc.perform(requestBuilder)
                    .andExpect(status().is(status));

            if (withContent) {
                resultActions.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
            } else {
                resultActions.andExpect(status().isNoContent());
            }

            if (responseFile != null) {
                resultActions.andExpect(content().json(loadFile(responseFile)));
            }

            return resultActions;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
