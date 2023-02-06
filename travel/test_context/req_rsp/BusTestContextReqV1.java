package ru.yandex.travel.api.endpoints.test_context.req_rsp;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.travel.orders.commons.proto.EBusBookOutcome;
import ru.yandex.travel.orders.commons.proto.EBusConfirmOutcome;
import ru.yandex.travel.orders.commons.proto.EBusRefundInfoOutcome;
import ru.yandex.travel.orders.commons.proto.EBusRefundOutcome;

@Data
@NoArgsConstructor
@ApiModel(value = "Настройки тестового контекста для автобусов")
public class BusTestContextReqV1 {
    @NotNull
    @ApiParam(value = "Результат создания бронирования", defaultValue = "BBO_SUCCESS", required = true)
    private EBusBookOutcome bookOutcome;

    @NotNull
    @ApiParam(value = "Результат подтверждения бронирования", defaultValue = "BCO_SUCCESS", required = true)
    private EBusConfirmOutcome confirmOutcome;

    @NotNull
    @ApiParam(value = "Результат получения стоимости к возврату", defaultValue = "BRIO_SUCCESS", required = true)
    private EBusRefundInfoOutcome refundInfoOutcome;

    @NotNull
    @ApiParam(value = "Результат проведения возврата", defaultValue = "BRO_SUCCESS", required = true)
    private EBusRefundOutcome refundOutcome;

    @ApiParam(value = "Секунд до автоотмены брони по таймауту", defaultValue = "0")
    private int expireAfterSeconds;
}
