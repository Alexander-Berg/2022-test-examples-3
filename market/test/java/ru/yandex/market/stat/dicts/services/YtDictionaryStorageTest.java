package ru.yandex.market.stat.dicts.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.loaders.jdbc.SchemelessDictionaryRecord;
import ru.yandex.market.stat.dicts.records.ProtestDictionary;
import ru.yandex.market.stat.dicts.records.TestDictionary;
import ru.yandex.market.stat.dicts.utils.YtServiceUtils;
import ru.yandex.market.stat.dicts.services.YtClusters;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
public class YtDictionaryStorageTest {
    public static final String DEFAULT_CLUSTER = "hahn";
    public static final String ROOT_PATH = "//home/market/testing/mstat/dictionaries";

    @Mock
    Yt yt;

    @Mock
    YtClusters ytClusters;

    @Mock
    YtDictionaryStorage dictionaryStorage;

    private DictionaryYtService ytService = new DictionaryYtService(yt, ROOT_PATH, null);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(ytClusters.getYtService(any())).thenReturn(ytService);
        when(dictionaryStorage.getYtClusters()).thenReturn(ytClusters);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetTableSchema() {
        Dictionary<?> dictionary = Dictionary.fromClass(TestDictionary.class);
        YTreeListNode schema = YtServiceUtils.getTableSchema(dictionary);
        assertThat(schema.size(), is(4));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetTablePathFromClass() {
        Dictionary<?> dictionary = Dictionary.fromClass(TestDictionary.class);
        String fullDictPath = dictionaryStorage.getYtClusters().getYtService(DEFAULT_CLUSTER).absolutePath(dictionary).toString();
        log.info(fullDictPath);
        assertThat(dictionary.getName(), is("test_dictionary"));
        assertThat(dictionary.getRelativePath(), is("test_dictionary"));
        assertThat(fullDictPath, is(ROOT_PATH + "/test_dictionary"));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testGetTableDir() {
        Dictionary<?> dictionary = Dictionary.from("subdir/tblname2", SchemelessDictionaryRecord.class);
        String fullDictPath = dictionaryStorage.getYtClusters().getYtService(DEFAULT_CLUSTER).absolutePath(dictionary).toString();
        log.info(fullDictPath);
        assertThat(dictionary.getName(), is("tblname2"));
        assertThat(dictionary.getRelativePath(), is("subdir/tblname2"));
        assertThat(fullDictPath, is(ROOT_PATH + "/subdir/tblname2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetTablePathWithDirFromClass() {
        Dictionary<?> dictionary = Dictionary.fromClass(ProtestDictionary.class);
        dictionaryStorage.getYtClusters();
        dictionaryStorage.getYtClusters().getYtService(DEFAULT_CLUSTER);
        dictionaryStorage.getYtClusters().getYtService(DEFAULT_CLUSTER).absolutePath(dictionary);
        dictionaryStorage.getYtClusters().getYtService(DEFAULT_CLUSTER).absolutePath(dictionary).toString();
        String fullDictPath = dictionaryStorage.getYtClusters().getYtService(DEFAULT_CLUSTER).absolutePath(dictionary).toString();
        log.info(fullDictPath);
        assertThat(dictionary.getName(), is("protest_dictionary"));
        assertThat(dictionary.getRelativePath(), is("testDir/protest_dictionary"));
        assertThat(fullDictPath, is(ROOT_PATH + "/testDir/protest_dictionary"));
    }

}
