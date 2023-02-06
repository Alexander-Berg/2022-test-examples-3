package ru.yandex.market.tsum.tms.tasks.gencfg_usage;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.tsum.clients.gencfg.GenCfgClient;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCard;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCardAudit;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCardCpu;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCardInstanceReqs;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCardLegacy;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCardRequirements;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupInfo;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstance;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstanceHostResources;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class GencfgUsageCountingManagerTest {
    private static final long GB = 1024L*1024L*1024L;

    @Mock
    private GenCfgClient genCfgClient;

    @Test
    public void testCount() {
        String genCfgTag = "stable-130-r1945";
        List<TestMasterGroup> masterGroups = Arrays.asList(
            TestMasterGroup.builder()
                .withName("IVA_MARKET_PREP_REPORT_GENERAL")
                .withInstances(Arrays.asList(
                    TestInstance.builder()
                        .withHostname("somehost1.market.yandex.net")
                        .withCpu(10)
                        .withMem(128)
                        .withHdd(400)
                        .withSsd(200)
                        .withPower(300)
                        .build(),
                    TestInstance.builder()
                        .withHostname("somehost2.market.yandex.net")
                        .withCpu(10)
                        .withModel("E5-2660")
                        .withMem(128)
                        .withHdd(100)
                        .withSsd(200)
                        .withPower(200)
                        .build()
                ))
                .withSlaves(Arrays.asList(
                    TestSlaveGroup.builder()
                        .withName("IVA_MARKET_PREP_REPORT_GENERAL_INT")
                        .withPower(80L)
                        .withMem(15 * GB)
                        .withHddBytes(20.0 * (double) GB)
                        .withSsdBytes(10.0 * (double) GB)
                        .withInstancePerHost(2)
                        .withHosts(Arrays.asList("somehost1.market.yandex.net", "somehost2.market.yandex.net"))
                        .build(),
                    TestSlaveGroup.builder()
                        .withName("IVA_MARKET_PREP_REPORT_GENERAL_API")
                        .withPower(120L)
                        .withMem(30 * GB)
                        .withHddBytes(40.0 * (double) GB)
                        .withSsdBytes(50.0 * (double) GB)
                        .withInstancePerHost(1)
                        .withHosts(Collections.singletonList("somehost1.market.yandex.net"))
                        .build()
                ))
                .build(),
            TestMasterGroup.builder()
                .withName("IVA_MARKET_PROD_GENERAL")
                .withInstances(Arrays.asList(
                    TestInstance.builder()
                        .withHostname("somehost3.market.yandex.net")
                        .withCpu(10)
                        .withMem(128)
                        .withHdd(100)
                        .withSsd(200)
                        .withPower(123)
                        .build(),
                    TestInstance.builder()
                        .withHostname("somehost4.market.yandex.net")
                        .withCpu(10)
                        .withMem(128)
                        .withHdd(100)
                        .withSsd(200)
                        .withPower(321)
                        .build()
                ))
                .withSlaves(Collections.singletonList(
                    TestSlaveGroup.builder()
                        .withName("IVA_MARKET_PROD_FRONT_BLUE_DESKTOP_CANARY")
                        .withFullHostAllocation()
                        .withPower(80L)
                        .withMem(25 * GB)
                        .withHddBytes(20.0 * (double) GB)
                        .withSsdBytes(40.0 * (double) GB)
                        .withInstancePerHost(1)
                        .withHosts(Arrays.asList("somehost3.market.yandex.net", "somehost4.market.yandex.net"))
                        .build()
                ))
                .build()
        );

        when(genCfgClient.getLastReleaseTag()).thenReturn(genCfgTag);

        for (TestMasterGroup masterGroup: masterGroups) {
            when(genCfgClient.getGroupInfo(genCfgTag, masterGroup.getName())).thenReturn(
                Optional.of(masterGroup.getGenCfgGroupInfo(genCfgTag))
            );
            when(genCfgClient.getGroupCard(genCfgTag, masterGroup.getName())).thenReturn(
                Optional.of(masterGroup.getGenCfgGroupCard())
            );
            for (TestSlaveGroup slaveGroup: masterGroup.getSlaves()) {
                when(genCfgClient.getGroupInfo(genCfgTag, slaveGroup.getName(), false)).thenReturn(
                    Optional.of(slaveGroup.getGenCfgGroupInfo(genCfgTag))
                );
                when(genCfgClient.getGroupCard(genCfgTag, slaveGroup.getName())).thenReturn(
                    Optional.of(slaveGroup.getGenCfgGroupCard())
                );
            }
        }

        GencfgUsageCountingManager manager = new GencfgUsageCountingManager(genCfgClient);
        GencfgUsageCountingManager.RtcCountingResult result = manager.count(
            masterGroups.stream().map(TestMasterGroup::getName).collect(Collectors.toList()));

        assertEquals(40, (int) result.getTotalCpu());
        assertEquals(512, (int) result.getTotalMemoryGb());
        assertEquals(700, (int) result.getTotalHddGb());
        assertEquals(800, (int) result.getTotalSsdGb());
        assertEquals(944, (int) result.getTotalPower());

        assertEquals(31, (int) result.getUsedCpu());
        assertEquals(346, (int) result.getUsedMemoryGb());
        assertEquals(320, (int) result.getUsedHddGb());
        assertEquals(490, (int) result.getUsedSsdGb());
        assertEquals(444, (int) result.getUsedPower());
    }

    static class TestSlaveGroup {
        private String name;
        private long power;
        private long mem;
        private double hddBytes;
        private double ssdBytes;
        private int instancePerHost;
        private List<String> hosts;
        private boolean fullHostAllocation;

        private TestSlaveGroup(String name, long power, long mem, double hddBytes, double ssdBytes,
                               int instancePerHost, List<String> hosts, boolean fullHostAllocation) {
            this.name = name;
            this.power = power;
            this.mem = mem;
            this.hddBytes = hddBytes;
            this.ssdBytes = ssdBytes;
            this.instancePerHost = instancePerHost;
            this.hosts = hosts;
            this.fullHostAllocation = fullHostAllocation;
        }

        public String getName() {
            return name;
        }

        public long getPower() {
            return power;
        }

        public long getMem() {
            return mem;
        }

        public double getHddBytes() {
            return hddBytes;
        }

        public double getSsdBytes() {
            return ssdBytes;
        }

        public int getInstancePerHost() {
            return instancePerHost;
        }

        public List<String> getHosts() {
            return hosts;
        }

        public GenCfgGroupInfo getGenCfgGroupInfo(String releaseTag) {
            GenCfgGroupInfo info = new GenCfgGroupInfo(name, releaseTag, null, null, null);
            info.setHosts(hosts);
            return info;
        }

        public GenCfgGroupCard getGenCfgGroupCard() {
            GenCfgGroupCard card = new GenCfgGroupCard();
            GenCfgGroupCardLegacy legacy = new GenCfgGroupCardLegacy();
            GenCfgGroupCardAudit audit = new GenCfgGroupCardAudit();
            GenCfgGroupCardCpu cpu = new GenCfgGroupCardCpu();
            cpu.setCpuClassName(fullHostAllocation ? "greedy": "normal");
            audit.setCpu(cpu);
            card.setAudit(audit);
            Map<String, String> funcs = new HashMap<>();
            funcs.put("instancePower", "exactly" + power);
            funcs.put("instanceCount", "exactly" + instancePerHost);
            legacy.setAllocationFunctions(funcs);
            card.setLegacy(legacy);
            GenCfgGroupCardRequirements reqs = new GenCfgGroupCardRequirements();
            GenCfgGroupCardInstanceReqs inst = new GenCfgGroupCardInstanceReqs();
            inst.setMemoryGuarantee(mem);
            inst.setHddQuota(hddBytes);
            inst.setSsdQuota(ssdBytes);
            reqs.setInstances(inst);
            card.setReqs(reqs);
            return card;
        }

        public static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private String name;
            private long power;
            private long mem;
            private double hddBytes;
            private double ssdBytes;
            private int instancePerHost;
            private List<String> hosts;
            private boolean fullHostAllocation;

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withPower(long power) {
                this.power = power;
                return this;
            }

            public Builder withMem(long mem) {
                this.mem = mem;
                return this;
            }

            public Builder withHddBytes(double hddBytes) {
                this.hddBytes = hddBytes;
                return this;
            }

            public Builder withSsdBytes(double ssdBytes) {
                this.ssdBytes = ssdBytes;
                return this;
            }

            public Builder withInstancePerHost(int instancePerHost) {
                this.instancePerHost = instancePerHost;
                return this;
            }

            public Builder withHosts(List<String> hosts) {
                this.hosts = hosts;
                return this;
            }

            public Builder withFullHostAllocation() {
                this.fullHostAllocation = true;
                return this;
            }

            public TestSlaveGroup build() {
                return new TestSlaveGroup(name, power, mem, hddBytes, ssdBytes, instancePerHost, hosts,
                    fullHostAllocation);
            }
        }
    }

    static class TestInstance {
        private int cpu;
        private int mem;
        private int hdd;
        private int ssd;
        private int power;
        private String hostname;
        private String model;

        private TestInstance(int cpu, int mem, int hdd, int ssd, int power, String hostname, String model) {
            this.cpu = cpu;
            this.mem = mem;
            this.hdd = hdd;
            this.ssd = ssd;
            this.power = power;
            this.hostname = hostname;
            this.model = model;
        }

        public int getCpu() {
            return cpu;
        }

        public int getMem() {
            return mem;
        }

        public int getHdd() {
            return hdd;
        }

        public int getSsd() {
            return ssd;
        }

        public int getPower() {
            return power;
        }

        public GenCfgInstance getGenCfgInstance() {
            return  new GenCfgInstance(null, hostname, null, 0, power, null, null, null, null,
                new GenCfgInstanceHostResources(cpu, mem, hdd, ssd, model), null);
        }

        public static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private int cpu;
            private int mem;
            private int hdd;
            private int ssd;
            private int power;
            private String hostname;
            private String model;

            public Builder withHostname(String hostname) {
                this.hostname = hostname;
                return this;
            }

            public Builder withCpu(int cpu) {
                this.cpu = cpu;
                return this;
            }

            public Builder withMem(int mem) {
                this.mem = mem;
                return this;
            }

            public Builder withHdd(int hdd) {
                this.hdd = hdd;
                return this;
            }

            public Builder withSsd(int ssd) {
                this.ssd = ssd;
                return this;
            }

            public Builder withPower(int power) {
                this.power = power;
                return this;
            }

            public Builder withModel(String model) {
                this.model = model;
                return this;
            }

            public TestInstance build() {
                return new TestInstance(cpu, mem, hdd, ssd, power, hostname, model);
            }
        }
    }

    static class TestMasterGroup {
        private String name;
        private List<TestInstance> instances;
        private List<TestSlaveGroup> slaves;

        private TestMasterGroup(String name, List<TestInstance> instances, List<TestSlaveGroup> slaves) {
            this.name = name;
            this.instances = instances;
            this.slaves = slaves;
        }

        public String getName() {
            return name;
        }

        public List<TestInstance> getInstances() {
            return instances;
        }

        public List<TestSlaveGroup> getSlaves() {
            return slaves;
        }

        public GenCfgGroupInfo getGenCfgGroupInfo(String releaseTag) {
            return new GenCfgGroupInfo(name, releaseTag, null, null,
                instances.stream().map(TestInstance::getGenCfgInstance).collect(Collectors.toList()));
        }

        public GenCfgGroupCard getGenCfgGroupCard() {
            GenCfgGroupCard card = new GenCfgGroupCard();
            card.setSlaves(slaves.stream().map(TestSlaveGroup::getName).collect(Collectors.toList()));
            return card;
        }

        public static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private String name;
            private List<TestInstance> instances;
            private List<TestSlaveGroup> slaves;

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withInstances(List<TestInstance> instances) {
                this.instances = instances;
                return this;
            }

            public Builder withSlaves(List<TestSlaveGroup> slaves) {
                this.slaves = slaves;
                return this;
            }

            public TestMasterGroup build() {
                return new TestMasterGroup(name, instances, slaves);
            }
        }
    }
}