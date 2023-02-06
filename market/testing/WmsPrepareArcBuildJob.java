package ru.yandex.market.tsum.pipelines.wms.jobs.testing;

import java.util.UUID;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipelines.common.resources.ArcadiaRef;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsServiceBranchesParam;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsServiceParam;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsTestingBuildConfig;

@Produces(single = ArcadiaRef.class)
public class WmsPrepareArcBuildJob implements JobExecutor<JobContext> {

    private static final String DEFAULT_ARC_BRANCH = "trunk";

    @WiredResource(optional = true)
    private WmsServiceBranchesParam branchesParam;

    @WiredResource
    private WmsServiceParam serviceParam;

    @WiredResource(optional = true)
    private WmsTestingBuildConfig buildConfig;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("dc81b450-c877-4f5e-b4c6-7bf9dfc14203");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new ArcadiaRef("arcadia-arc:/#" + getBranch()));
    }

    private String getBranch() {
        if (branchesParam != null && branchesParam.getBranches().containsKey(serviceParam.getServiceName())) {
            return branchesParam.getBranches().get(serviceParam.getServiceName());
        } else if (buildConfig != null && buildConfig.getServiceBranchesParam()
                .getBranches()
                .containsKey(serviceParam.getServiceName())) {
            return buildConfig.getServiceBranchesParam().getBranches().get(serviceParam.getServiceName());
        } else if (buildConfig != null) {
            return buildConfig.getArcadiaBranch();
        } else {
            return DEFAULT_ARC_BRANCH;
        }
    }
}
