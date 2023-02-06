package ru.yandex.market.common.mds.s3.spring.configuration;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.PureHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationValidator;
import ru.yandex.market.common.mds.s3.client.service.data.impl.ResourceConfigurationProviderImpl;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceConfigurationFactory;
import ru.yandex.market.common.mds.s3.spring.SpringTestConstants;
import ru.yandex.market.common.mds.s3.spring.service.MdsS3ResourceConfigurationCleaner;
import ru.yandex.market.common.mds.s3.spring.service.MdsS3ResourceConfigurationDetector;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit-тест для проверки Spring конфигураций.
 *
 * @author Vladislav Bauer
 */
public class MdsS3ConfigurationTest {

    private static final Class<?>[] EXPECTED_CLASSES = {
            // MdsS3BasicConfiguration
            AmazonS3.class,
            KeyGenerator.class,
            MdsS3Client.class,

            // MdsS3HistoryConfiguration
            PureHistoryMdsS3Client.class,
            NamedHistoryMdsS3Client.class,

            // MdsS3CleanerConfiguration
            MdsS3ResourceConfigurationCleaner.class,
            MdsS3ResourceConfigurationDetector.class,

            // MdsS3ValidatorConfiguration
            ResourceConfigurationValidator.class,

            // Прочее (root)
            JdbcTemplate.class,
            TransactionTemplate.class,
            ResourceConfigurationProvider.class
    };


    @Test
    public void testSpringConfigurationWithProvider() {
        final ApplicationContext context =
                new AnnotationConfigApplicationContext(TestRootConfigurationWithProvider.class);

        checkExpectedBeans(context);

        final ResourceConfigurationProvider provider = context.getBean(ResourceConfigurationProvider.class);
        final Collection<ResourceConfiguration> configurations = provider.getConfigurations();
        assertThat(configurations.size(), equalTo(1));
    }


    private void checkExpectedBeans(final ApplicationContext context) {
        for (final Class<?> expectedClass : EXPECTED_CLASSES) {
            final Object bean = context.getBean(expectedClass);
            assertThat(bean, notNullValue());
        }
    }


    @Configuration
    @Import(TestAppConfiguration.class)
    public static class TestRootConfigurationWithProvider extends MdsS3ResourceConfiguration {

        @Bean
        @Nonnull
        @Override
        public ResourceConfigurationFactory resourceConfigurationFactory() {
            return ResourceConfigurationFactory.create(SpringTestConstants.BUCKET_NAME);
        }

        @Bean
        @Nonnull
        @Override
        public ResourceConfigurationProvider resourceConfigurationProvider(
                @Nonnull final ResourceConfigurationFactory resourceConfigurationFactory
        ) {
            return new ResourceConfigurationProviderImpl(
                    resourceConfigurationFactory
                            .createConfigurationWithoutHistory(SpringTestConstants.NAME + "." + SpringTestConstants.EXT)
            );
        }

    }

    @Import({
            TestMdsS3BasicConfiguration.class,
            MdsS3LocationConfiguration.class,
            MdsS3HistoryConfiguration.class,
            MdsS3CleanerConfiguration.class,
            MdsS3ValidatorConfiguration.class
    })
    @Configuration
    @PropertySource("classpath:/test.properties")
    public static class TestAppConfiguration {

        @Bean
        public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        public JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        @Bean
        public TransactionTemplate transactionTemplate() {
            return mock(TransactionTemplate.class);
        }

    }

    @Configuration
    public static class TestMdsS3BasicConfiguration extends MdsS3BasicConfiguration {

        @Value("${access.key}")
        private String accessKey;

        @Value("${secret.key}")
        private String secretKey;

        @Value("${endpoint}")
        private String endpoint;


        @Nonnull
        @Override
        protected String getAccessKey() {
            return accessKey;
        }

        @Nonnull
        @Override
        protected String getSecretKey() {
            return secretKey;
        }

        @Nonnull
        @Override
        protected String getEndpoint() {
            return endpoint;
        }

    }

}
