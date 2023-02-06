package ru.yandex.market.crm.campaign.test.utils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.segments.export.SegmentTableManager;
import ru.yandex.market.crm.core.domain.segment.BuildStatus;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentBuild;
import ru.yandex.market.crm.core.domain.segment.SegmentBuild.Initiator;
import ru.yandex.market.crm.core.domain.segment.export.IdType;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author apershukov
 */
@Component
public class OfflineSegmentatorTestHelper {

    private final SegmentService segmentService;
    private final YtClient ytClient;
    private final SegmentTableManager tableManager;

    public OfflineSegmentatorTestHelper(SegmentService segmentService,
                                        YtClient ytClient,
                                        SegmentTableManager tableManager) {
        this.segmentService = segmentService;
        this.ytClient = ytClient;
        this.tableManager = tableManager;
    }

    public static UidPair pair(Uid uid, Uid attachedUid) {
        return new UidPair(uid, attachedUid);
    }

    public static UidPair pair(Uid uid) {
        return new UidPair(uid, null);
    }

    private static UidPair convertToPair(YTreeMapNode row) {
        Uid uid = Uid.of(
                UidType.valueOf(row.getString("id_type")),
                row.getString("id_value")
        );

        UidType attachedIdType = row.get("original_id_type")
                .filter(YTreeNode::isStringNode)
                .map(YTreeNode::stringValue)
                .map(UidType::valueOf)
                .orElse(null);

        String attachedIdValue = row.get("original_id_value")
                .filter(YTreeNode::isStringNode)
                .map(YTreeNode::stringValue)
                .orElse(null);

        Uid attachedUid = attachedIdType == null || attachedIdValue == null
                ? null
                : Uid.of(attachedIdType, attachedIdValue);

        return pair(uid, attachedUid);
    }

    public void assertSegmentPairs(Set<UidPair> expectedPairs,
                                   LinkingMode linkingMode,
                                   Segment segment) throws InterruptedException {
        assertSegmentPairs(expectedPairs, linkingMode, UidType.ALL, segment);
    }

    public void assertSegmentPairs(Set<UidPair> expectedPairs,
                                   LinkingMode linkingMode,
                                   Set<UidType> uidTypes,
                                   Segment segment) throws InterruptedException {
        SegmentBuild build = buildSegment(segment, linkingMode, uidTypes, true);

        YPath resultPath = tableManager.getSegmentTable(build.getResultDirectory());

        Set<UidPair> actualPairs = ytClient.read(resultPath, YTableEntryTypes.YSON).stream()
                .map(OfflineSegmentatorTestHelper::convertToPair)
                .collect(Collectors.toSet());

        assertEquals(expectedPairs, actualPairs);
    }

    public void assertCounts(Segment segment,
                             LinkingMode linkingMode,
                             Map<UidType, Integer> expectedIds) throws InterruptedException {
        SegmentBuild build = buildSegment(segment, linkingMode, expectedIds.keySet(), false);

        Map<IdType, Long> types = build.getCounts();

        expectedIds.forEach((uidType, expected) ->
                assertEquals((long) expected, (long) types.get(IdType.fromSourceType(uidType)))
        );
    }

    private SegmentBuild buildSegment(Segment segment,
                                      LinkingMode linkingMode,
                                      Set<UidType> uidTypes,
                                      boolean skipResultProcessing) throws InterruptedException {
        segment = segmentService.addSegment(segment);

        long buildId = segmentService.buildSegment(
                segment.getId(),
                linkingMode,
                uidTypes,
                skipResultProcessing,
                Initiator.SYSTEM,
                null
        );

        waitCounted(buildId);
        return segmentService.getBuild(buildId);
    }

    private void waitCounted(long buildId) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < 1800_000) {
            Thread.sleep(1000);

            SegmentBuild build = segmentService.getBuild(buildId);

            BuildStatus buildStatus = build.getStatus();
            if (buildStatus == BuildStatus.COUNTED) {
                return;
            } else if (buildStatus == BuildStatus.ERROR) {
                fail("Error on segment count:" + build.getMessage());
            }
        }

        fail("Segment count reached timeout");
    }

    public static class UidPair {

        final Uid uid;
        final Uid originalUid;

        private UidPair(Uid uid, Uid originalUid) {
            this.uid = uid;
            this.originalUid = originalUid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UidPair uidPair = (UidPair) o;
            return Objects.equals(uid, uidPair.uid) &&
                    Objects.equals(originalUid, uidPair.originalUid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uid, originalUid);
        }

        @Override
        public String toString() {
            return originalUid == null ? uid.toString() : String.format("{id: %s, originalId: %s}", uid, originalUid);
        }
    }
}
