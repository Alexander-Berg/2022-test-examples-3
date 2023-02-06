package ru.yandex.market.delivery.tracker.domain.admin;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.delivery.tracker.domain.converter.EnumConverter;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

public class AdminUnknownCheckpointStatusTest {
    private static final Predicate<String> NOT_UNKNOWN = Predicate.not("UNKNOWN"::equals);

    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @ParameterizedTest
    @EnumSource(value = AdminUnknownCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void allUnknownsHasPerEntityInnerValues(AdminUnknownCheckpointStatus value) {
        softly.assertThatCode(
            () -> DeliveryCheckpointStatus.findByIdAndEntityType(value.getId(), value.getEntityType())
        ).doesNotThrowAnyException();
    }

    /*
     * Order
     */
    @ParameterizedTest
    @EnumSource(value = AdminDeliveryOrderCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void deliveryOrderStatusesToUnknown(AdminDeliveryOrderCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(value = AdminFulfillmentOrderCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void fulfillmentOrderStatusesToUnknown(AdminFulfillmentOrderCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @Test
    void unknownCheckpointStatuesOrderAreAllPresentInTyped() {
        compareAdminUnknownCheckpointStatusByType(
            EntityType.ORDER,
            AdminDeliveryOrderCheckpointStatus.class,
            AdminFulfillmentOrderCheckpointStatus.class
        );
    }

    /*
     * Movement
     */
    @ParameterizedTest
    @EnumSource(value = AdminDeliveryMovementCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void deliveryMovementStatusesToUnknown(AdminDeliveryMovementCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(value = AdminFulfillmentMovementCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void fulfillmentMovementStatusesToUnknown(AdminFulfillmentMovementCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @Test
    void unknownCheckpointStatuesMovementAreAllPresentInTyped() {
        compareAdminUnknownCheckpointStatusByType(
            EntityType.MOVEMENT,
            AdminDeliveryMovementCheckpointStatus.class,
            AdminFulfillmentMovementCheckpointStatus.class
        );
    }

    /*
     * Inbound
     */
    @ParameterizedTest
    @EnumSource(value = AdminDeliveryInboundCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void deliveryInboundStatusesToUnknown(AdminDeliveryInboundCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(value = AdminFulfillmentInboundCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void fulfillmentInboundStatusesToUnknown(AdminFulfillmentInboundCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @Test
    void unknownCheckpointStatuesInboundAreAllPresentInTyped() {
        compareAdminUnknownCheckpointStatusByType(
            EntityType.INBOUND,
            AdminDeliveryInboundCheckpointStatus.class,
            AdminFulfillmentInboundCheckpointStatus.class
        );
    }

    /*
     * Outbound
     */
    @ParameterizedTest
    @EnumSource(value = AdminDeliveryOutboundCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void deliveryOutboundStatusesToUnknown(AdminDeliveryOutboundCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(value = AdminFulfillmentOutboundCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void fulfillmentOutboundStatusesToUnknown(AdminFulfillmentOutboundCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @Test
    void unknownCheckpointStatuesOutboundAreAllPresentInTyped() {
        compareAdminUnknownCheckpointStatusByType(
            EntityType.OUTBOUND,
            AdminDeliveryOutboundCheckpointStatus.class,
            AdminFulfillmentOutboundCheckpointStatus.class
        );
    }

    /*
     * Transfer
     */
    @ParameterizedTest
    @EnumSource(value = AdminFulfillmentTransferCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void fulfillmentTransferStatusesToUnknown(AdminFulfillmentTransferCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @Test
    void unknownCheckpointStatuesTransferAreAllPresentInTyped() {
        compareAdminUnknownCheckpointStatusByType(
            EntityType.TRANSFER,
            AdminFulfillmentTransferCheckpointStatus.class
        );
    }

    /*
     * InboundOld
     */
    @ParameterizedTest
    @EnumSource(value = AdminFulfillmentInboundOldCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void fulfillmentInboundOldStatusesToUnknown(AdminFulfillmentInboundOldCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @Test
    void unknownCheckpointStatuesInboundOldAreAllPresentInTyped() {
        compareAdminUnknownCheckpointStatusByType(
            EntityType.INBOUND_OLD,
            AdminFulfillmentInboundOldCheckpointStatus.class
        );
    }

    /*
     * OutboundOld
     */
    @ParameterizedTest
    @EnumSource(value = AdminFulfillmentOutboundOldCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void fulfillmentOutboundOldStatusesToUnknown(AdminFulfillmentOutboundOldCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @Test
    void unknownCheckpointStatuesOutboundOldAreAllPresentInTyped() {
        compareAdminUnknownCheckpointStatusByType(
            EntityType.OUTBOUND_OLD,
            AdminFulfillmentOutboundOldCheckpointStatus.class
        );
    }

    /*
     * ExternalOrder
     */
    @ParameterizedTest
    @EnumSource(value = AdminDeliveryExternalOrderCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void deliveryExternalOrderStatusesToUnknown(AdminDeliveryExternalOrderCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @Test
    void unknownCheckpointStatusesExternalOrderAreAllPresentInTyped() {
        compareAdminUnknownCheckpointStatusByType(
            EntityType.EXTERNAL_ORDER,
            AdminDeliveryExternalOrderCheckpointStatus.class
        );
    }

    /*
     * OrderReturn
     */
    @ParameterizedTest
    @EnumSource(value = AdminDeliveryOrderReturnCheckpointStatus.class, names = "UNKNOWN", mode = EXCLUDE)
    void deliveryOrderReturnStatusesToUnknown(AdminDeliveryOrderReturnCheckpointStatus value) {
        softly.assertThat(EnumConverter.convert(value, AdminUnknownCheckpointStatus.class))
            .isNotNull()
            .isNotEqualTo(AdminUnknownCheckpointStatus.UNKNOWN);
    }

    @Test
    void unknownCheckpointStatusesOrderReturnAreAllPresentInTyped() {
        compareAdminUnknownCheckpointStatusByType(
            EntityType.ORDER_RETURN,
            AdminDeliveryOrderReturnCheckpointStatus.class
        );
    }

    /**
     * Производит проверку, что по указанному {@link EntityType} все чекпоинты из {@link AdminUnknownCheckpointStatus}
     * будут соответствовать переданным типизированным чекпоинтам.
     *
     * @param entityType      тип сущности чекпоинта
     * @param adminTypesEnums соответствующие переданному типу админские перечисления сущностей
     */
    private void compareAdminUnknownCheckpointStatusByType(
        EntityType entityType,
        Class<? extends Enum<?>>... adminTypesEnums
    ) {
        Set<String> unknowns = Arrays.stream(AdminUnknownCheckpointStatus.values())
            .filter(status -> status.getEntityType() == entityType)
            .map(AdminUnknownCheckpointStatus::name)
            .filter(NOT_UNKNOWN)
            .collect(Collectors.toSet());

        Set<String> typed = Arrays.stream(adminTypesEnums)
            .map(Class::getEnumConstants)
            .flatMap(Arrays::stream)
            .map(Enum::name)
            .filter(NOT_UNKNOWN)
            .collect(Collectors.toSet());

        softly.assertThat(typed)
            .as("Check AdminUnknownCheckpointStatus and %s admin typed enum", entityType)
            .hasSameElementsAs(unknowns);
    }
}
