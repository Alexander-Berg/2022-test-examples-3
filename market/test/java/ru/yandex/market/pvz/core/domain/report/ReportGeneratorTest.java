package ru.yandex.market.pvz.core.domain.report;

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
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.report.payload.NoSourceDataException;
import ru.yandex.market.pvz.core.domain.report.payload.creator.ReportPayloadCreatorSelector;
import ru.yandex.market.pvz.core.domain.report.payload.manager.MockPayloadManager;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.pvz.core.domain.report.payload.manager.MockPayloadManager.MOCK_PAYLOAD;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_ORGANIZATION_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CAPACITY;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReportGeneratorTest {

    private final TestPickupPointFactory pickupPointFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;

    private final ReportGenerator reportGenerator;
    private final ReportRepository reportRepository;

    @MockBean
    private ReportPayloadCreatorSelector selector;

    @Spy
    private MockPayloadManager payloadManager;

    @BeforeEach
    void selectManager() {
        doReturn(payloadManager).when(selector).select(PvzReportType.PAID_ORDER);
        doReturn(Optional.of(ReportSourceData.builder().orders(List.of()).build()))
                .when(payloadManager).getSourceData(any(LegalPartner.class), any(), any());
        doReturn(Optional.of(ReportSourceData.builder().orders(List.of()).build()))
                .when(payloadManager).getSourceData(any(PickupPoint.class), any(), any());
        doReturn(Optional.of(ReportSourceData.builder().orders(List.of()).build()))
                .when(payloadManager).getSourceData(any(), any());
    }

    @Test
    void generateGeneralReport() {
        LocalDate dateFrom = LocalDate.of(2020, 8, 24);
        LocalDate dateTo = LocalDate.of(2020, 8, 30);

        Report actual = reportGenerator.generate(PvzReportType.PAID_ORDER, dateFrom, dateTo);
        assertThat(actual.getId()).isNotNull();

        Optional<Report> generatedReport = reportRepository.findById(actual.getId());
        assertThat(generatedReport).isNotEmpty();
        assertThat(generatedReport.get().getName()).isEqualTo(MOCK_PAYLOAD.getName());
        assertThat(generatedReport.get().getFilename()).isEqualTo(MOCK_PAYLOAD.getFilename());
        assertThat(generatedReport.get().getPayload()).isEqualTo(MOCK_PAYLOAD.getContent());
        assertThat(generatedReport.get().getReportLegalPartner()).isNull();
        assertThat(generatedReport.get().getReportPickupPoint()).isNull();
    }

    @Test
    void noPayloadForGeneralReport() {
        LocalDate dateFrom = LocalDate.of(2020, 8, 24);
        LocalDate dateTo = LocalDate.of(2020, 8, 30);

        doReturn(Optional.empty()).when(payloadManager).getSourceData(dateFrom, dateTo);

        assertThatThrownBy(() -> reportGenerator.generate(PvzReportType.PAID_ORDER, dateFrom, dateTo))
                .isExactlyInstanceOf(NoSourceDataException.class);
    }

    @Test
    void generatePartnerReport() {
        LocalDate dateFrom = LocalDate.of(2020, 8, 24);
        LocalDate dateTo = LocalDate.of(2020, 8, 30);

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        Report actual = reportGenerator.generate(legalPartner, PvzReportType.PAID_ORDER, dateFrom, dateTo);

        assertThat(actual.getId()).isNotNull();
        Optional<Report> generatedReport = reportRepository.findById(actual.getId());
        assertThat(generatedReport).isNotEmpty();
        assertThat(generatedReport.get().getName()).isEqualTo(MOCK_PAYLOAD.getName());
        assertThat(generatedReport.get().getFilename()).isEqualTo(MOCK_PAYLOAD.getFilename());
        assertThat(generatedReport.get().getPayload()).isEqualTo(MOCK_PAYLOAD.getContent());
        assertThat(generatedReport.get().getReportLegalPartner()).isNotNull();
        assertThat(generatedReport.get().getReportLegalPartner().getLegalPartner().getOrganization().getName())
                .isEqualTo(DEFAULT_ORGANIZATION_NAME);
        assertThat(generatedReport.get().getReportPickupPoint()).isNull();
    }

    @Test
    void noPayloadForPartnerReport() {
        LocalDate dateFrom = LocalDate.of(2020, 8, 24);
        LocalDate dateTo = LocalDate.of(2020, 8, 30);

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        doReturn(Optional.empty()).when(payloadManager).getSourceData(legalPartner, dateFrom, dateTo);
        assertThatThrownBy(() -> reportGenerator.generate(
                legalPartner, PvzReportType.PAID_ORDER, dateFrom, dateTo))
                .isExactlyInstanceOf(NoSourceDataException.class);
    }

    @Test
    void generatePickupPointReport() {
        LocalDate dateFrom = LocalDate.of(2020, 8, 24);
        LocalDate dateTo = LocalDate.of(2020, 8, 30);

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        Report actual = reportGenerator.generate(pickupPoint, PvzReportType.PAID_ORDER, dateFrom, dateTo);

        assertThat(actual.getId()).isNotNull();
        Optional<Report> generatedReport = reportRepository.findById(actual.getId());
        assertThat(generatedReport).isNotEmpty();
        assertThat(generatedReport.get().getName()).isEqualTo(MOCK_PAYLOAD.getName());
        assertThat(generatedReport.get().getFilename()).isEqualTo(MOCK_PAYLOAD.getFilename());
        assertThat(generatedReport.get().getPayload()).isEqualTo(MOCK_PAYLOAD.getContent());
        assertThat(generatedReport.get().getReportPickupPoint()).isNotNull();
        assertThat(generatedReport.get().getReportPickupPoint().getPickupPoint().getCapacity())
                .isEqualTo(DEFAULT_CAPACITY);
        assertThat(generatedReport.get().getReportLegalPartner()).isNull();
    }

    @Test
    void noPayloadForPickupPointReport() {
        LocalDate dateFrom = LocalDate.of(2020, 8, 24);
        LocalDate dateTo = LocalDate.of(2020, 8, 30);

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        doReturn(Optional.empty()).when(payloadManager).getSourceData(pickupPoint, dateFrom, dateTo);
        assertThatThrownBy(() -> reportGenerator.generate(pickupPoint, PvzReportType.PAID_ORDER, dateFrom, dateTo))
                .isExactlyInstanceOf(NoSourceDataException.class);
    }

}
