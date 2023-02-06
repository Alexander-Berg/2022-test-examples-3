package ru.yandex.market.stat.dicts.services;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.stat.dicts.common.DictionaryToDelete;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.loaders.TestLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class CleanDeletedDictionariesServiceTest {

    private List<DictionaryLoadersHolder> dictionaryLoadersHolders;

    @Mock
    private MetadataService metadataService;

    private CleanDeletedDictionariesService cleanDeletedDictionariesService;

    private Set<DictionaryToDelete> dictionariesToDelete = new HashSet<>();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        dictionaryLoadersHolders = new ArrayList<>();
        dictionaryLoadersHolders.add(
                new DictionaryLoadersHolder(
                        Arrays.asList(
                            new TestLoader(LoaderScale.DEFAULT),
                            new TestLoader(LoaderScale.DAYLY),
                            new TestLoader(LoaderScale.HOURLY)
                        )
                )
        );

        cleanDeletedDictionariesService = new CleanDeletedDictionariesService(
                metadataService, dictionaryLoadersHolders, null
        );

        dictionariesToDelete.add(new DictionaryToDelete("test_dictionary", LoaderScale.DAYLY));
        dictionariesToDelete.add(new DictionaryToDelete("test_dictionary", LoaderScale.DEFAULT));
        dictionariesToDelete.add(new DictionaryToDelete("test_dictionary", LoaderScale.HOURLY));
    }

    @Test
    public void testGetKnownToApplicationDictionaries() {
        assertThat(cleanDeletedDictionariesService.getKnownToApplicationDictionaries(), is(dictionariesToDelete));
    }

    @Test
    public void testGetCronTasksToDelete() {
        Set<String> expectedCronTasks = new HashSet<>();
        expectedCronTasks.add("hahn__test_dictionary_1d");
        expectedCronTasks.add("hahn__test_dictionary_1h");
        expectedCronTasks.add("hahn__test_dictionary");
        expectedCronTasks.add("arnold__test_dictionary_1d");
        expectedCronTasks.add("arnold__test_dictionary_1h");
        expectedCronTasks.add("arnold__test_dictionary");

        assertThat(cleanDeletedDictionariesService.getCronTasksToDelete(dictionariesToDelete), is(expectedCronTasks));
    }
}
