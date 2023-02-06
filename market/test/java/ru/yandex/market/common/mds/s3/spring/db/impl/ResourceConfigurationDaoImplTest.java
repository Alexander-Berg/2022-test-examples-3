package ru.yandex.market.common.mds.s3.spring.db.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory;
import ru.yandex.market.common.mds.s3.spring.db.ResourceConfigurationDao;
import ru.yandex.market.common.mds.s3.spring.model.ResourceConfigurationStatus;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.mds.s3.spring.SpringTestConstants.BUCKET_NAME;
import static ru.yandex.market.common.mds.s3.spring.SpringTestConstants.MODULE_NAME;
import static ru.yandex.market.common.mds.s3.spring.SpringTestConstants.TABLE_NAME;
import static ru.yandex.market.common.mds.s3.spring.model.ResourceConfigurationStatus.EXISTS;

/**
 * Unit-тесты для {@link ResourceConfigurationDaoImpl}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
@RunWith(Parameterized.class)
public class ResourceConfigurationDaoImplTest {

    private ResourceConfigurationDao instance;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ResourceConfiguration configuration;

    private final boolean withFolder;

    public ResourceConfigurationDaoImplTest(final boolean withFolder) {
        this.withFolder = withFolder;
        configuration = ResourceConfigurationProviderFactory.createWithHistoryAndLast(BUCKET_NAME, this.withFolder);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true}, {false}
        });
    }


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        instance = new ResourceConfigurationDaoImpl(jdbcTemplate, TABLE_NAME);
    }

    @Test
    public void testGetByStatus() {
        final List<ResourceConfiguration> result = Collections.singletonList(configuration);
        when(jdbcTemplate.query(anyString(), Mockito.<RowMapper<ResourceConfiguration>>any())).thenReturn(result);

        Arrays.asList(ResourceConfigurationStatus.values()).forEach(status ->
                assertThat(instance.getByStatus(status), is(result)));
    }

    @Test
    public void testDelete() {
        instance.delete(null);
        verifyNoMoreInteractions(jdbcTemplate);

        instance.delete(Collections.singletonList(configuration));
        verify(jdbcTemplate, times(1)).update(anyString());
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    public void testSetStatusFor() {
        instance.setStatusFor(EXISTS, null);
        verifyNoMoreInteractions(jdbcTemplate);

        instance.setStatusFor(EXISTS, Collections.singletonList(configuration));
        verify(jdbcTemplate, times(1)).update(anyString());
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    public void testSetStatusNotFor() {
        instance.setStatusNotFor(MODULE_NAME, EXISTS, null);
        instance.setStatusNotFor(MODULE_NAME, EXISTS, Collections.singletonList(configuration));
        verify(jdbcTemplate, times(2)).update(anyString());
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    public void testMerge() {
        instance.merge(MODULE_NAME, configuration);
        verify(jdbcTemplate, times(1)).execute(
                any(PreparedStatementCreator.class),
                any(PreparedStatementCallback.class)
        );
    }

}
