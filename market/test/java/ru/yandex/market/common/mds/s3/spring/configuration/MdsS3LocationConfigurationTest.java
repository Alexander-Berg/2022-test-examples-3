package ru.yandex.market.common.mds.s3.spring.configuration;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.mds.s3.spring.SpringTestConstants;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link MdsS3LocationConfiguration}.
 *
 * @author Vladislav Bauer
 */
public class MdsS3LocationConfigurationTest {

    @Test
    public void testGetResourceFactoryPositive() {
        final String bucketName = SpringTestConstants.BUCKET_NAME;
        final MdsS3LocationConfiguration configuration = createConfiguration(bucketName);
        final ResourceLocationFactory factory = configuration.resourceLocationFactory();

        assertThat(factory, notNullValue());
        assertThat(factory.getBucketName(), equalTo(bucketName));
    }

    @Test(expected = MdsS3Exception.class)
    public void testGetResourceFactoryNegative() {
        final MdsS3LocationConfiguration configuration = createConfiguration();
        final ResourceLocationFactory factory = configuration.resourceLocationFactory();

        fail(String.valueOf(factory));
    }

    @Test
    public void testEmptyPrefix() {
        final ApplicationContext context = new AnnotationConfigApplicationContext(TestMdsS3LocationConfiguration.class);
        final MdsS3LocationConfiguration contextBean = context.getBean(TestMdsS3LocationConfiguration.class);

        String defaultPathPrefix = contextBean.getDefaultPathPrefix();
        assertThat(defaultPathPrefix, nullValue());
    }

    private MdsS3LocationConfiguration createConfiguration(final String bucketName) {
        return new MdsS3LocationConfiguration() {
            @Override
            protected String getDefaultBucketName() {
                return bucketName;
            }
        };
    }

    private MdsS3LocationConfiguration createConfiguration() {
        return new MdsS3LocationConfiguration();
    }


    @Configuration
    static class TestMdsS3LocationConfiguration extends MdsS3LocationConfiguration {

        @Bean
        public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

    }

}
