package ru.yandex.market.tsum.pipelines.common.jobs.sandbox;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SandboxYaPackageJobTest {
    private static final String FIRST_REVISION = "r1";
    private static final String SECOND_REVISION = "r2";

    @Autowired
    private JobTester jobTester;

    private List<ChangelogEntry> changelogEntries;
    private List<ChangelogEntry> filteredChangelogEntries;

    @Before
    public void init() {
        changelogEntries = Arrays.asList(
            new ChangelogEntry(FIRST_REVISION, 0, "", "test"),
            new ChangelogEntry(SECOND_REVISION, 1, "", "test")
        );
        filteredChangelogEntries = Collections.singletonList(
            new ChangelogEntry(FIRST_REVISION, 0, "", "test")
        );
    }

    @Test
    public void getChangelogEntriesFilterByLaunchRulesWhenFilteredListIsEmptyTest() {
        getChangelogEntriesFilterByLaunchRulesWhenFilteredListIsEmpty(
            ConductorChangelogFiltrationType.FILTER_BY_LAUNCH_RULES);
    }

    @Test
    public void getChangelogEntriesFilterByLaunchRulesWhenFilteredListNotEmptyTest() {
        getChangelogEntriesFilterByLaunchRulesWhenFilteredListNotEmpty(
            ConductorChangelogFiltrationType.FILTER_BY_LAUNCH_RULES);
    }

    @Test
    public void getChangelogEntriesFilterByLaunchRulesWhenFilteredListIsEmptyForDefaultValueTest() {
        getChangelogEntriesFilterByLaunchRulesWhenFilteredListIsEmpty(ConductorChangelogFiltrationType.DEFAULT);
    }

    @Test
    public void getChangelogEntriesFilterByLaunchRulesWhenFilteredListNotEmptyForDefaultValueTest() {
        getChangelogEntriesFilterByLaunchRulesWhenFilteredListNotEmpty(ConductorChangelogFiltrationType.DEFAULT);
    }

    @Test
    public void getChangelogEntriesFilteredChangelogOrLaunchCommitTest() {
        ChangelogInfo changelogInfo = new ChangelogInfo(changelogEntries, Collections.emptyList());
        SandboxYaPackageJob job = createSandboxYaPackageJob(
            changelogInfo,
            null,
            ConductorChangelogFiltrationType.FILTERED_CHANGELOG_OR_LAUNCH_COMMIT
        );

        List<ChangelogEntry> gotChangelogEntries = job.getChangelogEntries(changelogInfo);
        Assert.assertEquals(1, gotChangelogEntries.size());
        Assert.assertEquals(SECOND_REVISION, gotChangelogEntries.iterator().next().getRevision());
    }

    @Test
    public void getChangelogEntriesFilteredChangelogOrLaunchCommitDeliveryTest() {
        ChangelogInfo changelogInfo = new ChangelogInfo(changelogEntries, Collections.emptyList());
        SandboxYaPackageJob job = createSandboxYaPackageJob(
            changelogInfo,
            createDeliveryPipelineParams(FIRST_REVISION),
            ConductorChangelogFiltrationType.FILTERED_CHANGELOG_OR_LAUNCH_COMMIT
        );

        List<ChangelogEntry> gotChangelogEntries = job.getChangelogEntries(changelogInfo);
        Assert.assertEquals(1, gotChangelogEntries.size());
        Assert.assertEquals(FIRST_REVISION, gotChangelogEntries.iterator().next().getRevision());
    }

    private void getChangelogEntriesFilterByLaunchRulesWhenFilteredListIsEmpty(
        ConductorChangelogFiltrationType conductorChangelogFiltrationType
    ) {
        ChangelogInfo changelogInfo = new ChangelogInfo(changelogEntries, Collections.emptyList());
        SandboxYaPackageJob job = createSandboxYaPackageJob(
            changelogInfo,
            null,
            conductorChangelogFiltrationType
        );
        Assert.assertEquals(Collections.emptyList(), job.getChangelogEntries(changelogInfo));
    }

    private void getChangelogEntriesFilterByLaunchRulesWhenFilteredListNotEmpty(
        ConductorChangelogFiltrationType conductorChangelogFiltrationType
    ) {
        ChangelogInfo changelogInfo = new ChangelogInfo(changelogEntries, filteredChangelogEntries);
        SandboxYaPackageJob job = createSandboxYaPackageJob(
            changelogInfo,
            null,
            conductorChangelogFiltrationType
        );
        Assert.assertEquals(filteredChangelogEntries, job.getChangelogEntries(changelogInfo));
    }

    private DeliveryPipelineParams createDeliveryPipelineParams(String revision) {
        DeliveryPipelineParams deliveryPipelineParams = Mockito.mock(DeliveryPipelineParams.class);
        Mockito.when(deliveryPipelineParams.getRevision()).thenReturn(revision);
        return deliveryPipelineParams;
    }

    private SandboxYaPackageJob createSandboxYaPackageJob(
        ChangelogInfo changelogInfo,
        DeliveryPipelineParams deliveryPipelineParams,
        ConductorChangelogFiltrationType filtrationType
    ) {
        JobInstanceBuilder<SandboxYaPackageJob> jobBuilder = jobTester.jobInstanceBuilder(SandboxYaPackageJob.class)
            .withBean(Mockito.mock(SandboxClient.class))
            .withResources(Mockito.mock(SandboxTaskJobConfig.class))
            .withResource(changelogInfo)
            .withResource(
                SandboxYaPackageJobConfig.builder("pkg")
                    .setChangelogFiltrationType(filtrationType)
                    .build()
            );
        if (deliveryPipelineParams != null) {
            jobBuilder.withResource(deliveryPipelineParams);
        }
        return jobBuilder.create();
    }
}
