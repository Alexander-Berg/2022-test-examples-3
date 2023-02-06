package ru.yandex.market.wms.reporter.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import ru.yandex.market.wms.common.spring.enums.ReportFormat;
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.reporter.config.ReporterTestConfig;
import ru.yandex.market.wms.reporter.dao.entity.ReportHeader;
import ru.yandex.market.wms.reporter.dao.entity.ReportParam;
import ru.yandex.market.wms.reporter.dao.entity.ReportTask;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        ReporterTestConfig.class
})
@ActiveProfiles(Profiles.TEST)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@AutoConfigureMockMvc
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {"reporterConnection"})
public class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Test
    public void reportsListContainsTestReport() {
        List<ReportHeader> reports = reportService.getReports();
        Optional<ReportHeader> report = reports.stream()
                .filter(r -> r.getCode().equals("TestReport1"))
                .findFirst();
        assertTrue(report.isPresent(), "Report TestOne not found");
    }

    @Test
    public void reportParametersTest() {
        List<ReportParam> params = reportService.getReportParams("TestReport1");
        assertEquals(4, params.size());
    }

    @Test
    public void reportSupportedFormatsTest() {
        List<ReportFormat> formats = reportService.getSupportedFormats("TestReport1");
        assertEquals(4, formats.size());
        assertTrue(formats.contains(ReportFormat.JSON));
        assertTrue(formats.contains(ReportFormat.HTML));
        assertTrue(formats.contains(ReportFormat.CSV));
        assertTrue(formats.contains(ReportFormat.ZPL));
    }

    @Test
    public void generateTest() throws ExecutionException, InterruptedException, TimeoutException {
        ReportTask task = reportService.generate("TestReport1", Collections.emptyList());
        task.getFuture().get(5, TimeUnit.SECONDS);
    }
}
