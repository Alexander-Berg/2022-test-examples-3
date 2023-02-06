package ru.yandex.market.tsum.test_data;

import java.util.Collections;

import org.springframework.data.mongodb.core.convert.MongoConverter;

import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.release.dao.DeliveryMachineSettings;
import ru.yandex.market.tsum.release.dao.title_providers.OrdinalTitleProvider;

import static org.mockito.Mockito.mock;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.02.18
 */
public class TestProjectFactory {
    private TestProjectFactory() {
    }

    public static ProjectEntity project(String projectId, String stageGroupId, String pipeId) {
        return createProject(
            projectId,
            machineSettingsBuilder(stageGroupId, pipeId)
                .withGithubSettings("market-infra/test-pipeline")
                .build()
        );
    }

    public static ProjectEntity arcadiaProject(String projectId, String stageGroupId, String pipeId) {
        return createProject(
            projectId,
            machineSettingsBuilder(stageGroupId, pipeId)
                .withArcadiaSettings()
                .build()
        );
    }

    private static ProjectEntity createProject(String projectId, DeliveryMachineSettings deliveryMachineSettings) {
        return new ProjectEntity(
            projectId, "test title",
            Collections.singletonList(new DeliveryMachineEntity(deliveryMachineSettings, mock(MongoConverter.class)))
        );
    }

    private static DeliveryMachineSettings.Builder machineSettingsBuilder(String stageGroupId, String pipeId) {
        return DeliveryMachineSettings.builder()
            .withStageGroupId(stageGroupId)
            .withPipeline(pipeId, "test-pipe", mock(OrdinalTitleProvider.class));
    }
}
