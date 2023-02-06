package ru.yandex.market.logistics.lom.converter;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.enums.BalancePaymentStatus;
import ru.yandex.market.logistics.lom.entity.enums.BillingProductType;
import ru.yandex.market.logistics.lom.entity.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.entity.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.CancellationSegmentStatus;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.entity.enums.ContactType;
import ru.yandex.market.logistics.lom.entity.enums.CourierType;
import ru.yandex.market.logistics.lom.entity.enums.DeliveryType;
import ru.yandex.market.logistics.lom.entity.enums.FileType;
import ru.yandex.market.logistics.lom.entity.enums.ItemChangeReason;
import ru.yandex.market.logistics.lom.entity.enums.LocationType;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.RegistryStatus;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentType;
import ru.yandex.market.logistics.lom.entity.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.entity.enums.TaxSystem;
import ru.yandex.market.logistics.lom.entity.enums.VatType;
import ru.yandex.market.logistics.lom.entity.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;

@DisplayName("Конвертация перечислений между внутренним представлением и внешним")
class ModelEnumConverterTest extends AbstractTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.tags.OrderTag.class)
    @DisplayName("Конвертация OrderTag model->entity")
    void orderTagFromExternal(ru.yandex.market.logistics.lom.model.enums.tags.OrderTag orderTag) {
        softly.assertThat(enumConverter.convert(
            orderTag,
            OrderTag.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(OrderTag.class)
    @DisplayName("Конвертация OrderTag entity->model")
    void orderTagToExternal(OrderTag orderTag) {
        ru.yandex.market.logistics.lom.model.enums.tags.OrderTag converted = enumConverter.convert(
            orderTag,
            ru.yandex.market.logistics.lom.model.enums.tags.OrderTag.class
        );

        softly
            .assertThat(converted != null || !orderTag.isConvertedToModel())
            .withFailMessage(String.format(
                "Order tag %s with isConvertedToModel=%s is converted to %s",
                orderTag,
                orderTag.isConvertedToModel(),
                converted
            ))
            .isTrue();
    }

    @ParameterizedTest
    @EnumSource(WaybillSegmentTag.class)
    @DisplayName("Конвертация WaybillSegmentTag entity->model")
    void waybillSegmentTagToExternal(WaybillSegmentTag waybillSegmentTag) {
        softly.assertThat(enumConverter.convert(
            waybillSegmentTag,
            ru.yandex.market.logistics.lom.model.enums.tags.WaybillSegmentTag.class
        ))
            .isNotNull()
            .isNotEqualTo(ru.yandex.market.logistics.lom.model.enums.tags.WaybillSegmentTag.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(BalancePaymentStatus.class)
    @DisplayName("Конвертация BalancePaymentStatus entity->model")
    void balancePaymentStatusToExternal(BalancePaymentStatus balancePaymentStatus) {
        softly.assertThat(enumConverter.convert(
            balancePaymentStatus,
            ru.yandex.market.logistics.lom.model.enums.BalancePaymentStatus.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(BillingProductType.class)
    @DisplayName("Конвертация BillingProductType entity->model")
    void billingProductTypeToExternal(BillingProductType billingProductType) {
        softly.assertThat(enumConverter.convert(
            billingProductType,
            ru.yandex.market.logistics.lom.model.enums.BillingProductType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Конвертация CancellationOrderReason entity->model")
    void cancellationOrderReasonToExternal(CancellationOrderReason<?> cancellationOrderReason) {
        softly.assertThat(
            Optional.of(cancellationOrderReason)
                .map(CancellationOrderReason::getName)
                .map(ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason::valueOf)
                .orElse(null)
        )
            .isNotNull()
            .isNotEqualTo(ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason.UNKNOWN);
    }

    @Nonnull
    private static Stream<Arguments> cancellationOrderReasonToExternal() {
        return CancellationOrderReason.valuesStream()
            .filter(reason -> reason != CancellationOrderReason.UNKNOWN)
            .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Конвертация CancellationOrderReason model->entity")
    void cancellationOrderReasonFromExternal(
        ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason<?> cancellationOrderReason
    ) {
        softly.assertThat(
            Optional.of(cancellationOrderReason)
                .map(ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason::getName)
                .map(CancellationOrderReason::valueOf)
                .orElse(null)
        )
            .isNotNull()
            .isNotEqualTo(CancellationOrderReason.UNKNOWN);
    }

    @Nonnull
    private static Stream<Arguments> cancellationOrderReasonFromExternal() {
        return ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason.valuesStream()
            .filter(reason -> reason != ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason.UNKNOWN)
            .map(Arguments::of);
    }

    @ParameterizedTest
    @EnumSource(CancellationOrderStatus.class)
    @DisplayName("Конвертация CancellationOrderStatus entity->model")
    void cancellationOrderStatusToExternal(CancellationOrderStatus cancellationOrderStatus) {
        softly.assertThat(enumConverter.convert(
            cancellationOrderStatus,
            ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus.class
        ))
            .isNotNull()
            .isNotEqualTo(ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = "UNKNOWN"
    )
    @DisplayName("Конвертация CancellationOrderStatus model->entity")
    void cancellationOrderStatusFromExternal(
        ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus cancellationOrderStatus
    ) {
        softly.assertThat(enumConverter.convert(
            cancellationOrderStatus,
            CancellationOrderStatus.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(CancellationSegmentStatus.class)
    @DisplayName("Конвертация CancellationSegmentStatus entity->model")
    void cancellationSegmentStatusToExternal(CancellationSegmentStatus cancellationSegmentStatus) {
        softly.assertThat(enumConverter.convert(
            cancellationSegmentStatus,
            ru.yandex.market.logistics.lom.model.enums.CancellationSegmentStatus.class
        ))
            .isNotNull()
            .isNotEqualTo(ru.yandex.market.logistics.lom.model.enums.CancellationSegmentStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(
        value = ChangeOrderRequestReason.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = "UNKNOWN"
    )
    @DisplayName("Конвертация ChangeOrderRequestReason entity->model")
    void changeOrderRequestReasonToExternal(ChangeOrderRequestReason changeOrderRequestReason) {
        softly.assertThat(enumConverter.convert(
            changeOrderRequestReason,
            ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason.class
        ))
            .isNotNull()
            .isNotEqualTo(ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = "UNKNOWN"
    )
    @DisplayName("Конвертация ChangeOrderRequestReason model->entity")
    void changeOrderRequestReasonFromExternal(
        ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason changeOrderRequestReason
    ) {
        softly.assertThat(enumConverter.convert(
            changeOrderRequestReason,
            ChangeOrderRequestReason.class
        ))
            .isNotNull()
            .isNotEqualTo(ChangeOrderRequestReason.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(ChangeOrderRequestStatus.class)
    @DisplayName("Конвертация ChangeOrderRequestStatus entity->model")
    void changeOrderRequestStatusToExternal(ChangeOrderRequestStatus changeOrderRequestStatus) {
        softly.assertThat(enumConverter.convert(
            changeOrderRequestStatus,
            ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus.class
        ))
            .usingRecursiveComparison()
            .isEqualTo(changeOrderRequestStatus);
    }

    @ParameterizedTest
    @EnumSource(ChangeOrderRequestType.class)
    @DisplayName("Конвертация ChangeOrderRequestType entity->model")
    void changeOrderRequestTypeToExternal(ChangeOrderRequestType changeOrderRequestType) {
        softly.assertThat(enumConverter.convert(
            changeOrderRequestType,
            ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType.class
        ))
            .isNotNull()
            .isNotEqualTo(ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(ContactType.class)
    @DisplayName("Конвертация ContactType entity->model")
    void contactTypeToExternal(ContactType contactType) {
        softly.assertThat(enumConverter.convert(
            contactType,
            ru.yandex.market.logistics.lom.model.enums.ContactType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.ContactType.class)
    @DisplayName("Конвертация ContactType model->entity")
    void contactTypeFromExternal(ru.yandex.market.logistics.lom.model.enums.ContactType contactType) {
        softly.assertThat(enumConverter.convert(
            contactType,
            ContactType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(CourierType.class)
    @DisplayName("Конвертация CourierType entity->model")
    void courierTypeToExternal(CourierType courierType) {
        softly.assertThat(enumConverter.convert(
            courierType,
            ru.yandex.market.logistics.lom.model.enums.CourierType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.CourierType.class)
    @DisplayName("Конвертация CourierType model->entity")
    void courierTypeFromExternal(ru.yandex.market.logistics.lom.model.enums.CourierType courierType) {
        softly.assertThat(enumConverter.convert(
            courierType,
            CourierType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(DeliveryType.class)
    @DisplayName("Конвертация DeliveryType entity->model")
    void deliveryTypeToExternal(DeliveryType deliveryType) {
        softly.assertThat(enumConverter.convert(
            deliveryType,
            ru.yandex.market.logistics.lom.model.enums.DeliveryType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.DeliveryType.class)
    @DisplayName("Конвертация DeliveryType model->entity")
    void deliveryTypeFromExternal(ru.yandex.market.logistics.lom.model.enums.DeliveryType deliveryType) {
        softly.assertThat(enumConverter.convert(
            deliveryType,
            DeliveryType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(FileType.class)
    @DisplayName("Конвертация FileType entity->model")
    void fileTypeToExternal(FileType fileType) {
        softly.assertThat(enumConverter.convert(
            fileType,
            ru.yandex.market.logistics.lom.model.enums.FileType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ItemChangeReason.class)
    @DisplayName("Конвертация ItemChangeReason entity->model")
    void itemChangeReasonToExternal(ItemChangeReason itemChangeReason) {
        softly.assertThat(enumConverter.convert(
            itemChangeReason,
            ru.yandex.market.logistics.lom.model.enums.ItemChangeReason.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.ItemChangeReason.class)
    @DisplayName("Конвертация ItemChangeReason model->entity")
    void itemChangeReasonFromExternal(ru.yandex.market.logistics.lom.model.enums.ItemChangeReason itemChangeReason) {
        softly.assertThat(enumConverter.convert(
            itemChangeReason,
            ItemChangeReason.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(LocationType.class)
    @DisplayName("Конвертация LocationType entity->model")
    void locationTypeToExternal(LocationType locationType) {
        softly.assertThat(enumConverter.convert(
            locationType,
            ru.yandex.market.logistics.lom.model.enums.LocationType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.LocationType.class)
    @DisplayName("Конвертация LocationType model->entity")
    void locationTypeFromExternal(ru.yandex.market.logistics.lom.model.enums.LocationType locationType) {
        softly.assertThat(enumConverter.convert(
            locationType,
            LocationType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("Конвертация OrderStatus entity->model")
    void orderStatusToExternal(OrderStatus orderStatus) {
        softly.assertThat(enumConverter.convert(
            orderStatus,
            ru.yandex.market.logistics.lom.model.enums.OrderStatus.class
        ))
            .isNotNull()
            .isNotEqualTo(ru.yandex.market.logistics.lom.model.enums.OrderStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.logistics.lom.model.enums.OrderStatus.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = "UNKNOWN"
    )
    @DisplayName("Конвертация OrderStatus model->entity")
    void orderStatusFromExternal(ru.yandex.market.logistics.lom.model.enums.OrderStatus orderStatus) {
        softly.assertThat(enumConverter.convert(
            orderStatus,
            OrderStatus.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(PartnerSubtype.class)
    @DisplayName("Конвертация PartnerSubtype entity->model")
    void partnerSubtypeToExternal(PartnerSubtype partnerSubtype) {
        softly.assertThat(enumConverter.convert(
            partnerSubtype,
            ru.yandex.market.logistics.lom.model.enums.PartnerSubtype.class
        ))
            .isNotNull()
            .isNotEqualTo(ru.yandex.market.logistics.lom.model.enums.PartnerSubtype.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(PartnerType.class)
    @DisplayName("Конвертация PartnerType entity->model")
    void partnerTypeToExternal(PartnerType partnerType) {
        softly.assertThat(enumConverter.convert(
            partnerType,
            ru.yandex.market.logistics.lom.model.enums.PartnerType.class
        ))
            .isNotNull()
            .isNotEqualTo(ru.yandex.market.logistics.lom.model.enums.PartnerType.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.logistics.lom.model.enums.PartnerType.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = "UNKNOWN"
    )
    @DisplayName("Конвертация PartnerType model->entity")
    void partnerTypeFromExternal(ru.yandex.market.logistics.lom.model.enums.PartnerType partnerType) {
        softly.assertThat(enumConverter.convert(
            partnerType,
            PartnerType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(RegistryStatus.class)
    @DisplayName("Конвертация RegistryStatus entity->model")
    void registryStatusToExternal(RegistryStatus registryStatus) {
        softly.assertThat(enumConverter.convert(
            registryStatus,
            ru.yandex.market.logistics.lom.model.enums.RegistryStatus.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.RegistryStatus.class)
    @DisplayName("Конвертация RegistryStatus model->entity")
    void registryStatusFromExternal(ru.yandex.market.logistics.lom.model.enums.RegistryStatus registryStatus) {
        softly.assertThat(enumConverter.convert(
            registryStatus,
            RegistryStatus.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = "UNKNOWN"
    )
    @DisplayName("Конвертация SegmentStatus entity->model")
    void registryStatusToExternal(SegmentStatus registryStatus) {
        softly.assertThat(enumConverter.convert(
            registryStatus,
            ru.yandex.market.logistics.lom.model.enums.SegmentStatus.class
        ))
            .isNotNull()
            .isNotEqualTo(ru.yandex.market.logistics.lom.model.enums.SegmentStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.logistics.lom.model.enums.SegmentStatus.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = "UNKNOWN"
    )
    @DisplayName("Конвертация SegmentStatus model->entity")
    void registryStatusFromExternal(ru.yandex.market.logistics.lom.model.enums.SegmentStatus registryStatus) {
        softly.assertThat(enumConverter.convert(
            registryStatus,
            SegmentStatus.class
        ))
            .isNotNull()
            .isNotEqualTo(SegmentStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(SegmentType.class)
    @DisplayName("Конвертация SegmentType entity->model")
    void segmentTypeToExternal(SegmentType segmentType) {
        softly.assertThat(enumConverter.convert(
            segmentType,
            ru.yandex.market.logistics.lom.model.enums.SegmentType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.SegmentType.class)
    @DisplayName("Конвертация SegmentType model->entity")
    void segmentTypeFromExternal(ru.yandex.market.logistics.lom.model.enums.SegmentType segmentType) {
        softly.assertThat(enumConverter.convert(
            segmentType,
            SegmentType.class
        ))
            .isNotNull();
    }

    @Test
    @DisplayName("Множества типов сегментов последней мили одинаковы во внутреннем и внешнем представлении")
    void lastMileSegmentTypesAreEqual() {
        softly.assertThat(
            SegmentType.LAST_MILE_SEGMENT_TYPES.stream()
                .map(segmentType -> enumConverter.convert(
                    segmentType,
                    ru.yandex.market.logistics.lom.model.enums.SegmentType.class
                ))
                .collect(Collectors.toSet())
        )
            .containsExactlyInAnyOrderElementsOf(
                ru.yandex.market.logistics.lom.model.enums.SegmentType.LAST_MILE_SEGMENT_TYPES
            );
    }

    @ParameterizedTest
    @EnumSource(ShipmentApplicationStatus.class)
    @DisplayName("Конвертация ShipmentApplicationStatus entity->model")
    void shipmentApplicationStatusToExternal(ShipmentApplicationStatus shipmentApplicationStatus) {
        softly.assertThat(enumConverter.convert(
            shipmentApplicationStatus,
            ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus.class)
    @DisplayName("Конвертация ShipmentApplicationStatus model->entity")
    void shipmentApplicationStatusFromExternal(
        ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus shipmentApplicationStatus
    ) {
        softly.assertThat(enumConverter.convert(
            shipmentApplicationStatus,
            ShipmentApplicationStatus.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ShipmentOption.class)
    @DisplayName("Конвертация ShipmentOption entity->model")
    void shipmentOptionToExternal(ShipmentOption shipmentOption) {
        softly.assertThat(enumConverter.convert(
            shipmentOption,
            ru.yandex.market.logistics.lom.model.enums.ShipmentOption.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.ShipmentOption.class)
    @DisplayName("Конвертация ShipmentOption model->entity")
    void shipmentOptionFromExternal(ru.yandex.market.logistics.lom.model.enums.ShipmentOption shipmentOption) {
        softly.assertThat(enumConverter.convert(
            shipmentOption,
            ShipmentOption.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ShipmentType.class)
    @DisplayName("Конвертация ShipmentType entity->model")
    void shipmentTypeToExternal(ShipmentType shipmentType) {
        softly.assertThat(enumConverter.convert(
            shipmentType,
            ru.yandex.market.logistics.lom.model.enums.ShipmentType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.ShipmentType.class)
    @DisplayName("Конвертация ShipmentType model->entity")
    void shipmentTypeFromExternal(ru.yandex.market.logistics.lom.model.enums.ShipmentType shipmentType) {
        softly.assertThat(enumConverter.convert(
            shipmentType,
            ShipmentType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(StorageUnitType.class)
    @DisplayName("Конвертация StorageUnitType entity->model")
    void storageUnitTypeToExternal(StorageUnitType storageUnitType) {
        softly.assertThat(enumConverter.convert(
            storageUnitType,
            ru.yandex.market.logistics.lom.model.enums.StorageUnitType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.StorageUnitType.class)
    @DisplayName("Конвертация StorageUnitType model->entity")
    void storageUnitTypeFromExternal(ru.yandex.market.logistics.lom.model.enums.StorageUnitType storageUnitType) {
        softly.assertThat(enumConverter.convert(
            storageUnitType,
            StorageUnitType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(TaxSystem.class)
    @DisplayName("Конвертация TaxSystem entity->model")
    void taxSystemToExternal(TaxSystem taxSystem) {
        softly.assertThat(enumConverter.convert(
            taxSystem,
            ru.yandex.market.logistics.lom.model.enums.TaxSystem.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.TaxSystem.class)
    @DisplayName("Конвертация TaxSystem model->entity")
    void taxSystemFromExternal(ru.yandex.market.logistics.lom.model.enums.TaxSystem taxSystem) {
        softly.assertThat(enumConverter.convert(
            taxSystem,
            TaxSystem.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(VatType.class)
    @DisplayName("Конвертация VatType entity->model")
    void vatTypeToExternal(VatType vatType) {
        softly.assertThat(enumConverter.convert(
            vatType,
            ru.yandex.market.logistics.lom.model.enums.VatType.class
        ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.VatType.class)
    @DisplayName("Конвертация VatType model->entity")
    void vatTypeFromExternal(ru.yandex.market.logistics.lom.model.enums.VatType vatType) {
        softly.assertThat(enumConverter.convert(
            vatType,
            VatType.class
        ))
            .isNotNull();
    }
}
