package ru.yandex.market.tsum.pipelines.common.jobs.teamcity;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 15.01.18
 */
public class PackageArtifactTest {
    @Test
    public void parsePackageArtifactInDeprecatedFormat() {
        String changelogString = "nothing changed";
        PackageArtifact packageArtifact = PackageArtifact.fromJson("{packages: [{changelog: \"" + changelogString +
            "\"}]}");

        Assert.assertEquals(changelogString, packageArtifact.getPackages().get(0).getChangelog());
    }

    @Test
    public void parsePackageArtifact() {
        String commitMessage = "some change";
        PackageArtifact packageArtifact =
            PackageArtifact.fromJson("{packages: [{changelogDetails: [{change: \"" + commitMessage + "\", author: " +
                "\"algebraic\"}]}]}");

        ChangelogEntry changelogEntry = packageArtifact.getPackages().get(0).getChangelogEntries().get(0);
        Assert.assertEquals(commitMessage, changelogEntry.getChange());
        Assert.assertEquals("algebraic", changelogEntry.getAuthor());
    }
}
