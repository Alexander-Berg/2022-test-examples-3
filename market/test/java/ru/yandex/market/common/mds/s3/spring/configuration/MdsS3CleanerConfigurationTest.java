package ru.yandex.market.common.mds.s3.spring.configuration;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.spring.db.ResourceConfigurationDao;
import ru.yandex.market.common.mds.s3.spring.service.MdsS3ResourceConfigurationDetector;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link MdsS3CleanerConfiguration}.
 *
 * @author Vladislav Bauer
 */
public class MdsS3CleanerConfigurationTest {

    @Test(expected = MdsS3Exception.class)
    public void testGetModuleNameNegative() {
        final MdsS3CleanerConfiguration configuration = new MdsS3CleanerConfiguration();
        assertThat(configuration.getModuleName(), nullValue());

        final ResourceConfigurationDao dao = Mockito.mock(ResourceConfigurationDao.class);
        final ResourceConfigurationProvider provider = Mockito.mock(ResourceConfigurationProvider.class);
        final MdsS3ResourceConfigurationDetector detector =
                configuration.mdsS3ResourceConfigurationDetector(dao, provider);

        fail(String.valueOf(detector));
    }

    @Test(expected = MdsS3Exception.class)
    public void testGetConfigurationTableNameNegative() {
        final MdsS3CleanerConfiguration configuration = new MdsS3CleanerConfiguration();
        assertThat(configuration.getConfigurationTableName(), nullValue());

        final JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        final ResourceConfigurationDao dao = configuration.resourceConfigurationDao(jdbcTemplate);

        fail(String.valueOf(dao));
    }

}
