package ru.yandex.market.fulfillment.wrap.marschroute.service.util;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill.WaybillInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybillStatus;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusHistory;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusType;
import ru.yandex.market.logistic.api.utils.DateTime;

class InboundHistoryFactoryScenarios {

    static final ResourceId RESOURCE_ID = new ResourceId("1", "2");

    private static final LocalDateTime DATE_CREATE = LocalDateTime.of(2017, 1, 1, 14, 0);
    private static final LocalDateTime DATE_ARRIVE = DATE_CREATE.plusDays(2);
    private static final LocalDateTime DATE_ACCEPTANCE = DATE_ARRIVE.plusMinutes(20);
    private static final LocalDateTime DATE_ACCEPTED = DATE_ACCEPTANCE.plusHours(2);

    /**
     * Сценарий, когда заявка обладает единственным статусом - Утверждена (1201).
     */
    static Arguments firstScenario() {
        return Arguments.of(
            new WaybillInfo()
                .setDateCreate(DATE_CREATE)
                .setStatus(MarschrouteWaybillStatus.INBOUND_CONFIRMED)
                .setDateStatus(DATE_CREATE),
            new InboundStatusHistory(Collections.singletonList(
                new InboundStatus(RESOURCE_ID, InboundStatusType.CREATED, DateTime.fromLocalDateTime(DATE_CREATE))),
                RESOURCE_ID
            )
        );
    }

    /**
     * Сценарий, когда заявка обладает единственным статусом - Модель (121).
     */
    static Arguments secondScenario() {
        return Arguments.of(
            new WaybillInfo()
                .setDateCreate(DATE_CREATE)
                .setStatus(MarschrouteWaybillStatus.INBOUND_MODEL)
                .setDateStatus(DATE_CREATE),
            new InboundStatusHistory(Collections.singletonList(
                new InboundStatus(RESOURCE_ID, InboundStatusType.CREATED, DateTime.fromLocalDateTime(DATE_CREATE))),
                RESOURCE_ID
            )
        );
    }

    /**
     * Сценарий, в котором фактический date_create имеет более позднее значение, нежели чем статус Утверждена (1201).
     * Результатом должна быть история, в которой единственный статус CREATED - имеющий значение date_create, а не date_status.
     * <p>
     * https://helpdesk.marschroute.ru/issues/55977
     */
    static Arguments thirdScenario() {
        return Arguments.of(
            new WaybillInfo()
                .setDateCreate(DATE_CREATE)
                .setStatus(MarschrouteWaybillStatus.INBOUND_CONFIRMED)
                .setDateStatus(DATE_CREATE.minusMinutes(20)),
            new InboundStatusHistory(Collections.singletonList(
                new InboundStatus(RESOURCE_ID, InboundStatusType.CREATED, DateTime.fromLocalDateTime(DATE_CREATE))),
                RESOURCE_ID
            )
        );
    }

    /**
     * Сценарий, в котором на стороне Маршрута заявка была отменена сразу после ее создания.
     * Текущий статус Отменена (120).
     */
    static Arguments fourthScenario() {
        return Arguments.of(
            new WaybillInfo()
                .setDateCreate(DATE_CREATE)
                .setStatus(MarschrouteWaybillStatus.INBOUND_CANCELLED)
                .setDateStatus(DATE_CREATE.plusMinutes(20)),
            new InboundStatusHistory(Arrays.asList(
                new InboundStatus(RESOURCE_ID, InboundStatusType.CANCELLED, DateTime.fromLocalDateTime(DATE_CREATE.plusMinutes(20))),
                new InboundStatus(RESOURCE_ID, InboundStatusType.CREATED, DateTime.fromLocalDateTime(DATE_CREATE))),
                RESOURCE_ID
            )
        );
    }

