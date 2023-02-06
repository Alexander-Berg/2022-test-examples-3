package ru.yandex.market.tsum;

import java.util.Arrays;
import java.util.Date;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.tsum.multitesting.MultitestingDao;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironmentLaunch;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 13.04.18
 */
public class LaunchUrlProviderImplTest {
    private final ObjectId releasePipeLaunchId = new ObjectId(new Date());
    private final ObjectId deliveryPipeLaunchId = new ObjectId(new Date());
    private final ObjectId mtPipeLaunchId = new ObjectId(new Date());
    private final ObjectId otherPipeLaunchId = new ObjectId(new Date());

    private final String deliveryMachineId = "deliveryMachine";
    private final String projectId = "project";

    private final String notDeliveryReleaseId = "notDeliveryRelease";
    private final String deliveryReleaseId = "deliveryRelease";

    private final String mtEnvironmentName = "environment";

    private final String url = "http://localhost:3000";
    private final String jobId = "jobId";
    private final int jobLaunchNumber = 42;

    @Mock
    private ReleaseDao releaseDao;
    @Mock
    private MultitestingDao multitestingDao;

    private LaunchUrlProviderImpl urlProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        urlProvider = new LaunchUrlProviderImpl(releaseDao, multitestingDao, url);
    }

    @Test
    public void getPipeLaunchUrlOfReleasePipe() {
        Release notDeliveryRelease = mock(Release.class);
        Mockito.when(notDeliveryRelease.getDeliveryMachineId()).thenReturn(null);
        Mockito.when(notDeliveryRelease.getProjectId()).thenReturn(projectId);
        Mockito.when(notDeliveryRelease.getId()).thenReturn(notDeliveryReleaseId);

        Mockito.when(notDeliveryRelease.getUrl(any()))
            .thenReturn("http://localhost:3000/pipe/projects/project/release/notDeliveryRelease");

        Mockito.when(releaseDao.getReleaseByPipeLaunchId(releasePipeLaunchId)).thenReturn(notDeliveryRelease);

        Assert.assertEquals(
            url + "/pipe/projects/" + projectId + "/release/" + notDeliveryReleaseId,
            urlProvider.getPipeLaunchUrl(releasePipeLaunchId)
        );

        Assert.assertEquals(
            url + "/pipe/projects/" + projectId + "/release/" + notDeliveryReleaseId + getJobParams(),
            urlProvider.getJobLaunchDetailsUrl(releasePipeLaunchId, jobId, jobLaunchNumber)
        );
    }

    @Test
    public void getPipeLaunchUrlOfDeliveryPipe() {
        Release deliveryRelease = mock(Release.class);
        Mockito.when(deliveryRelease.getDeliveryMachineId()).thenReturn(deliveryMachineId);
        Mockito.when(deliveryRelease.getProjectId()).thenReturn(projectId);
        Mockito.when(deliveryRelease.getId()).thenReturn(deliveryReleaseId);

        Mockito.when(deliveryRelease.getUrl(any()))
            .thenReturn("http://localhost:3000/pipe/projects/project/delivery-dashboard/deliveryMachine/release" +
                "/deliveryRelease");

        Mockito.when(releaseDao.getReleaseByPipeLaunchId(deliveryPipeLaunchId)).thenReturn(deliveryRelease);

        Assert.assertEquals(
            url + "/pipe/projects/" + projectId + "/delivery-dashboard/" +
                deliveryMachineId + "/release/" + deliveryReleaseId,
            urlProvider.getPipeLaunchUrl(deliveryPipeLaunchId)
        );

        Assert.assertEquals(
            url + "/pipe/projects/" + projectId + "/delivery-dashboard/" +
                deliveryMachineId + "/release/" + deliveryReleaseId + getJobParams(),
            urlProvider.getJobLaunchDetailsUrl(deliveryPipeLaunchId, jobId, jobLaunchNumber)
        );
    }

    @Test
    public void getPipeLaunchUrlOfMtPipe() {
        MultitestingEnvironmentLaunch mtLaunch = mock(MultitestingEnvironmentLaunch.class);
        Mockito.when(mtLaunch.getPipeLaunchId()).thenReturn(mtPipeLaunchId.toString());
        MultitestingEnvironmentLaunch anotherMtLaunch = mock(MultitestingEnvironmentLaunch.class);
        Mockito.when(anotherMtLaunch.getPipeLaunchId()).thenReturn("anotherMtLaunch");

        MultitestingEnvironment environment = mock(MultitestingEnvironment.class);
        Mockito.when(environment.getProject()).thenReturn(projectId);
        Mockito.when(environment.getName()).thenReturn(mtEnvironmentName);
        Mockito.when(environment.getLaunches()).thenReturn(Arrays.asList(anotherMtLaunch, mtLaunch));

        Mockito.when(multitestingDao.getEnvironmentByPipeLaunchId(mtPipeLaunchId.toString())).thenReturn(environment);

        Mockito.when(releaseDao.getReleaseByPipeLaunchId(mtPipeLaunchId)).thenReturn(null);

        Assert.assertEquals(
            url + "/pipe/projects/" + projectId + "/multitestings/environments/" + mtEnvironmentName + "/launch/" + 2,
            urlProvider.getPipeLaunchUrl(mtPipeLaunchId)
        );

        Assert.assertEquals(
            url + "/pipe/projects/" + projectId + "/multitestings/environments/" +
                mtEnvironmentName + "/launch/" + 2 + getJobParams(),
            urlProvider.getJobLaunchDetailsUrl(mtPipeLaunchId, jobId, jobLaunchNumber)
        );
    }

    private String getJobParams() {
        return "?selectedJob=" + jobId + "&launchNumber=" + jobLaunchNumber;
    }

    @Test
    public void getLaunchUrlOfOtherPipe() {
        Mockito.when(releaseDao.getReleaseByPipeLaunchId(otherPipeLaunchId)).thenReturn(null);
        Mockito.when(multitestingDao.getEnvironmentByPipeLaunchId(otherPipeLaunchId.toString())).thenReturn(null);

        Assert.assertEquals(
            url + "/pipe/launch/" + otherPipeLaunchId,
            urlProvider.getPipeLaunchUrl(otherPipeLaunchId)
        );

        Assert.assertEquals(
            url + "/pipe/launch/" + otherPipeLaunchId + getJobParams(),
            urlProvider.getJobLaunchDetailsUrl(otherPipeLaunchId, jobId, jobLaunchNumber)
        );
    }
}
