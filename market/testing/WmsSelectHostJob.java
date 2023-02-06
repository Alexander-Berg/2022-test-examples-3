package ru.yandex.market.tsum.pipelines.wms.jobs.testing;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsum.clients.bitbucket.BitbucketClient;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketCommit;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipe.engine.definition.job.ExecutorInfo;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResourceList;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsDeliveryParams;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsHostConfig;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsMigrationConfig;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsProjectConfig;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsServiceBranchesParam;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsTestingBuildConfig;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsTestingHostsParam;

@ExecutorInfo(
        title = "Wms testing hosts deploy start job",
        description = "Джоба для выбора стенда"
)
@Produces(single = {WmsDeliveryParams.class}, multiple = {WmsMigrationConfig.class})
public class WmsSelectHostJob implements JobExecutor<JobContext> {

    private static final UUID ID = UUID.fromString("c6c276e5-4fb2-42b8-a469-d24079e070bf");

    @Autowired
    private BitbucketClient bbClient;

    @WiredResource(optional = true)
    WmsTestingHostsParam wmsTestingParams;

    @WiredResource
    WmsProjectConfig projectConfig;

    @WiredResourceList(value = WmsHostConfig.class, optional = true)
    List<WmsHostConfig> wmsHostConfigList;

    @WiredResource(optional = true)
    WmsServiceBranchesParam serviceBranchesParam;

    @WiredResource(optional = true)
    WmsTestingBuildConfig buildConfig;

    @Override
    public UUID getSourceCodeId() {
        return ID;
    }

    @Override
    public void execute(JobContext context) throws Exception {
        Optional<WmsHostConfig> hostConfigOptional = getWmsHostConfig();
        if (hostConfigOptional.isPresent()) {
            WmsHostConfig hostConfig = hostConfigOptional.get();

            context.resources().produce(new WmsMigrationConfig(hostConfig.getSecret(),
                    hostConfig.getDatabaseHost(),
                    hostConfig.getPort())
            );
            context.progress()
                    .updateText(String.format("Database: %s:%s",
                            hostConfig.getDatabaseHost(),
                            hostConfig.getDatabasePort()));
        }
        context.resources().produce(WmsDeliveryParams.builder()
                .withProject(projectConfig.getProject())
                .withRepositoryName(projectConfig.getRepositoryName())
                .withBranch(wmsTestingParams != null ? wmsTestingParams.getBranch() : buildConfig.getInforBranch())
                .withRevision(getBBRevision(context))
                .build()
        );
    }

    private Optional<WmsHostConfig> getWmsHostConfig() {
        if (wmsHostConfigList == null || wmsHostConfigList.isEmpty()) {
            return Optional.empty();
        }
        if (wmsTestingParams != null) {
            return wmsHostConfigList.stream()
                    .filter(x -> x.getHostname().equals(wmsTestingParams.getHostname()))
                    .findFirst();
        }
        return Optional.empty();
    }

    private String getBBRevision(JobContext context) {
        Iterator<BitbucketCommit> commitIterator = bbClient.getCommitIterator(projectConfig.getProject(),
                projectConfig.getRepositoryName(),
                getBranch());
        if (!commitIterator.hasNext()) {
            context.actions().failJob("There is no commits in branch", SupportType.NONE);
        }
        return commitIterator.next().getId();
    }

    private String getBranch() {
        if (wmsTestingParams != null) {
            return wmsTestingParams.getRevision();
        } else if (buildConfig != null) {
            return buildConfig.getInforBranch();
        }
        return "master";
    }
}
