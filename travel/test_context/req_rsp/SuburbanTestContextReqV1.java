package ru.yandex.travel.api.endpoints.test_context.req_rsp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.travel.orders.commons.proto.TSuburbanTestContextHandlerErrorType;


@Data
@NoArgsConstructor
@ApiModel(value = "Настройки тестового контекста для Электричек")
public class SuburbanTestContextReqV1 {
    @ApiParam(value = "Фиксированное тело билета для генерации штрих-кода", defaultValue = "")
    private String ticketBody = "";

    @ApiParam(value = "Фиксированный номер билета", defaultValue = "")
    private Integer ticketNumber = -1;

    @ApiParam(value = "Цена при бронировании с точки зрения партнера (может отличаться от тарифов)", defaultValue = "")
    private Float actualPrice = (float) -1;

    @ApiParam(value = "Бронь валидна в течение n секунд", defaultValue = "")
    private Integer validForSeconds = -1;

    @ApiParam(value = "Тип ошибки для /book (Мовиста) или /Create (ИМ)")
    private TSuburbanTestContextHandlerErrorType bookHandlerErrorType;

    @ApiParam(value = "Количество ошибочных вызовов /book (Мовиста) или /Create (ИМ)")
    private Integer bookHandlerErrorCount = 0;

    @ApiParam(value = "Тип ошибки для /confirm (Мовиста) или /Confirm (ИМ)")
    private TSuburbanTestContextHandlerErrorType confirmHandlerErrorType;

    @ApiParam(value = "Количество ошибочных вызовов /confirm (Мовиста) или /Confirm (ИМ)")
    private Integer confirmHandlerErrorCount = 0;

    @ApiParam(value = "Тип ошибки для /info (Мовиста) или /OrderInfo (ИМ)")
    private TSuburbanTestContextHandlerErrorType orderInfoHandlerErrorType;

    @ApiParam(value = "Количество ошибочных вызовов /info (Мовиста) или /OrderInfo (ИМ)")
    private Integer orderInfoHandlerErrorCount = 0;

    @ApiParam(value = "Тип ошибки для /TicketBarcode (ИМ)")
    private TSuburbanTestContextHandlerErrorType ticketBarcodeHandlerErrorType;

    @ApiParam(value = "Количество ошибочных вызовов /TicketBarcode (ИМ)")
    private Integer ticketBarcodeHandlerErrorCount = 0;

    @ApiParam(value = "Тип ошибки для /blankPdf (Мовиста) или /Blank (ИМ)")
    private TSuburbanTestContextHandlerErrorType blankPdfHandlerErrorType;

    @ApiParam(value = "Количество ошибочных вызовов /blankPdf (Мовиста) или /Blank (ИМ)")
    private Integer blankPdfHandlerErrorCount = 0;
}
