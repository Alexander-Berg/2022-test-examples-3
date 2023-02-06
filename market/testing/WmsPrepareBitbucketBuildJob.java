package ru.yandex.market.tsum.pipelines.wms.jobs.testing;

import java.util.UUID;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.ExecutorInfo;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipelines.common.resources.BranchRef;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsServiceBranchesParam;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsServiceParam;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsTestingBuildConfig;

@ExecutorInfo(
    title = "Get brunch name from common settings",
    description = "Джоба для выбора ветки деплоя"
)
@Produces(single = BranchRef.class)
public class WmsPrepareBitbucketBuildJob implements JobExecutor<JobContext> {

    private static final String DEFAULT_GIT_BRANCH = "master";

    @WiredResource(optional = true)
    private WmsServiceBranchesParam branchesParam;

    @WiredResource
    private WmsServiceParam serviceParam;

    @WiredResource(optional = true)
    private WmsTestingBuildConfig buildConfig;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("ca6a4590-2598-4e11-9faa-fd8402e059c7");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new BranchRef(getBranch()));
    }

    private String getBranch() {
        if (branchesParam != null) {
            return branchesParam.getBranches().getOrDefault(serviceParam.getServiceName(), DEFAULT_GIT_BRANCH);
        } else if (buildConfig != null) {
            return buildConfig.getUiBranch();
        } else {
            return DEFAULT_GIT_BRANCH;
        }
    }
}
