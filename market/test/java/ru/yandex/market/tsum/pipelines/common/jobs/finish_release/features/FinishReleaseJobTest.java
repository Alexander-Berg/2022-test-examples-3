package ru.yandex.market.tsum.pipelines.common.jobs.finish_release.features;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 29.06.18
 */
public class FinishReleaseJobTest {

    @Test
    public void getOldChangelogEntries() {
        List<ChangelogEntry> oldChangelogEntries = Arrays.asList(
            new ChangelogEntry(null, "MARKETINFRATEST-12"),
            new ChangelogEntry(null, "MARKETINFRATEST-13"),
            new ChangelogEntry(null, "MARKETINFRATEST-22")
        );

        List<ChangelogInfo> changelog = Arrays.asList(
            new ChangelogInfo(
                Arrays.asList(
                    new ChangelogEntry(null, "MARKETINFRATEST-11"),
                    new ChangelogEntry(null, "MARKETINFRATEST-12"),
                    new ChangelogEntry(null, "MARKETINFRATEST-13")
                )
            ),
            new ChangelogInfo(
                Arrays.asList(
                    new ChangelogEntry(null, "MARKETINFRATEST-21"),
                    new ChangelogEntry(null, "MARKETINFRATEST-22"),
                    new ChangelogEntry(null, "MARKETINFRATEST-23")
                )
            )
        );

        List<ChangelogInfo> exactChangelog = Arrays.asList(
            new ChangelogInfo(
                Arrays.asList(
                    new ChangelogEntry(null, "MARKETINFRATEST-11")
                )
            ),
            new ChangelogInfo(
                Arrays.asList(
                    new ChangelogEntry(null, "MARKETINFRATEST-21"),
                    new ChangelogEntry(null, "MARKETINFRATEST-23")
                )
            )
        );

        FinishReleaseFeature feature = new FinishReleaseFeature();
        feature.setChangelog(changelog);

        List<ChangelogEntry> entries = feature.getOldChangelogEntries(exactChangelog);
        Assert.assertEquals(
            oldChangelogEntries,
            entries
        );
    }
}
