package ru.yandex.market.common.mds.s3.spring.db.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceLifeTime;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy.HISTORY_ONLY;
import static ru.yandex.market.common.mds.s3.spring.SpringTestConstants.BUCKET_NAME;
import static ru.yandex.market.common.mds.s3.spring.SpringTestConstants.EXT;
import static ru.yandex.market.common.mds.s3.spring.SpringTestConstants.FOLDER;
import static ru.yandex.market.common.mds.s3.spring.SpringTestConstants.NAME;

/**
 * Unit-тесты для {@link ResourceConfigurationRowMapper}.
 *
 * @author Vladislav Bauer
 */
public class ResourceConfigurationRowMapperTest {

    private static final int HISTORY_STRATEGY_CODE = 1;
    private static final String TTL_UNIT = "HOURS";
    private static final int TTL = 10;


    @Test
    public void testMapper() throws Exception {
        final ResultSet rs = createResultSet();
        final ResourceConfiguration configuration = ResourceConfigurationRowMapper.INSTANCE.mapRow(rs, 0);
        final ResourceLifeTime lifeTime = configuration.getLifeTime().orElseThrow(RuntimeException::new);
        final ResourceFileDescriptor fileDescriptor = configuration.getFileDescriptor();

        assertThat(configuration.getBucketName(), equalTo(BUCKET_NAME));
        assertThat(configuration.getHistoryStrategy(), equalTo(HISTORY_ONLY));
        assertThat(fileDescriptor.getName(), equalTo(NAME));
        assertThat(fileDescriptor.getExtension().orElse(null), equalTo(EXT));
        assertThat(lifeTime.getTtlUnit(), equalTo(ChronoUnit.HOURS));
        assertThat(lifeTime.getTtl(), equalTo(TTL));
        assertThat(fileDescriptor.getFolder().orElse(null), equalTo(FOLDER));
    }


    private ResultSet createResultSet() throws SQLException {
        final ResultSet rs = Mockito.mock(ResultSet.class);
        when(rs.getString(ResourceConfigurationRowMapper.COLUMN_BUCKET)).thenReturn(BUCKET_NAME);
        when(rs.getString(ResourceConfigurationRowMapper.COLUMN_NAME)).thenReturn(NAME);
        when(rs.getString(ResourceConfigurationRowMapper.COLUMN_EXTENSION)).thenReturn(EXT);
        when(rs.getInt(ResourceConfigurationRowMapper.COLUMN_HISTORY_STRATEGY)).thenReturn(HISTORY_STRATEGY_CODE);
        when(rs.getString(ResourceConfigurationRowMapper.COLUMN_TTL_UNIT)).thenReturn(TTL_UNIT);
        when(rs.getInt(ResourceConfigurationRowMapper.COLUMN_TTL)).thenReturn(TTL);
        when(rs.getString(ResourceConfigurationRowMapper.COLUMN_FOLDER)).thenReturn(FOLDER);
        return rs;
    }

}
