package ru.yandex.market.tpl.core.external.boxbot;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.locker.boxbot.request.InitReturnExtraditionDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.CellDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.CellSizeDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.DeliveryItemDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.ExtraditionItemDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.ExtraditionShipmentDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.ReferenceDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.ShipmentDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.response.SizeCellsDto;

public class BoxBotMapperTest {
    @Test
    public void mapReferenceTest() {
        var ref = mockReference();
        var marketRef = BoxBotMapper.INSTANCE.map(ref);
        Assertions.assertEquals(marketRef.getShipments().get(0).getItems().get(0).getBarcode(), "ABC-abc-1234");
        Assertions.assertEquals(marketRef.getShipments().get(1).getItems().get(0).getBarcode(), "ABC-abc-3331");
        Assertions.assertEquals(marketRef.getShipmentExtradition().get(0).getItems().size(), 2);
    }

    private ReferenceDto mockReference() {
        String postCert = "cert1";
        String sysCert = "cert2";
        String rootCert = "cert3";
        boolean checkSystemCert = false;
        String sign = "1234567890123456";

        DeliveryItemDto delivery1 = new DeliveryItemDto(1, 1, "ABC-adc-1234", "ABC-abc-1234", sign, 0);
        DeliveryItemDto delivery2 = new DeliveryItemDto(1, 1, "ABC-abc-3331", null, sign, 0);
        DeliveryItemDto delivery3 = new DeliveryItemDto(1, 1, "ABC-abc-3334", "ABC-abc-1236", sign, 0);

        CellDto cellDto = new CellDto(3, 1, 3, "A3", 1, 3, 7, 8);
        ExtraditionItemDto extraditionItemDto = new ExtraditionItemDto(1, 2, cellDto, "ABC-abc-1236", sign, "ABC-abc" +
                "-1236");

        CellDto cellDto2 = new CellDto(4, 1, 4, "A4", 1, 4, 7, 8);
        ExtraditionItemDto extraditionItemDto2 = new ExtraditionItemDto(1, 2, cellDto2, "ABC-abc-1236", sign, "ABC" +
                "-abc-1236");

        InitReturnExtraditionDto initRet1 = new InitReturnExtraditionDto(1234, 123L);
        InitReturnExtraditionDto initRet2 = new InitReturnExtraditionDto(1235, 123L);

        CellSizeDto cellSizeDto = new CellSizeDto(1, 1, "asd", "red");
        CellDto cellf1 = new CellDto(3, 1, 1, "A3", 1, 3, 7, 8);
        CellDto cellf2 = new CellDto(3, 1, 2, "A4", 1, 3, 7, 8);
        SizeCellsDto sizeCellsDto = new SizeCellsDto(cellSizeDto, Arrays.asList(cellf1, cellf2));

        ExtraditionShipmentDto extraditionShipmentDto = new ExtraditionShipmentDto("ABC-adc-1234",
                Arrays.asList(extraditionItemDto, extraditionItemDto2));

        ShipmentDto shipment1 = new ShipmentDto(33, "ABC-adc-1234", Arrays.asList(delivery1));
        ShipmentDto shipment2 = new ShipmentDto(34, "ABC-abc-3331", Arrays.asList(delivery2));

        ReferenceDto dto = new ReferenceDto(
                postCert,
                sysCert,
                rootCert,
                checkSystemCert,

                Arrays.asList(delivery1, delivery2, delivery3),
                Arrays.asList(extraditionItemDto, extraditionItemDto2),
                Arrays.asList(extraditionShipmentDto),
                Arrays.asList(initRet1, initRet2),
                Arrays.asList(sizeCellsDto),
                null,
                Arrays.asList(shipment1, shipment2));
        return dto;
    }
}
