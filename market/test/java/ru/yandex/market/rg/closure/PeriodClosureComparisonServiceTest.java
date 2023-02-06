package ru.yandex.market.rg.closure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.BankOrderInfoDao;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.rg.closure.PeriodClosureComparisonService.OebsComparisonRowData;
import ru.yandex.market.rg.closure.model.PaymentReferType;
import ru.yandex.market.rg.closure.model.PeriodClosureOutcomeOebsRow;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.rg.closure.model.PaymentReferType.DELIVERY_SUBSIDY;
import static ru.yandex.market.rg.closure.model.PaymentReferType.PAYMENTS;
import static ru.yandex.market.rg.closure.model.PaymentReferType.PLUS;
import static ru.yandex.market.rg.closure.model.PaymentReferType.REFUNDS;
import static ru.yandex.market.rg.closure.model.PaymentReferType.SUBSIDY;
import static ru.yandex.market.rg.closure.model.PaymentReferType.WILL_BE_PAID;

public class PeriodClosureComparisonServiceTest extends FunctionalTest {

    private static final long CONTRACT_ID = 2699440;

    private static final LocalDate REPORT_MONTH_DATE = LocalDate.of(2022, Month.FEBRUARY, 1);

    @Autowired
    private BankOrderInfoDao bankOrderInfoDao;

    @Autowired
    private PartnerContractService partnerContractService;

    private PeriodClosureComparisonService service;

    @BeforeEach
    void setUp() {
        PeriodClosureYtDao ytDao = Mockito.mock(PeriodClosureYtDao.class);

        Mockito.when(ytDao.readPeriodFromOebs(any(), any(), eq(PeriodClosureOutcomeOebsRow.class))).thenReturn(
                List.of(
                        new PeriodClosureOutcomeOebsRow()
                                .setContractId(CONTRACT_ID)
                                .setContractName("ОФ-674993")
                                .setDistributionSetId(17791L)
                                .setId(17114127L)
                                .setDistributionSetName("AR Синий Маркет (Спасибо) YMAR, руб")
                                .setPeriodStartDate(REPORT_MONTH_DATE)
                                .setPeriodEndDate(REPORT_MONTH_DATE.plusMonths(1))
                                .setRewardAmount(BigDecimal.valueOf(100))
                                .setRewardWithNds(BigDecimal.valueOf(100)),
                        new PeriodClosureOutcomeOebsRow()
                                .setContractId(CONTRACT_ID)
                                .setContractName("ОФ-674993")
                                .setDistributionSetId(15129L)
                                .setId(17114127L)
                                .setDistributionSetName("AR Синий Маркет (субсидии) YMAR, руб")
                                .setPeriodStartDate(REPORT_MONTH_DATE)
                                .setPeriodEndDate(REPORT_MONTH_DATE.plusMonths(1))
                                .setRewardAmount(BigDecimal.valueOf(1433))
                                .setRewardWithNds(BigDecimal.valueOf(1719.6)),
                        new PeriodClosureOutcomeOebsRow()
                                .setContractId(CONTRACT_ID)
                                .setContractName("ОФ-675720")
                                .setDistributionSetId(21231L)
                                .setId(17099401L)
                                .setDistributionSetName("AR Синий Маркет (Плюс 2.0) YMAR, руб")
                                .setPeriodStartDate(REPORT_MONTH_DATE)
                                .setPeriodEndDate(REPORT_MONTH_DATE.plusMonths(1))
                                .setRewardAmount(BigDecimal.valueOf(10003))
                                .setRewardWithNds(BigDecimal.valueOf(10003)),
                        new PeriodClosureOutcomeOebsRow()
                                .setContractId(CONTRACT_ID)
                                .setContractName("ОФ-675720")
                                .setDistributionSetId(17791L)
                                .setId(17099401L)
                                .setDistributionSetName("AR Синий Маркет (Спасибо) YMAR, руб")
                                .setPeriodStartDate(REPORT_MONTH_DATE)
                                .setPeriodEndDate(REPORT_MONTH_DATE.plusMonths(1))
                                .setRewardAmount(BigDecimal.valueOf(10003))
                                .setRewardWithNds(BigDecimal.valueOf(10003))
                )
        );
        Mockito.when(ytDao.isPeriodExists(any(), any())).thenReturn(true);

        service = new PeriodClosureComparisonService(
                bankOrderInfoDao,
                partnerContractService,
                ytDao
        );
    }

    @Test
    @DbUnitDataSet(before = "PeriodClosureComparisonServiceTest.before.csv")
    void getOraclePayments() {
        Map<Long, Map<PaymentReferType, BigDecimal>> subsidiesByContract =
                service.getSubsidiesContract(REPORT_MONTH_DATE, REPORT_MONTH_DATE.plusMonths(1));

        Map<PaymentReferType, BigDecimal> contractASubsidies = subsidiesByContract.get(CONTRACT_ID);
        Assertions.assertEquals(contractASubsidies.get(SUBSIDY).intValue(), 1824);
        Assertions.assertEquals(contractASubsidies.get(PLUS).intValue(), 311);
        Assertions.assertEquals(contractASubsidies.get(DELIVERY_SUBSIDY).intValue(), 2572);

        Map<PaymentReferType, BigDecimal> contractBSubsidies = subsidiesByContract.get(1499548L);
        Assertions.assertEquals(contractBSubsidies.get(SUBSIDY).intValue(), 2470);
        Assertions.assertEquals(contractBSubsidies.get(PLUS).intValue(), 5192);

        Map<Long, Map<PaymentReferType, BigDecimal>> paymentsByContract =
                service.getPaymentsOnContract(REPORT_MONTH_DATE, REPORT_MONTH_DATE.plusMonths(1));

        Map<PaymentReferType, BigDecimal> contractPayments = paymentsByContract.get(1499499L);
        Assertions.assertEquals(contractPayments.get(PAYMENTS).intValue(), 13909);
        Assertions.assertEquals(contractPayments.get(REFUNDS).intValue(), 1246);
        Assertions.assertEquals(contractPayments.get(WILL_BE_PAID).intValue(), 15155);
    }

    @Test
    void getSubsidiesClosures() {
        Map<Long, List<OebsComparisonRowData>> closuresOnContract =
                service.getClosuresOnContract(REPORT_MONTH_DATE, PeriodClosureOutcomeOebsRow.class);

        List<OebsComparisonRowData> contractClosures = closuresOnContract.get(CONTRACT_ID);
        Assertions.assertEquals(closureByType(contractClosures, SUBSIDY).intValue(), 1719);
        Assertions.assertEquals(closureByType(contractClosures, PLUS).intValue(), 10003);
    }

    private BigDecimal closureByType(List<OebsComparisonRowData> contractClosures, PaymentReferType type) {
        return contractClosures.stream()
                .filter(c -> c.getReferType() == type)
                .map(OebsComparisonRowData::getAmount)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }
}
