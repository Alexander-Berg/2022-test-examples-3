package ru.yandex.market.fintech.banksint.job.installment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.InstallmentResourceMapper;
import ru.yandex.market.fintech.banksint.service.mds.MdsS3Service;

class InstallmentGroupResourceValidationJobTest extends FunctionalTest {
    @Autowired
    InstallmentResourceValidationResubmissionJob installmentGroupResourceValidationJob;

    @Autowired
    InstallmentResourceMapper installmentResourceMapper;
    @Autowired
    MdsS3Service mdsS3Service;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("InstallmentGroupResourceValidationJob.sql"));

        Mockito.doAnswer(new MdsS3ServiceDownloadFileMockitoAnswer("test-file3.xlsm"))
                .when(mdsS3Service)
                .downloadFileWithUrl(Mockito.eq("url3"), Mockito.any());
        Mockito.doAnswer(new MdsS3ServiceDownloadFileMockitoAnswer("test-file4.xlsm"))
                .when(mdsS3Service)
                .downloadFileWithUrl(Mockito.eq("url4"), Mockito.any());
    }

    @Ignore
    @Test
    void mainJobFunctions() {
        JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        installmentGroupResourceValidationJob.doJob(mockContext);
    }

    @AllArgsConstructor
    public static class MdsS3ServiceDownloadFileMockitoAnswer implements Answer<Void> {
        private final String resourceFileName;

        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            OutputStream os = invocation.getArgument(1);
            try (InputStream is = this.getClass().getResourceAsStream(resourceFileName)) {
                IOUtils.copy(is, os);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

}
