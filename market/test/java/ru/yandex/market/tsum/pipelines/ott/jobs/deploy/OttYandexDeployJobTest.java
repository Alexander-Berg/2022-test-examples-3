package ru.yandex.market.tsum.pipelines.ott.jobs.deploy;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.tsum.pipelines.common.jobs.deploy.YandexDeployDockerTagResource;
import ru.yandex.market.tsum.pipelines.ott.config.OttServiceDeployConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class OttYandexDeployJobTest {
    private OttYandexDeployJob job = new OttYandexDeployJob();

    @Test
    public void apiDowntimeTags() {
        setupJobConfig("ott-api", List.of("backend", "backend-canary", "backend-playback-permissions", "station"));
        List<String> tags = job.buildDowntimeTags();
        assertThat(tags).containsExactlyInAnyOrder(
            "api_release_downtime",
            "api_production_release_downtime",
            "api_production_canary_release_downtime",
            "api_production_playback_permissions_release_downtime",
            "api_production_station_release_downtime"
        );
    }

    @Test
    public void ooDowntimeTags() {
        setupJobConfig("ott-oo", List.of("backend", "ya-main", "ya-module", "ya-module-retail"));
        List<String> tags = job.buildDowntimeTags();
        assertThat(tags).containsExactlyInAnyOrder(
            "ott_oo_release_downtime",
            "ott_oo_production_release_downtime",
            "ott_oo_production_ya_main_release_downtime",
            "ott_oo_production_ya_module_release_downtime",
            "ott_oo_production_ya_module_retail_release_downtime"
        );
    }

    @Test
    public void extraDowntimeTags() {
        setupJobConfig(
            "ott-dataset-content-features-generator",
            List.of("backend"),
            List.of("ott_dataset_c_features_generator_production_release_downtime")
        );
        List<String> tags = job.buildDowntimeTags();
        assertThat(tags).containsExactlyInAnyOrder(
            "ott_dataset_content_features_generator_release_downtime",
            "ott_dataset_content_features_generator_production_release_downtime",
            "ott_dataset_c_features_generator_production_release_downtime"
        );
    }

    private void setupJobConfig(String serviceName, List<String> unitIds) {
        setupJobConfig(serviceName, unitIds, List.of());
    }

    private void setupJobConfig(String serviceName, List<String> unitIds, List<String> extraTags) {
        OttServiceDeployConfig serviceDeployConfig = new OttServiceDeployConfig(
            true,
            serviceName,
            "production",
            0,
            extraTags
        );
        ReflectionTestUtils.setField(job, "serviceDeployConfig", serviceDeployConfig);

        List<YandexDeployDockerTagResource> dockerTagResources = unitIds.stream()
            .map(this::dockerTagResource)
            .collect(Collectors.toList());
        ReflectionTestUtils.setField(job, "dockerTagResources", dockerTagResources);
    }

    private YandexDeployDockerTagResource dockerTagResource(String unitId) {
        return new YandexDeployDockerTagResource(
            unitId,
            unitId,
            unitId,
            unitId
        );
    }
}
