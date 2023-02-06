package ru.yandex.market.logistics.werewolf.testutils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.werewolf.model.entity.DocOrder;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;

@UtilityClass
public class RtaOrdersDataFactory {
    @Nonnull
    public DocOrder.DocOrderBuilder docOrderBuilder(BigDecimal itemsSum) {
        return DocOrder.builder()
            .assessedCost(BigDecimal.valueOf(100.5))
            .itemsSum(itemsSum)
            .partnerId("asdsf123")
            .yandexId("asdsf123-LO-321")
            .placesCount(1)
            .weight(BigDecimal.valueOf(111.1));
    }

    @Nonnull
    public RtaOrdersData.RtaOrdersDataBuilder rtaOrdersDataBuilder(@Nullable BigDecimal itemsSum) {
        return RtaOrdersData.builder()
            .partnerLegalName("Partner legal name")
            .senderLegalName("Sender legal name")
            .shipmentId("testshipmentid")
            .shipmentDate(LocalDate.of(2020, 1, 1))
            .senderId("1")
            .orders(List.of(docOrderBuilder(itemsSum).build()));
    }
}
