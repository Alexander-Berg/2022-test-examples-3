package ru.yandex.market.pers.qa.mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.market.pers.qa.CoreMockConfiguration;

@Configuration
public class QaMockConfiguration extends CoreMockConfiguration {
    @Autowired
    private WebApplicationContext wac;

    @Bean
    @Qualifier("mockMvc")
    public MockMvc getMockMvc() {
        return MockMvcBuilders.webAppContextSetup(this.wac)
                .dispatchOptions(true).build();
    }

    @Bean
    public SaasMocks saasMockHelper() {
        return new SaasMocks();
    }
}
