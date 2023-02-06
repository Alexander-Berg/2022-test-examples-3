package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.clients.gencfg.GenCfgCType;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCard;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCardInstanceReqs;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCardLegacy;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCardRequirements;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCardVolumeReqs;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupInfo;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstance;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstanceHbf;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstanceHbfInterface;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstanceHbfInterfaces;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstanceHostResources;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstancePortoLimits;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstanceStorage;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstanceStorages;
import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.clients.gencfg.GenCfgVolume;
import ru.yandex.market.tsum.multitesting.GenCfgGroupSpec;
import ru.yandex.market.tsum.pipelines.sre.resources.CloneNannyServiceConfig;

public class CreateRtcServiceSpecFromServiceJobTest {
    private final CreateRtcServiceSpecFromServiceJob job = new CreateRtcServiceSpecFromServiceJob();

    private GenCfgGroupCard groupCard;

    private GenCfgGroupInfo groupList;

    @Before
    public void setConfig() {
        CloneNannyServiceConfig config = new CloneNannyServiceConfig();
        config.setNannyServiceName("testing_market_super_service_vla");
        config.setNewDc("SAS");
        job.config = config;
    }

    @Before
    public void setGenCfgGroupCard() {
        groupCard = new GenCfgGroupCard();

        GenCfgGroupCardRequirements reqs = new GenCfgGroupCardRequirements();
        GenCfgGroupCardInstanceReqs instances = new GenCfgGroupCardInstanceReqs();
        instances.setMemoryGuarantee(4294967296L);
        reqs.setInstances(instances);
        List<GenCfgGroupCardVolumeReqs> volumes = new ArrayList<>();
        GenCfgGroupCardVolumeReqs volumeLogs = new GenCfgGroupCardVolumeReqs();
        volumeLogs.setGuestMp("/logs");
        volumeLogs.setQuota(32212254720D);
        volumes.add(volumeLogs);
        GenCfgGroupCardVolumeReqs volumeCores = new GenCfgGroupCardVolumeReqs();
        volumeCores.setGuestMp("/cores");
        volumeCores.setQuota(8589934592D);
        volumes.add(volumeCores);
        reqs.setVolumes(volumes);
        groupCard.setReqs(reqs);

        GenCfgGroupCardLegacy legacy = new GenCfgGroupCardLegacy();
        Map<String, String> funcs = new HashMap<>();
        funcs.put("instancePower", "exactly80");
        legacy.setAllocationFunctions(funcs);
        groupCard.setLegacy(legacy);

        Map<String, Object> tags = new HashMap<>();
        tags.put("ctype", "testing");
        groupCard.setTags(tags);
    }

    @Before
    public void setGenCfgGroupInfo() {
        List<GenCfgInstance> instances = new ArrayList<>();
        GenCfgInstance instance = new GenCfgInstance(
            "",
            "",
            GenCfgLocation.SAS,
            65536,
            800,
            new GenCfgInstanceHbf(
                new GenCfgInstanceHbfInterfaces(
                    new GenCfgInstanceHbfInterface(
                        "hostname",
                        "ipv6addr"
                    )
                )
            ),
            new GenCfgInstancePortoLimits(),
            new GenCfgInstanceStorages(new GenCfgInstanceStorage(100)),
            new ArrayList<String>(),
            new GenCfgInstanceHostResources(4, 16, 512, 0, "noname"),
            new ArrayList<>()
        );
        instances.add(instance);
        groupList = new GenCfgGroupInfo(
            "VLA_MARKET_TEST_SUPER_SERVICE",
            "release",
            "master",
            new ArrayList<>(),
            instances
        );
    }

    @Test
    public void testCreateGroupSpec() {
        List<GenCfgVolume> volumes = new ArrayList<>();
        volumes.add(new GenCfgVolume(0, ""));
        GenCfgGroupSpec expected = GenCfgGroupSpec.newBuilder()
            .withCpuCount(2)
            .withMemoryGb(4)
            .withInstances(1)
            .withDiskGb(0)
            .withCType(GenCfgCType.TESTING)
            .withLocation(GenCfgLocation.SAS)
            .withVolumes(volumes)
            .build();

        GenCfgGroupSpec result = job.createGroupSpec(groupCard, groupList);
        Assert.assertEquals(expected, result);
    }
}
