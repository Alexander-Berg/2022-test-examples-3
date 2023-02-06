package ru.yandex.market.olap2.load;

import org.junit.Test;

import ru.yandex.market.olap2.load.tasks.ClickhouseLoadTask;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.olap2.load.partitioning.PartitionType.hyphenate;
import static ru.yandex.market.olap2.load.tasks.ClickhouseLoadTask.convertToChTableName;
import static ru.yandex.market.olap2.load.tasks.ClickhouseLoadTask.cutDistributedFromName;

public class ClickhouseLoadTaskTest {
    @Test
    public void mustMakeNonHistoricalTableNameFromPath() {
        assertThat(ClickhouseLoadTask.tableNameFromPath(
            "//home/market/production/mstat/analyst/regular/tmp/cubes_vertica/dim_category", "vertica"),
            is("cubes_vertica__dim_category"));
        assertThat(ClickhouseLoadTask.tableNameFromPath(
            "//cubes.vertica/dim[category]", "vertica"),
            is("cubes_vertica__dim_category_"));
        assertThat(ClickhouseLoadTask.tableNameFromPath(
            "//home/market/prestable/mstat/analyst/regular/tmp/cubes_vertica_prestable/dim_category", "vertica"),
            is("cubes_vertica_prestable__dim_category"));
        assertThat(ClickhouseLoadTask.tableNameFromPath(
            "//home/market/prestable/mstat/analyst/regular/tmp/cubes_vertica_prestable/dim_category", "clickhouse"),
            is("cubes_clickhouse_prestable__dim_category"));
    }

    @Test
    public void mustMakeHistoricalTableNameFromPath() {
        assertThat(ClickhouseLoadTask.tableNameFromPath(
                "//home/market/production/mstat/analyst/regular/cubes_vertica/fact/2020-10", "clickhouse", true),
                is("cubes_clickhouse__fact"));
    }

    @Test(expected = RuntimeException.class)
    public void mustFailToMakeTableNameFromPath() {
        ClickhouseLoadTask.tableNameFromPath("dim_category", "");
    }

    @Test
    public void testMonthPartitionedTask() {
        ClickhouseLoadTask t = new TestClickhouseLoadTask("eid1", "//some/paritioned/table/2018-03", 201803);
        assertThat(t.isHistoricalTable(), is(true));
        assertTrue(!t.getCreatedAt().isEmpty());
        assertThat(t.getPartition(), is(201803));
        assertThat(t.getPath(), is("//some/paritioned/table"));
        assertThat(t.getPathWithPartition(), is("//some/paritioned/table/2018-03"));
        assertThat(t.getStepEventId(), is("eid1"));
        assertThat(t.getTable(), is("cubes_clickhouse__table"));
    }

    @Test
    public void testDayPartitionedTask() {
        ClickhouseLoadTask t = new TestClickhouseLoadTask("eid2", "//some/paritioned/table/2018-03-21", 20180321);
        assertThat(t.getPartition(), is(20180321));
        assertThat(t.getPathWithPartition(), is("//some/paritioned/table/2018-03-21"));
    }

    @Test
    public void testMonthQPartitionedTask() {
        ClickhouseLoadTask t = new TestClickhouseLoadTask("eid3", "//some/paritioned/table/2018-03-2", 2018032);
        assertThat(t.getPartition(), is(2018032));
        assertThat(t.getPathWithPartition(), is("//some/paritioned/table/2018-03-2"));
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

    @Test
    public void testMonthQHyphenation() {
        assertThat(hyphenate(2018033), is("2018-03-3"));
        assertThat(hyphenate(2001122), is("2001-12-2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyphenationFailOnFormat() {
        hyphenate(123);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyphenationFailOnNull() {
        hyphenate(null);
    }

    @Test
    public void testTableNameFromPathBasic() {
        String expected = "cubes_clickhouse__MARKETBI_6944_cube_show_click_banner";
        String actual = convertToChTableName("MARKETBI-6944_cube_show_click_banner");

        assertEquals(expected, actual);
    }

    @Test
    public void testTableNameFromPathBasicPrest() {
        String expected = "cubes_clickhouse_prestable__MARKETBI_6944_cube_show_click_banner";
        String actual = convertToChTableName("MARKETBI-6944_cube_show_click_banner", true);

        assertEquals(expected, actual);
    }

    @Test
    public void testTableNameFromPathNoPrefix() {
        String expected = "cubes_clickhouse__cube_show_click_banner";
        String actual = convertToChTableName("cube_show_click_banner");
        assertEquals(expected, actual);
    }

    @Test
    public void testTableNameFromPathNoPrefixPrest() {
        String expected = "cubes_clickhouse_prestable__cube_show_click_banner";
        String actual = convertToChTableName("cube_show_click_banner", true);
        assertEquals(expected, actual);
    }

    @Test
    public void testCutDistributedFromNameBase() {
        String expected = "cube_show_click_banner";
        String actual = cutDistributedFromName("cube_show_click_banner_distributed");
        assertEquals(expected, actual);
    }

    @Test
    public void testCutDistributedFromNameNonDistributed() {
        String expected = "cube_show_click_banner";
        String actual = cutDistributedFromName("cube_show_click_banner");
        assertEquals(expected, actual);
    }
}
