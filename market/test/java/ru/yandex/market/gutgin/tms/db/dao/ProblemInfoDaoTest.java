package ru.yandex.market.gutgin.tms.db.dao;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.base.BaseDbGutGinTest;
import ru.yandex.market.gutgin.tms.engine.problem.ProblemEntry;
import ru.yandex.market.gutgin.tms.engine.problem.ProblemInfo;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.ir.http.ProtocolMessage;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;

import java.sql.Timestamp;

public class ProblemInfoDaoTest extends BaseDbGutGinTest {

    private static final Timestamp FIXED_TIMESTAMP = Timestamp.valueOf("2019-03-19 10:54:27");

    private ProblemInfoDao problemInfoDao;

    @Before
    public void init() {
        problemInfoDao = new ProblemInfoDao(configuration);
    }

    @Test
    public void testRead() {
        final ProblemInfo orig = ProblemInfo.builder()
            .setDescription("description")
            .addThrowable(new RuntimeException("runtimeexception"))
            .addThrowable(new IllegalStateException("illegalstateexception"))
            .addExternalData(
                "some service",
                PartnerContent.ProcessRequest
                    .newBuilder()
                    .setProcessRequestId(12345)
                    .build(),
                ProtocolMessage.Message
                    .newBuilder()
                    .setCode("code")
                    .setTemplate("template")
                    .setParams("params")
                    .build()
            )
            .addExternalData(
                "some service 2",
                PartnerContent.GetFileTemplateRequest
                    .newBuilder()
                    .addShopSkuId("123")
                    .build(),
                null
            )
            .build();
        final Long problemId = problemInfoDao.insert(orig);
        final ProblemInfo target = problemInfoDao.read(problemId);

        Assertions.assertThat(target.getDescription())
            .isEqualTo(orig.getDescription());
        Assertions.assertThat(target.getThrowable())
            .containsAll(orig.getThrowable());
        Assertions.assertThat(target.getExternalData())
            .containsAll(orig.getExternalData());

    }

    @Test
    public void testInsertAndReadProblemInfoWithEmptyThrowableMessage() {
        ProblemInfo expected = ProblemInfo.builder()
            .setDescription("description 2")
            .addThrowable(new RuntimeException())
            .addThrowable(new IllegalStateException())
            .build();

        Long problemId = problemInfoDao.insert(expected);
        ProblemInfo actual = problemInfoDao.read(problemId);

        Assertions.assertThat(actual.getDescription())
            .isEqualTo(expected.getDescription());
        Assertions.assertThat(actual.getThrowable())
            .containsAll(expected.getThrowable());
    }

    @Test
    public void testInsertAndCheckTimestamp() {
        ProblemInfo expected = ProblemInfo.builder()
            .setTs(FIXED_TIMESTAMP)
            .setDescription("description 2")
            .addThrowable(new RuntimeException())
            .addThrowable(new IllegalStateException())
            .build();

        problemInfoDao.insert(expected);
        ProblemEntry problemEntry = problemInfoDao.getNotAnalyzedProblems(1).stream()
            .limit(1)
            .findAny()
            .orElseThrow(IllegalStateException::new);

        Assertions.assertThat(problemEntry.getProblemTs()).isEqualTo(FIXED_TIMESTAMP);
    }

    @Test
    public void testInsertAndReadGood() {
        ProblemInfo expected = ProblemInfo.builder()
            .setTs(FIXED_TIMESTAMP)
            .setDescription("description 2")
            .addThrowable(new RuntimeException())
            .addThrowable(new IllegalStateException())
            .setPipelineType(PipelineType.GOOD_CONTENT_SINGLE_XLS)
            .build();

        Long expectedId = problemInfoDao.insert(expected);
        ProblemEntry problemEntry = problemInfoDao
            .getNotAnalyzedProblemsWithEqualsPipeType(1, PipelineType.GOOD_CONTENT_SINGLE_XLS)
            .stream()
            .limit(1)
            .findAny()
            .orElseThrow(IllegalStateException::new);
        Assertions.assertThat(problemEntry.getProblemId()).isEqualTo(expectedId);
    }

    @Test
    public void testInsertAndReadNotGood() {
        ProblemInfo expected = ProblemInfo.builder()
            .setTs(FIXED_TIMESTAMP)
            .setDescription("description 2")
            .addThrowable(new RuntimeException())
            .addThrowable(new IllegalStateException())
            .setPipelineType(PipelineType.SINGLE_XLS)
            .build();

        Long expectedId = problemInfoDao.insert(expected);
        ProblemEntry problemEntry = problemInfoDao
            .getNotAnalyzedProblemsWithNotEqualsPipeType(1, PipelineType.GOOD_CONTENT_SINGLE_XLS)
            .stream()
            .limit(1)
            .findAny()
            .orElseThrow(IllegalStateException::new);
        Assertions.assertThat(problemEntry.getProblemId()).isEqualTo(expectedId);
    }

}