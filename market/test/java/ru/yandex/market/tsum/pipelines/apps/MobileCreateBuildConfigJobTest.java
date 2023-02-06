package ru.yandex.market.tsum.pipelines.apps;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.apps.jobs.MobileCreateBuildConfigJob;
import ru.yandex.market.tsum.pipelines.apps.resources.MobileBuildTypeResource;
import ru.yandex.market.tsum.pipelines.apps.resources.MobilePlatformResource;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.TeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;

public class MobileCreateBuildConfigJobTest {

    private JobInstanceBuilder<MobileCreateBuildConfigJob> jobInstanceBuilder;

    @Before
    public void setUp() throws Exception {
        MobilePlatformResource platformResource = new MobilePlatformResource();
        platformResource.setPlatform(MobilePlatform.IOS);

        jobInstanceBuilder = JobInstanceBuilder.create(MobileCreateBuildConfigJob.class)
            .withResource(platformResource)
            .withResource(new StartrekTicket("BLUEMARKETAPPS-4242"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void expectIllegalArgumentException_IfUnsupportedQueueProvided() throws Exception {
        MobilePlatformResource platformResource = new MobilePlatformResource();
        platformResource.setPlatform(MobilePlatform.IOS);

        MobileCreateBuildConfigJob job = JobInstanceBuilder
            .create(MobileCreateBuildConfigJob.class)
            .withResource(platformResource)
            .withResource(new StartrekTicket("SOMEQUEUE-4242"))
            .create();

        TestJobContext context = new TestJobContext();
        job.execute(context);
    }

    @Test
    public void expectQaBuildTypeByDefault_IfBuildTypeIsNotPresent() throws Exception {
        MobileCreateBuildConfigJob job = jobInstanceBuilder.create();
        TestJobContext context = new TestJobContext();

        job.execute(context);

        Assert.assertEquals(BlueAppsPipelineUtils.TEAMCITY_IOS_QA_JOB_ID,
            context.getResource(TeamcityBuildConfig.class).getJobName());
    }


    @Test
    public void expectUITestJobTeamcityConfig_IfUITestBuildTypeProvided() throws Exception {
        MobileBuildTypeResource buildTypeResource = new MobileBuildTypeResource();
        buildTypeResource.setBuildType(MobileBuildType.UI_TESTS);

        MobileCreateBuildConfigJob job = jobInstanceBuilder
            .withResource(buildTypeResource)
            .create();

        TestJobContext context = new TestJobContext();

        job.execute(context);

        Assert.assertEquals(BlueAppsPipelineUtils.TEAMCITY_IOS_UI_TESTS_JOB_ID,
            context.getResource(TeamcityBuildConfig.class).getJobName());
    }

    @Test
    public void expectAndroidJobTeamcityConfig_IfAndroidPlatformProvided() throws Exception {
        MobileBuildTypeResource buildTypeResource = new MobileBuildTypeResource();
        buildTypeResource.setBuildType(MobileBuildType.UI_TESTS);

        MobilePlatformResource platformResource = new MobilePlatformResource();
        platformResource.setPlatform(MobilePlatform.ANDROID);

        MobileCreateBuildConfigJob job = JobInstanceBuilder
            .create(MobileCreateBuildConfigJob.class)
            .withResource(platformResource)
            .withResource(buildTypeResource)
            .withResource(new StartrekTicket("BLUEMARKETAPPS-4242"))
            .create();

        TestJobContext context = new TestJobContext();

        job.execute(context);

        Assert.assertEquals(BlueAppsPipelineUtils.TEAMCITY_ANDROID_UI_TESTS_JOB_ID,
            context.getResource(TeamcityBuildConfig.class).getJobName());
    }

}
