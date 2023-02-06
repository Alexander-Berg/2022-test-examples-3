package ru.yandex.travel.api.endpoints.test_context.req_rsp;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.travel.orders.commons.proto.ETrainInsuranceCheckoutConfirmOutcome;
import ru.yandex.travel.orders.commons.proto.ETrainInsuranceCheckoutOutcome;
import ru.yandex.travel.orders.commons.proto.ETrainInsurancePricingOutcome;
import ru.yandex.travel.orders.commons.proto.ETrainRefundCheckoutOutcome;
import ru.yandex.travel.orders.commons.proto.ETrainRefundPricingOutcome;
import ru.yandex.travel.orders.commons.proto.ETrainReservationConfirmOutcome;
import ru.yandex.travel.orders.commons.proto.ETrainReservationCreateOutcome;

@Data
@NoArgsConstructor
@ApiModel(value = "Настройки тестового контекста для ЖД")
public class TrainTestContextReqV1 {
    @NotNull
    @ApiParam(value = "Результат получения цены страховки", defaultValue = "IPO_SUCCESS", required = true)
    private ETrainInsurancePricingOutcome insurancePricingOutcome;

    @NotNull
    @ApiParam(value = "Результат выписывания страховки", defaultValue = "ICO_SUCCESS", required = true)
    private ETrainInsuranceCheckoutOutcome insuranceCheckoutOutcome;

    @NotNull
    @ApiParam(value = "Результат подтверждения выписанной страховки (после оплаты)", defaultValue = "ICCO_SUCCESS",
            required = true)
    private ETrainInsuranceCheckoutConfirmOutcome insuranceCheckoutConfirmOutcome;

    @NotNull
    @ApiParam(value = "Результат получения стоимости к возврату", defaultValue = "RPO_SUCCESS", required = true)
    private ETrainRefundPricingOutcome refundPricingOutcome;

    @NotNull
    @ApiParam(value = "Результат проведения авто-возврата", defaultValue = "RCO_SUCCESS", required = true)
    private ETrainRefundCheckoutOutcome refundCheckoutOutcome;

    @NotNull
    @ApiParam(value = "Результат создания бронирования", defaultValue = "RCRO_SUCCESS", required = true)
    private ETrainReservationCreateOutcome createReservationOutcome;

    @NotNull
    @ApiParam(value = "Результат подтверждения бронирования", defaultValue = "RCOO_SUCCESS", required = true)
    private ETrainReservationConfirmOutcome confirmReservationOutcome;

    @ApiParam(value = "Период возврата билета через кассу (в секундах)", defaultValue = "0")
    private Integer officeReturnDelayInSeconds;

    @ApiParam(value = "Возвраты билетов через кассу в формате `<период_1>:<сумма_1>[,<период_N>:<сумма_N>...]`, " +
            "где `период_N` это период в секундах до запуска возврата, `сумма_N` — сумма к возврату в рублях.",
            example = "10:1000,20:500")
    private List<String> officeReturns;

    @ApiParam(value = "Период получения билета в кассе (в секундах). Если не задано или 0 - билет не должен быть " +
            "получен в кассе", defaultValue = "0")
    private Integer officeAcquireDelayInSeconds;

    @ApiParam(value = "Период времени в секундах, после которого запрос на обновление данных в ИМ будет завершаться " +
            "таймаутом. Если не задано или 0 - таймаута не будет", defaultValue = "0")
    private Integer alwaysTimeoutAfterConfirmingInSeconds;
}
