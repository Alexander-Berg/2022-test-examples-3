package ru.yandex.market.olap2.services;

import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.retry.support.RetryTemplate;
import ru.yandex.market.olap2.dao.LoadStatus;
import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.load.tasks.ClickhouseLoadTask;
import ru.yandex.market.olap2.load.tasks.Task;
import ru.yandex.market.olap2.model.TaskFinishedDuringExecutionException;
import ru.yandex.market.olap2.sla.SlaCubesHolder;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.olap2.services.JugglerEventsSender.CRIT;
import static ru.yandex.market.olap2.services.JugglerEventsSender.SLA_CUBE_TAG;

public class JugglerEventsSenderTest {
    SlaCubesHolder cubes;
    private JugglerEventsSender jugglerEventsSender;
    private JSONObject jugglerEvent;
    @Mock
    private CloseableHttpClient jugglerHttpClient;
    private RetryTemplate jugglerRetryTemplate;
    @Mock
    private MetadataDao metadataDao;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        jugglerRetryTemplate = new RetryTemplate();

        this.jugglerEventsSender = new JugglerEventsSender(
                metadataDao, cubes, jugglerHttpClient, jugglerRetryTemplate,
                "//home/market/testing/mstat/analyst/regular/cubes_vertica",
                "testing", false
        );


    }

    @Test
    public void testConstructEvent() throws JSONException {
        jugglerEvent = jugglerEventsSender.constructEvent(new LoadStatus("my_test_cube", "2;SOME_CRITICAL"));
        assertThat(jugglerEvent.getString("source"), is("coco_loader-testing"));
        JSONObject firstElement = jugglerEvent.getJSONArray("events").getJSONObject(0);

        assertThat(firstElement.getString("service"), is("olap2etl/my_test_cube"));
        assertThat(firstElement.getString("host"), is("coco_loader-testing"));
        assertThat(firstElement.getString("status"), is("CRIT"));
        assertThat(firstElement.getString("description"), is("SOME_CRITICAL"));
    }

    @Test
    public void testConstructEventForTask() throws JSONException {
        Task task = new ClickhouseLoadTask("event", "//home/a/b/c/my_test_cube", null, null,
                null, null, null, null, null,
                0, "high", null, null, null, null, null);
        jugglerEvent = jugglerEventsSender.constructEventForTask("WARN", "load started", task);
        assertThat(jugglerEvent.getString("source"), is("coco_loader-testing"));
        JSONObject firstElement = jugglerEvent.getJSONArray("events").getJSONObject(0);

        assertThat(firstElement.getString("service"), is("olap2etl/task_my_test_cube"));
        assertThat(firstElement.getString("host"), is("coco_loader-testing"));
        assertThat(firstElement.getString("status"), is("WARN"));
        assertThat(firstElement.getString("description"), is("load started"));
        String tags = firstElement.getJSONArray("tags").toString();
        assertThat(tags, containsString(SLA_CUBE_TAG));
    }

    @Test
    public void testConstructEventForFailedTask() throws JSONException {
        Task task = new ClickhouseLoadTask("event", "//home/a/b/c/my_test_cube", null, null,
                null, null, null, null, null,
                0, "high", null, null, null, null, null);
        RuntimeException e = new RuntimeException("Task failed");
        jugglerEvent = jugglerEventsSender.constructEventForTask(CRIT, jugglerEventsSender.getCauseRecursive(e).getMessage(), task);
        assertThat(jugglerEvent.getString("source"), is("coco_loader-testing"));
        JSONObject firstElement = jugglerEvent.getJSONArray("events").getJSONObject(0);

        assertThat(firstElement.getString("service"), is("olap2etl/task_my_test_cube"));
        assertThat(firstElement.getString("host"), is("coco_loader-testing"));
        assertThat(firstElement.getString("status"), is("CRIT"));
        assertThat(firstElement.getString("description"), is("Task failed"));
        String tags = firstElement.getJSONArray("tags").toString();
        assertThat(tags, containsString(SLA_CUBE_TAG));
    }


    @Test
    public void testConstructEventForSkippedTask() throws JSONException {
        Task task = new ClickhouseLoadTask("event", "//home/a/b/c/my_test_cube", null, null,
                null, null, null, null, null,
                0, "high", null, null, null, null, null);
        RuntimeException e = new TaskFinishedDuringExecutionException();

        Mockito.when(metadataDao.getLastTaskStatus(task)).thenReturn(Arrays.asList("SKIPPED", "PUBLISH_ALREADY_LOADED_in_615975c98b7e05f05945b035"));
        jugglerEvent = jugglerEventsSender.sendTaskFail(task, e);
        assertThat(jugglerEvent.getString("source"), is("coco_loader-testing"));
        JSONObject firstElement = jugglerEvent.getJSONArray("events").getJSONObject(0);

        assertThat(firstElement.getString("service"), is("olap2etl/task_my_test_cube"));
        assertThat(firstElement.getString("host"), is("coco_loader-testing"));
        assertThat(firstElement.getString("status"), is("OK"));
        assertThat(firstElement.getString("description"), containsString("Task skipped"));
        assertThat(firstElement.getString("description"), containsString("PUBLISH_ALREADY_LOADED_in_615975c98b7e05f05945b035"));
        String tags = firstElement.getJSONArray("tags").toString();
        assertThat(tags, containsString(SLA_CUBE_TAG));
    }
}
