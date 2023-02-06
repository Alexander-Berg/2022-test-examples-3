package ru.yandex.market.pvz.core.domain.report;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.report.payload.creator.ReportPayloadCreatorSelector;
import ru.yandex.market.pvz.core.domain.report.payload.manager.MockPayloadManager;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReportCommandServiceTest {

    private final TestLegalPartnerFactory legalPartnerFactory;

    private final ReportGenerator reportGenerator;
    private final ReportCommandService reportCommandService;
    private final ReportRepository reportRepository;

    @MockBean
    private ReportPayloadCreatorSelector selector;

    @Spy
    private MockPayloadManager payloadManager;

    @BeforeEach
    void selectManager() {
        doReturn(payloadManager).when(selector).select(PvzReportType.PAID_ORDER);
        doReturn(payloadManager).when(selector).select(PvzReportType.EXECUTED_ORDERS_REPORT);
        doReturn(Optional.of(ReportSourceData.builder().orders(List.of()).build()))
                .when(payloadManager).getSourceData(any(LegalPartner.class), any(), any());
        doReturn(Optional.of(ReportSourceData.builder().orders(List.of()).build()))
                .when(payloadManager).getSourceData(any(LegalPartner.class), any(), any(), any());
    }

    @Test
    void testReportProperties() {
        LocalDate dateFrom = LocalDate.of(2020, 8, 24);
        LocalDate dateTo = LocalDate.of(2020, 8, 30);

        String paymentOrderNumber = "123";
        LocalDate paymentDate = LocalDate.of(2020, 9, 5);

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        Report report = reportGenerator.generate(legalPartner, PvzReportType.PAID_ORDER, dateFrom, dateTo);

        Report toDelete = reportGenerator.generate(legalPartner,
                PvzReportType.EXECUTED_ORDERS_REPORT,
                LocalDate.of(2020, 9, 1),
                LocalDate.of(2020, 9, 30)
        );

        Instant paidAt = Instant.ofEpochMilli(100);
        Instant uploadedToYtAt = Instant.ofEpochMilli(200);

        reportCommandService.markUploadedToYt(List.of(report.getId()), uploadedToYtAt);
        Report afterUpload = reportRepository.findByIdOrThrow(report.getId());
        assertThat(afterUpload.getProperties().isUploadToYtRequired()).isEqualTo(false);
        assertThat(afterUpload.getProperties().getUploadedToYtAt()).isNotNull();
        assertThat(reportRepository.existsById(toDelete.getId())).isTrue();

        reportCommandService.markPaid(report.getId(), paidAt, paymentOrderNumber, paymentDate);
        Report afterPaid = reportRepository.findByIdOrThrow(report.getId());
        assertThat(afterPaid.getProperties().getPayedAt()).isEqualTo(paidAt);
        assertThat(afterPaid.getProperties().getUploadedToYtAt()).isEqualTo(uploadedToYtAt);
        assertThat(afterPaid.getProperties().isUploadToYtRequired()).isEqualTo(true);
        assertThat(afterPaid.getProperties().getUploadedToYtAt()).isNotNull();
        assertThat(reportRepository.existsById(toDelete.getId())).isFalse();
    }
}
