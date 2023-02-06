package ru.yandex.market.reporting.generator.domain;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import ru.yandex.market.reporting.generator.service.domain.RegionCategoryId;
import ru.yandex.market.reporting.generator.service.domain.SeriesGroups;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
public class SeriesGroupsTest {

    private List<String> dayOfWeek = Lists.newArrayList("Mon", "Tue", "Wed", "Thu", "Fri");

    private RegionCategoryId group1 = new RegionCategoryId(1, 1);
    private RegionCategoryId group2 = new RegionCategoryId(2, 1);
    private RegionCategoryId group3 = new RegionCategoryId(3, 1);

    @Test
    public void put_with_gaps() throws Exception {
        SeriesGroups<String, Integer> seriesGroups = new SeriesGroups<>();
        seriesGroups.put(group1, Pair.of("Tue", 1));
        seriesGroups.put(group1, Pair.of("Fri", 1));
        seriesGroups.put(group2, Pair.of("Mon", 1));
        seriesGroups.put(group2, Pair.of("Wed", 2));
        seriesGroups.fillGaps(asList(1L, 2L, 3L), asList(1L), dayOfWeek, -1);

        assertThat(seriesGroups.get(group1).singleSeriesItemsStream().collect(toList()), contains(
                Pair.of("Mon", -1),
                Pair.of("Tue", 1),
                Pair.of("Wed", -1),
                Pair.of("Thu", -1),
                Pair.of("Fri", 1)));

        assertThat(seriesGroups.get(group2).singleSeriesItemsStream().collect(toList()), contains(
                Pair.of("Mon", 1),
                Pair.of("Tue", -1),
                Pair.of("Wed", 2),
                Pair.of("Thu", -1),
                Pair.of("Fri", -1)));

        assertThat(seriesGroups.get(group3).singleSeriesItemsStream().collect(toList()), contains(
                Pair.of("Mon", -1),
                Pair.of("Tue", -1),
                Pair.of("Wed", -1),
                Pair.of("Thu", -1),
                Pair.of("Fri", -1)));
    }

    @Test(expected = IllegalStateException.class)
    public void put_illegal_key() {
        SeriesGroups<String, Integer> seriesGroups = new SeriesGroups<>();
        seriesGroups.put(group1, Pair.of("AnyThing", 1));
        seriesGroups.fillGaps(asList(1L), asList(1L), dayOfWeek, -1);
    }

    @Test(expected = IllegalStateException.class)
    public void put_illegal_order() {
        SeriesGroups<String, Integer> seriesGroups = new SeriesGroups<>();
        seriesGroups.put(group1, Pair.of("Tue", 1));
        seriesGroups.put(group1, Pair.of("Mon", 1));
        seriesGroups.fillGaps(asList(1L), asList(1L), dayOfWeek, -1);
    }

    @Test
    public void filterPercentile() throws Exception {
        SeriesGroups<String, Integer> seriesGroups = new SeriesGroups<>();
        seriesGroups.put(group1, Pair.of("Mon", 10));
        seriesGroups.put(group1, Pair.of("Tue", 5));
        seriesGroups.put(group1, Pair.of("Wed", 4));
        seriesGroups.put(group1, Pair.of("Thu", 3));
        seriesGroups.put(group2, Pair.of("Mon", 3));
        seriesGroups.put(group2, Pair.of("Tue", 2));
        seriesGroups.put(group2, Pair.of("Wed", 1));

        seriesGroups.filterTop(0.80, 0);

        assertThat(seriesGroups.get(group1).singleSeriesItemsStream().collect(toList()), contains(
                Pair.of("Mon", 10),
                Pair.of("Tue", 5),
                Pair.of("Wed", 4)));

        assertThat(seriesGroups.get(group2).singleSeriesItemsStream().collect(toList()), contains(
                Pair.of("Mon", 3),
                Pair.of("Tue", 2)));

        seriesGroups.filterTop(0.50, 0);

        assertThat(seriesGroups.get(group1).singleSeriesItemsStream().collect(toList()), contains(
                Pair.of("Mon", 10)));

        assertThat(seriesGroups.get(group2).singleSeriesItemsStream().collect(toList()), contains(
                Pair.of("Mon", 3)));
    }

}