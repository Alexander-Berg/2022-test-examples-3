package ru.yandex.market.tsum.pipelines.ott.jobs.testing;

import java.util.UUID;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.ExecutorInfo;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipelines.common.jobs.platform.DeployJobResource;
import ru.yandex.market.tsum.pipelines.ott.config.OttEnvironmentConfig;
import ru.yandex.market.tsum.pipelines.ott.resources.TeamCityBuildInfo;

@ExecutorInfo(
    title = "OTT Docker trunk config",
    description = "Собирает конфиги для деплоя в qloud по билду trunk в TeamCity"
)
@Produces(single = DeployJobResource.class)
public class OttDockerConfigTrunkJob implements JobExecutor {
    @WiredResource
    private TeamCityBuildInfo teamCityBuildInfo;
    @WiredResource
    private OttEnvironmentConfig environmentConfig;

    @Override
    public void execute(JobContext context) throws Exception {
        DeployJobResource deployJobResource = new DeployJobResource(environmentConfig.getQloudApplication());
        environmentConfig.getComponents()
            .forEach(component -> {
                    deployJobResource.withComponentTag(
                        component,
                        DeployJobResource.tagOf(environmentConfig.getDockerRepo(), teamCityBuildInfo.getBuildNumber())
                    );
                }
            );
        context.resources().produce(deployJobResource);
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("2effc6cf-8028-479d-a10d-36e497382e23");
    }
}
