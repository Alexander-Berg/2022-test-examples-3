package ru.yandex.travel.api.endpoints.test_context.req_rsp;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.travel.orders.commons.proto.EAviaCheckAvailabilityOnRedirOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaCheckAvailabilityOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaConfirmationOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaMqEventOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaTokenizationOutcome;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "Настройки тестового контекста для авиа")
public class AviaTestContextReqV1 {
    @NotNull
    @ApiParam(value = "Результат проверки доступности варианта при редиректе на BoY", defaultValue = "CAOR_SUCCESS",
            required = true)
    private EAviaCheckAvailabilityOnRedirOutcome checkAvailabilityOnRedirOutcome;

    @NotNull
    @ApiParam(value = "Результат проверки доступности варианта перед созданием заказа", defaultValue = "CAO_SUCCESS",
            required = true)
    private EAviaCheckAvailabilityOutcome checkAvailabilityBeforeBookingOutcome;

    @NotNull
    @ApiParam(value = "Результат токенизации карты", defaultValue = "TO_SUCCESS", required = true)
    private EAviaTokenizationOutcome tokenizationOutcome;

    @NotNull
    @ApiParam(value = "Результат подтверждения бронирования заказа", defaultValue = "CO_SUCCESS", required = true)
    private EAviaConfirmationOutcome confirmationOutcome;

    @NotNull
    @ApiParam(value = "Тип MQ события, присылаемого аэрофлотом о статусе заказа", defaultValue = "MEO_NO_EVENT",
            required = true)
    private EAviaMqEventOutcome mqEventOutcome;

    @ApiParam(value = "Авиа варианты. JSON с вариантами для выдачи и дальнейшей работы в BoY", defaultValue = "",
            required = true)
    private String aviaVariants;

    @ApiParam(value = "Использовать aviaVariants на выдаче или запускать реальный поиск")
    private boolean mockAviaVariants;
}
