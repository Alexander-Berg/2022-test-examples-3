package ru.yandex.market.logistics.mqm.entity.lom.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.utils.convertEnum

@DisplayName("Проверка конвертации перечислений из LOM")
open class LomEnumTest: AbstractTest() {

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.DeliveryType::class)
    fun convertDeliveryType(value: ru.yandex.market.logistics.lom.model.enums.DeliveryType) {
        assertThat(convertEnum<DeliveryType>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.logistics.lom.model.enums.OrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["UNKNOWN"]
    )
    fun convertOrderStatus(value: ru.yandex.market.logistics.lom.model.enums.OrderStatus) {
        assertThat(convertEnum<OrderStatus>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.SegmentStatus::class)
    fun convertSegmentStatus(value: ru.yandex.market.logistics.lom.model.enums.SegmentStatus) {
        assertThat(convertEnum<SegmentStatus>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.SegmentType::class)
    fun convertSegmentType(value: ru.yandex.market.logistics.lom.model.enums.SegmentType) {
        assertThat(convertEnum<SegmentType>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.logistics.lom.model.enums.PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["UNKNOWN"]
    )
    fun convertPartnerType(value: ru.yandex.market.logistics.lom.model.enums.PartnerType) {
        assertThat(convertEnum<PartnerType>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.PartnerSubtype::class)
    fun convertPartnerSubtype(value: ru.yandex.market.logistics.lom.model.enums.PartnerSubtype) {
        assertThat(convertEnum<PartnerSubtype>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.PlatformClient::class)
    fun convertPlatformClient(value: ru.yandex.market.logistics.lom.model.enums.PlatformClient) {
        assertThat(convertEnum<PlatformClient>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.ShipmentType::class)
    fun convertShipmentType(value: ru.yandex.market.logistics.lom.model.enums.ShipmentType) {
        assertThat(convertEnum<ShipmentType>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.tags.WaybillSegmentTag::class)
    fun convertWaybillSegmentTag(value: ru.yandex.market.logistics.lom.model.enums.tags.WaybillSegmentTag) {
        assertThat(convertEnum<WaybillSegmentTag>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.tags.OrderTag::class)
    fun convertOrderTag(value: ru.yandex.market.logistics.lom.model.enums.tags.OrderTag) {
        assertThat(convertEnum<OrderTag>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.LocationType::class)
    fun convertLocationType(value: ru.yandex.market.logistics.lom.model.enums.LocationType) {
        assertThat(convertEnum<LocationType>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus::class)
    fun convertCancellationOrderStatus(value: ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus) {
        assertThat(convertEnum<CancellationOrderStatus>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.PaymentMethod::class)
    fun convertCancellationOrderStatus(value: ru.yandex.market.logistics.lom.model.enums.PaymentMethod) {
        assertThat(convertEnum<PaymentMethod>(value)).isNotNull
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.lom.model.enums.StorageUnitType::class)
    fun convertCancellationOrderStatus(value: ru.yandex.market.logistics.lom.model.enums.StorageUnitType) {
        assertThat(convertEnum<StorageUnitType>(value)).isNotNull
    }
}
