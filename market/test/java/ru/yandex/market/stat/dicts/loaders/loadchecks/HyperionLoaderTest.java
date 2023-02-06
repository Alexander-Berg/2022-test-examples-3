package ru.yandex.market.stat.dicts.loaders.loadchecks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.stat.dicts.common.ConversionStrategy;
import ru.yandex.market.stat.dicts.config.HyperionConfig;
import ru.yandex.market.stat.dicts.config.factory.DataSourceProvider;
import ru.yandex.market.stat.dicts.config.factory.JdbcTemplateProvider;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoadIterator;
import ru.yandex.market.stat.dicts.loaders.HyperionLoader;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.loaders.jdbc.JdbcLoadConfigFromFile;
import ru.yandex.market.stat.dicts.loaders.jdbc.JdbcTaskDefinition;
import ru.yandex.market.stat.dicts.services.DictionaryStorage;
import ru.yandex.market.stat.utils.DateUtil;

import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HyperionLoaderTest {
    public static final String DEFAULT_CLUSTER = "hahn";

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    DictionaryStorage dictionaryStorage;

    @Mock
    JdbcTaskDefinition task;

    @Before
    public void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        dictionaryStorage = mock(DictionaryStorage.class);
        task = mock(JdbcTaskDefinition.class);

        when(task.isValid()).thenReturn(true);
        when(task.getScale()).thenReturn(LoaderScale.DEFAULT);
        when(task.getRelativePath()).thenReturn("//home");
    }

    @Test
    public void needLoad() {
        JdbcLoadConfigFromFile.JdbcLoadConfigFromFileBuilder builder = JdbcLoadConfigFromFile.builder().conversionStrategy(ConversionStrategy.LEGACY);

        HyperionLoader check = new HyperionLoader(jdbcTemplate, dictionaryStorage, task, DateUtil.fixedClock("2018-05-02T01:00:00"));
        assertThat(check.needLoad(DEFAULT_CLUSTER, LocalDateTime.parse("2018-05-01T00:59:59")), equalTo(true));

        check = new HyperionLoader(jdbcTemplate, dictionaryStorage, task, DateUtil.fixedClock("2018-05-02T08:58:00"));
        assertThat(check.needLoad(DEFAULT_CLUSTER, LocalDateTime.parse("2018-05-01T08:59:59")), equalTo(true));

        check = new HyperionLoader(jdbcTemplate, dictionaryStorage, task, DateUtil.fixedClock("2018-05-02T09:09:00"));
        assertThat(check.needLoad(DEFAULT_CLUSTER, LocalDateTime.parse("2018-05-02T09:01:00")), equalTo(false));

        check = new HyperionLoader(jdbcTemplate, dictionaryStorage, task, DateUtil.fixedClock("2018-05-02T09:30:00"));
        assertThat(check.needLoad(DEFAULT_CLUSTER, LocalDateTime.parse("2018-05-02T01:01:59")), equalTo(true));

        check = new HyperionLoader(jdbcTemplate, dictionaryStorage, task, DateUtil.fixedClock("2018-05-02T09:30:00"));
        assertThat(check.needLoad(DEFAULT_CLUSTER, LocalDateTime.parse("2018-05-02T09:01:59")), equalTo(false));

        check = new HyperionLoader(jdbcTemplate, dictionaryStorage, task, DateUtil.fixedClock("2018-05-02T14:30:00"));
        assertThat(check.needLoad(DEFAULT_CLUSTER, LocalDateTime.parse("2018-05-02T09:01:59")), equalTo(true));

        check = new HyperionLoader(jdbcTemplate, dictionaryStorage, task, DateUtil.fixedClock("2018-05-02T14:30:00"));
        assertThat(check.needLoad(DEFAULT_CLUSTER, LocalDateTime.parse("2018-05-02T14:01:59")), equalTo(false));
    }
}
