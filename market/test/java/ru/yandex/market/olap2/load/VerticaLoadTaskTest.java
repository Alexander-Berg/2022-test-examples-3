package ru.yandex.market.olap2.load;

import org.junit.Test;
import ru.yandex.market.olap2.load.tasks.VerticaLoadTask;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.olap2.load.partitioning.PartitionType.hyphenate;

public class VerticaLoadTaskTest {
    @Test
    public void mustMakeNonHistoricalTableNameFromPath() {
        assertThat(VerticaLoadTask.tableNameFromPath(
            "//home/market/production/mstat/analyst/regular/tmp/cubes_vertica/dim_category", "vertica"),
            is("cubes_vertica__dim_category"));
        assertThat(VerticaLoadTask.tableNameFromPath(
            "//cubes.vertica/dim[category]", "vertica"),
            is("cubes_vertica__dim_category_"));
        assertThat(VerticaLoadTask.tableNameFromPath(
            "//home/market/prestable/mstat/analyst/regular/tmp/cubes_vertica_prestable/dim_category", "vertica"),
            is("cubes_vertica_prestable__dim_category"));
        assertThat(VerticaLoadTask.tableNameFromPath(
            "//home/market/prestable/mstat/analyst/regular/tmp/cubes_vertica_prestable/dim_category", "clickhouse"),
            is("cubes_clickhouse_prestable__dim_category"));
    }

    @Test(expected = RuntimeException.class)
    public void mustFailToMakeTableNameFromPath() {
        VerticaLoadTask.tableNameFromPath("dim_category", "");
    }

    @Test
    public void testMonthPartitionedTask() {
        VerticaLoadTask t = new TestVerticaLoadTask("eid1", "//some/paritioned/table/2018-03", 201803);
        assertThat(t.isHistoricalTable(), is(true));
        assertTrue(!t.getCreatedAt().isEmpty());
        assertThat(t.getPartition(), is(201803));
        assertThat(t.getPath(), is("//some/paritioned/table"));
        assertThat(t.getPathWithPartition(), is("//some/paritioned/table/2018-03"));
        assertThat(t.getStepEventId(), is("eid1"));
        assertThat(t.getTable(), is("cubes_vertica__table"));
    }

    @Test
    public void testDayPartitionedTask() {
        VerticaLoadTask t = new TestVerticaLoadTask("eid2", "//some/paritioned/table/2018-03-21", 20180321);
        assertThat(t.getPartition(), is(20180321));
        assertThat(t.getPathWithPartition(), is("//some/paritioned/table/2018-03-21"));
    }

    @Test
    public void testMonthHyphenation() {
        assertThat(hyphenate(201803), is("2018-03"));
        assertThat(hyphenate(200112), is("2001-12"));
    }

    @Test
    public void testDayHyphenation() {
        assertThat(hyphenate(20180331), is("2018-03-31"));
        assertThat(hyphenate(20011228), is("2001-12-28"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyphenationFailOnFormat() {
        hyphenate(1234567);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyphenationFailOnNull() {
        hyphenate(null);
    }
}
