package ru.yandex.virustotal;

import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.testng.Assert;
import org.testng.collections.Lists;

import ru.yandex.bannerstorage.harvester.queues.automoderation.VirusTotalPollQueueObserver;
import ru.yandex.bannerstorage.harvester.queues.automoderation.services.virustotal.VirusTotalAntiVirusScanResult;
import ru.yandex.bannerstorage.harvester.queues.automoderation.services.virustotal.VirusTotalScanResult;

/**
 * @author elwood
 */
public class VirusTotalTests {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void testWhiteList() {
        List<VirusTotalPollQueueObserver.VirustotalScanInfo> scans =
                Lists.newArrayList(
                        createScan("GoodAntivirus", "RealVirus"),
                        createScan("BadAntivirus", "FalsePositiveVirus"),
                        createScan("UnknownAntivirus", "SomeUnknownVirus"));
        Map<VirusTotalPollQueueObserver.VirustotalScanInfo, List<Pair<String, String>>> detections =
                VirusTotalPollQueueObserver.getViruses(
                        objectMapper,
                        scans,
                        Lists.newArrayList(
                                Pair.of("BadAntivirus", "FalsePositiveVirus")));
        Assert.assertEquals(
                detections.values().stream().flatMap(Collection::stream).count(), 2);
        Assert.assertTrue(
                detections.values().stream().flatMap(Collection::stream)
                        .noneMatch(p -> p.getLeft().equals("BadAntivirus") && p.getRight().equals("FalsePositiveVirus")));
        Assert.assertTrue(
                detections.values().stream().flatMap(Collection::stream)
                        .anyMatch(p -> p.getLeft().equals("GoodAntivirus") && p.getRight().equals("RealVirus")));
        Assert.assertTrue(
                detections.values().stream().flatMap(Collection::stream)
                        .anyMatch(p -> p.getLeft().equals("UnknownAntivirus") && p.getRight().equals("SomeUnknownVirus")));
    }

    private VirusTotalPollQueueObserver.VirustotalScanInfo createScan(String antivirus, String virus) {
        VirusTotalPollQueueObserver.VirustotalScanInfo scanInfo = new VirusTotalPollQueueObserver.VirustotalScanInfo();
        scanInfo.setVirustotalScanResult(
                toJson(
                        objectMapper,
                        new VirusTotalScanResult(
                                1,
                                "",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                1,
                                1,
                                ImmutableMap.of(
                                        antivirus,
                                        new VirusTotalAntiVirusScanResult(true, null, null, virus)))));
        return scanInfo;
    }

    private static String toJson(ObjectMapper objectMapper, Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
