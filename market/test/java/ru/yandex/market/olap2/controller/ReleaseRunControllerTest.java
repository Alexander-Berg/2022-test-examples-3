package ru.yandex.market.olap2.controller;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.olap2.controller.ReleaseRunController.Result;
import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.load.TaskGenerator;
import ru.yandex.market.olap2.model.LoadTaskStatus;
import ru.yandex.market.olap2.model.StepEventsQueryResult;
import ru.yandex.market.olap2.model.YtCluster;
import ru.yandex.market.olap2.step.model.StepEvent;
import ru.yandex.market.olap2.step.model.StepEventParams;
import ru.yandex.market.olap2.util.ManifestUtil;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.olap2.controller.ReleaseRunController.ResultState.FAILED;
import static ru.yandex.market.olap2.controller.ReleaseRunController.ResultState.IN_PROGRESS;
import static ru.yandex.market.olap2.controller.ReleaseRunController.ResultState.OK;
import static ru.yandex.market.olap2.model.LoadTaskStatus.FAILURE;
import static ru.yandex.market.olap2.model.LoadTaskStatus.LOADING_INTO_CH;
import static ru.yandex.market.olap2.model.LoadTaskStatus.REJECTED;
import static ru.yandex.market.olap2.model.LoadTaskStatus.SKIPPED;
import static ru.yandex.market.olap2.model.LoadTaskStatus.SUCCESS;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseRunControllerTest {

    private final static long REVISION = 1337L;
    private final static String BASE_TASK_ID = "randomidentifier";
    private final static String RELEASE_TASK_ID = BASE_TASK_ID + "-release-" + REVISION;
    private final static String PATH = "//tmp/test/qq";
    private final static String DESTINATION = "clickhouse";

    @Mock
    private MetadataDao dao;
    @Mock
    private TaskGenerator generator;
    @Mock
    private ManifestUtil manifestUtil;

    private ReleaseRunController controller;

    @Before
    public void init() {
        controller = new ReleaseRunController(dao, generator, manifestUtil);
        Mockito.reset(dao, generator, manifestUtil);
    }

    @Test
    public void foundExistingFinishedTask() {
        when(manifestUtil.getRevisionFromManifest()).thenReturn(REVISION);
        when(dao.getCurrentReleaseJob(REVISION)).thenReturn(Optional.of(buildResultForStatus(SUCCESS)));

        ResponseEntity<Result> response = controller.checkReleaseJob();

        verify(dao, never()).getLastSuccessfulForPartitioningCube(anyString());
        verify(generator, never()).checkNewStepEvents();
        assertSoftly(softly -> {
            softly.assertThat(response).isNotNull();
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            softly.assertThat(response.getBody())
                    .isEqualTo(new Result(OK, "taskId=" + RELEASE_TASK_ID));
        });
    }

    @Test
    public void foundExistingFailedNonRetryableTask() {
        when(manifestUtil.getRevisionFromManifest()).thenReturn(REVISION);
        when(dao.getCurrentReleaseJob(REVISION)).thenReturn(Optional.of(buildResultForStatus(REJECTED)));

        ResponseEntity<Result> response = controller.checkReleaseJob();

        verify(dao, never()).getLastSuccessfulForPartitioningCube(anyString());
        verify(generator, never()).checkNewStepEvents();
        assertSoftly(softly -> {
            softly.assertThat(response).isNotNull();
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            softly.assertThat(response.getBody())
                    .isEqualTo(new Result(FAILED, "id=" + RELEASE_TASK_ID + ", message=Failed loading a release task"));
        });
    }

    @Test
    public void foundExistingSkippedTask() {
        when(manifestUtil.getRevisionFromManifest()).thenReturn(REVISION);
        when(dao.getCurrentReleaseJob(REVISION)).thenReturn(Optional.of(buildResultForStatus(SKIPPED)));

        ResponseEntity<Result> response = controller.checkReleaseJob();

        verify(dao, never()).getLastSuccessfulForPartitioningCube(anyString());
        verify(generator, never()).checkNewStepEvents();
        assertSoftly(softly -> {
            softly.assertThat(response).isNotNull();
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            softly.assertThat(response.getBody())
                    .isEqualTo(new Result(FAILED, "id=" + RELEASE_TASK_ID + ", message=Failed loading a release task"));
        });
    }

    @Test
    public void foundExistingRejectedRetryable() {
        when(manifestUtil.getRevisionFromManifest()).thenReturn(REVISION);
        when(dao.getCurrentReleaseJob(REVISION)).thenReturn(Optional.of(buildResultForStatus(FAILURE)));

        ResponseEntity<Result> response = controller.checkReleaseJob();

        verify(dao, never()).getLastSuccessfulForPartitioningCube(anyString());
        verify(generator, never()).checkNewStepEvents();
        assertSoftly(softly -> {
            softly.assertThat(response).isNotNull();
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            softly.assertThat(response.getBody())
                    .isEqualTo(new Result(
                            IN_PROGRESS,
                            "id=" + RELEASE_TASK_ID + ", message=Task has been rejected, current retry count is 1"));
        });
    }

    @Test
    public void foundExistingInProgress() {
        when(manifestUtil.getRevisionFromManifest()).thenReturn(REVISION);
        StepEventsQueryResult result = buildResultForStatus(LOADING_INTO_CH);
        when(dao.getCurrentReleaseJob(REVISION)).thenReturn(Optional.of(result));

        ResponseEntity<Result> response = controller.checkReleaseJob();

        verify(dao, never()).getLastSuccessfulForPartitioningCube(anyString());
        verify(generator, never()).checkNewStepEvents();
        assertSoftly(softly -> {
            softly.assertThat(response).isNotNull();
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            softly.assertThat(response.getBody())
                    .isEqualTo(new Result(
                            IN_PROGRESS, "id=" + RELEASE_TASK_ID + ", message=Current state is " + LOADING_INTO_CH)
                    );
        });
    }

    @Test
    public void noExistingCreatingANewOne() {
        when(manifestUtil.getRevisionFromManifest()).thenReturn(REVISION);
        when(dao.getCurrentReleaseJob(REVISION)).thenReturn(Optional.empty());
        when(dao.getLastSuccessfulForPartitioningCube(anyString())).thenReturn(Optional.of(buildStepEvent()));

        ResponseEntity<Result> response = controller.checkReleaseJob();

        ArgumentCaptor<StepEvent> captor = ArgumentCaptor.forClass(StepEvent.class);
        verify(dao, times(1)).insertStepEvent(captor.capture());
        verify(generator, times(1)).checkNewStepEvents();
        assertSoftly(softly -> {
            softly.assertThat(response).isNotNull();
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            softly.assertThat(response.getBody()).isEqualTo(new Result(IN_PROGRESS,
                    "id="+ RELEASE_TASK_ID + ", message=Created a new one"));
            softly.assertThat(captor.getValue()).isNotNull();
            softly.assertThat(captor.getValue().getId()).isEqualTo(RELEASE_TASK_ID);
            softly.assertThat(captor.getValue().getStepEventParams()).isNotNull();
            softly.assertThat(captor.getValue().getStepEventParams().getRevisionNumber()).isEqualTo(REVISION);
        });
    }

    private StepEvent buildStepEvent() {
        StepEvent event = new StepEvent();
        event.setName("marketstat_olap2_publish");
        event.setId(BASE_TASK_ID);
        event.setTimeCreated(now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        StepEventParams params = new StepEventParams();
        params.setPriority("High");
        params.setRevisionNumber(REVISION);
        params.setCluster("hahn");
        params.setDestination(DESTINATION);
        params.setPath(PATH);
        event.setStepEventParams(params);
        return event;
    }

    private StepEventsQueryResult buildResultForStatus(LoadTaskStatus status) {
        boolean loaded = false, dataRejected = false;
        int retryCount = 0;
        switch (status) {
            case REJECTED:
            case SKIPPED:
                loaded = false;
                dataRejected = true;
                break;
            case FAILURE:
                loaded = false;
                dataRejected = false;
                retryCount = 1;
                break;
            case LOADING_INTO_CH:
            case PREPARING:
            case GOT_FROM_STEP:
                loaded = false;
                dataRejected = false;
                break;
            case SUCCESS:
                loaded = true;
                dataRejected = false;
        }

        return new StepEventsQueryResult(
                RELEASE_TASK_ID,
                PATH,
                5,
                DESTINATION,
                retryCount,
                "HIGH",
                new YtCluster("hahn"),
                100L,
                50L,
                status,
                now(),
                loaded,
                dataRejected
        );
    }
}
