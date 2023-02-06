package ru.yandex.market.tsum.pipelines.common.jobs.github;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.core.notify.common.startrek.ReleaseType;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseTypeResource;

public class CreateReleaseBranchJobTest {
    @Test
    public void shouldGenerationBranchVersionCorrectly() {
        String branchName = CreateReleaseBranchJob.createBranchName(
            new ReleaseTypeResource(ReleaseType.RELEASE),
            new ReleaseInfo(new FixVersion(123L, "2017.4.50 asdasd"), "MARKETCHECKOUT-1234")
        );

        Assert.assertEquals("release/2017.4.50_MARKETCHECKOUT-1234", branchName);
    }

    @Test
    public void shouldGenerationBranchVersionCorrectlyNoSpace() {
        String branchName = CreateReleaseBranchJob.createBranchName(
            new ReleaseTypeResource(ReleaseType.RELEASE),
            new ReleaseInfo(new FixVersion(123L, "2017.4.50asdasd"), "MARKETCHECKOUT-1234")
        );

        Assert.assertEquals("release/2017.4.50_MARKETCHECKOUT-1234", branchName);
    }

    @Test
    public void shouldGenerationBranchVersionCorrectlyNoName() {
        String branchName = CreateReleaseBranchJob.createBranchName(
            new ReleaseTypeResource(ReleaseType.RELEASE),
            new ReleaseInfo(new FixVersion(123L, "2017.4.50"), "MARKETCHECKOUT-1234")
        );

        Assert.assertEquals("release/2017.4.50_MARKETCHECKOUT-1234", branchName);
    }
}
