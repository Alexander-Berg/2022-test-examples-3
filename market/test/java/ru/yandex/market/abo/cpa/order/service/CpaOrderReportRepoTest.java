package ru.yandex.market.abo.cpa.order.service;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.order.model.CpaOrderReport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.abo.cpa.order.export.ExportOrdersService.REPORT_ENCODING;

/**
 * @author agavrikov
 * @date 05.05.18
 */
public class CpaOrderReportRepoTest extends EmptyTest {

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    CpaOrderReportRepo cpaOrderReportRepo;

    @Test
    public void testRepo() {
        CpaOrderReport cpaOrderReport = initCpaOrderReport();
        cpaOrderReportRepo.save(cpaOrderReport);
        CpaOrderReport dbCpaOrderReport = cpaOrderReportRepo.findByIdOrNull(cpaOrderReport.getAuthorUid());
        assertEquals(cpaOrderReport, dbCpaOrderReport);

        cpaOrderReportRepo.deleteById(cpaOrderReport.getAuthorUid());
        assertNull(cpaOrderReportRepo.findByIdOrNull(cpaOrderReport.getAuthorUid()));
    }

    @Test
    public void testUpdateReport() throws Exception {
        CpaOrderReport cpaOrderReport = initCpaOrderReport();
        cpaOrderReportRepo.save(cpaOrderReport);

        String report = "report";
        byte[] content = report.getBytes(REPORT_ENCODING);
        cpaOrderReportRepo.updateReport(cpaOrderReport.getAuthorUid(), content, content.length);
        entityManager.clear();

        CpaOrderReport dbCpaOrderReport = cpaOrderReportRepo.findByIdOrNull(cpaOrderReport.getAuthorUid());
        assertEquals(100, dbCpaOrderReport.getCompleteness());
        assertArrayEquals(content, dbCpaOrderReport.getContent());
        assertEquals(content.length, dbCpaOrderReport.getContentSize());
    }

    @Test
    public void testDeleteHangedReports() throws Exception {
        cpaOrderReportRepo.saveAll(initCpaOrderReportList());

        String report = "report";
        byte[] content = report.getBytes(REPORT_ENCODING);
        cpaOrderReportRepo.updateReport(3L, content, content.length);

        assertEquals(3, cpaOrderReportRepo.findAll().size());
        cpaOrderReportRepo.deleteHangedReports();
        assertEquals(1, cpaOrderReportRepo.findAll().size());
    }

    private static CpaOrderReport initCpaOrderReport() {
        return new CpaOrderReport(1, "order.csv");
    }

    private static List<CpaOrderReport> initCpaOrderReportList() {
        List<CpaOrderReport> cpaOrderReportList = new ArrayList<>();
        cpaOrderReportList.add(new CpaOrderReport(1, "order.csv"));
        cpaOrderReportList.add(new CpaOrderReport(2, "order.csv"));
        cpaOrderReportList.add(new CpaOrderReport(3, "order.csv"));
        return cpaOrderReportList;
    }
}
