package ru.yandex.market.tsum.pipelines.common.jobs.tank;

import org.junit.Test;

import ru.yandex.market.tsum.pipelines.common.resources.SimpleTankApiGeneratorConfig;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.misc.test.Assert;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 25.07.2019
 */
public class SimpleTankApiJobConfigGeneratorJobTest {

    @Test
    public void testConfigGeneration() {
        SimpleTankApiJobConfigGeneratorJob generatorJob = new SimpleTankApiJobConfigGeneratorJob();

        SimpleTankApiGeneratorConfig tankApiGeneratorConfig = createTestConfig();
        String config = generatorJob.createConfiguration(tankApiGeneratorConfig, null);

        String expectedConfig = "phantom:\n" +
            "    address: graphite-web-1.graphite-web.stress.market-graphite.market-infra.stable.qloud-d.yandex.net\n" +
            "    ammofile: https://storage-int.mds.yandex.net/get-load-ammo/15349/965435a0b07e4c7982103b9a33f57319\n" +
            "    load_profile: {load_type: rps, schedule: 'const(10,10m)'}\n" +
            "    uris: []\n" +
            "uploader:\n" +
            "    enabled: true\n" +
            "    job_dsc: \"\"\n" +
            "    job_name: \"\"\n" +
            "    operator: robot-market-infra\n" +
            "    package: yandextank.plugins.DataUploader\n" +
            "    task: MARKETINFRA-4006\n" +
            "    ver: \"\"";

        Assert.equals(config, expectedConfig);
    }

    @Test
    public void testConfigGenerationWithTask() {
        SimpleTankApiJobConfigGeneratorJob generatorJob = new SimpleTankApiJobConfigGeneratorJob();

        SimpleTankApiGeneratorConfig tankApiGeneratorConfig = createTestConfig();
        StartrekTicket startrekTicket = new StartrekTicket("MARKETINFRA-4932");
        String config = generatorJob.createConfiguration(tankApiGeneratorConfig, startrekTicket);

        String expectedConfig = "phantom:\n" +
            "    address: graphite-web-1.graphite-web.stress.market-graphite.market-infra.stable.qloud-d.yandex.net\n" +
            "    ammofile: https://storage-int.mds.yandex.net/get-load-ammo/15349/965435a0b07e4c7982103b9a33f57319\n" +
            "    load_profile: {load_type: rps, schedule: 'const(10,10m)'}\n" +
            "    uris: []\n" +
            "uploader:\n" +
            "    enabled: true\n" +
            "    job_dsc: \"\"\n" +
            "    job_name: \"\"\n" +
            "    operator: robot-market-infra\n" +
            "    package: yandextank.plugins.DataUploader\n" +
            "    task: MARKETINFRA-4932\n" +
            "    ver: \"\"";

        Assert.equals(config, expectedConfig);
    }

    private SimpleTankApiGeneratorConfig createTestConfig() {
        SimpleTankApiGeneratorConfig generatorConfig = new SimpleTankApiGeneratorConfig();
        generatorConfig.setTarget("graphite-web-1.graphite-web.stress.market-graphite.market-infra.stable.qloud-d" +
            ".yandex.net");
        generatorConfig.setAmmofile("https://storage-int.mds.yandex" +
            ".net/get-load-ammo/15349/965435a0b07e4c7982103b9a33f57319");
        generatorConfig.setLoadPattern("const(10,10m)");
        generatorConfig.setOperator("robot-market-infra");
        generatorConfig.setTask("MARKETINFRA-4006");

        return generatorConfig;
    }
}
