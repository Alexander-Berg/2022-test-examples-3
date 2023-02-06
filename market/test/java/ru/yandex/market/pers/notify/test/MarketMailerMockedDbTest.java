package ru.yandex.market.pers.notify.test;


import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.pers.grade.client.GradeClient;

@ContextConfiguration(classes = MarketMailerMockedDbTest.MailerTestConfig.class, inheritLocations = false)
public abstract class MarketMailerMockedDbTest extends MockedDbTest {
    @Autowired
    private MarketMailerTestEnvironment marketMailerTestEnvironment;
    @Autowired
    protected GradeClient gradeClient;

    @BeforeEach
    public void setUpMarketMailer() throws Exception {
        marketMailerTestEnvironment.setUp();
    }

    @Configuration
    @ImportResource("classpath:market-mailer-test-bean.xml")
    @PropertySource(value = {
            "classpath:mail-core/memcached.properties",
            "classpath:test-application.properties",
            "classpath:market-mailer-test.properties",
            "local-application.properties"
    }, ignoreResourceNotFound = true, encoding = "UTF-8")
    static class MailerTestConfig {

    }
}
