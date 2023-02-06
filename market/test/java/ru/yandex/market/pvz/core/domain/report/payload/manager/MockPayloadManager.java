package ru.yandex.market.pvz.core.domain.report.payload.manager;

import java.time.LocalDate;
import java.util.Optional;

import lombok.Data;

import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.report.PvzReportType;
import ru.yandex.market.pvz.core.domain.report.ReportSourceData;
import ru.yandex.market.pvz.core.domain.report.payload.ReportPayload;
import ru.yandex.market.pvz.core.domain.report.post_processor.ReportPostProcessor;

public class MockPayloadManager extends EmptyReportPayloadManager<MockPayloadManager.MockPayload> {

    public static final MockPayload MOCK_PAYLOAD = new MockPayload();

    @Override
    public PvzReportType getReportType() {
        return null;
    }

    @Override
    public byte[] getBinaryContent(MockPayload payload) {
        return MOCK_PAYLOAD.getContent();
    }

    @Override
    public Optional<ReportPostProcessor> getPostProcessor() {
        return Optional.empty();
    }

    @Override
    public Optional<ReportSourceData> getSourceData(LegalPartner legalPartner, LocalDate dateFrom, LocalDate dateTo) {
        return Optional.of(ReportSourceData.builder()
                .legalPartner(legalPartner)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build());
    }

    @Override
    public Optional<ReportSourceData> getSourceData(PickupPoint pickupPoint, LocalDate dateFrom, LocalDate dateTo) {
        return Optional.of(ReportSourceData.builder()
                .pickupPoint(pickupPoint)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build());
    }

    @Override
    public Optional<ReportSourceData> getSourceData(LocalDate dateFrom, LocalDate dateTo) {
        return Optional.of(ReportSourceData.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build());
    }

    @Override
    public MockPayload create(ReportSourceData sourceData) {
        return MOCK_PAYLOAD;
    }

    @Data
    public static class MockPayload implements ReportPayload {

        @Override
        public String getName() {
            return "Отчет";
        }

        @Override
        public String getFilename() {
            return "Отчет.xlsx";
        }

        public byte[] getContent() {
            return new byte[]{1, 2, 3};
        }
    }
}

