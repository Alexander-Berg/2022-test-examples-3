package ru.yandex.market.crm.campaign.services.segments;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.test.AbstractServiceMediumTest;
import ru.yandex.market.crm.core.domain.segment.BuildStatus;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentBuild;
import ru.yandex.market.crm.core.domain.segment.SegmentBuild.Initiator;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.util.LiluCollectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segmentBuild;

/**
 * @author apershukov
 */
public class SegmentServiceMediumTest extends AbstractServiceMediumTest {

    @Inject
    private SegmentBuildsDAO buildsDAO;

    @Inject
    private SegmentService segmentService;

    @Inject
    private YtClient ytClient;

    @Inject
    private YtFolders ytFolders;

    private Segment segment;

    @BeforeEach
    public void setUp() {
        segment = segmentService.addSegment(
                segment(plusFilter())
        );
    }

    /**
     * В случае если построение сегмента завершается при уже существующих
     * двух завершенных билдах все три билда сегмента сохраняются
     */
    @Test
    public void testFinishBuildingWithTwoExistingBuilds() {
        SegmentBuild build1 = saveBuild(completedBuild());
        SegmentBuild build2 = saveBuild(completedBuild());
        SegmentBuild build3 = saveBuild(inProgressBuild());

        completeBuild(build3);

        Map<Long, SegmentBuild> existingBuilds = getBuilds();

        assertEquals(3, existingBuilds.size());
        assertTrue(existingBuilds.containsKey(build1.getId()));
        assertTrue(existingBuilds.containsKey(build2.getId()));

        SegmentBuild actualBuild3 = existingBuilds.get(build3.getId());
        assertEquals(BuildStatus.COUNTED, actualBuild3.getStatus());
    }

    /**
     * В случае если при завершении построения сегмента законченных билдов становится более пяти
     * удаляется самый старый билд
     */
    @Test
    public void testKeepBuildCountEqualTo5() {
        List<SegmentBuild> builds = Stream.generate(() -> saveBuild(completedBuild()))
                .limit(5)
                .collect(Collectors.toList());

        SegmentBuild oldestBuild = builds.get(0);

        ytClient.createDirectory(oldestBuild.getResultDirectory(), true, true);

        SegmentBuild inProgress = saveBuild(inProgressBuild());

        assertEquals(6, getBuilds().size());

        completeBuild(inProgress);

        Map<Long, SegmentBuild> existingBuilds = getBuilds();
        assertEquals(5, existingBuilds.size());

        assertFalse(existingBuilds.containsKey(oldestBuild.getId()));
        assertFalse(ytClient.exists(oldestBuild.getResultDirectory()));
    }

    /**
     * Незавершенные билды не учитываются при ограничении количества билдов для сегмента
     */
    @Test
    public void testDoNotRemoveRunningBuilds() {
        List<SegmentBuild> builds = Stream.generate(() -> saveBuild(inProgressBuild()))
                .limit(6)
                .collect(Collectors.toList());

        completeBuild(builds.get(builds.size() - 1));

        assertEquals(6, getBuilds().size());
    }

    /**
     * Билд, используемый в другом сегменте, не подлежит удалению
     */
    @Test
    public void testDoNotRemoveUsedBuild() {
        List<SegmentBuild> completedBuilds = Stream.generate(() -> saveBuild(completedBuild()))
                .limit(5)
                .collect(Collectors.toList());

        SegmentBuild inProgressBuild1 = saveBuild(inProgressBuild());
        SegmentBuild inProgressBuild2 = saveBuild(inProgressBuild());

        segmentService.addSegment(
                segment(
                        segmentBuild(completedBuilds.get(0).getId())
                )
        );

        completeBuild(inProgressBuild1);
        completeBuild(inProgressBuild2);

        Map<Long, SegmentBuild> builds = getBuilds();
        assertEquals(6, builds.size());
        assertTrue(builds.containsKey(completedBuilds.get(0).getId()));
        assertFalse(builds.containsKey(completedBuilds.get(1).getId()));
    }

