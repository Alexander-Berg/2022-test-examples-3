package ru.yandex.market.tpl.core.test;

import java.util.List;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusRequestDto;

@UtilityClass
public class TestTplApiRequestFactory {

    public static UpdateItemsInstancesPurchaseStatusRequestDto.
            UpdateItemsInstancesPurchaseStatusRequestByOrderDto buildUpdateOrderItemRequest(String externalOrderId) {
        return UpdateItemsInstancesPurchaseStatusRequestDto.
                UpdateItemsInstancesPurchaseStatusRequestByOrderDto
                .builder()
                .externalOrderId(externalOrderId)
                .build();
    }

    public static UpdateItemsInstancesPurchaseStatusRequestDto.
            UpdateItemsInstancesPurchaseStatusRequestByOrderDto buildUpdateOrderItemRequest(
            String externalOrderId,
            List<String> purchaseItems,
            List<String> returnItems,
            List<String> unknownItems
    ) {
        return UpdateItemsInstancesPurchaseStatusRequestDto.
                UpdateItemsInstancesPurchaseStatusRequestByOrderDto
                .builder()
                .externalOrderId(externalOrderId)
                .purchaseItemsInstances( purchaseItems == null ? null :
                    purchaseItems.stream().map(TestTplApiRequestFactory::buildPurchaseI).collect(Collectors.toList()))
                .returnItemsInstances( returnItems == null ? null :
                    returnItems.stream().map(TestTplApiRequestFactory::buildReturnI).collect(Collectors.toList()))
                .unknownItemsInstances( unknownItems == null ? null :
                    unknownItems.stream().map(TestTplApiRequestFactory::buildUnknownI).collect(Collectors.toList()))
                .build();
    }

    public static UpdateItemsInstancesPurchaseStatusRequestDto.PurchaseItemInstanceDto buildPurchaseI(
            String uit
    ) {
        return UpdateItemsInstancesPurchaseStatusRequestDto.PurchaseItemInstanceDto.builder()
                .uit(uit)
                .build();
    }

    public static UpdateItemsInstancesPurchaseStatusRequestDto.ReturnItemInstanceDto buildReturnI(
            String uit
    ) {
        return UpdateItemsInstancesPurchaseStatusRequestDto.ReturnItemInstanceDto.builder()
                .uit(uit)
                .build();
    }

    public static UpdateItemsInstancesPurchaseStatusRequestDto.UnknownItemInstanceDto buildUnknownI(
            String uit
    ) {
        return UpdateItemsInstancesPurchaseStatusRequestDto.UnknownItemInstanceDto.builder()
                .uit(uit)
                .build();
    }
}
