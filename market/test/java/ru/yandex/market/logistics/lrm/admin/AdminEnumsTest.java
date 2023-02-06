package ru.yandex.market.logistics.lrm.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminBusinessProcessStatus;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminBusinessProcessType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminControlPointStatus;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminControlPointType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminDestinationPointType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminEntityType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminLogisticPointType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnBoxStatus;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnReasonType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnSegmentChangeStatus;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnSegmentChangeType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnSource;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnStatus;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnSubreason;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminShipmentDestinationType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminShipmentRecipientType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminStatusSource;
import ru.yandex.market.logistics.lrm.converter.EnumConverter;
import ru.yandex.market.logistics.lrm.model.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.BusinessProcessType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ControlPointStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ControlPointType;
import ru.yandex.market.logistics.lrm.model.entity.enums.DestinationPointType;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.model.entity.enums.LogisticPointType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnBoxStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnReasonType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSegmentChangeStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSegmentChangeType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSource;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSubreason;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentDestinationType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentRecipientType;
import ru.yandex.market.logistics.lrm.model.entity.enums.StatusSource;

@DisplayName("Маппинг енамов админки в енамы модели")
class AdminEnumsTest extends LrmTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("Статус возврата: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminReturnStatus.class)
    void adminToModel(AdminReturnStatus status) {
        softly.assertThat(enumConverter.convert(status, ReturnStatus.class)).isNotNull();
    }

    @DisplayName("Статус возврата: model -> admin")
    @ParameterizedTest
    @EnumSource(ReturnStatus.class)
    void modelToAdmin(ReturnStatus status) {
        softly.assertThat(enumConverter.convert(status, AdminReturnStatus.class)).isNotNull();
    }

    @DisplayName("Источник статуса: model -> admin")
    @ParameterizedTest
    @EnumSource(StatusSource.class)
    void modelToAdmin(StatusSource status) {
        softly.assertThat(enumConverter.convert(status, AdminStatusSource.class)).isNotNull();
    }

    @DisplayName("Инициатор возврата: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminReturnSource.class)
    void adminToModel(AdminReturnSource returnSource) {
        softly.assertThat(enumConverter.convert(returnSource, ReturnSource.class)).isNotNull();
    }

    @DisplayName("Инициатор возврата: model -> admin")
    @ParameterizedTest
    @EnumSource(ReturnSource.class)
    void modelToAdmin(ReturnSource returnSource) {
        softly.assertThat(enumConverter.convert(returnSource, AdminReturnSource.class)).isNotNull();
    }

    @DisplayName("Статус грузоместа: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminReturnBoxStatus.class)
    void adminToModel(AdminReturnBoxStatus boxStatus) {
        softly.assertThat(enumConverter.convert(boxStatus, ReturnBoxStatus.class)).isNotNull();
    }

    @DisplayName("Статус грузоместа: model -> admin")
    @ParameterizedTest
    @EnumSource(ReturnBoxStatus.class)
    void modelToAdmin(ReturnBoxStatus boxStatus) {
        softly.assertThat(enumConverter.convert(boxStatus, AdminReturnBoxStatus.class)).isNotNull();
    }

    @DisplayName("Статус бизнес-процесса: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminBusinessProcessStatus.class)
    void adminToModel(AdminBusinessProcessStatus businessProcessStatus) {
        softly.assertThat(enumConverter.convert(businessProcessStatus, BusinessProcessStatus.class)).isNotNull();
    }

    @DisplayName("Статус бизнес-процесса: model -> admin")
    @ParameterizedTest
    @EnumSource(BusinessProcessStatus.class)
    void modelToAdmin(BusinessProcessStatus businessProcessStatus) {
        softly.assertThat(enumConverter.convert(businessProcessStatus, AdminBusinessProcessStatus.class)).isNotNull();
    }

    @DisplayName("Тип бизнес-процесса: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminBusinessProcessType.class)
    void adminToModel(AdminBusinessProcessType businessProcessType) {
        softly.assertThat(enumConverter.convert(businessProcessType, BusinessProcessType.class)).isNotNull();
    }

    @DisplayName("Тип бизнес-процесса: model -> admin")
    @ParameterizedTest
    @EnumSource(BusinessProcessType.class)
    void modelToAdmin(BusinessProcessType businessProcessType) {
        softly.assertThat(enumConverter.convert(businessProcessType, AdminBusinessProcessType.class)).isNotNull();
    }

    @DisplayName("Тип сущности бизнес-процесса: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminEntityType.class)
    void adminToModel(AdminEntityType businessProcessType) {
        softly.assertThat(enumConverter.convert(businessProcessType, EntityType.class)).isNotNull();
    }

    @DisplayName("Тип сущности бизнес-процесса: model -> admin")
    @ParameterizedTest
    @EnumSource(EntityType.class)
    void modelToAdmin(EntityType businessProcessType) {
        softly.assertThat(enumConverter.convert(businessProcessType, AdminEntityType.class)).isNotNull();
    }

    @DisplayName("Подпричина возврата: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminReturnSubreason.class)
    void adminToModel(AdminReturnSubreason returnSubreason) {
        softly.assertThat(enumConverter.convert(returnSubreason, ReturnSubreason.class)).isNotNull();
    }

    @DisplayName("Подпричина возврата: model -> admin")
    @ParameterizedTest
    @EnumSource(ReturnSubreason.class)
    void modelToAdmin(ReturnSubreason returnSubreason) {
        softly.assertThat(enumConverter.convert(returnSubreason, AdminReturnSubreason.class)).isNotNull();
    }

    @DisplayName("Тип причины возврата: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminReturnReasonType.class)
    void adminToModel(AdminReturnReasonType reasonType) {
        softly.assertThat(enumConverter.convert(reasonType, ReturnReasonType.class)).isNotNull();
    }

    @DisplayName("Тип причины возврата: model -> admin")
    @ParameterizedTest
    @EnumSource(ReturnReasonType.class)
    void modelToAdmin(ReturnReasonType reasonType) {
        softly.assertThat(enumConverter.convert(reasonType, AdminReturnReasonType.class)).isNotNull();
    }

    @DisplayName("Тип назначения отгрузки: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminShipmentRecipientType.class)
    void adminToModel(AdminShipmentRecipientType shipmentRecipientType) {
        softly.assertThat(enumConverter.convert(shipmentRecipientType, ShipmentRecipientType.class)).isNotNull();
    }

    @DisplayName("Тип назначения отгрузки: model -> admin")
    @ParameterizedTest
    @EnumSource(ShipmentRecipientType.class)
    void modelToAdmin(ShipmentRecipientType shipmentRecipientType) {
        softly.assertThat(enumConverter.convert(shipmentRecipientType, AdminShipmentRecipientType.class)).isNotNull();
    }

    @DisplayName("Статус сегмента: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminReturnSegmentStatus.class)
    void adminToModel(AdminReturnSegmentStatus segmentStatus) {
        softly.assertThat(enumConverter.convert(segmentStatus, ReturnSegmentStatus.class)).isNotNull();
    }

    @DisplayName("Статус сегмента: model -> admin")
    @ParameterizedTest
    @EnumSource(ReturnSegmentStatus.class)
    void modelToAdmin(ReturnSegmentStatus segmentStatus) {
        softly.assertThat(enumConverter.convert(segmentStatus, AdminReturnSegmentStatus.class)).isNotNull();
    }

    @DisplayName("Тип точки назначения отгрузки: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminShipmentDestinationType.class)
    void adminToModel(AdminShipmentDestinationType destinationType) {
        softly.assertThat(enumConverter.convert(destinationType, ShipmentDestinationType.class)).isNotNull();
    }

    @DisplayName("Тип точки назначения отгрузки: model -> admin")
    @ParameterizedTest
    @EnumSource(ShipmentDestinationType.class)
    void modelToAdmin(ShipmentDestinationType destinationType) {
        softly.assertThat(enumConverter.convert(destinationType, AdminShipmentDestinationType.class)).isNotNull();
    }

    @DisplayName("Тип точки назначения отгрузки: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminLogisticPointType.class)
    void adminToModel(AdminLogisticPointType pointType) {
        softly.assertThat(enumConverter.convert(pointType, LogisticPointType.class)).isNotNull();
    }

    @DisplayName("Тип точки назначения отгрузки: model -> admin")
    @ParameterizedTest
    @EnumSource(LogisticPointType.class)
    void modelToAdmin(LogisticPointType pointType) {
        softly.assertThat(enumConverter.convert(pointType, AdminLogisticPointType.class)).isNotNull();
    }

    @DisplayName("Тип точки назначения возврата: admin -> model")
    @ParameterizedTest
    @EnumSource(AdminDestinationPointType.class)
    void adminToModel(AdminDestinationPointType pointType) {
        softly.assertThat(enumConverter.convert(pointType, DestinationPointType.class)).isNotNull();
    }

    @DisplayName("Тип точки назначения возврата: model -> admin")
    @ParameterizedTest
    @EnumSource(DestinationPointType.class)
    void modelToAdmin(DestinationPointType pointType) {
        softly.assertThat(enumConverter.convert(pointType, AdminDestinationPointType.class)).isNotNull();
    }

    @DisplayName("Тип запроса на изменение сегмента возврата: model -> admin")
    @ParameterizedTest
    @EnumSource(ReturnSegmentChangeType.class)
    void modelToAdmin(ReturnSegmentChangeType value) {
        softly.assertThat(enumConverter.convert(value, AdminReturnSegmentChangeType.class)).isNotNull();
    }

    @DisplayName("Статус запроса на изменение сегмента возврата: model -> admin")
    @ParameterizedTest
    @EnumSource(ReturnSegmentChangeStatus.class)
    void modelToAdmin(ReturnSegmentChangeStatus value) {
        softly.assertThat(enumConverter.convert(value, AdminReturnSegmentChangeStatus.class)).isNotNull();
    }

    @DisplayName("Статус контрольной точки: model -> admin")
    @ParameterizedTest
    @EnumSource(ControlPointStatus.class)
    void modelToAdmin(ControlPointStatus value) {
        softly.assertThat(enumConverter.convert(value, AdminControlPointStatus.class)).isNotNull();
    }

    @DisplayName("Тип контрольной точки: model -> admin")
    @ParameterizedTest
    @EnumSource(ControlPointType.class)
    void modelToAdmin(ControlPointType value) {
        softly.assertThat(enumConverter.convert(value, AdminControlPointType.class)).isNotNull();
    }
}
