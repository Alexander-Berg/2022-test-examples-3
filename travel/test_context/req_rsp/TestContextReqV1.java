package ru.yandex.travel.api.endpoints.test_context.req_rsp;

import java.time.LocalDate;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import ru.yandex.travel.api.infrastucture.RequiredWhenOtherHasValue;
import ru.yandex.travel.hotels.proto.EHotelCancellation;
import ru.yandex.travel.hotels.proto.EHotelConfirmationOutcome;
import ru.yandex.travel.hotels.proto.EHotelDataLookupOutcome;
import ru.yandex.travel.hotels.proto.EHotelOfferOutcome;
import ru.yandex.travel.hotels.proto.EHotelRefundOutcome;
import ru.yandex.travel.hotels.proto.EHotelReservationOutcome;
import ru.yandex.travel.hotels.proto.EPansionType;
import ru.yandex.travel.hotels.proto.EPartnerId;


@Data
@NoArgsConstructor
@ApiModel(value = "Настройки тестового контекста")
@RequiredWhenOtherHasValue.List({
        @RequiredWhenOtherHasValue(field = "cancellation",
                otherField = "forceAvailability",
                otherFieldValues = "true",
                message = "Cancellation must be specified"),
        @RequiredWhenOtherHasValue(field = "pansionType",
                otherField = "forceAvailability",
                otherFieldValues = "true",
                message = "Pansion Type must be specified"),
        @RequiredWhenOtherHasValue(field = "offerName",
                otherField = "forceAvailability",
                otherFieldValues = "true",
                message = "Offer Name must be specified"),
        @RequiredWhenOtherHasValue(field = "priceAmount",
                otherField = "forceAvailability",
                otherFieldValues = "true",
                message = "Price Amount must be specified"),
        @RequiredWhenOtherHasValue(field = "partiallyRefundRate",
                otherField = "cancellation",
                otherFieldValues = {"CR_PARTIALLY_REFUNDABLE", "CR_CUSTOM"},
                message = "partiallyRefundRate must be specified"),
        @RequiredWhenOtherHasValue(field = "partiallyRefundableInMinutes",
                otherField = "cancellation",
                otherFieldValues = "CR_CUSTOM",
                message = "partiallyRefundRate must be specified"),
        @RequiredWhenOtherHasValue(field = "nonRefundableInMinutes",
                otherField = "cancellation",
                otherFieldValues = "CR_CUSTOM",
                message = "partiallyRefundRate must be specified")
})
public class TestContextReqV1 {
    private final static String TOMORROW = LocalDate.now().plusDays(1).toString();

    @ApiParam(value = "ID отеля", defaultValue = "742", required = true)
    private String originalId;
    @ApiParam(value = "Партнер", defaultValue = "PI_TRAVELLINE", required = true)
    private EPartnerId partnerId;
    @ApiParam(value = "Дата заезда. Если не задана, подразумевается 'Завтра'")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkinDate;
    @ApiParam(value = "Дата выезда. Если не задана, подразумевается 'Через день после даты заезда'")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkoutDate;
    @ApiParam(value = "Количество гостей в формате 'N-C1,C2,..', где N количество взрослых, C1,C2,.. - возраста детей",
            defaultValue = "2", required = true)
    private String occupancy;

    @ApiParam(value = "Заменяет партнерскую выдачу офферов на сгенерированный оффер", defaultValue = "false")
    private boolean forceAvailability;
    @ApiParam(value = "Политика отмены сгенерированного оффера")
    private EHotelCancellation cancellation;
    @ApiParam(value = "Тип питания сгенерированного оффера")
    private EPansionType pansionType;
    @ApiParam(value = "Имя сгенерированного оффера")
    private String offerName;
    @ApiParam(value = "Цена сгенерированного оффера")
    private Integer priceAmount;
    @ApiParam(value = "Процент штрафа в сгенерированном оффере")
    private Double partiallyRefundRate;
    @ApiParam(value = "Период бесплатной отмены сгенерированного оффера (в минутах)")
    private Integer partiallyRefundableInMinutes;
    @ApiParam(value = "Период отмены сгенерированного оффера со штрафом (в минутах)")
    private Integer nonRefundableInMinutes;

    @ApiParam(value = "Результат проверки оффера на API при получении оффера по токену", defaultValue = "OO_SUCCESS",
            required = true)
    private EHotelOfferOutcome getOfferOutcome;
    @ApiParam(value = "Результат проверки оффера на API при получении создании заказа", defaultValue = "OO_SUCCESS",
            required = true)
    private EHotelOfferOutcome createOrderOutcome;
    @ApiParam(value = "Источник партнерских данных про отель при генерации оффера", defaultValue = "HO_REAL",
            required = true)
    private EHotelDataLookupOutcome hotelDataLookupOutcome;

    @ApiParam(value = "Результат предварительного бронирования заказа", defaultValue = "RO_SUCCESS", required = true)
    private EHotelReservationOutcome reservationOutcome;
    @ApiParam(value = "Результат подтверждения бронирования заказа", defaultValue = "CO_SUCCESS", required = true)
    private EHotelConfirmationOutcome confirmationOutcome;
    @ApiParam(value = "Результат возврата  заказа", defaultValue = "RF_SUCCESS", required = true)
    private EHotelRefundOutcome refundOutcome;
    @ApiParam(value = "Множитель изменения цены для различных mismatch-результатов", defaultValue = "1.1")
    private Double priceMismatchRate;
    @ApiParam(value = "ID существующего заказа в Дельфине (для симуляции дублей)")
    private String existingDolphinOrder;
    @ApiParam(value = "Игнорировать минимальную длину интервала рассрочки и уменьшить интервал безопасности")
    private boolean ignorePaymentScheduleRestrictions;
    @ApiParam(value = "Скидка на оффер")
    private Integer discountAmount;
    @ApiParam(value = "Стоимость питания (включенная в общую цену оффера)")
    private Integer mealPrice;

    @ApiParam(value = "Сделать офферы постпейными")
    private boolean isPostPay;
}
