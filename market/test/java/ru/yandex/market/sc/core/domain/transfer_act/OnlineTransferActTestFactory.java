package ru.yandex.market.sc.core.domain.transfer_act;

import java.time.LocalDate;
import java.util.List;

import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import ru.yandex.market.tpl.common.transferact.client.model.ActorDto;
import ru.yandex.market.tpl.common.transferact.client.model.ActorTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.ItemQualifierDto;
import ru.yandex.market.tpl.common.transferact.client.model.PendingTransferDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryItemTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferQualifierDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferQualifierTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferStatus;
import ru.yandex.market.tpl.common.transferact.client.model.TwoActorQualifierDto;

public class OnlineTransferActTestFactory {

    @NotNull
    public static TransferDto createTransferDto(String transferId,
                                                 LocalDate localDate,
                                                 String sortingCenterExternalId,
                                                 String courierUid,
                                                 List<String> receivedOrderExternalIds,
                                                 List<String> skippedOrderExternalIds) {
        TransferDto transferDto = new TransferDto();
        transferDto.setId(transferId);

        transferDto.setTransferQualifier(createTransferQualifier(
                localDate,
                sortingCenterExternalId,
                courierUid
        ));

        transferDto.setStatus(TransferStatus.CREATED);
        transferDto.setReceivedItems(StreamEx.of(receivedOrderExternalIds)
                .map(OnlineTransferActTestFactory::createOrderQualifier)
                .toList()
        );
        transferDto.setSkippedItems(StreamEx.of(skippedOrderExternalIds)
                .map(OnlineTransferActTestFactory::createOrderQualifier)
                .toList()
        );

        return transferDto;
    }

    @NotNull
    private static ItemQualifierDto createOrderQualifier(String externalId) {
        ItemQualifierDto itemQualifierDto = new ItemQualifierDto();
        itemQualifierDto.setType(RegistryItemTypeDto.ORDER);
        itemQualifierDto.setExternalId(externalId);
        itemQualifierDto.setPlaceId(externalId);
        return itemQualifierDto;
    }

    @NotNull
    public static PendingTransferDto createPendingTransferDto(String transferId,
                                                        LocalDate localDate,
                                                        String sortingCenterExternalId,
                                                        String courierUid) {
        PendingTransferDto pendingTransferDto = new PendingTransferDto();
        pendingTransferDto.setId(transferId);

        pendingTransferDto.setTransferQualifier(createTransferQualifier(
                localDate,
                sortingCenterExternalId,
                courierUid
        ));
        return pendingTransferDto;
    }

    @NotNull
    private static TransferQualifierDto createTransferQualifier(LocalDate localDate,
                                                                String sortingCenterExternalId,
                                                                String courierUid) {
        TransferQualifierDto transferQualifier = new TransferQualifierDto();

        transferQualifier.setType(TransferQualifierTypeDto.TWO_ACTOR);
        TwoActorQualifierDto twoActorQualifier = new TwoActorQualifierDto();
        twoActorQualifier.setLocalDate(localDate);

        ActorDto actorFrom = new ActorDto();
        actorFrom.setType(ActorTypeDto.MARKET_SC);
        actorFrom.setExternalId(sortingCenterExternalId);
        twoActorQualifier.setActorFrom(actorFrom);

        ActorDto actorTo = new ActorDto();
        actorTo.setType(ActorTypeDto.MARKET_COURIER);
        actorTo.setExternalId(courierUid);
        twoActorQualifier.setActorTo(actorTo);

        transferQualifier.setTwoActorQualifier(twoActorQualifier);
        return transferQualifier;
    }

}
