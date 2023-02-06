package ru.yandex.market.clickphite.dictionary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickphite.ClickphiteService;
import ru.yandex.market.clickphite.metric.mocks.ComplicatedMonitoringMock;
import ru.yandex.market.monitoring.MonitoringStatus;
import ru.yandex.market.monitoring.MonitoringUnit;

import static org.mockito.Mockito.when;

/**
 * Created by astepanel on 22.06.17.
 */
public class DictionaryServiceTest {
    private static class SomeBigDictionary implements Dictionary {

        @Override
        public String getTable() {
            return "SomeBigDictionary";
        }

        @Override
        public List<Column> getColumns() {
            return Arrays.asList(
                new Column("some_date", ColumnType.Date),
                new Column("some_id", ColumnType.Int64, "-1"),
                new Column("some_another_id", ColumnType.Int64, "-1"),
                new Column("some_value", ColumnType.ArrayUInt64, "-1")
            );
        }

        @Override
        public List<Column> getCalculatedColumns() {
            return Arrays.asList(
                new Column("some_calculated_value", ColumnType.ArrayUInt64, "-1")
            );
        }
    }

    @Test
    public void preProcessTablesTest() {
        //Given
        SomeBigDictionary someBigDictionary = new SomeBigDictionary();


        ClickhouseService clickhouseService = Mockito.mock(ClickhouseService.class);

        DictionaryService dictionaryService = new DictionaryService();
        dictionaryService.setClickhouseService(clickhouseService);

        // When
        String host = "someHost";
        when(clickhouseService.tableExists("dict.SomeBigDictionary", host)).thenReturn(false);
        dictionaryService.preProcessTables(someBigDictionary, host);


        //Then
        InOrder inOrder = Mockito.inOrder(clickhouseService);

        inOrder.verify(clickhouseService).createDatabaseIfNotExists("dict", host);

        inOrder.verify(clickhouseService).tableExists("dict.SomeBigDictionary", host);

        inOrder.verify(clickhouseService).createTable(
            "dict.SomeBigDictionary",
            someBigDictionary.getAllColumns(),
            someBigDictionary.getEngine(),
            host,
            someBigDictionary.getEngineSpec());

        inOrder.verify(clickhouseService).dropTable(
            "dict.SomeBigDictionary_new",
            host
        );

        inOrder.verify(clickhouseService).createTable(
            someBigDictionary.getDb() + "." + someBigDictionary.getTable() + DictionaryService.NEW_POSTFIX,
            someBigDictionary.getAllColumns(),
            someBigDictionary.getEngine(),
            host,
            someBigDictionary.getEngineSpec()
        );

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void postProcessTablesTest() {
        ClickhouseService clickhouseService = Mockito.mock(ClickhouseService.class);

        DictionaryService dictionaryService = new DictionaryService();
        dictionaryService.setClickhouseService(clickhouseService);

        // When
        dictionaryService.postProcessTables("dict.SomeBigDictionary", "someHost");

        //Then
        InOrder inOrder = Mockito.inOrder(clickhouseService);
        inOrder.verify(clickhouseService).dropTable(
            "dict.SomeBigDictionary_old",
            "someHost"
        );
        inOrder.verify(clickhouseService).doubleRenameTable(
            "dict.SomeBigDictionary",
            "dict.SomeBigDictionary_old",
            "dict.SomeBigDictionary_new",
            "dict.SomeBigDictionary",
            "someHost"
        );
        inOrder.verifyNoMoreInteractions();

    }

    public static class MonitoringTest {
        private static final RuntimeException MOCK_EXCEPTION = new RuntimeException("Mock exception");

        private DictionaryService dictionaryService;
        private ComplicatedMonitoringMock complicatedMonitoringMock;
        private ClickhouseService clickhouseService;
        private DictionaryLoadTask dictionaryLoadTask;

        @Before
        public void setUp() throws Exception {
            dictionaryService = new DictionaryService();

            complicatedMonitoringMock = new ComplicatedMonitoringMock();
            dictionaryService.setMonitoring(complicatedMonitoringMock);

            dictionaryLoadTask = Mockito.mock(DictionaryLoadTask.class);
            when(dictionaryLoadTask.getDictionary()).thenReturn(new SomeBigDictionary());
            dictionaryService.setDictionaries(Collections.singletonList(dictionaryLoadTask));

            ClickphiteService clickphiteService = Mockito.mock(ClickphiteService.class);
            when(clickphiteService.isMaster()).thenReturn(true);
            dictionaryService.setClickphiteService(clickphiteService);

            clickhouseService = Mockito.mock(ClickhouseService.class);
            dictionaryService.setClickhouseService(clickhouseService);

            dictionaryService.setupMonitoringUnits();
        }

        @Test
        public void oneHostThreeFailures() throws Exception {
            when(clickhouseService.getClusterHosts())
                .thenReturn(Collections.singletonList("welder01v.market.yandex.net"));
            when(dictionaryLoadTask.getLoader()).thenThrow(MOCK_EXCEPTION);

            dictionaryService.updateDictionaries();

            MonitoringUnit unit = complicatedMonitoringMock.getUnit();
            Assert.assertEquals(MonitoringStatus.WARNING, unit.getStatus());
            Assert.assertEquals(
                "Can't update dictionary 'SomeBigDictionary' on hosts welder01v.market.yandex.net",
                unit.getMessage()
            );
            Assert.assertThat(unit.getException(), CoreMatchers.instanceOf(RuntimeException.class));
        }

        @Test
        public void twoHostsFirstFailedSecondsSucceeded() throws Exception {
            when(clickhouseService.getClusterHosts()).thenReturn(
                Arrays.asList("welder01v.market.yandex.net", "welder02v.market.yandex.net")
            );

            DictionaryLoader dictionaryLoader = Mockito.mock(DictionaryLoader.class);
            when(dictionaryLoadTask.getLoader())
                // Падаем на всех 3-х ретраях на 1-м хосте
                .thenThrow(MOCK_EXCEPTION)
                .thenThrow(MOCK_EXCEPTION)
                .thenThrow(MOCK_EXCEPTION)
                .thenReturn(dictionaryLoader);
            dictionaryService.updateDictionaries();

            MonitoringUnit unit = complicatedMonitoringMock.getUnit();
            Assert.assertEquals(MonitoringStatus.WARNING, unit.getStatus());
            Assert.assertEquals(
                "Can't update dictionary 'SomeBigDictionary' on hosts welder01v.market.yandex.net", unit.getMessage()
            );
            Assert.assertThat(unit.getException(), CoreMatchers.instanceOf(RuntimeException.class));
        }
    }

}
