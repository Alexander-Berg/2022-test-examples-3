package ru.yandex.market.pvz.internal.domain.report;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.report.PvzReportType;
import ru.yandex.market.pvz.core.domain.report.ReportGenerator;
import ru.yandex.market.pvz.core.domain.report.ReportTarget;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.manual.dto.ManualGenerateReportDto;
import ru.yandex.market.pvz.internal.controller.manual.mapper.ManualReportDtoMapper;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReportManualServiceTest {

    private static final PvzReportType REPORT_TYPE = PvzReportType.PAID_ORDER;
    private static final LocalDate DATE_FROM = LocalDate.of(2020, 9, 18);
    private static final LocalDate DATE_TO = LocalDate.of(2020, 10, 18);

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;

    @MockBean
    private ReportGenerator reportGenerator;

    @MockBean
    private ManualReportDtoMapper dtoMapper;

    private final ReportManualService reportManualService;

    @Test
    void generateGeneral() {
        ManualGenerateReportDto reportDto = ManualGenerateReportDto.builder()
                .reportType(REPORT_TYPE)
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .target(ReportTarget.GENERAL)
                .build();

        reportManualService.generate(reportDto);

        verify(reportGenerator, times(1)).generate(REPORT_TYPE, DATE_FROM, DATE_TO, true);
    }

    @Test
    void generatePartnerReport() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        ManualGenerateReportDto reportDto = ManualGenerateReportDto.builder()
                .reportType(REPORT_TYPE)
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .target(ReportTarget.PARTNER)
                .legalPartnerId(legalPartner.getId())
                .build();

        reportManualService.generate(reportDto);

        verify(reportGenerator, times(1)).generate(legalPartner.getId(), REPORT_TYPE, DATE_FROM, DATE_TO, true);
    }

    @Test
    void partnerIdNotProvided() {
        ManualGenerateReportDto reportDto = ManualGenerateReportDto.builder()
                .reportType(REPORT_TYPE)
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .target(ReportTarget.PARTNER)
                .build();

        assertThatThrownBy(() -> reportManualService.generate(reportDto))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void generatePickupPointReport() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        ManualGenerateReportDto reportDto = ManualGenerateReportDto.builder()
                .reportType(REPORT_TYPE)
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .target(ReportTarget.PICKUP_POINT)
                .pvzMarketId(pickupPoint.getPvzMarketId())
                .build();

        reportManualService.generate(reportDto);

        verify(reportGenerator, times(1)).generate(pickupPoint, REPORT_TYPE, DATE_FROM, DATE_TO, true);
    }

    @Test
    void pickupPointIdNotProvided() {
        ManualGenerateReportDto reportDto = ManualGenerateReportDto.builder()
                .reportType(REPORT_TYPE)
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .target(ReportTarget.PICKUP_POINT)
                .build();

        assertThatThrownBy(() -> reportManualService.generate(reportDto))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void pickupPointNotFound() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        ManualGenerateReportDto reportDto = ManualGenerateReportDto.builder()
                .reportType(REPORT_TYPE)
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .target(ReportTarget.PICKUP_POINT)
                .pvzMarketId(pickupPoint.getPvzMarketId() + 1)
                .build();

        assertThatThrownBy(() -> reportManualService.generate(reportDto))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }
}
