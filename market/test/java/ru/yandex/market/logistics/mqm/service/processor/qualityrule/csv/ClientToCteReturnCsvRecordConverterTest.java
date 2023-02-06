package ru.yandex.market.logistics.mqm.service.processor.qualityrule.csv;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.mqm.entity.PlanFact;
import ru.yandex.market.logistics.mqm.entity.additionaldata.ReturnClientToCtePlanFactAdditionalData;

import static org.assertj.core.api.Assertions.assertThat;

class ClientToCteReturnCsvRecordConverterTest {

    @Test
    void convert() {

        List<ReturnClientToCtePlanFactAdditionalData.ReturnItem> items = List.of(
            new ReturnClientToCtePlanFactAdditionalData.ReturnItem(1000L, 1, 0),
            new ReturnClientToCtePlanFactAdditionalData.ReturnItem(1001L, 3, 2),
            new ReturnClientToCtePlanFactAdditionalData.ReturnItem(1002L, 1, 1),
            new ReturnClientToCtePlanFactAdditionalData.ReturnItem(1003L, 3, 0)
        );
        ReturnClientToCtePlanFactAdditionalData additionalData = new ReturnClientToCtePlanFactAdditionalData(
            1L,
            3L,
            2L,
            "BARCODE1",
            LocalDateTime.of(2021, 1, 1, 1, 1),
            items,
            "PVZ"
        );
        PlanFact planFact = new PlanFact().setData(additionalData);

        ClientToCteReturnCsvRecordConverter converter = new ClientToCteReturnCsvRecordConverter();
        ClientToCteReturnCsvRecord csvRecord = converter.convert(planFact);
        assertThat(csvRecord.getReturnId()).isEqualTo(additionalData.getReturnId());
        assertThat(csvRecord.getOrderId()).isEqualTo(additionalData.getOrderId());
        assertThat(csvRecord.getBarcode()).isEqualTo(additionalData.getBarcode());
        assertThat(csvRecord.getStartDate()).isEqualTo(additionalData.getArrivedAt());
        assertThat(csvRecord.getPickupPointType()).isEqualTo(additionalData.getPickupPointType());
        assertThat(csvRecord.getItems()).isEqualTo("5/8: 1000, 1001, 1003, 1003, 1003.");
    }

    @Test
    void itemsIsNull() {
        ReturnClientToCtePlanFactAdditionalData additionalData = new ReturnClientToCtePlanFactAdditionalData(
            1L,
            3L,
            2L,
            "BARCODE1",
            LocalDateTime.of(2021, 1, 1, 1, 1),
            null,
            "PVZ"
        );
        PlanFact planFact = new PlanFact().setData(additionalData);

        ClientToCteReturnCsvRecordConverter converter = new ClientToCteReturnCsvRecordConverter();
        ClientToCteReturnCsvRecord csvRecord = converter.convert(planFact);
        assertThat(csvRecord.getReturnId()).isEqualTo(additionalData.getReturnId());
        assertThat(csvRecord.getOrderId()).isEqualTo(additionalData.getOrderId());
        assertThat(csvRecord.getBarcode()).isEqualTo(additionalData.getBarcode());
        assertThat(csvRecord.getStartDate()).isEqualTo(additionalData.getArrivedAt());
        assertThat(csvRecord.getPickupPointType()).isEqualTo(additionalData.getPickupPointType());
        assertThat(csvRecord.getItems()).isNull();
    }
}
