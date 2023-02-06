package ru.yandex.market.rg.closure;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.rg.closure.PeriodClosureComparisonService.OebsComparisonRowData;
import ru.yandex.market.rg.closure.model.PeriodClosureComparisonYtSchema;
import ru.yandex.market.rg.closure.model.PeriodClosureIncomeComparisonYtSchema;
import ru.yandex.market.rg.closure.model.PeriodClosureIncomeOebsRow;
import ru.yandex.market.rg.closure.model.PeriodClosureOutcomeComparisonYtSchema;
import ru.yandex.market.rg.closure.model.PeriodClosureOutcomeOebsRow;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.core.supplier.PartnerContractType.INCOME;
import static ru.yandex.market.core.supplier.PartnerContractType.OUTCOME;
import static ru.yandex.market.rg.closure.model.PaymentReferType.COMPENSATION_FOR_LOST;
import static ru.yandex.market.rg.closure.model.PaymentReferType.DELIVERY_SUBSIDY;
import static ru.yandex.market.rg.closure.model.PaymentReferType.FEES;
import static ru.yandex.market.rg.closure.model.PaymentReferType.PAID;
import static ru.yandex.market.rg.closure.model.PaymentReferType.PAID_BY_CUSTOMER;
import static ru.yandex.market.rg.closure.model.PaymentReferType.PAYMENTS;
import static ru.yandex.market.rg.closure.model.PaymentReferType.PLUS;
import static ru.yandex.market.rg.closure.model.PaymentReferType.REFUNDS;
import static ru.yandex.market.rg.closure.model.PaymentReferType.SUBSIDY;
import static ru.yandex.market.rg.closure.model.PaymentReferType.WILL_BE_PAID;

public class PeriodClosureComparisonExecutorTest extends FunctionalTest {

    private static final long CONTRACT_ID = 2699440;

    private static final long NOT_IN_OEBS_CONTRACT_ID = 47634530;

    private static final LocalDate REPORT_MONTH_DATE = LocalDate.of(2022, Month.FEBRUARY, 1);

    @Autowired
    private EnvironmentService environmentService;

    private PeriodClosureComparisonExecutor executor;

    @BeforeEach
    void setUp() {
        PeriodClosureComparisonService comparisonService = Mockito.mock(PeriodClosureComparisonService.class);

        Mockito.when(comparisonService.getSubsidiesContract(any(), any())).thenReturn(
                Map.of(CONTRACT_ID,
                        Map.of(SUBSIDY, BigDecimal.valueOf(1200),
                                PLUS, BigDecimal.valueOf(249.9),
                                DELIVERY_SUBSIDY, BigDecimal.valueOf(102)),
                        NOT_IN_OEBS_CONTRACT_ID,
                        Map.of(SUBSIDY, BigDecimal.valueOf(100),
                                PLUS, BigDecimal.valueOf(0),
                                DELIVERY_SUBSIDY, BigDecimal.valueOf(0)))
        );

        Mockito.when(comparisonService.getPaymentsOnContract(any(), any())).thenReturn(
                Map.of(CONTRACT_ID,
                        Map.of(PAYMENTS, BigDecimal.valueOf(856),
                                REFUNDS, BigDecimal.valueOf(172.12),
                                FEES, BigDecimal.valueOf(2150),
                                WILL_BE_PAID, BigDecimal.valueOf(480),
                                PAID, BigDecimal.valueOf(5628),
                                PAID_BY_CUSTOMER, BigDecimal.valueOf(5234),
                                COMPENSATION_FOR_LOST, BigDecimal.valueOf(232))));

        Mockito.when(comparisonService.getClosuresOnContract(any(), eq(PeriodClosureOutcomeOebsRow.class)))
                .thenReturn(
                        Map.of(CONTRACT_ID,
                                List.of(
                                        new OebsComparisonRowData(SUBSIDY, BigDecimal.valueOf(1500)),
                                        new OebsComparisonRowData(PLUS, BigDecimal.valueOf(250)),
                                        new OebsComparisonRowData(DELIVERY_SUBSIDY, BigDecimal.valueOf(86)))));

        Mockito.when(comparisonService.getClosuresOnContract(any(), eq(PeriodClosureIncomeOebsRow.class)))
                .thenReturn(
                        Map.of(CONTRACT_ID,
                                List.of(
                                        new OebsComparisonRowData(PAYMENTS, BigDecimal.valueOf(856)),
                                        new OebsComparisonRowData(REFUNDS, BigDecimal.valueOf(124)),
                                        new OebsComparisonRowData(FEES, BigDecimal.valueOf(516)),
                                        new OebsComparisonRowData(WILL_BE_PAID, BigDecimal.valueOf(523)),
                                        new OebsComparisonRowData(PAID, BigDecimal.valueOf(5862)),
                                        new OebsComparisonRowData(PAID_BY_CUSTOMER, BigDecimal.valueOf(231)),
                                        new OebsComparisonRowData(COMPENSATION_FOR_LOST, BigDecimal.valueOf(150)))));

        executor = new PeriodClosureComparisonExecutor(
                Clock.fixed(Instant.parse("2022-02-17T10:00:00Z"), ZoneOffset.systemDefault()),
                comparisonService,
                environmentService
        );
    }

