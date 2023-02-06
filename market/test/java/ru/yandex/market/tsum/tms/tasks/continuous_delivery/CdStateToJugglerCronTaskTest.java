package ru.yandex.market.tsum.tms.tasks.continuous_delivery;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.tsum.clients.juggler.JugglerPushEvent;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.LaunchState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRefImpl;
import ru.yandex.market.tsum.release.dao.ReleaseDao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 05.10.18
 */

public class CdStateToJugglerCronTaskTest {
    private static final String HOST = "tsum-test-dms";

    @Test
    public void generatesEvents() {
        ReleaseDao releaseDao = mock(ReleaseDao.class);
        PipeLaunchDao pipeLaunchDao = mock(PipeLaunchDao.class);
        ProjectsDao projectsDao = mock(ProjectsDao.class);

        DeliveryMachineEntity dm = new DeliveryMachineEntity("", "cd-id-1", null);
        DeliveryMachineEntity dm2 = new DeliveryMachineEntity("", "cd-id-2", null);
        DeliveryMachineEntity dm3 = new DeliveryMachineEntity("", "cd-id-3", null);
        DeliveryMachineEntity dm4 = new DeliveryMachineEntity("", "cd-id-4", null);

        PipeLaunch launch2 = PipeLaunch.builder()
            .withStages(Collections.emptyList())
            .withStagesGroupId("cd-id-2")
            .withLaunchRef(PipeLaunchRefImpl.create("cd-id-2"))
            .withProjectId("prj")
            .withTriggeredBy("me").build();

        PipeLaunch launch3 = PipeLaunch.builder()
            .withStages(Collections.emptyList())
            .withStagesGroupId("cd-id-3")
            .withLaunchRef(PipeLaunchRefImpl.create("cd-id-3"))
            .withLaunchState(LaunchState.FAILURE)
            .withProjectId("prj")
            .withTriggeredBy("me").build();

        PipeLaunch launch4 = PipeLaunch.builder()
            .withStages(Collections.emptyList())
            .withStagesGroupId("cd-id-4")
            .withLaunchRef(PipeLaunchRefImpl.create("cd-id-4"))
            .withLaunchState(LaunchState.WAITING_FOR_MANUAL_TRIGGER)
            .withProjectId("prj")
            .withTriggeredBy("me").build();

        when(projectsDao.list(false))
            .thenReturn(Arrays.asList(
                new ProjectEntity("prj", "", Arrays.asList(dm, dm2, dm3, dm4)),
                new ProjectEntity("prj-withoud-cd", "")
                )
            );

        when(releaseDao.getRunningReleases()).thenReturn(Collections.emptyList());

        when(pipeLaunchDao.getByIds(Mockito.anyList())).thenReturn(Arrays.asList(launch2, launch3, launch4));

        CdStateToJugglerCronTask task = new CdStateToJugglerCronTask(
            null, releaseDao, pipeLaunchDao, projectsDao, HOST, "https://local/"
        );

        List<JugglerPushEvent> events = task.getEvents();

        Assert.assertEquals(4, events.size());

        Assert.assertEquals(JugglerPushEvent.Status.OK, events.get(0).getStatus());
        Assert.assertEquals(JugglerPushEvent.Status.OK, events.get(1).getStatus());
        Assert.assertEquals(JugglerPushEvent.Status.CRIT, events.get(2).getStatus());
        Assert.assertEquals(JugglerPushEvent.Status.WARN, events.get(3).getStatus());

        Assert.assertEquals(HOST + "-prj", events.get(0).getHost());
        Assert.assertEquals("cd-id-1", events.get(0).getService());
    }
}
