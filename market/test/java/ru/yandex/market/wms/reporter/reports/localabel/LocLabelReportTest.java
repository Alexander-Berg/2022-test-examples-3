package ru.yandex.market.wms.reporter.reports.localabel;

import java.util.List;

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

import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.reporter.config.ReporterTestConfig;
import ru.yandex.market.wms.reporter.reports.loclabel.LocLabelReport;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
public class LocLabelReportTest {

    @Autowired
    LocLabelReport locLabelReport;

    @Test
    public void getLocationsTest() {
        String fromLoc1 = "R01-001-S1";
        String toLoc1 = "R02-003-S2";
        int limit = 500;
        List<String> ids1 = locLabelReport.getLocations(fromLoc1, toLoc1, limit);
        ids1.forEach(System.out::println);

        String fromLoc2 = "C1-01-01A1";
        String toLoc2 = "C1-01-02F3";
        List<String> ids2 = locLabelReport.getLocations(fromLoc2, toLoc2, limit);
        ids2.forEach(System.out::println);

        String fromLoc3 = "BUF-A01";
        String toLoc3 = "BUF-B03";
        List<String> ids3 = locLabelReport.getLocations(fromLoc3, toLoc3, limit);
        ids3.forEach(System.out::println);

        assertEquals(ids1.size(), 12);
        assertEquals(ids2.size(), 36);
        assertEquals(ids3.size(), 6);
    }

}
