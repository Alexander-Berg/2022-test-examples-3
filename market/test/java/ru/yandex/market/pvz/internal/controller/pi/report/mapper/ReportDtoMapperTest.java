package ru.yandex.market.pvz.internal.controller.pi.report.mapper;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceipt;
import ru.yandex.market.pvz.core.domain.report.PvzReportType;
import ru.yandex.market.pvz.core.domain.report.ReportViewForPeriod;
import ru.yandex.market.pvz.core.domain.report.properties.OebsMatchingStatus;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOebsReceiptFactory;
import ru.yandex.market.pvz.internal.controller.pi.report.dto.ReportActionDto;
import ru.yandex.market.pvz.internal.controller.pi.report.dto.ReportActionType;
import ru.yandex.market.pvz.internal.controller.pi.report.dto.ReportPageDto;
import ru.yandex.market.pvz.internal.controller.pi.report.spec.ReportMatchingStatusDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@Import(ReportDtoMapper.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReportDtoMapperTest {

    private static final long ID = 1;
    private static final String NAME = "report name";
    private static final LocalDate PAYMENT_DATE = LocalDate.of(2020, 11, 3);

    private final TestOebsReceiptFactory oebsReceiptFactory;
    private final ReportDtoMapper reportDtoMapper;
    private final TestableClock clock;

    @Test
    void testOkMapping() {
        ReportViewForPeriod report = buildReport(
                LocalDate.of(2020, 11, 5),
                PvzReportType.PAID_ORDER, OebsMatchingStatus.OK);

        ReportPageDto reportPageDto = reportDtoMapper.map(report);

        assertThat(reportPageDto.getId()).isEqualTo(report.getId());
        assertThat(reportPageDto.getName()).isEqualTo(report.getReportName());
        assertThat(reportPageDto.getIsPaid()).isTrue();
        assertThat(reportPageDto.getPaymentStatus()).isEqualTo(ReportMatchingStatusDto.OK);

    }

    @Test
    void testMapNotPayableReport() {
        ReportViewForPeriod report = buildReport(
                LocalDate.of(2020, 11, 5),
                PvzReportType.EXECUTED_ORDERS_REPORT, OebsMatchingStatus.NOT_FOUND);
        ReportPageDto reportPageDto = reportDtoMapper.map(report);

        assertThat(reportPageDto.getId()).isEqualTo(report.getId());
        assertThat(reportPageDto.getName()).isEqualTo(report.getReportName());
        assertThat(reportPageDto.getIsPaid()).isNull();
        assertThat(reportPageDto.getPaymentStatus()).isNull();
    }

    @Test
    void testNotFoundMapping() {
        ReportViewForPeriod report = buildReport(
                LocalDate.of(2020, 11, 5), PvzReportType.PAID_ORDER,
                OebsMatchingStatus.OK, OebsMatchingStatus.SUM_MISMATCH, OebsMatchingStatus.NOT_FOUND);

        createReceiptForDate(LocalDate.of(2020, 11, 6));
        setDate(LocalDate.of(2020, 11, 14));
        ReportPageDto reportPageDto = reportDtoMapper.map(report);

        assertThat(reportPageDto.getId()).isEqualTo(report.getId());
        assertThat(reportPageDto.getName()).isEqualTo(report.getReportName());
        assertThat(reportPageDto.getIsPaid()).isTrue();
        assertThat(reportPageDto.getPaymentStatus()).isEqualTo(ReportMatchingStatusDto.NOT_FOUND);
    }

    @Test
    void testCheckingBecauseOfTimeout() {
        ReportViewForPeriod report = buildReport(
                LocalDate.of(2020, 11, 5), PvzReportType.PAID_ORDER,
                OebsMatchingStatus.OK, OebsMatchingStatus.SUM_MISMATCH, OebsMatchingStatus.NOT_FOUND);

        createReceiptForDate(LocalDate.of(2020, 11, 6));
        setDate(LocalDate.of(2020, 11, 12));
        ReportPageDto reportPageDto = reportDtoMapper.map(report);

        assertThat(reportPageDto.getId()).isEqualTo(report.getId());
        assertThat(reportPageDto.getName()).isEqualTo(report.getReportName());
        assertThat(reportPageDto.getIsPaid()).isTrue();
        assertThat(reportPageDto.getPaymentStatus()).isEqualTo(ReportMatchingStatusDto.CHECKING);
    }

    @Test
    void testCheckingBecauseOfNoPaymentsForDate() {
        ReportViewForPeriod report = buildReport(
                LocalDate.of(2020, 11, 5), PvzReportType.PAID_ORDER,
                OebsMatchingStatus.OK, OebsMatchingStatus.SUM_MISMATCH, OebsMatchingStatus.NOT_FOUND);

        createReceiptForDate(LocalDate.of(2020, 11, 5));
        ReportPageDto reportPageDto = reportDtoMapper.map(report);

        assertThat(reportPageDto.getId()).isEqualTo(report.getId());
        assertThat(reportPageDto.getName()).isEqualTo(report.getReportName());
        assertThat(reportPageDto.getIsPaid()).isTrue();
        assertThat(reportPageDto.getPaymentStatus()).isEqualTo(ReportMatchingStatusDto.CHECKING);
    }

    @Test
    void testSumMismatchMapping() {
        ReportViewForPeriod report = buildReport(
                LocalDate.of(2020, 11, 5),
                PvzReportType.PAID_ORDER, OebsMatchingStatus.SUM_MISMATCH);
        ReportPageDto reportPageDto = reportDtoMapper.map(report);

        assertThat(reportPageDto.getId()).isEqualTo(report.getId());
        assertThat(reportPageDto.getName()).isEqualTo(report.getReportName());
        assertThat(reportPageDto.getIsPaid()).isTrue();
        assertThat(reportPageDto.getPaymentStatus()).isEqualTo(ReportMatchingStatusDto.SUM_MISMATCH);
    }

    @Test
    void testEditPaymentAction() {
        ReportViewForPeriod report = buildReport(
                LocalDate.of(2020, 11, 5),
                PvzReportType.PAID_ORDER, OebsMatchingStatus.SUM_MISMATCH);
        ReportPageDto reportPageDto = reportDtoMapper.map(report);

        assertThat(reportPageDto.getActions()).isEqualTo(List.of(
            new ReportActionDto(ReportActionType.EDIT_REPORT)
        ));
    }

    @ParameterizedTest
    @MethodSource("paymentOrderNumberSource")
    void testPaymentOrderNumber(List<String> paymentOrderNumbers) {
        ReportViewForPeriod report = ReportViewForPeriod.builder()
                .id(ID)
                .type(PvzReportType.PAID_ORDER)
                .reportName(NAME)
                .isPaid(true)
                .paymentOrderNumbers(paymentOrderNumbers)
                .lastPaymentDate(LocalDate.now())
                .lastPaymentDate(PAYMENT_DATE)
                .oebsMatchingStatuses(List.of(OebsMatchingStatus.OK))
                .lastPaymentDate(LocalDate.now())
                .build();

        ReportPageDto reportPageDto = reportDtoMapper.map(report);

        assertThat(reportPageDto.getPaymentOrderNumber()).isEqualTo(
                paymentOrderNumbers.isEmpty() ? null : paymentOrderNumbers.get(0)
        );
    }

    static List<List<String>> paymentOrderNumberSource() {
        return List.of(
            List.of(),
            List.of("41"),
            List.of("41", "42", "43")
        );
    }

    private ReportViewForPeriod buildReport(LocalDate payedAt, PvzReportType type, OebsMatchingStatus... statuses) {
        return ReportViewForPeriod.builder()
                .id(ID)
                .type(type)
                .reportName(NAME)
                .isPaid(true)
                .lastPaymentDate(LocalDate.now())
                .lastPaymentDate(PAYMENT_DATE)
                .oebsMatchingStatuses(List.of(statuses))
                .lastPaymentDate(payedAt)
                .build();
    }

    private OebsReceipt createReceiptForDate(LocalDate date) {
        return oebsReceiptFactory.create(TestOebsReceiptFactory.OebsReceiptTestParams.builder()
                .paymentOrderDate(date)
                .build());
    }

    private void setDate(LocalDate now) {
        clock.setFixed(now.atStartOfDay().toInstant(DateTimeUtil.DEFAULT_ZONE_ID), clock.getZone());
    }

}
