package ru.yandex.market.tsum.pipelines.wms.jobs.testing;

import java.util.UUID;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipelines.common.resources.ArcadiaRef;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsTestingBuildConfig;

@Produces(single = ArcadiaRef.class)
public class WmsPrepareMigrationBuildJob implements JobExecutor<JobContext> {

    @WiredResource(optional = true)
    private WmsTestingBuildConfig buildConfig;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("6358f640-0766-4c00-be61-372760538f2a");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        String branch = buildConfig != null ? buildConfig.getMigrationBranch() : "trunk";
        context.resources().produce(new ArcadiaRef("arcadia-arc:/#" + branch));
    }

}
