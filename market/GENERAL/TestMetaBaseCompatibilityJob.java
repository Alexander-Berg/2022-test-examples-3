package ru.yandex.market.tsum.pipelines.report.jobs;

import java.util.List;
import java.util.UUID;

import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.clients.sandbox.TaskInputDto;
import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJob;
import ru.yandex.market.tsum.pipelines.report.resources.ReportBuildInfo;
import ru.yandex.market.tsum.pipelines.report.resources.TestMetaBaseCompatibilityJobConfig;

public class TestMetaBaseCompatibilityJob extends SandboxTaskJob {
    public static final String TASK_NAME = "MARKET_REPORT_META_BASE_COMPATIBILITY";

    @WiredResource
    private ReportBuildInfo reportBuildInfo;

    @WiredResource
    private TestMetaBaseCompatibilityJobConfig config;

    @Override
    protected void prepareTask(TsumJobContext context, TaskInputDto taskNew) throws Exception {
        super.prepareTask(context, taskNew);
        taskNew.addCustomField(
            "svn_arcadia_url",
            String.format("%s@%s", reportBuildInfo.getBranchPath(), reportBuildInfo.getBranchRevision()));
        taskNew.addCustomFieldIfNotNull("arc_yav_token", config.getArcYavSecret());
        taskNew.addCustomFieldIfNotNull("arc_yav_token_key", config.getArcYavSecretKey());
        if (config.getTestBaseSearch()) {
            taskNew.addCustomField("test_base_search", true);
        }
    }

    @Override
    protected void processResult(TsumJobContext context, SandboxTask task, List<TaskResource> resources)
        throws Exception {
        // The job doesn't produce any resources.
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("9521866a-42be-4962-90b0-4cdc594b9652");
    }
}
