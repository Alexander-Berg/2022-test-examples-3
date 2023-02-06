package ru.yandex.market.delivery.transport_manager.service.external.tpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueue;
import ru.yandex.market.tpl.client.dropoff.TplDropoffCargoClient;
import ru.yandex.market.tpl.client.dropoff.dto.DropoffCargoCreateCommandDto;
import ru.yandex.market.tpl.client.dropoff.dto.DropoffCargoDto;

class TplBarcodeSenderTest extends AbstractContextualTest {
    @Autowired
    private TplDropoffCargoClient tplDropoffCargoClient;

    private TplBarcodeSender tplBarcodeSender;

    @Captor
    ArgumentCaptor<DropoffCargoCreateCommandDto> dtoArgumentCaptor;

    @BeforeEach
    void setUp() {
        Mockito.when(tplDropoffCargoClient.createCargo(Mockito.any())).thenReturn(
            ResponseEntity.ok(dropoffCargoDto())
        );
        tplBarcodeSender = new TplBarcodeSender(tplDropoffCargoClient);
    }

    @Test
    void send() {
        tplBarcodeSender.send(
            new UnitQueue()
            .setUnitId("AABBCC")
            .setPointFromId(100500L)
            .setPointToId(100600L)
        );
        Mockito.verify(tplDropoffCargoClient).createCargo(dtoArgumentCaptor.capture());

        DropoffCargoCreateCommandDto value = dtoArgumentCaptor.getValue();
        softly.assertThat(value).isEqualTo(dto());
    }

    private static DropoffCargoCreateCommandDto dto() {
        DropoffCargoCreateCommandDto dropoffCargoCreateCommandDto = new DropoffCargoCreateCommandDto();
        dropoffCargoCreateCommandDto.setBarcode("AABBCC");
        dropoffCargoCreateCommandDto.setLogisticPointIdFrom("100500");
        dropoffCargoCreateCommandDto.setLogisticPointIdTo("100600");

        return dropoffCargoCreateCommandDto;
    }

    private static DropoffCargoDto dropoffCargoDto() {
        DropoffCargoDto dropoffCargoDto = new DropoffCargoDto();
        dropoffCargoDto.setBarcode("123");
        dropoffCargoDto.setStatus("wfew");

        return dropoffCargoDto;
    }
}
