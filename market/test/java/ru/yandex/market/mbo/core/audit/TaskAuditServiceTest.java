package ru.yandex.market.mbo.core.audit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.gwt.models.audit.AuditActionBuilder;
import ru.yandex.market.mbo.gwt.models.audit.AuditActionGroup;
import ru.yandex.market.mbo.gwt.models.audit.AuditFilter;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;

/**
 * @author danfertev
 * @since 24.03.2019
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TaskAuditServiceTest {
    private static final String TASK_ID_STRING = "333";
    private static final long TASK_ID_LONG = 333L;
    private static final int OFFSET = 0;
    private static final int LIMIT = 10;

    private TaskAuditService taskAuditService;
    private AuditService auditService;

    @Before
    public void setUp() {
        auditService = mock(AuditService.class);
        taskAuditService = new TaskAuditService();
        taskAuditService.setAuditService(auditService);
    }

    @Test
    public void testGetAuditCountReturnsNumberOfActionGroup() {
        Date now = new Date();
        Mockito.when(auditService.loadAudit(anyInt(), anyInt(), any(), anyBoolean())).thenReturn(
            Arrays.asList(
                AuditActionBuilder.newBuilder(1L, "name", now).setSourceId(TASK_ID_STRING).create(),
                AuditActionBuilder.newBuilder(1L, "name", now).setSourceId(TASK_ID_STRING).create()
            )
        );

        int auditCount = taskAuditService.getAuditCount(TASK_ID_LONG, new AuditFilter());
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    public void testGroupByEntityId() {
        Date now = new Date();
        Mockito.when(auditService.loadAudit(anyInt(), anyInt(), any(), anyBoolean())).thenReturn(
            Arrays.asList(
                AuditActionBuilder.newBuilder(1L, "name", now).setSourceId(TASK_ID_STRING).create(),
                AuditActionBuilder.newBuilder(1L, "name", now).setSourceId(TASK_ID_STRING).create(),
                AuditActionBuilder.newBuilder(2L, "name", now).setSourceId(TASK_ID_STRING).create()
            )
        );

        List<AuditActionGroup> auditGroups = taskAuditService.loadAudit(TASK_ID_LONG, OFFSET, LIMIT, new AuditFilter());
        assertThat(auditGroups.size()).isEqualTo(2);
        AuditActionGroup byEntityId1 = auditGroups.get(0);
        AuditActionGroup byEntityId2 = auditGroups.get(1);
        assertThat(byEntityId1.getActions()).hasSize(2);
        assertThat(byEntityId1.getEntityId()).isEqualTo(1L);
        assertThat(byEntityId2.getActions()).hasSize(1);
        assertThat(byEntityId2.getEntityId()).isEqualTo(2L);
    }

    @Test
    public void testGroupByDate() {
        Date date1 = new Date();
        Date date2 = new Date(0);
        Mockito.when(auditService.loadAudit(anyInt(), anyInt(), any(), anyBoolean())).thenReturn(
            Arrays.asList(
                AuditActionBuilder.newBuilder(1L, "name", date1).setSourceId(TASK_ID_STRING).create(),
                AuditActionBuilder.newBuilder(1L, "name", date1).setSourceId(TASK_ID_STRING).create(),
                AuditActionBuilder.newBuilder(1L, "name", date2).setSourceId(TASK_ID_STRING).create()
            )
        );

        List<AuditActionGroup> auditGroups = taskAuditService.loadAudit(TASK_ID_LONG, OFFSET, LIMIT, new AuditFilter());
        assertThat(auditGroups.size()).isEqualTo(2);
        AuditActionGroup byDate1 = auditGroups.get(0);
        AuditActionGroup byDate2 = auditGroups.get(1);
        assertThat(byDate1.getActions()).hasSize(2);
        assertThat(byDate1.getDate()).isEqualTo(date1);
        assertThat(byDate2.getActions()).hasSize(1);
        assertThat(byDate2.getDate()).isEqualTo(date2);
    }

    @Test
    public void testHardLimit() {
        Mockito.when(auditService.loadAudit(anyInt(), anyInt(), any(), anyBoolean())).thenReturn(
            IntStream.range(0, 100_100)
                .mapToObj(i -> AuditActionBuilder.newBuilder(1L, "name", new Date(i))
                    .setSourceId(TASK_ID_STRING).create())
                .collect(Collectors.toList())
        );

        int auditCount = taskAuditService.getAuditCount(TASK_ID_LONG, new AuditFilter());
        assertThat(auditCount).isEqualTo(100_000);
    }
}
