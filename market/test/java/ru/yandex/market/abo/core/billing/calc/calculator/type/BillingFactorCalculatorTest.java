package ru.yandex.market.abo.core.billing.calc.calculator.type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.billing.calc.row.BillingReportRow;
import ru.yandex.market.abo.core.billing.factor.BillingFactorManager;
import ru.yandex.market.abo.core.billing.factor.BillingFactorService;
import ru.yandex.market.abo.core.billing.factor.model.BillingFactorExecutionHistory;
import ru.yandex.market.abo.core.billing.factor.model.BillingFactorRuleDetails;
import ru.yandex.market.abo.core.billing.factor.model.BillingFactorRuleRequest;
import ru.yandex.market.abo.core.billing.factor.model.JsonBillingFactorExecutionHistoryCondition;
import ru.yandex.market.abo.core.billing.report.BillingReport;
import ru.yandex.market.abo.core.billing.report.BillingReportService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author komarovns
 * @date 28.02.19
 */
class BillingFactorCalculatorTest extends EmptyTest {
    private static final long FACTOR_REPORT_ID = 14;
    private static final long USER_A = 0;
    private static final long USER_B = 1;

    @Autowired
    private BillingFactorService billingFactorService;
    @Autowired
    private BillingFactorManager billingFactorManager;
    @Autowired
    private BillingReportService billingReportService;
    @Autowired
    private BillingFactorCalculator billingFactorCalculator;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Кейс:
     * Первый юзер попал под два правила, первое входит в период биллинга, второе нет
     * Второй юзер попал под одно правило, но оно успело вычислиться дважды за период биллинга
     */
    @Test
    void calculateTest() {
        BillingReport report = billingReportService.load(FACTOR_REPORT_ID);

        createRuleWithHistory(USER_A, 1);
        createRuleWithHistory(USER_A, 100);
        createRuleWithHistory(USER_B, 1, 2);

        List<BillingReportRow> rows = billingFactorCalculator
                .calculate(report, LocalDate.now().minusDays(2), LocalDate.now().plusDays(1));

        assertEquals(BigDecimal.ONE, rows.get(0).getSum());
        assertEquals(BigDecimal.ONE.add(BigDecimal.ONE), rows.get(1).getSum());
    }

    private BillingFactorRuleDetails createRuleWithHistory(long yaUid, int... executionDaysBefore) {
        BillingFactorRuleRequest request = new BillingFactorRuleRequest();
        request.setReportId(4);
        request.setFactorUnit(BillingFactorRuleDetails.FactorUnit.PERCENT);
        request.setFactorValue(BigDecimal.valueOf(10));
        request.setPeriod(1);
        request.setTitle("");
        request.setConditions(new BillingFactorRuleRequest.FactorCondition[0]);
        BillingFactorRuleDetails details = billingFactorManager.saveRuleWithDetails(request);
        for (int day : executionDaysBefore) {
            createHistory(yaUid, BigDecimal.ONE, LocalDateTime.now().minusDays(day), details);
        }
        entityManager.flush();
        entityManager.clear();
        return details;
    }

    private void createHistory(long yaUid, BigDecimal amount, LocalDateTime executionDate,
                               BillingFactorRuleDetails details) {
        BillingFactorExecutionHistory history = new BillingFactorExecutionHistory(
                yaUid, details, amount, new JsonBillingFactorExecutionHistoryCondition[0]
        );
        history.setExecutionDate(executionDate);
        billingFactorService.save(Collections.singletonList(history));
    }
}