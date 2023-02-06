package ru.yandex.market.delivery.mdbapp.configuration;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.mockito.Mockito.mock;

@RunWith(value = Parameterized.class)
@ContextConfiguration(classes = TvmConfigurationTest.Config.class)
public class TvmConfigurationTest {

    @Autowired
    private TvmTicketChecker internalTvmTicketChecker;

    @Parameter
    public String url;

    @Parameter(1)
    public boolean needCheck;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
            {"/partner", true},
            {"/internal/support", true},
            {"/su/pport", true},
            {"/admin/mdb/ping", true},
            {"/qu/eue/statistic", true}
        });
    }

    @Before
    public void setUpContext() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Test
    public void testAdmin() {
        Assert.assertEquals(internalTvmTicketChecker.isRequestedUrlAcceptable(url), needCheck);
    }

    @Ignore
    @Configuration
    @PropertySource("classpath:application-tvm.properties")
    @EnableConfigurationProperties
    public static class Config {

        @Bean
        @ConfigurationProperties("tvm.internal")
        public TvmTicketChecker internalTvmTicketChecker() {
            return new TvmTicketCheckerImpl();
        }

        @Bean
        public TvmClientApi tvmClientApi() {
            return mock(TvmClientApi.class);
        }
    }
}
