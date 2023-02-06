package ru.yandex.market.tsum.pipelines.sre.rtc.jobs;

import java.math.RoundingMode;
import java.util.List;

import com.google.common.math.IntMath;
import org.junit.Test;

import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.multitesting.GenCfgGroupSpec;
import ru.yandex.market.tsum.pipelines.sre.rtc.jobs.CreateRtcServiceSpecJob.NannyServiceResourceRequest;
import ru.yandex.market.tsum.pipelines.sre.rtc.jobs.CreateRtcServiceSpecJob.ResourceReportRow;
import ru.yandex.market.tsum.pipelines.sre.rtc.jobs.CreateRtcServiceSpecJob.ResourceStatistics;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tsum.clients.gencfg.GenCfgCType.PRODUCTION;
import static ru.yandex.market.tsum.clients.gencfg.GenCfgCType.TESTING;
import static ru.yandex.market.tsum.clients.gencfg.GenCfgLocation.IVA;
import static ru.yandex.market.tsum.clients.gencfg.GenCfgLocation.MAN;
import static ru.yandex.market.tsum.clients.gencfg.GenCfgLocation.SAS;
import static ru.yandex.market.tsum.pipelines.sre.rtc.jobs.CreateRtcServiceSpecJob.calculateResourceStatistics;

public class CreateRtcServiceSpecJobComputeStatisticsTest {
    private static final int TEST_NUMBER_OF_INSTANCES = 1;
    private static final int TEST_MIN_POWER = GenCfgGroupSpec.POWER_IN_CPU / 2;
    private static final int TEST_MEMORY_MEGABYTES = 4096;
    private static final int TEST_TOTAL_STORAGE_GIGABYTES = 30;

    private static final int PROD_NUMBER_OF_INSTANCES = 5;
    private static final int PROD_MIN_POWER = 6 * GenCfgGroupSpec.POWER_IN_CPU;
    private static final int PROD_MEMORY_MEGABYTES = 16384;
    private static final int PROD_TOTAL_STORAGE_GIGABYTES = 200;

    static final List<NannyServiceResourceRequest> REQUESTS = List.of(
        testingRequest(SAS),
        testingRequest(IVA),
        productionRequest(SAS),
        productionRequest(IVA),
        productionRequest(MAN)
    );

    private static final ResourceStatistics EXPECTED_STATS = new ResourceStatistics(
        List.of(
            testingLocationStats(SAS),
            testingLocationStats(IVA),
            productionLocationStats(SAS),
            productionLocationStats(IVA),
            productionLocationStats(MAN)
        ),

        new ResourceReportRow(
            "*", "*",
            17, // total instances
            null, // "total CPU/instance", doesn't make sense, we don't calculate it
            null, // "total RAM/instance", doesn't make sense, we don't calculate it
            92,   // total CPU
            253952 // total RAM
        )
    );

    static NannyServiceResourceRequest testingRequest(GenCfgLocation location) {
        return new NannyServiceResourceRequest(location, TESTING,
            TEST_NUMBER_OF_INSTANCES, TEST_MIN_POWER, TEST_MEMORY_MEGABYTES, TEST_TOTAL_STORAGE_GIGABYTES);
    }

    private static ResourceReportRow testingLocationStats(GenCfgLocation location) {
        int cpuPerInstance = IntMath.divide(TEST_MIN_POWER, GenCfgGroupSpec.POWER_IN_CPU,
            RoundingMode.CEILING);

        return new ResourceReportRow(
            location.toString(), TESTING.toString(),
            TEST_NUMBER_OF_INSTANCES, cpuPerInstance, TEST_MEMORY_MEGABYTES,
            cpuPerInstance * TEST_NUMBER_OF_INSTANCES, TEST_MEMORY_MEGABYTES * TEST_NUMBER_OF_INSTANCES);
    }

    static NannyServiceResourceRequest productionRequest(GenCfgLocation location) {
        return new NannyServiceResourceRequest(location, PRODUCTION,
            PROD_NUMBER_OF_INSTANCES, PROD_MIN_POWER, PROD_MEMORY_MEGABYTES, PROD_TOTAL_STORAGE_GIGABYTES);
    }

    private static ResourceReportRow productionLocationStats(GenCfgLocation location) {
        int cpuPerInstance = IntMath.divide(PROD_MIN_POWER, GenCfgGroupSpec.POWER_IN_CPU,
            RoundingMode.CEILING);

        return new ResourceReportRow(
            location.toString(), PRODUCTION.toString(),
            PROD_NUMBER_OF_INSTANCES, cpuPerInstance, PROD_MEMORY_MEGABYTES,
            cpuPerInstance * PROD_NUMBER_OF_INSTANCES, PROD_MEMORY_MEGABYTES * PROD_NUMBER_OF_INSTANCES);
    }

    @Test
    public void testComputeStatistics() {
        assertThat(calculateResourceStatistics(REQUESTS)).isEqualToComparingFieldByFieldRecursively(EXPECTED_STATS);
    }
}
