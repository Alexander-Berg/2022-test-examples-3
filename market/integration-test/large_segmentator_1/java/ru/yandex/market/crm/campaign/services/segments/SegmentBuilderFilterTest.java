package ru.yandex.market.crm.campaign.services.segments;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.market.crm.campaign.services.segments.export.SegmentTableManager;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.UidPair;
import ru.yandex.market.crm.core.domain.segment.BuildStatus;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentBuild;
import ru.yandex.market.crm.core.domain.segment.SegmentBuild.Initiator;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segmentBuild;

/**
 * @author apershukov
 */
public class SegmentBuilderFilterTest extends AbstractServiceLargeTest {

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @Inject
    private SegmentService segmentService;

    @Inject
    private SegmentBuildsDAO segmentBuildsDAO;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private SegmentTableManager segmentTableManager;

    @Test
    public void testGetPuidsFromPuidSegmentBuild() throws Exception {
        Segment builtSegment = segmentService.addSegment(
                segment(plusFilter())
        );

        LocalDateTime startTime = LocalDateTime.now();

        YPath resultDirectory = ytFolders.getSegmentDirectory(builtSegment.getId())
                .child(startTime.truncatedTo(ChronoUnit.SECONDS).toString());

        SegmentBuild build = segmentBuildsDAO.insert(
                new SegmentBuild()
                        .setSegmentId(builtSegment.getId())
                        .setStatus(BuildStatus.COUNTED)
                        .setInitiator(Initiator.USER)
                        .setStartTime(startTime)
                        .setFinishTime(LocalDateTime.now())
                        .setMode(LinkingMode.NONE)
                        .setIdTypes(Set.of(UidType.PUID))
                        .setResultDirectory(resultDirectory)
        );

        ytClient.write(
                segmentTableManager.getSegmentTable(resultDirectory),
                List.of(
                        YTree.mapBuilder()
                                .key("id_value").value("111")
                                .key("id_type").value("PUID")
                                .key("original_id_value").value((Object) null)
                                .key("original_id_type").value((Object) null)
                                .buildMap()
                )
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asPuid(111L))
        );

        Segment segment = segment(
                segmentBuild(build.getId())
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.PUID),
                segment
        );
    }
}
