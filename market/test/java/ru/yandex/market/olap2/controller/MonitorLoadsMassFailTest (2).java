package ru.yandex.market.olap2.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.olap2.dao.FailStatus;
import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.model.SlaCubesHolder;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MonitorLoadsMassFailTest {

    private static final String YT_PATH = "//tmp/cube_path";
    private MonitorLoadsController controller;

    @Mock
    private MetadataDao metadataDao;
    @Mock
    private SlaCubesHolder cubes;


    @Before
    public void init() {
        controller = new MonitorLoadsController(metadataDao, cubes, "hahn", YT_PATH);
    }

    @Test
    //все выгрузки ок
    public void testAllOK() {
        List<FailStatus> loadStatuses = Collections.singletonList(new FailStatus("OK", 20));
        when(metadataDao.getLoadStatuses(YT_PATH, "clickhouse", 3)).thenReturn(loadStatuses);

        ResponseEntity<String> result = controller.chMassFailCheck();
        verify(metadataDao).getLoadStatuses(YT_PATH, "clickhouse", 3);
        assertThat(result.getBody(), is(JugglerConstants.OK));
    }

    @Test
    //все выгрузки ок
    public void testAllOKVertica() {
        List<FailStatus> loadStatuses = Collections.singletonList(new FailStatus("OK", 20));
        when(metadataDao.getLoadStatuses(YT_PATH, "vertica", 3)).thenReturn(loadStatuses);

        ResponseEntity<String> result = controller.verticaMassFailCheck();
        verify(metadataDao).getLoadStatuses(YT_PATH, "vertica", 3);
        assertThat(result.getBody(), is(JugglerConstants.OK));
    }

    @Test
    //некоторые выгрузки падают, но хороших больше
    public void testFewFailsIsOk() {
        List<FailStatus> loadStatuses = Arrays.asList(
                new FailStatus("FAILED", 2),
                new FailStatus("OK", 10)
        );

        when(metadataDao.getLoadStatuses(YT_PATH, "clickhouse", 3)).thenReturn(loadStatuses);
        ResponseEntity<String> result = controller.chMassFailCheck();
        verify(metadataDao).getLoadStatuses(YT_PATH, "clickhouse", 3);
        assertThat(result.getBody(), is(JugglerConstants.OK));
    }

    @Test
    //выгрузки падают, но понятных статусов < 10
    public void testOnlyWarnWhenFewLoads() {
        List<FailStatus> loadStatuses = Arrays.asList(
                new FailStatus("FAILED", 5),
                new FailStatus("LOADING", 7)
        );

        when(metadataDao.getLoadStatuses(YT_PATH, "clickhouse", 3)).thenReturn(loadStatuses);
        ResponseEntity<String> result = controller.chMassFailCheck();
        verify(metadataDao).getLoadStatuses(YT_PATH, "clickhouse", 3);
        assertThat(result.getBody(),
                is(JugglerConstants.WARN + "Failing while loading to clickhouse: failed=5, ok=0"));
    }


    @Test
    //выгрузки падают, их > 10, упавших > 3*ок
    public void testMassFail() {
        List<FailStatus> loadStatuses = Arrays.asList(
                new FailStatus("FAILED", 5),
                new FailStatus("LOADING", 7),
                new FailStatus("MAY BE FAILING", 7)
        );

        when(metadataDao.getLoadStatuses(YT_PATH, "clickhouse", 3)).thenReturn(loadStatuses);
        ResponseEntity<String> result = controller.chMassFailCheck();
        verify(metadataDao).getLoadStatuses(YT_PATH, "clickhouse", 3);
        assertThat(result.getBody(),
                is(JugglerConstants.CRIT + "Mass fail while loading to clickhouse: failed=12, ok=0"));
    }


    @Test
    //выгрузки падают, их > 10, упавших > 3*ок
    public void testMassFailWithOk() {
        List<FailStatus> loadStatuses = Arrays.asList(
                new FailStatus("FAILED", 5),
                new FailStatus("MAY BE FAILING", 7),
                new FailStatus("LOADING", 7),
                new FailStatus("OK", 2)
        );

        when(metadataDao.getLoadStatuses(YT_PATH, "vertica", 3)).thenReturn(loadStatuses);
        ResponseEntity<String> result = controller.verticaMassFailCheck();
        verify(metadataDao).getLoadStatuses(YT_PATH, "vertica", 3);
        assertThat(result.getBody(),
                is(JugglerConstants.CRIT + "Mass fail while loading to vertica: failed=12, ok=2"));
    }

    @Test
    //выгрузки падают, их > 10, failed < 3*ок но failed>=ok
    public void testMassFailWithSomeOks() {
        List<FailStatus> loadStatuses = Arrays.asList(
                new FailStatus("FAILED", 5),
                new FailStatus("MAY BE FAILING", 1),
                new FailStatus("OK", 5)
        );

        when(metadataDao.getLoadStatuses(YT_PATH, "clickhouse", 3)).thenReturn(loadStatuses);
        ResponseEntity<String> result = controller.chMassFailCheck();
        verify(metadataDao).getLoadStatuses(YT_PATH, "clickhouse", 3);
        assertThat(result.getBody(),
                is(JugglerConstants.WARN + "Failing while loading to clickhouse: failed=6, ok=5"));
    }
}
