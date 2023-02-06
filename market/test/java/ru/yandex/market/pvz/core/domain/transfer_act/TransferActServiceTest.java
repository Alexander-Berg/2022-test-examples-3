package ru.yandex.market.pvz.core.domain.transfer_act;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.pvz.core.domain.shipment.ShipmentCommandService;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.tpl.common.transferact.client.model.ActorDto;
import ru.yandex.market.tpl.common.transferact.client.model.ActorTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferQualifierDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferStatus;
import ru.yandex.market.tpl.common.transferact.client.model.TwoActorQualifierDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class TransferActServiceTest {

    private static final String TRANSFER_ID = "str";

    private final TransferActService transferActService;

    @MockBean
    private ShipmentCommandService shipmentCommandService;

    @ParameterizedTest
    @CsvSource({"CLOSED,FINISHED", "CANCELLED,CANCELLED"})
    void closeTransferStateUpdateShouldCloseReceive(TransferStatus transferStatus, ShipmentStatus shipmentStatus) {
        TransferDto transfer = createTransfer(transferStatus, ActorTypeDto.MARKET_PVZ);
        transferActService.updateTransferState(transfer);
        verify(shipmentCommandService).closeReceive(TRANSFER_ID, shipmentStatus);
    }

    @ParameterizedTest
    @EnumSource(value = ActorTypeDto.class, mode = EnumSource.Mode.EXCLUDE, names = "MARKET_PVZ")
    void updateNotPvzTransferActShouldBeIgnored(ActorTypeDto actorType) {
        TransferDto transfer = createTransfer(TransferStatus.CLOSED, actorType);
        transferActService.updateTransferState(transfer);
        verify(shipmentCommandService, never()).closeReceive(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = TransferStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"CLOSED", "CANCELLED"})
    void updateNotClosedTransferActShouldBeIgnored(TransferStatus transferStatus) {
        TransferDto transfer = createTransfer(transferStatus, ActorTypeDto.MARKET_PVZ);
        transferActService.updateTransferState(transfer);
        verify(shipmentCommandService, never()).closeReceive(any(), any());
    }

    private TransferDto createTransfer(TransferStatus transferStatus, ActorTypeDto actorType) {
        return new TransferDto()
                .id(TRANSFER_ID)
                .status(transferStatus)
                .transferQualifier(new TransferQualifierDto().twoActorQualifier(
                        new TwoActorQualifierDto().actorTo(new ActorDto().type(actorType))
                ));
    }
}
