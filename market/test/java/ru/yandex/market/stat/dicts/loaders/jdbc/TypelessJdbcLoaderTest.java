package ru.yandex.market.stat.dicts.loaders.jdbc;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.stat.dicts.loaders.DictionaryLoadIterator;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.services.YtDictionaryStorage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class TypelessJdbcLoaderTest {
    public static final String DEFAULT_CLUSTER = "hahn";

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    YtDictionaryStorage dictionaryStorage;

    @Mock
    JdbcTaskDefinition task;

    TypelessJdbcLoader loader;

    final long ttlDays = 3;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {

        when(task.isValid()).thenReturn(true);
        when(task.getTtlDays()).thenReturn(ttlDays);
        when(task.getScale()).thenReturn(LoaderScale.DEFAULT);
        when(task.getRelativePath()).thenReturn("//home");

        DictionaryLoadIterator<SchemelessDictionaryRecord> iterator = mock(DictionaryLoadIterator.class);

        when(dictionaryStorage.getStand()).thenReturn("local");

        JdbcLoader<SchemelessDictionaryRecord> jdbcLoader = mock(JdbcLoader.class);
        when(jdbcLoader.iterator(any())).thenReturn(iterator);
        when(jdbcLoader.iterator(eq(DEFAULT_CLUSTER), any())).thenReturn(iterator);
        when(jdbcLoader.getDictionaryStorage()).thenReturn(dictionaryStorage);

        loader = spy(new TypelessJdbcLoader(jdbcTemplate, dictionaryStorage, task));
        loader.jdbcLoader = jdbcLoader;
    }

    @Test
    public void notSetTtlWhenLoad() throws Exception {
        System.out.println(LocalDateTime.now());

        loader.load(DEFAULT_CLUSTER, LocalDateTime.now());
    }
}
