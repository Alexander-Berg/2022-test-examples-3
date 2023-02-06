package ru.yandex.market.pers.test.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.application.monitoring.ComplexMonitoring;

@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
    LiquibaseAutoConfiguration.class, GsonAutoConfiguration.class,
    SpringDataWebAutoConfiguration.class, FreeMarkerAutoConfiguration.class,
    QuartzAutoConfiguration.class
})
@Configuration
public class CoreMockConfiguration {

    @Autowired
    private WebApplicationContext wac;

    @Bean
    @Qualifier("mockMvc")
    public MockMvc getMockMvc() {
        return MockMvcBuilders.webAppContextSetup(this.wac)
            .dispatchOptions(true).build();
    }

    @Bean
    public ComplexMonitoring getComplexMonitoring() {
        return PersTestMocksHolder.registerMock(ComplexMonitoring.class);
    }

}