    /**
     * Сборки сегментов старше одного месяца удаляются
     */
    @Test
    void testRemoveOldBuilds() {
        var now = LocalDateTime.now();

        var build1 = saveBuildAndCreateDir(completedBuild(now.minusDays(35)));
        var build2 = saveBuildAndCreateDir(completedBuild(now.minusDays(31)));
        var build3 = saveBuildAndCreateDir(completedBuild(now.minusDays(25)));
        var build4 = saveBuildAndCreateDir(completedBuild(now.minusDays(7)));
        var build5 = saveBuildAndCreateDir(inProgressBuild());

        segmentService.deleteStaleBuilds();

        assertNotExists(build1);
        assertNotExists(build2);
        assertExists(build3);
        assertExists(build4);
        assertExists(build5);
    }

    /**
     * В случае если после удаления старых сборок директория сегмента осталась пустой,
     * она так же удаляется.
     */
    @Test
    void testEmptySegmentDirectoryIsBeingRemovedAlongWithBuildDirs() {
        var now = LocalDateTime.now();

        var build1 = saveBuildAndCreateDir(completedBuild(now.minusDays(40)));
        var build2 = saveBuildAndCreateDir(completedBuild(now.minusDays(39)));

        segmentService.deleteStaleBuilds();

        assertNotExists(build1);
        assertNotExists(build2);

        assertFalse(ytClient.exists(ytFolders.getSegmentDirectory(segment.getId())));
    }

    /**
     * Сборка сегмента, используемая в другом сегменте, не может быть удалена по причине устаревания
     */
    @Test
    void testUsedSegmentBuildIsNotBeingRemoved() {
        var build = saveBuildAndCreateDir(completedBuild(LocalDateTime.now().minusDays(35)));

        segmentService.addSegment(
                segment(segmentBuild(build.getId()))
        );

        segmentService.deleteStaleBuilds();

        assertExists(build);
    }

    private void completeBuild(SegmentBuild build) {
        build.setStatus(BuildStatus.COUNTED)
                .setFinishTime(LocalDateTime.now());

        segmentService.updateBuild(build);
    }

    private Map<Long, SegmentBuild> getBuilds() {
        return buildsDAO.getForSegment(segment.getId()).stream()
                .collect(LiluCollectors.index(SegmentBuild::getId));
    }

    private SegmentBuild saveBuild(SegmentBuild build) {
        buildsDAO.insert(build);
        return build;
    }

    private SegmentBuild saveBuildAndCreateDir(SegmentBuild build) {
        saveBuild(build);
        ytClient.createDirectory(build.getResultDirectory(), true, true);
        return build;
    }

    private SegmentBuild completedBuild(LocalDateTime finishTime) {
        return buildingFact(finishTime.minusHours(1))
                .setFinishTime(finishTime)
                .setStatus(BuildStatus.COUNTED);
    }

    private SegmentBuild completedBuild() {
        return completedBuild(LocalDateTime.now());
    }

    private SegmentBuild buildingFact(LocalDateTime startTime) {
        var resultPath = ytFolders.getSegmentDirectory(segment.getId())
                .child(startTime.truncatedTo(ChronoUnit.SECONDS).toString());

        return new SegmentBuild()
                .setSegmentId(segment.getId())
                .setMode(LinkingMode.ALL)
                .setInitiator(Initiator.USER)
                .setStartTime(startTime)
                .setResultDirectory(resultPath);
    }

    private SegmentBuild inProgressBuild() {
        return buildingFact(LocalDateTime.now())
                .setStatus(BuildStatus.IN_PROGRESS);
    }

    private void assertExists(SegmentBuild build) {
        assertExistence(build, true);
    }

    private void assertNotExists(SegmentBuild build) {
        assertExistence(build, false);
    }

    private void assertExistence(SegmentBuild build, boolean exists) {
        var builds = buildsDAO.getForSegment(build.getSegmentId()).stream()
                .map(SegmentBuild::getId)
                .collect(Collectors.toSet());

        var buildId = build.getId();
        var resultDir = build.getResultDirectory();

        if (exists) {
            assertThat(builds, hasItem(buildId));
            assertTrue(ytClient.exists(resultDir));
        } else {
            assertThat(builds, not(hasItem(buildId)));
            assertFalse(ytClient.exists(resultDir));
        }
    }
}
