package ru.yandex.market.tsum.pipelines.report;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.report.jobs.CreateReportBuildInfoJob;
import ru.yandex.market.tsum.pipelines.report.resources.ReportBuildInfo;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 03.05.18
 */
public class ReportPipelineTest {
    public static class CreateReportBuildInfoJobTest {

        private JobInstanceBuilder<CreateReportBuildInfoJob> jobInstanceBuilder;

        @Before
        public void setUp() throws Exception {
            jobInstanceBuilder = JobInstanceBuilder.create(CreateReportBuildInfoJob.class)
                .withResource(new ReleaseInfo(new FixVersion(1L, "2018.1.1"), "MARKETOUT-42"));
        }

        @Test
        public void testRegularRelease() throws Exception {
            CreateReportBuildInfoJob job = jobInstanceBuilder
                .withResource(
                    new DeliveryPipelineParams(
                        "100500", "100400", "100400"
                    )
                )
                .create();

            TestJobContext context = new TestJobContext();
            job.execute(context);

            Assert.assertEquals(
                new ReportBuildInfo("arcadia:/arc/trunk/arcadia", "100500", "2018.1.1.0"),
                context.getResource(ReportBuildInfo.class)
            );
        }

        @Test
        public void testHotfix() throws Exception {
            CreateReportBuildInfoJob job = jobInstanceBuilder
                .withResource(
                    new DeliveryPipelineParams(
                        "100500",
                        "100400",
                        "100400",
                        "/branches/junk/market/infra/hotfix.1524844800320.7c63"
                    )
                )
                .create();

            TestJobContext context = new TestJobContext();
            job.execute(context);

            Assert.assertEquals(
                new ReportBuildInfo(
                    "arcadia:/arc/branches/junk/market/infra/hotfix.1524844800320.7c63/arcadia",
                    "HEAD",
                    "2018.1.1.0"
                ),
                context.getResource(ReportBuildInfo.class)
            );
        }
    }

}
