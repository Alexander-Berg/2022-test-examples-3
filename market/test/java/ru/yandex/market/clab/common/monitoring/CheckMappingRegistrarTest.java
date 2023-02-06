package ru.yandex.market.clab.common.monitoring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;

import javax.annotation.Nonnull;

import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 26.12.2018
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = {
    CheckMappingRegistrarTest.Context.class,
})
@AutoConfigureMockMvc(secure = false)
@EnableWebMvc
public class CheckMappingRegistrarTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void test() throws Exception {
        mockMvc.perform(get("/check/empty-baskets"))
            .andExpect(matchAll(
                content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN),
                content().string("1;5 baskets are empty"))
            );
   }

    @Configuration
    public static class Context {

        @Autowired
        private ApplicationContext context;

        @Autowired
        private RequestMappingHandlerMapping handlerMapping;

        @Bean
        public CheckProvider emptyBasketMonitoring() {
            return new EmptyBasketMonitoring();
        }

        @Bean
        public CheckMappingRegistrar checkMappingRegistrar() {
            return new CheckMappingRegistrar(context, handlerMapping);
        }
    }

    @MonitoringCheck("empty-baskets")
    public static class EmptyBasketMonitoring implements CheckProvider {
        @Nonnull
        @Override
        public ComplexMonitoring.Result check() {
            return new ComplexMonitoring.Result(MonitoringStatus.WARNING, "5 baskets are empty");
        }
    }
}
