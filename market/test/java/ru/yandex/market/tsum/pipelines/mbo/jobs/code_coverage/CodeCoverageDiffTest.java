package ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage.models.CodeCoverage;
import ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage.models.CodeCoverageBuilder;
import ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage.models.CodeCoverageDiff;
import ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage.models.CodeCoverageType;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CodeCoverageDiffTest {

    @Test
    public void testNoChanges() {
        CodeCoverage codeCoverage = new CodeCoverageBuilder()
            .setCovered(CodeCoverageType.BRANCH, 10).setMissed(CodeCoverageType.BRANCH, 10)
            .build();

        CodeCoverageDiff diff = new CodeCoverageDiff(codeCoverage, codeCoverage);
        Assert.assertEquals("50%", diff.getAfterCoverageStr(CodeCoverageType.BRANCH));
        Assert.assertEquals("+0%", diff.getDiffStr(CodeCoverageType.BRANCH));
    }

    @Test
    public void testIncrease() {
        CodeCoverage before = new CodeCoverageBuilder()
            .setCovered(CodeCoverageType.BRANCH, 10).setMissed(CodeCoverageType.BRANCH, 10)
            .build();
        CodeCoverage after = new CodeCoverageBuilder()
            .setCovered(CodeCoverageType.BRANCH, 20).setMissed(CodeCoverageType.BRANCH, 5)
            .build();

        CodeCoverageDiff diff = new CodeCoverageDiff(before, after);
        Assert.assertEquals("80%", diff.getAfterCoverageStr(CodeCoverageType.BRANCH));
        Assert.assertEquals("+30%", diff.getDiffStr(CodeCoverageType.BRANCH));
    }

    @Test
    public void testDecrease() {
        CodeCoverage before = new CodeCoverageBuilder()
            .setCovered(CodeCoverageType.BRANCH, 20).setMissed(CodeCoverageType.BRANCH, 5)
            .build();
        CodeCoverage after = new CodeCoverageBuilder()
            .setCovered(CodeCoverageType.BRANCH, 10).setMissed(CodeCoverageType.BRANCH, 10)
            .build();

        CodeCoverageDiff diff = new CodeCoverageDiff(before, after);
        Assert.assertEquals("50%", diff.getAfterCoverageStr(CodeCoverageType.BRANCH));
        Assert.assertEquals("-30%", diff.getDiffStr(CodeCoverageType.BRANCH));
    }

    @Test
    public void testLittleIncrease() {
        CodeCoverage before = new CodeCoverageBuilder()
            .setCovered(CodeCoverageType.BRANCH, 999).setMissed(CodeCoverageType.BRANCH, 1001)
            .build();
        CodeCoverage after = new CodeCoverageBuilder()
            .setCovered(CodeCoverageType.BRANCH, 1000).setMissed(CodeCoverageType.BRANCH, 1000)
            .build();

        CodeCoverageDiff diff = new CodeCoverageDiff(before, after);
        Assert.assertEquals("50%", diff.getAfterCoverageStr(CodeCoverageType.BRANCH));
        Assert.assertEquals("+0.05%", diff.getDiffStr(CodeCoverageType.BRANCH));
    }

    @Test
    public void testLittleDecrease() {
        CodeCoverage before = new CodeCoverageBuilder()
            .setCovered(CodeCoverageType.BRANCH, 1000).setMissed(CodeCoverageType.BRANCH, 1000)
            .build();
        CodeCoverage after = new CodeCoverageBuilder()
            .setCovered(CodeCoverageType.BRANCH, 999).setMissed(CodeCoverageType.BRANCH, 1001)
            .build();

        CodeCoverageDiff diff = new CodeCoverageDiff(before, after);
        Assert.assertEquals("49.95%", diff.getAfterCoverageStr(CodeCoverageType.BRANCH));
        Assert.assertEquals("-0.05%", diff.getDiffStr(CodeCoverageType.BRANCH));
    }
}
