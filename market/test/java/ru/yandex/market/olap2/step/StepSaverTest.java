package ru.yandex.market.olap2.step;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.step.model.StepEvent;
import ru.yandex.market.olap2.step.model.StepEventParams;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StepSaverTest {

    private final static String CONFIG_PATH = "/configs/allowed_tables.yaml";

    private StepSaver saver;

    private MetadataDao metadataDao = mock(MetadataDao.class);

    @Before
    public void setup() {
        saver = new StepSaver(metadataDao, CONFIG_PATH);
    }

    @Test
    public void saveNewTable() {
        StepEvent st = new StepEvent();
        st.setId("step_event_id");
        StepEventParams params = new StepEventParams();
        params.setDestination("vertica");
        params.setPath("//tmp/test/some_table");
        st.setStepEventParams(params);
        when(metadataDao.insertStepEvent(st)).thenReturn(1);
        saver.save(st);
        verify(metadataDao, times(1)).rejectStepEvent(
                "step_event_id",
                "[NEW_VERTICA_TABLE] Event for path //tmp/test/some_table can not be processed," +
                        " since no new tables are allowed in vertica");
    }


    @Test
    public void saveOldTable() {
        StepEvent st = new StepEvent();
        st.setId("step_event_id");
        StepEventParams params = new StepEventParams();
        params.setDestination("vertica");
        String anyAllowedTable = "dim_pp";
        params.setPath("//tmp/test/" + anyAllowedTable);
        st.setStepEventParams(params);
        when(metadataDao.insertStepEvent(st)).thenReturn(1);
        saver.save(st);
        verify(metadataDao, never()).rejectStepEvent(anyString(), anyString());
    }


}
