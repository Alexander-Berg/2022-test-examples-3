package ru.yandex.market.notifier.configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import liquibase.integration.spring.SpringLiquibase;
import org.mockito.Mockito;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;

import ru.yandex.bolts.collection.Option;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checkout.config.PushApiClientConfig;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.logbroker.ping.Tvm2Checker;
import ru.yandex.market.notifier.ResourceLoadUtil;
import ru.yandex.market.notifier.config.NotifierConfig;
import ru.yandex.market.notifier.core.OXMHelper;
import ru.yandex.market.notifier.service.NotifierPropertiesHolder;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.PersNotifyVerifier;
import ru.yandex.market.notifier.util.RecipeAwarePostgres;
import ru.yandex.market.starter.quartz.processors.PartitionedCronTriggerBeanFactoryPostProcessor;
import ru.yandex.market.tms.quartz2.util.QrtzLogTableCleaner;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Semenov (alxsemn@yandex-team.ru)
 */
@Configuration
@Import({
        NotifierConfig.class,
        PartitionedCronTriggerBeanFactoryPostProcessor.class,
        PushApiClientConfig.class
})
@ImportResource({
        // Prod configs
        "classpath:market-notifier-client.xml",

        "classpath:WEB-INF/checkouter-serialization.xml",
        // Mocks
        "classpath:market-notifier-mocks.xml"
})
@PropertySource(value = {
        "classpath:application.properties",
        "classpath:application-test.properties"
})
@ComponentScan("ru.yandex.market.notifier.config")
public class TestNotifierConfig {

    @ConditionalOnMissingBean(name = "pushApiMock")
    @Bean
    public PushApi pushApiClient() {
        return mock(PushApi.class);
    }

    @Nonnull
    @Bean
    public PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer =
                new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setLocalOverride(true);
        propertySourcesPlaceholderConfigurer.setIgnoreResourceNotFound(false);
        propertySourcesPlaceholderConfigurer.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return propertySourcesPlaceholderConfigurer;
    }

    @Bean
    @DependsOn("liquibase")
    public NotifierPropertiesHolder notifierPropertiesHolder(SqlSessionTemplate sqlSessionTemplate) {
        return new NotifierPropertiesHolder(sqlSessionTemplate);
    }

    // Db Config
    @Nonnull
    @Bean
    public SpringLiquibase liquibase(@Nonnull @Qualifier("masterDS") DataSource dataSource) {
        final SpringLiquibase springLiquibase = new SpringLiquibase();

        springLiquibase.setDataSource(dataSource);
        springLiquibase.setChangeLog("classpath:/changelog/changelog-notifier.xml");
        springLiquibase.setChangeLogParameters(
                Map.of("is-unit-testing", "true")
        );

        return springLiquibase;
    }

    // Test DB
    @Nonnull
    @Bean(destroyMethod = "close")
    public Object embeddedPostgres(@Value("${market.notifier.use.recipe:false}") boolean useRecipe) throws IOException {
        if (useRecipe) {
            return new RecipeAwarePostgres();
        }
        return EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofMinutes(1))
                .start();
    }

    // Utils
    @Nonnull
    @Bean
    public ResourceLoadUtil resourceLoadUtil() {
        return new ResourceLoadUtil();
    }

    @Nonnull
    @Bean
    public PersNotifyVerifier persNotifyVerifier() {
        return new PersNotifyVerifier();
    }

    @Nonnull
    @Bean
    public EventTestUtils eventTestUtils() {
        return new EventTestUtils();
    }

    @Nonnull
    @Bean
    public OXMHelper oxmHelper() {
        return new OXMHelper();
    }

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }

    @Bean
    public Tvm2 logbrokerTvm2() {
        var mock = Mockito.mock(Tvm2.class);
        when(mock.getServiceTicket(anyInt())).thenReturn(Option.of("1"));
        return mock;
    }

    @Bean
    public Tvm2Checker tvm2Checker() {
        return Mockito.mock(Tvm2Checker.class);
    }

    @Bean
    public QrtzLogTableCleaner qrtzLogTableCleaner() {
        var mock = Mockito.mock(QrtzLogTableCleaner.class);
        doNothing().when(mock).clean();
        return mock;
    }
}
