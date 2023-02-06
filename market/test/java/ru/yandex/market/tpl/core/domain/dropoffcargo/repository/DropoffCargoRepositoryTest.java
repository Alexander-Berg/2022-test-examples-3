package ru.yandex.market.tpl.core.domain.dropoffcargo.repository;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoFlowStatus;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class DropoffCargoRepositoryTest extends TplAbstractTest {

    private final DropoffCargoRepository subject;

    @Test
    void persistsCorrectly() {
        DropoffCargo expected = getDropoffCargo("barcode-some");

        var persisted = subject.save(expected);

        DropoffCargo result = subject.findByIdOrThrow(persisted.getId());

        assertThat(result.getId()).isNotNull();
        assertEquals(expected, result);
    }

    @Test
    void findByBarcode() {
        String barcode = "barcode-some";

        DropoffCargo dropoffCargo = getDropoffCargo(barcode);
        DropoffCargo dropoffCargoExcess = getDropoffCargo(barcode + "-postfix");

        var expected = subject.save(dropoffCargo);
        subject.save(dropoffCargoExcess);

        Optional<DropoffCargo> resultOpt = subject.findByBarcodeAndReferenceIdIsNull(barcode);

        assertThat(resultOpt.isPresent()).isTrue();
        assertThat(resultOpt.get().getId()).isEqualTo(expected.getId());
    }

    @NotNull
    private DropoffCargo getDropoffCargo(String barcode) {
        DropoffCargo dropoffCargo = new DropoffCargo();
        dropoffCargo.setStatus(DropoffCargoFlowStatus.DELIVERED_TO_LOGISTIC_POINT);
        dropoffCargo.setBarcode(barcode);
        dropoffCargo.setLogisticPointIdFrom("point-from");
        dropoffCargo.setLogisticPointIdTo("point-to");
        return dropoffCargo;
    }

    private void assertEquals(DropoffCargo expected, DropoffCargo result) {
        assertThat(result.getBarcode()).isEqualTo(expected.getBarcode());
        assertThat(result.getLogisticPointIdFrom()).isEqualTo(expected.getLogisticPointIdFrom());
        assertThat(result.getLogisticPointIdTo()).isEqualTo(expected.getLogisticPointIdTo());
    }

}
