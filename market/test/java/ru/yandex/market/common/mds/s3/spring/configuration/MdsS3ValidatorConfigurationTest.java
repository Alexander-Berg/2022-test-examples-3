package ru.yandex.market.common.mds.s3.spring.configuration;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationValidator;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link MdsS3ValidatorConfiguration}.
 *
 * @author Vladislav Bauer
 */
public class MdsS3ValidatorConfigurationTest {

    @Test
    public void testGetResourceConfigurationValidator() {
        final MdsS3ValidatorConfiguration configuration = new MdsS3ValidatorConfiguration();
        final ResourceConfigurationValidator validator = configuration.resourceConfigurationValidator();

        // Проверить наличие default-ного валидатора
        assertThat(validator, not(nullValue()));
    }

}
