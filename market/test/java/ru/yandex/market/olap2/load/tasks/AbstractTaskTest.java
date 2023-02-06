package ru.yandex.market.olap2.load.tasks;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.graphite.Graphite;
import ru.yandex.market.olap2.load.exceptions.TMFailedStatusException;
import ru.yandex.market.olap2.load.partitioning.PartitioningExpression;
import ru.yandex.market.olap2.step.StepSender;

import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.olap2.load.tasks.AbstractTask.anyCauseIs;
import static ru.yandex.market.olap2.load.tasks.AbstractTask.tableNameFromPath;
import static ru.yandex.market.olap2.load.tasks.AbstractTask.trimPersonalPrefix;

public class AbstractTaskTest {

    @Test
    public void testAnyCauseIs() {
        Exception e = new Exception("1", new Exception("2", new MyErr()));
        assertTrue(anyCauseIs(e, MyErr.class));
        assertTrue(anyCauseIs(e, Exception.class));
        assertTrue(anyCauseIs(new MyErr(), MyErr.class));
        assertFalse(anyCauseIs(e, RuntimeException.class));
    }

    @Test
    public void testYtFieldErrorIsHandled() {
        AbstractTask t = getMockTask();
        Exception ex = new TMFailedStatusException("cannot decode value of ClickHouse type Nullable(Decimal(15,8)) from Yt value");
        t.fail(ex);
        Mockito.verify(t.getMetadataDaoImpl(), Mockito.times(1)).rejectStepEvent(t, ExceptionUtils.getStackTrace(ex));
    }

    private static AbstractTask getMockTask() {
        return new AbstractTask(
                UUID.randomUUID().toString(),
                "//some/paritioned/table/2018-03-21",
                null,
                Mockito.mock(MetadataDao.class),
                Mockito.mock(Graphite.class),
                "dest",
                1,
                Mockito.mock(StepSender.class),
                null,
                null)
        {
            @Override
            public PartitioningExpression getPartitioningExpression() {
                return null;
            }

            @Override
            public void prepare() throws Exception {

            }

            @Override
            public void copyTable() throws Exception {

            }

            @Override
            public TaskPriority getPriority() {
                return null;
            }
        };
    }

    @Test
    public void testTableNameFromPathBasic() {
        String expected = "MARKETBI-6944_cube_show_click_banner";
        String actual = tableNameFromPath("//home/market/prestable/mstat/dwh/presentation/MARKETBI-6944_cube_show_click_banner");

        assertEquals(expected, actual);
    }

    @Test
    public void testTableNameFromPathNoPrefix() {
        String expected = "cube_show_click_banner";
        String actual = tableNameFromPath("//home/market/prestable/mstat/dwh/presentation/cube_show_click_banner");
        assertEquals(expected, actual);
    }

    @Test
    public void testTableNameFromPathProd() {
        String expected = "cube_show_click_banner";
        String actual = tableNameFromPath("//home/market/production/mstat/dwh/presentation/cube_show_click_banner/");
        assertEquals(expected, actual);
    }

    @Test
    public void testTrimPersonalPrefixMarketTicket() {
        String expected = "cube_show_click_banner";
        String actual = trimPersonalPrefix("MARKETBI-6944_cube_show_click_banner");
        assertEquals(expected, actual);
    }

    @Test
    public void testTrimPersonalPrefixMstatTicket() {
        String expected = "cube_show_click_banner";
        String actual = trimPersonalPrefix("MSTAT-6944-branch-description_cube_show_click_banner");
        assertEquals(expected, actual);
    }

    @Test
    public void testTrimPersonalPrefixNoPrefix() {
        String expected = "cube_show_click_banner";
        String actual = trimPersonalPrefix("cube_show_click_banner");
        assertEquals(expected, actual);
    }

    @Test
    public void testTrimPersonalPrefixWrongPrefix() {
        String expected = "solanj_cube_show_click_banner";
        String actual = trimPersonalPrefix("solanj_cube_show_click_banner");
        assertEquals(expected, actual);
    }

    private static class MyErr extends Exception {
    }
}
