package ru.yandex.market.tpl.core.domain.dropoffcargo;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class DropoffCargoCommandServiceTest extends TplAbstractTest {

    private final DropoffCargoCommandService subject;
    private final DropoffCargoRepository repository;

    @Test
    void createOrGet() {
        var barcode = "barcode-test";
        var barcode2 = "barcode-test-2";

        DropoffCargo cargo1 = subject.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(barcode)
                        .logisticPointIdFrom("test")
                        .logisticPointIdTo("test")
                        .build()
        );
        subject.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(barcode2)
                        .logisticPointIdFrom("test")
                        .logisticPointIdTo("test")
                        .build()
        );
        DropoffCargo cargoDuplicate = subject.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(barcode)
                        .logisticPointIdFrom("test")
                        .logisticPointIdTo("test")
                        .build()
        );

        assertThat(repository.findAll().size()).isEqualTo(2);
        assertThat(cargo1.getId()).isEqualTo(cargoDuplicate.getId());
    }

}
