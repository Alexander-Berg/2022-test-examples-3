package ru.yandex.market.tpl.internal.service.dropoff;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.client.dropoff.dto.DropoffCargoCreateCommandDto;
import ru.yandex.market.tpl.client.dropoff.dto.DropoffCargoDto;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoFlowStatus;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.internal.TplIntAbstractTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor
class DropoffCargoServiceTest extends TplIntAbstractTest {

    private final DropoffCargoService commandService;
    private final DropoffCargoRepository dropoffCargoRepository;

    @Test
    void forceUpdateStatus() {
        //given
        var dto = new DropoffCargoCreateCommandDto();
        dto.setBarcode("barcode");
        dto.setLogisticPointIdFrom("LogisticPointIdFrom");
        dto.setLogisticPointIdTo("LogisticPointIdTo");
        DropoffCargoDto cargo = commandService.createCargo(dto);


        DropoffCargo dropoffCargo = dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(cargo.getBarcode()).orElseThrow();

        //when
        commandService.forceUpdateStatus(dropoffCargo.getId(), DropoffCargoFlowStatus.CANCELLED);

        //then
        DropoffCargo updatedCargo = dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(cargo.getBarcode()).orElseThrow();
        assertEquals(DropoffCargoFlowStatus.CANCELLED, updatedCargo.getStatus());
    }


    @Test
    void notUniqBarcode() {
        //given
        var dto = new DropoffCargoCreateCommandDto();
        dto.setBarcode("barcode");
        dto.setLogisticPointIdFrom("LogisticPointIdFrom");
        dto.setLogisticPointIdTo("LogisticPointIdTo");
        DropoffCargoDto cargo = commandService.createCargo(dto);


        DropoffCargo dropoffCargo = dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(cargo.getBarcode()).orElseThrow();

        //when
        commandService.forceUpdateStatus(dropoffCargo.getId(), DropoffCargoFlowStatus.CANCELLED);

        //then
        DropoffCargo updatedCargo = dropoffCargoRepository.findByBarcodeAndReferenceIdIsNull(cargo.getBarcode()).orElseThrow();
        assertEquals(DropoffCargoFlowStatus.CANCELLED, updatedCargo.getStatus());
    }
}
