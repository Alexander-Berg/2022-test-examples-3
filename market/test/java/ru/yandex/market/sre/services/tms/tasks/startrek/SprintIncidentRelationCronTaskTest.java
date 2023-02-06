package ru.yandex.market.sre.services.tms.tasks.startrek;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.startrek.client.Sprints;
import ru.yandex.startrek.client.model.SearchRequest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SprintIncidentRelationCronTaskTest extends StartekTestPreset {

    @InjectMocks
    protected SprintIncidentRelationCronTask sprintIncidentRelationCronTask;

    ObjectMapper mapper;

    @Before
    public void setUp() {
        expectedException = ExpectedException.none();
        sprintIncidentRelationCronTask.startrekSession = mockStartrekSession;
        sprintIncidentRelationCronTask.incidentQueueName = TEST_INCIDENT_QUEUE_NAME;
        sprintIncidentRelationCronTask.boardId = 1L;
        mapper = createMapper();
    }

    @Test
    public void prepareSearchRequest() {
        SearchRequest request = sprintIncidentRelationCronTask.prepareSearchRequest();
        assertNotNull(request);
        assertTrue("Необходимо фильтровать по названию очереди",
                request.toString().contains(sprintIncidentRelationCronTask.incidentQueueName));
    }

    @Test
    public void beforeBatchProcess() {
        ListF<Sprints> sprints = Cf.list();
        when(mockStartrekSession.boards().getBoardSprints(anyLong())).thenReturn((List) sprints);
    }

    @Test
    public void openSprint() {
        when(mockStartrekSession.sprints().create(argThat(sprint ->
                        sprint.getEndDate().isAfter(Instant.now()) &&
                                sprint.getStartDate().isBefore(Instant.now())
                ))
        ).thenReturn(null);
        sprintIncidentRelationCronTask.openSprint();
        verify(mockStartrekSession.sprints(), times(1)).create(any());
    }

    @Test
    public void cronExpression() {
        assertNotNull(sprintIncidentRelationCronTask.cronExpression());
    }
}