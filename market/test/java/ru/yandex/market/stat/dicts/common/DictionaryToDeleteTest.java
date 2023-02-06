package ru.yandex.market.stat.dicts.common;

import org.junit.Test;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author sstradomsky
 */
public class DictionaryToDeleteTest {

    @Test
    public void testGetCronTaskSuffixDaily() {
        DictionaryToDelete dictionary = new DictionaryToDelete("test", LoaderScale.DAYLY);

        assertThat(dictionary.getCronTaskSuffix(), is("_1d"));
    }

    @Test
    public void testGetCronTaskSuffixHourly() {
        DictionaryToDelete dictionary = new DictionaryToDelete("test", LoaderScale.HOURLY);

        assertThat(dictionary.getCronTaskSuffix(), is("_1h"));
    }

    @Test
    public void testGetCronTaskSuffixDefault() {
        DictionaryToDelete dictionary = new DictionaryToDelete("test", LoaderScale.DEFAULT);

        assertThat(dictionary.getCronTaskSuffix(), is(""));
    }

    @Test
    public void testGetCronTaskSuffixDefaultMonth() {
        DictionaryToDelete dictionary = new DictionaryToDelete("test", LoaderScale.DEFAULT_MONTH);

        assertThat(dictionary.getCronTaskSuffix(), is(""));
    }

    @Test
    public void testGetCronTasks() {
        DictionaryToDelete dictionary = new DictionaryToDelete("test", LoaderScale.DAYLY);

        List<String> expectedCronTasks = new LinkedList<>();
        expectedCronTasks.add("hahn__test_1d");
        expectedCronTasks.add("arnold__test_1d");

        assertThat(dictionary.getCronTasks(), is(expectedCronTasks));
    }
}
