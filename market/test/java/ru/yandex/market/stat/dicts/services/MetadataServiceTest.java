package ru.yandex.market.stat.dicts.services;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import ru.yandex.market.stat.dicts.common.DictionaryToDelete;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MetadataServiceTest {

    @Test
    public void testMinuteOfDay() {
        Time time = new Time(Timestamp.valueOf("2020-09-01 02:15:23").getTime());
        assertThat(MetadataService.getMinuteOfDay(time), is(135L));
    }

    @Test
    public void testGetDeleteFromLoadsCondition() {
        Set<DictionaryToDelete> dictionariesToDelete = new TreeSet<>();
        dictionariesToDelete.add(new DictionaryToDelete("test1", LoaderScale.DAYLY));
        dictionariesToDelete.add(new DictionaryToDelete("test2", LoaderScale.HOURLY));

        String expectedCondition = "(dictionary = 'test1' and scale = '1d') or (dictionary = 'test2' and scale = '1h')";

        assertThat(MetadataService.getDeleteFromLoadsCondition(dictionariesToDelete), is(expectedCondition));
    }

}
