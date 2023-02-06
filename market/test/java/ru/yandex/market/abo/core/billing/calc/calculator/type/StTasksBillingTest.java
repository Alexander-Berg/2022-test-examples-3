package ru.yandex.market.abo.core.billing.calc.calculator.type;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.billing.rate.BillingColor;
import ru.yandex.market.abo.core.billing.report.BillingReport;
import ru.yandex.market.abo.core.billing.report.BillingReportService;
import ru.yandex.market.abo.core.billing.report.BillingResult;
import ru.yandex.market.abo.core.billing.report.ReportType;
import ru.yandex.market.abo.core.billing.report.st.BillingStTasksService;
import ru.yandex.market.abo.core.billing.report.st.StBillingTaskWrapper;
import ru.yandex.market.abo.core.startrek.billing.StartrekBillingTicket;
import ru.yandex.market.abo.core.startrek.billing.StartrekBillingTicketRepo;

import static java.util.stream.Collectors.toList;

/**
 * @author antipov93.
 * @date 24.07.18.
 */
public class StTasksBillingTest extends BillingReportCalculatorTest {

    @Autowired
    private BillingReportService billingReportService;
    @Autowired
    private BillingStTasksService billingStTasksService;
    @Autowired
    private StartrekBillingTicketRepo startrekBillingTicketRepo;

    @Override
    protected void populateData() {
        saveAssessorInfos();
        saveReport();
        IntStream.range(0, RND.nextInt(3) + 2).forEach(i -> saveTask());

        List<BillingResult> stTasks = billingReportService.load(reportId).getResultList();

        assessorIds.forEach(id -> stTasks.forEach(task -> {
                    List<StartrekBillingTicket> tickets = IntStream.range(0, RND.nextInt(6) + 5).mapToObj(i -> {
                        StartrekBillingTicket ticket = new StartrekBillingTicket();
                        ticket.setStartrekId(String.valueOf(RND.nextLong()));
                        ticket.setVersion(1);
                        ticket.setKey("AAAA-1111");
                        ticket.setResolvedAt(LocalDateTime.now());
                        ticket.setAssignee(String.valueOf(id));
                        ticket.setBillingResultId(task.getId());
                        return ticket;
                    }).collect(toList());
                    startrekBillingTicketRepo.saveAll(tickets);
                    addItemsToUser(id, task.getResultIndex(), tickets.size());
                })
        );
    }

    private void saveAssessorInfos() {
        assessorIds.forEach(id -> assessorService.saveAssessorInfo(id, String.valueOf(id), "", false));
    }

    private void saveReport() {
        BillingReport billingReport = new BillingReport();
        billingReport.setType(ReportType.STARTREK);
        billingReport.setActive(true);
        billingReport.setDescription("");
        billingReport.setName("");
        billingReportService.save(billingReport);
        this.reportId = billingReport.getId();
        flushAndClear();
    }

    private void saveTask() {
        StBillingTaskWrapper stTaskWrapper = new StBillingTaskWrapper();
        stTaskWrapper.setReportId(reportId);
        stTaskWrapper.setName("");

        stTaskWrapper.setLoadedAt(LocalDateTime.now());
        stTaskWrapper.setStFilterId(1L);

        stTaskWrapper.setExcelTitle("");
        stTaskWrapper.setUnit("");
        stTaskWrapper.setColor(BillingColor.GREEN);

        // this billing rate would be overridden in BillingReportCalculatorTest.createBillingRates
        stTaskWrapper.setRate(BigDecimal.ONE);
        stTaskWrapper.setRateApplySince(LocalDate.now().minusYears(1));
        billingStTasksService.createStTask(stTaskWrapper);
        flushAndClear();
    }
}
