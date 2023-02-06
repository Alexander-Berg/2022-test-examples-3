package ru.yandex.market.tsum.pipelines.common.jobs.delivery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.tsum.clients.arcadia.TrunkArcadiaClient;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.release.dao.ArcadiaSettings;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.delivery.ArcadiaVcsChange;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 04.04.18
 */
public class ChangelogUtilsTest {
    private static final String PIPE_ID = "pipeId";
    private static final String PROJECT_ID = "projectId";
    private static final String PIPE_LAUNCH_ID = "pipeLaunchId";

    public static final String RESULT_MESSAGE = "ticket_id for molly scan. Issue: MARKETFRONTECH-799" +
        "\n\n" +
        "REVIEW: 1157228";

    public static final String SVN_LOG_MESSAGE_1 = "ticket_id for molly scan. Issue: MARKETFRONTECH-799\n" +
        "\n" +
        "<!-- DEVEXP BEGIN -->\n" +
        "![review](https://codereview.common-int.yandex-team.ru/badges/review-complete-green.svg) ![author]" +
        "(https://codereview.common-int.yandex-team.ru/badges/author-ok-green.svg)\n" +
        "<!-- DEVEXP END -->\n" +
        "\n" +
        "REVIEW: 1157228";

    public static final String SVN_LOG_MESSAGE_2 = "ticket_id for molly scan. Issue: MARKETFRONTECH-799\n" +
        "\n" +
        "description ..." +
        "\n\n" +
        "REVIEW: 1157228";

    @Mock
    private TrunkArcadiaClient arcadiaClient;
    ArcadiaSettings arcadiaSettings = new ArcadiaSettings();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void hasChangesToRelease_NoRunningPipelines_EmptyChangelog() {
        boolean hasChangesToRelease = ChangelogUtils.hasChangesToRelease(
            Collections.emptyList(),
            Collections.emptyList()
        );

        assertFalse(hasChangesToRelease);
    }

    @Test
    public void hasChangesToRelease_NoRunningPipelines_NonEmptyChangelog() {
        boolean hasChangesToRelease = ChangelogUtils.hasChangesToRelease(
            Arrays.asList(changelogEntry("3"), changelogEntry("2")),
            Collections.emptyList()
        );

        assertTrue(hasChangesToRelease);
    }

    @Test
    public void hasChangesToRelease_SeveralRunningPipelines() {
        boolean hasChangesToRelease = ChangelogUtils.hasChangesToRelease(
            Arrays.asList(changelogEntry("3"), changelogEntry("2")),
            Arrays.asList(
                createRelease("2", "", "2", Instant.now()),
                createRelease("3", "", "3", Instant.now())
            )
        );

        assertFalse(hasChangesToRelease);
    }

    @Test
    public void hasChangesToRelease_FilesWhiteList() {
        boolean hasChangesToRelease = ChangelogUtils.hasChangesToRelease(
            Arrays.asList(
                changelogEntry("3", 3),
                changelogEntry("4", 4),
                changelogEntry("5", 5)
            ),
            Arrays.asList(
                createRelease("2", "", "3", Instant.now()),
                createRelease("3", "", "5", Instant.now())
            )
        );

        assertFalse(hasChangesToRelease);
    }

    @Test
    public void getCurrentReleaseChanges() {
        List<Release> releases = Arrays.asList(
            createRelease("1", "title 1", "1", Instant.ofEpochMilli(1)),
            createRelease("2", "title 2", "2", Instant.ofEpochMilli(2)),
            createRelease("3", "title 3", "3", Instant.ofEpochMilli(3))
        );

        List<ArcadiaVcsChange> expected = Arrays.asList(
            new ArcadiaVcsChange("4", Instant.ofEpochMilli(4), null, null, null),
            new ArcadiaVcsChange("5", Instant.ofEpochMilli(5), null, null, null)
        );

        List<ArcadiaVcsChange> changes = new ArrayList<>();
        changes.add(
            new ArcadiaVcsChange("1", Instant.ofEpochMilli(1), null, null, null)
        );
        changes.add(
            new ArcadiaVcsChange("2", Instant.ofEpochMilli(2), null, null, null)
        );
        changes.add(
            new ArcadiaVcsChange("3", Instant.ofEpochMilli(3), null, null, null)
        );
        changes.addAll(expected);

        assertEquals(
            expected,
            ChangelogUtils.getCurrentReleaseChanges(releases, changes)
        );
    }

    @Test
    public void removeDevexpMessage() {
        Assert.assertEquals(
            RESULT_MESSAGE,
            ChangelogUtils.removeDevexpMessage(SVN_LOG_MESSAGE_1)
        );
    }

    @Test
    public void removeEverythingExceptCommitAndReviewMessages() {
        Assert.assertEquals(
            RESULT_MESSAGE,
            ChangelogUtils.removeEverythingExceptCommitAndReviewMessages(RESULT_MESSAGE)
        );

        Assert.assertEquals(
            RESULT_MESSAGE,
            ChangelogUtils.removeEverythingExceptCommitAndReviewMessages(SVN_LOG_MESSAGE_1)
        );

        Assert.assertEquals(
            RESULT_MESSAGE,
            ChangelogUtils.removeEverythingExceptCommitAndReviewMessages(SVN_LOG_MESSAGE_1)
        );

        Assert.assertEquals(
            RESULT_MESSAGE,
            ChangelogUtils.removeEverythingExceptCommitAndReviewMessages(SVN_LOG_MESSAGE_2)
        );
    }

    private ChangelogEntry changelogEntry(String revision) {
        return new ChangelogEntry(revision, "change " + revision);
    }

    private ChangelogEntry changelogEntry(String revision, long timestamp) {
        return new ChangelogEntry(revision, timestamp, "change " + revision, null, null);
    }

    private Release createRelease(String id, String title, String revision, Instant commitDate) {
        return Release.builder()
            .withId(id)
            .withProjectId(PROJECT_ID)
            .withPipeId(PIPE_ID)
            .withPipeLaunchId(PIPE_LAUNCH_ID)
            .withTitle(title)
            .withCommit(revision, commitDate)
            .build();
    }
}
