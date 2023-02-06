package ru.yandex.travel.api.endpoints.test_context.req_rsp;

import java.time.Duration;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import ru.yandex.travel.api.infrastucture.RequiredWhenOtherHasValue;
import ru.yandex.travel.orders.commons.proto.EPaymentOutcome;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "Настройки платёжного тестового контекста")
@RequiredWhenOtherHasValue.List({
        @RequiredWhenOtherHasValue(field = "paymentFailureResponseCode",
                otherField = "paymentOutcome",
                otherFieldValues = "PO_FAILURE",
                message = "paymentFailureResponseCode should be specified for PO_FAILURE outcome"),
        @RequiredWhenOtherHasValue(field = "paymentFailureResponseDescription",
                otherField = "paymentOutcome",
                otherFieldValues = "PO_FAILURE",
                message = "paymentFailureResponseDescription should be specified for PO_FAILURE outcome"),
})
public class PaymentTestContextReqV1 {
    @ApiParam(value = "Статус оплаты", defaultValue = "PO_SUCCESS")
    private EPaymentOutcome paymentOutcome;

    @ApiParam(value = "Код статуса оплаты в случае ошибки",
            examples = @Example({@ExampleProperty("USER_CANCELLED"), @ExampleProperty("AUTHORIZATION_REJECT")}))
    private String paymentFailureResponseCode; // https://wiki.yandex-team.ru/trust/payments/rc/

    @ApiParam(value = "Описание ошибки оплаты")
    private String paymentFailureResponseDescription;

    @ApiParam(value = "Минимальная задержка перед получением результата оплаты; " +
            "если не указана, то считается равной 0 сек.; Формат: https://en.wikipedia.org/wiki/ISO_8601#Durations",
            type = "string", examples = @Example({@ExampleProperty("PT10S")}))
    private Duration minUserActionDelay;

    @ApiParam(value = "Максимальная задержка перед получением результата оплаты; " +
            "если не указана, то равна минимальной; Формат: https://en.wikipedia.org/wiki/ISO_8601#Durations",
            type = "string", examples = @Example({@ExampleProperty("PT30S")}))
    private Duration maxUserActionDelay;

    @ApiParam(value = "Ссылка на тестовую форму оплаты")
    private String paymentUrl;
}
