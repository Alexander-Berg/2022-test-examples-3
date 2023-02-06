package ru.yandex.market.logistics.management.service.validation;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.type.ShipmentType;

@DisplayName("Проверка консистентности способа отгрузки и перемещающего партнера")
class PartnerRelationShipmentValidationServiceTest extends AbstractTest {

    private PartnerRelationShipmentValidationService service = new PartnerRelationShipmentValidationService();

    @Test
    @DisplayName("Проверка всех валидных комбинаций shipmentType + пустой movingPartnerId")
    void testValidCasesNullMovingPartner() {
        service.validate(1L, 2L, null, ShipmentType.IMPORT);
        service.validate(1L, 2L, null, ShipmentType.WITHDRAW);
    }

    @Test
    @DisplayName("Проверка всех валидных комбинаций shipmentType + непустой movingPartnerId")
    void testValidCasesNotNullMovingPartner() {
        service.validate(1L, 2L, 1L, ShipmentType.IMPORT);
        service.validate(1L, 2L, 2L, ShipmentType.WITHDRAW);
        service.validate(1L, 2L, 3L, ShipmentType.TPL);
    }

    @Test
    @DisplayName("Проверка на shipmentType = null")
    void testNullShipmentType() {
        assertThrowsIllegalStateException(() -> service.validate(1L, 2L, 1L, null));
    }

    @Test
    @DisplayName("Проверка всех невалидных комбинаций с shipmentType = [IMPORT, WITHDRAW]")
    void testInvalidCasesForImportWithdraw() {
        assertThrowsIllegalStateException(() -> service.validate(1L, 2L, 2L, ShipmentType.IMPORT));
        assertThrowsIllegalStateException(() -> service.validate(1L, 2L, 1L, ShipmentType.WITHDRAW));

        assertThrowsIllegalStateException(() -> service.validate(1L, 2L, 3L, ShipmentType.IMPORT));
        assertThrowsIllegalStateException(() -> service.validate(1L, 2L, 3L, ShipmentType.WITHDRAW));
    }

    @Test
    @DisplayName("Проверка всех невалидных комбинаций с shipmentType = [3PL]")
    void testInvalidCasesForTpl() {
        assertThrowsIllegalStateException(() -> service.validate(1L, 2L, null, ShipmentType.TPL));
        assertThrowsIllegalStateException(() -> service.validate(1L, 2L, 1L, ShipmentType.TPL));
        assertThrowsIllegalStateException(() -> service.validate(1L, 2L, 2L, ShipmentType.TPL));
    }

    private void assertThrowsIllegalStateException(ThrowableAssert.ThrowingCallable callable) {
        softly.assertThatThrownBy(callable)
            .isInstanceOf(IllegalStateException.class);
    }
}
