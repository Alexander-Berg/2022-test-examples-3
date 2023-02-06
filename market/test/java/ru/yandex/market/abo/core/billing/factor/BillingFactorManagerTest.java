package ru.yandex.market.abo.core.billing.factor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.billing.factor.model.BillingFactorExecutionHistory;
import ru.yandex.market.abo.core.billing.factor.model.BillingFactorRule;
import ru.yandex.market.abo.core.billing.factor.model.BillingFactorRuleCondition;
import ru.yandex.market.abo.core.billing.factor.model.BillingFactorRuleDetails;
import ru.yandex.market.abo.core.billing.factor.model.BillingFactorRuleRequest;
import ru.yandex.market.abo.core.billing.rate.BillingItem;
import ru.yandex.market.abo.core.billing.report.BillingReport;
import ru.yandex.market.abo.core.billing.report.BillingReportService;
import ru.yandex.market.abo.core.billing.report.BillingResult;
import ru.yandex.market.abo.core.billing.report.BillingResultService;
import ru.yandex.market.abo.core.billing.report.ReportType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 25.02.19
 */
class BillingFactorManagerTest extends EmptyTestWithTransactionTemplate {
    private static final long RULE_ID = -1;
    private static final long REPORT_ID = 1;
    private static final long RESULT_ID = 2;
    private static final String RULE_TITLE = "some title";

    @InjectMocks
    BillingFactorManager billingFactorManager;

    @Mock
    BillingFactorService billingFactorService;
    @Mock
    BillingReportService billingReportService;
    @Mock
    BillingResultService billingResultService;
    @Mock
    BillingReport billingReport;
    @Mock
    BillingResult billingResult;
    @Mock
    BillingFactorRuleDetails activeDetails;
    @Mock
    BillingFactorRule activeRule;
    @Mock
    BillingReport factorReport;
    @Mock
    BillingResult factorResult;
    @Mock
    BillingItem factorItem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(billingFactorService.loadRule(anyLong())).thenReturn(new BillingFactorRule(null));
        when(billingResultService.load(RESULT_ID)).thenReturn(billingResult);
        when(billingReportService.load(ReportType.FACTOR)).thenReturn(Collections.singletonList(factorReport));
        when(billingFactorService.loadRule(anyLong())).thenReturn(activeRule);
        when(activeRule.getResult()).thenReturn(factorResult);
        when(factorResult.getItem()).thenReturn(factorItem);
    }

    @ParameterizedTest
    @CsvSource({", 0, 1", "-1, 1, 0"})
    void saveRuleTest(Long ruleId, int loadRuleTimes, int loadReportTimes) {

        billingFactorManager.saveRuleWithDetails(createRequest(ruleId));

        verify(billingFactorService, times(loadRuleTimes)).loadRule(anyLong());
        verify(billingReportService, times(loadReportTimes)).load(REPORT_ID);

        ArgumentCaptor<BillingFactorRule> newRule = ArgumentCaptor.forClass(BillingFactorRule.class);
        if (ruleId == null) {
            verify(billingFactorService).save(newRule.capture());
            assertEquals(RULE_TITLE, newRule.getValue().getTitle());
        } else {
            verify(billingFactorService).save(activeRule);
            verify(activeRule).setTitle(RULE_TITLE);
        }
    }

    @Test
    void saveNewRuleTest() {
        billingFactorManager.saveRuleWithDetails(createRequest(null));

        verify(billingReportService).load(REPORT_ID);
        ArgumentCaptor<BillingFactorRule> newRule = ArgumentCaptor.forClass(BillingFactorRule.class);
        verify(billingFactorService).save(newRule.capture());
        assertEquals(RULE_TITLE, newRule.getValue().getTitle());
        assertEquals(RULE_TITLE, newRule.getValue().getResult().getTitle());
        assertEquals(RULE_TITLE, newRule.getValue().getResult().getItem().getName());
    }

    @Test
    void updateExistingRuleTest() {
        billingFactorManager.saveRuleWithDetails(createRequest(RULE_ID));

        verify(billingFactorService).loadRule(RULE_ID);
        verify(billingFactorService).save(activeRule);
        verify(activeRule).setTitle(RULE_TITLE);
        verify(factorResult).setTitle(RULE_TITLE);
        verify(factorItem).setName(RULE_TITLE);
    }

    @Test
    void updateDetailsTest() {
        when(billingFactorService.loadActiveDetails(any())).thenReturn(activeDetails);

        billingFactorManager.saveRuleWithDetails(createRequest(RULE_ID));

        verify(activeDetails).setDeletionTime(any());
        verify(billingFactorService).save(activeDetails);
    }

    @Test
    void loadDetailsForCalculateTest_maxExecutionDate() {
        List<BillingFactorRuleDetails> details = Arrays.asList(
                createDetails(1, 1, 1),
                createDetails(2, 2, 14),
                createDetails(3, 3, 3)
        );
        List<BillingFactorExecutionHistory> histories = details.stream()
                .map(det -> {
                    BillingFactorExecutionHistory history = new BillingFactorExecutionHistory(0, det, null, null);
                    history.setExecutionDate(LocalDateTime.now());
                    return history;
                })
                .collect(Collectors.toList());
        histories.get(0).setExecutionDate(LocalDateTime.now().minusDays(5)); //заодно проверим фильтрацию в конце

        when(billingFactorService.loadActiveRulesDetails()).thenReturn(details);
        when(billingFactorService.loadHistory(any(List.class), any())).thenReturn(histories);

        assertEquals(
                Collections.singletonList(details.get(0)),
                billingFactorManager.loadDetailsForCalculate()
        );

        ArgumentCaptor<LocalDateTime> historyFrom = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(billingFactorService).loadHistory(any(), historyFrom.capture());
        assertEquals(14, ChronoUnit.DAYS.between(historyFrom.getValue(), LocalDateTime.now()));
    }

    @Test
    void loadDetailsForCalculateTest_firstCalculate() {
        when(billingFactorService.loadActiveRulesDetails())
                .thenReturn(Collections.singletonList(createDetails(1, 1, 1)));
        when(billingFactorService.loadHistory(any(List.class), any())).thenReturn(Collections.emptyList());
        when(billingFactorService.loadRuleCreationTime(anyLong())).thenReturn(LocalDateTime.now());

        assertEquals(Collections.emptyList(), billingFactorManager.loadDetailsForCalculate());

        verify(billingFactorService).loadRuleCreationTime(anyLong());
    }

    private static BillingFactorRuleDetails createDetails(long id, long ruleId, int period) {
        BillingFactorRule rule = new BillingFactorRule(null);
        rule.setId(ruleId);
        BillingFactorRuleDetails details = new BillingFactorRuleDetails();
        details.setRule(rule);
        details.setPeriod(period);
        details.setId(id);
        return details;
    }

    private static BillingFactorRuleRequest.FactorCondition[] createConditions() {
        BillingFactorRuleRequest.FactorCondition condition = new BillingFactorRuleRequest.FactorCondition();
        condition.setSign(BillingFactorRuleCondition.Sign.GREATER);
        condition.setResultId(RESULT_ID);
        return new BillingFactorRuleRequest.FactorCondition[]{condition};
    }

    private static BillingFactorRuleRequest createRequest(Long ruleId) {
        BillingFactorRuleRequest request = new BillingFactorRuleRequest();
        request.setRuleId(ruleId);
        request.setReportId(REPORT_ID);
        request.setTitle(RULE_TITLE);
        request.setConditions(createConditions());
        return request;
    }
}
