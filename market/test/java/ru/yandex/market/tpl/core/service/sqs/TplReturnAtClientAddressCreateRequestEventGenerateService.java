package ru.yandex.market.tpl.core.service.sqs;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.les.dto.TplClientDto;
import ru.yandex.market.logistics.les.dto.TplDimensionsDto;
import ru.yandex.market.logistics.les.dto.TplLocationDto;
import ru.yandex.market.logistics.les.dto.TplRequestIntervalDto;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressItemDto;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressReasonType;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressCreateRequestEvent;

@UtilityClass
public class TplReturnAtClientAddressCreateRequestEventGenerateService {

    public TplReturnAtClientAddressCreateRequestEvent generateEvent(
            TplReturnAtClientAddressCreateRequestEventGenerateParam param
    ) {
        return new TplReturnAtClientAddressCreateRequestEvent(
                param.getId(),
                param.getOrderId(),
                param.getReturnId(),
                param.getReturnExternalId(),
                param.getDeliveryServiceId(),
                param.getInterval(),
                param.getItems(),
                param.getLocation(),
                param.getClient()
        );
    }

    @Getter
    @Builder(toBuilder = true)
    public static final class TplReturnAtClientAddressCreateRequestEventGenerateParam {

        @Builder.Default
        private String id = UUID.randomUUID().toString();

        @Builder.Default
        private String orderId = UUID.randomUUID().toString();

        @Builder.Default
        private String returnId = UUID.randomUUID().toString();

        @Builder.Default
        private String returnExternalId = UUID.randomUUID().toString();

        @Builder.Default
        private Long deliveryServiceId = 123L;

        private TplRequestIntervalDto interval;

        @Builder.Default
        private List<TplReturnAtClientAddressItemDto> items = List.of(
                new TplReturnAtClientAddressItemDto(
                        345L,
                        "itemSku",
                        "itemName",
                        "CategoryName",
                        "description",
                        "photoUrl",
                        "detailsUrl",
                        new TplDimensionsDto(
                                1L, 1L, 1L, 1L
                        ),
                        TplReturnAtClientAddressReasonType.BAD_QUALITY,
                        "Bad quality",
                        null,
                        null
                )
        );

        @Builder.Default
        private TplLocationDto location = new TplLocationDto(
                "City", "Street", "house",
                "1", "123", "5", "1236", BigDecimal.ONE, BigDecimal.TEN,
                "comment"
        );

        @Builder.Default
        private TplClientDto client = new TplClientDto("Client", "email.com", "+7111");
    }
}