    @Test
    @DbUnitDataSet(before = "PeriodClosureComparisonServiceTest.before.csv")
    void job() {
        List<PeriodClosureComparisonYtSchema> outcome = executor.collectForPeriod(REPORT_MONTH_DATE, OUTCOME);
        PeriodClosureOutcomeComparisonYtSchema outcomeSchema =
                (PeriodClosureOutcomeComparisonYtSchema) outcome
                        .stream().filter(sc -> sc.getContractId() == CONTRACT_ID).findFirst().get();

        Assertions.assertEquals(120000L, outcomeSchema.getSubsidy());
        Assertions.assertEquals(30000L, outcomeSchema.getSubsidyDiff());
        Assertions.assertEquals(24990L, outcomeSchema.getPlus());
        Assertions.assertEquals(0L, outcomeSchema.getPlusDiff());
        Assertions.assertEquals(10200L, outcomeSchema.getDeliverySubsidy());
        Assertions.assertEquals(1600L, outcomeSchema.getDeliverySubsidyDiff());
        Assertions.assertEquals(0L, outcomeSchema.getVatRate());

        List<PeriodClosureComparisonYtSchema> income = executor.collectForPeriod(REPORT_MONTH_DATE, INCOME);
        PeriodClosureIncomeComparisonYtSchema incomeSchema =
                (PeriodClosureIncomeComparisonYtSchema) income
                        .stream().filter(sc -> sc.getContractId() == CONTRACT_ID).findFirst().get();

        Assertions.assertEquals(85600, incomeSchema.getPayments());
        Assertions.assertEquals(0, incomeSchema.getPaymentsDiff());
        Assertions.assertEquals(17212, incomeSchema.getRefunds());
        Assertions.assertEquals(4812, incomeSchema.getRefundsDiff());
        Assertions.assertEquals(215000, incomeSchema.getFees());
        Assertions.assertEquals(163400, incomeSchema.getFeesDiff());
        Assertions.assertEquals(48000, incomeSchema.getWillBePaid());
        Assertions.assertEquals(4300, incomeSchema.getWillBePaidDiff());
        Assertions.assertEquals(562800, incomeSchema.getPaid());
        Assertions.assertEquals(23400, incomeSchema.getPaidDiff());
        Assertions.assertEquals(523400, incomeSchema.getPaidByCustomer());
        Assertions.assertEquals(500300, incomeSchema.getPaidByCustomerDiff());
        Assertions.assertEquals(23200, incomeSchema.getCompensationForLost());
        Assertions.assertEquals(8200, incomeSchema.getCompensationForLostDiff());
    }
}
