package ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage.models.CodeCoverage;
import ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage.models.CodeCoverageBuilder;
import ru.yandex.market.tsum.pipelines.mbo.jobs.code_coverage.models.CodeCoverageType;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class JaCoCoParserTest {

    private static final String JACOCO_PATH_1 = "/artifacts/jacoco_release_branch.xml";
    private static final String JACOCO_PATH_2 = "/artifacts/jacoco_master_branch.xml";

    private JaCoCoParser jaCoCoParser;

    @Before
    public void setUp() {
        jaCoCoParser = new JaCoCoParser();
    }

    @Test
    public void testParsing1() throws IOException {
        try (InputStream inputStream = this.getClass().getResourceAsStream(JACOCO_PATH_1)) {
            CodeCoverage actual = jaCoCoParser.parseXml(inputStream);
            CodeCoverage expected = new CodeCoverageBuilder()
                .setMissed(CodeCoverageType.INSTRUCTION, 2658).setCovered(CodeCoverageType.INSTRUCTION, 330)
                .setMissed(CodeCoverageType.BRANCH, 191).setCovered(CodeCoverageType.BRANCH, 21)
                .setMissed(CodeCoverageType.LINE, 533).setCovered(CodeCoverageType.LINE, 84)
                .setMissed(CodeCoverageType.COMPLEXITY, 238).setCovered(CodeCoverageType.COMPLEXITY, 39)
                .setMissed(CodeCoverageType.METHOD, 142).setCovered(CodeCoverageType.METHOD, 29)
                .setMissed(CodeCoverageType.CLASS, 19).setCovered(CodeCoverageType.CLASS, 6)
                .build();

            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void testParsing2() throws IOException {
        try (InputStream inputStream = this.getClass().getResourceAsStream(JACOCO_PATH_2)) {
            CodeCoverage actual = jaCoCoParser.parseXml(inputStream);
            CodeCoverage expected = new CodeCoverageBuilder()
                .setMissed(CodeCoverageType.INSTRUCTION, 2813).setCovered(CodeCoverageType.INSTRUCTION, 175)
                .setMissed(CodeCoverageType.BRANCH, 202).setCovered(CodeCoverageType.BRANCH, 10)
                .setMissed(CodeCoverageType.LINE, 580).setCovered(CodeCoverageType.LINE, 37)
                .setMissed(CodeCoverageType.COMPLEXITY, 260).setCovered(CodeCoverageType.COMPLEXITY, 17)
                .setMissed(CodeCoverageType.METHOD, 159).setCovered(CodeCoverageType.METHOD, 12)
                .setMissed(CodeCoverageType.CLASS, 21).setCovered(CodeCoverageType.CLASS, 4)
                .build();

            Assert.assertEquals(expected, actual);
        }
    }
}
