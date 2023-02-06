package ru.yandex.market.wms.scheduler.testUtils;

import java.time.Instant;

import lombok.experimental.UtilityClass;

import ru.yandex.market.wms.common.model.dto.LogisticUnitDTO;
import ru.yandex.market.wms.common.model.enums.LogisticUnitStatus;
import ru.yandex.market.wms.common.model.enums.LogisticUnitType;

@UtilityClass
public class BuildUtils {
    public LogisticUnitDTO buildLogisticUnitDTO(String unitKey, String externalOrderKey) {
        return LogisticUnitDTO.builder()
                .registerKey("12312312")
                .externReceiptKey("sdfsdfsdf")
                .unitKey(unitKey)
                .externalOrderKey(externalOrderKey)
                .type(LogisticUnitType.UNKNOWN)
                .status(LogisticUnitStatus.NEW)
                .receiptKey("")
                .storerKey("")
                .count(0)
                .boxesInOrder(0)
                .shouldBeReceived(false)
                .addDate(Instant.now())
                .addWho("who")
                .editDate(Instant.now())
                .editWho("who")
                .stockType("")
                .build();
    }
}