    /**
     * Сценарий, в котором на стороне Маршрута заявка была отменена после прибытия машины на склад.
     * Текущий статус Отменена (120).
     */
    static Arguments fifthScenario() {
        return Arguments.of(
            new WaybillInfo()
                .setDateCreate(DATE_CREATE)
                .setDateArrive(DATE_ARRIVE)
                .setStatus(MarschrouteWaybillStatus.INBOUND_CANCELLED)
                .setDateStatus(DATE_ARRIVE.plusMinutes(20)),
            new InboundStatusHistory(Arrays.asList(
                new InboundStatus(RESOURCE_ID, InboundStatusType.CANCELLED, DateTime.fromLocalDateTime(DATE_ARRIVE.plusMinutes(20))),
                new InboundStatus(RESOURCE_ID, InboundStatusType.ARRIVED, DateTime.fromLocalDateTime(DATE_ARRIVE)),
                new InboundStatus(RESOURCE_ID, InboundStatusType.CREATED, DateTime.fromLocalDateTime(DATE_CREATE))),
                RESOURCE_ID
            )
        );
    }

    /**
     * Сценарий, в котором машина прибыла и началась приемка. Текущий статус Приемка 1 (122).
     */
    static Arguments sixthScenario() {
        return Arguments.of(
            new WaybillInfo()
                .setDateCreate(DATE_CREATE)
                .setDateArrive(DATE_ARRIVE)
                .setDateReceptionBegin(DATE_ACCEPTANCE)
                .setStatus(MarschrouteWaybillStatus.INBOUND_INSPECTION1)
                .setDateStatus(DATE_ACCEPTANCE),
            new InboundStatusHistory(Arrays.asList(
                new InboundStatus(RESOURCE_ID, InboundStatusType.ACCEPTANCE, DateTime.fromLocalDateTime(DATE_ACCEPTANCE)),
                new InboundStatus(RESOURCE_ID, InboundStatusType.ARRIVED, DateTime.fromLocalDateTime(DATE_ARRIVE)),
                new InboundStatus(RESOURCE_ID, InboundStatusType.CREATED, DateTime.fromLocalDateTime(DATE_CREATE))),
                RESOURCE_ID
            )
        );
    }

    /**
     * Сценарий, в котором машина прибыла и началась приемка. Текущий статус Приемка 2 (123).
     */
    static Arguments seventhScenario() {
        return Arguments.of(
            new WaybillInfo()
                .setDateCreate(DATE_CREATE)
                .setDateArrive(DATE_ARRIVE)
                .setDateReceptionBegin(DATE_ACCEPTANCE)
                .setStatus(MarschrouteWaybillStatus.INBOUND_INSPECTION2)
                .setDateStatus(DATE_ACCEPTANCE.plusMinutes(50)),
            new InboundStatusHistory(Arrays.asList(
                new InboundStatus(RESOURCE_ID, InboundStatusType.ACCEPTANCE, DateTime.fromLocalDateTime(DATE_ACCEPTANCE)),
                new InboundStatus(RESOURCE_ID, InboundStatusType.ARRIVED, DateTime.fromLocalDateTime(DATE_ARRIVE)),
                new InboundStatus(RESOURCE_ID, InboundStatusType.CREATED, DateTime.fromLocalDateTime(DATE_CREATE))),
                RESOURCE_ID
            )
        );
    }

    /**
     * Сценарий, в котором машина прибыла и началась приемка. Текущий статус - Оприходовано.
     */
    static Arguments eighthScenario() {
        return Arguments.of(
            new WaybillInfo()
                .setDateCreate(DATE_CREATE)
                .setDateArrive(DATE_ARRIVE)
                .setDateReceptionBegin(DATE_ACCEPTANCE)
                .setStatus(MarschrouteWaybillStatus.INBOUND_ACCEPTED)
                .setDateStatus(DATE_ACCEPTED),
            new InboundStatusHistory(Arrays.asList(
                new InboundStatus(RESOURCE_ID, InboundStatusType.ACCEPTED, DateTime.fromLocalDateTime(DATE_ACCEPTED)),
                new InboundStatus(RESOURCE_ID, InboundStatusType.ACCEPTANCE, DateTime.fromLocalDateTime(DATE_ACCEPTANCE)),
                new InboundStatus(RESOURCE_ID, InboundStatusType.ARRIVED, DateTime.fromLocalDateTime(DATE_ARRIVE)),
                new InboundStatus(RESOURCE_ID, InboundStatusType.CREATED, DateTime.fromLocalDateTime(DATE_CREATE))),
                RESOURCE_ID
            )
        );
    }
}
