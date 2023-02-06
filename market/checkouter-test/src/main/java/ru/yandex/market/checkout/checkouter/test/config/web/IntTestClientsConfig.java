package ru.yandex.market.checkout.checkouter.test.config.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.util.TvmTicketProviderTestImpl;

@ConditionalOnWebApplication
@ImportResource("classpath:WEB-INF/checkouter-client-no-serialization.xml")
@Configuration
public class IntTestClientsConfig {

    private static final String MOCK_CHECKOUTER_HTTP_CLIENT_FACTORY_BEAN_NAME = "mockCheckouterHttpClientFactory";

    @Bean(name = MOCK_CHECKOUTER_HTTP_CLIENT_FACTORY_BEAN_NAME)
    public MockMvcClientHttpRequestFactory mockCheckouterHttpClientFactory(MockMvc mockMvc) {
        return new MockMvcClientHttpRequestFactory(mockMvc);
    }

    @Bean
    public static CheckouterRestTemplateBeanFactoryPostProcessor checkouterRestTemplateBeanPostProcessor() {
        return new CheckouterRestTemplateBeanFactoryPostProcessor(
                "checkouterRestTemplate",
                MOCK_CHECKOUTER_HTTP_CLIENT_FACTORY_BEAN_NAME
        );
    }

    @Bean
    public TvmTicketProviderTestImpl tvmTicketProviderTest() {
        return new TvmTicketProviderTestImpl();
    }

    @Bean
    public CheckouterClientBeanPostProcessor checkouterClientBeanPostProcessor() {
        return new CheckouterClientBeanPostProcessor(tvmTicketProviderTest());
    }
}
