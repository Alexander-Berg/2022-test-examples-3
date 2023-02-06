package ru.yandex.market.tsum.pipelines.lcmp.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentSpecResource;
import ru.yandex.market.tsum.pipelines.lcmp.resources.InstallationSpecResource;
import ru.yandex.market.tsum.pipelines.lcmp.resources.NannyServiceSpec;
import ru.yandex.market.tsum.pipelines.lcmp.resources.PgaasSpec;
import ru.yandex.market.tsum.pipelines.lcmp.resources.YaDeployServiceSpec;
import ru.yandex.market.tsum.registry.proto.model.EnvironmentType;

public class TestComponentSpecGenerator {
    private TestComponentSpecGenerator() {

    }

    public static ComponentSpecResource generate(List<String>... installations) {
        ComponentSpecResource componentSpec = new ComponentSpecResource();
        List<InstallationSpecResource> installationSpecs = new ArrayList<>();
        for (int i = 0; i < installations.length; i++) {
            List<String> installation = installations[i];
            InstallationSpecResource installationSpec = new InstallationSpecResource(
                "inst_" + i, EnvironmentType.TESTING);
            List<NannyServiceSpec> nannyServiceSpecs = installation.stream()
                .map(s -> NannyServiceSpec.builder().withName(s).withGenCfgGroupName(s).build())
                .collect(Collectors.toList());
            List<YaDeployServiceSpec> yaDeployServiceSpecs = installation.stream()
                .map(s -> YaDeployServiceSpec.builder().withStageName(s).build())
                .collect(Collectors.toList());
            installationSpec.setNannyServiceSpecs(nannyServiceSpecs);
            installationSpec.setYaDeployServiceSpecs(yaDeployServiceSpecs);
            installationSpec.setPgaasSpec(PgaasSpec.builder().withClusterUuid("cluster_" + i)
                .withClusterName("cluster_" + i).build());
            installationSpecs.add(installationSpec);
        }
        componentSpec.setInstallations(installationSpecs);
        return componentSpec;
    }
}
