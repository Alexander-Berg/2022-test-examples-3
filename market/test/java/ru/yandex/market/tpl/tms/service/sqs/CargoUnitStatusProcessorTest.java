package ru.yandex.market.tpl.tms.service.sqs;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.les.sc.CargoUnit;
import ru.yandex.market.logistics.les.sc.CargoUnitType;
import ru.yandex.market.logistics.les.sc.DoCargoUnitStatusEvent;
import ru.yandex.market.logistics.les.sc.DoUnitStatusEventType;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class CargoUnitStatusProcessorTest extends TplTmsAbstractTest {

    public static final String BARCODE = "BARCODE";
    public static final String REF = "REF_ID";
    public static final Long LOGISTIC_POINT_ID_TO = 123L;
    public static final Long LOGISTIC_POINT_ID_FROM = 234L;
    public static final long EXPECTED_NEW_VERSION = 1L;
    public static final long EXPECTED_OLD_VERSION = 0L;
    private final CargoUnitStatusProcessor cargoUnitStatusProcessor;
    private final DropoffCargoCommandService dropoffCargoCommandService;
    private final DropoffCargoRepository dropoffCargoRepository;

    @Test
    void process_when_needsUpdate() {
        //given
        var cargo = buildCargoWithVersion(null);

        //when
        cargoUnitStatusProcessor.process(buildEvent(DoUnitStatusEventType.DELETED_FROM_READY, EXPECTED_NEW_VERSION));

        //then
        var processedCargo = dropoffCargoRepository.findByIdOrThrow(cargo.getId());
        assertThat(processedCargo.getVersion()).isEqualTo(EXPECTED_NEW_VERSION);
        assertThat(processedCargo.getIsDeleted()).isTrue();
    }

    @Test
    void process_when_NotNeedsUpdate() {
        //given
        var cargo = buildCargoWithVersion(EXPECTED_NEW_VERSION);

        //when
        cargoUnitStatusProcessor.process(buildEvent(DoUnitStatusEventType.DELETED_FROM_READY, EXPECTED_OLD_VERSION));

        //then
        var processedCargo = dropoffCargoRepository.findByIdOrThrow(cargo.getId());
        assertThat(processedCargo.getVersion()).isEqualTo(EXPECTED_NEW_VERSION);
        assertThat(processedCargo.getIsDeleted()).isFalse();
    }

    @Test
    void process_when_needsCreate() {
        //when
        cargoUnitStatusProcessor.process(buildEvent(DoUnitStatusEventType.DELETED_FROM_READY, EXPECTED_OLD_VERSION));

        //then
        var cargos = dropoffCargoRepository.findAll();
        assertThat(cargos).hasSize(1);
        var processedCargo = cargos.get(0);
        assertThat(processedCargo.getVersion()).isEqualTo(EXPECTED_OLD_VERSION);
        assertThat(processedCargo.getIsDeleted()).isTrue();
    }

    private DropoffCargo buildCargoWithVersion(Long version) {
        return dropoffCargoCommandService.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(BARCODE)
                        .referenceId(REF)
                        .logisticPointIdTo(String.valueOf(LOGISTIC_POINT_ID_TO))
                        .logisticPointIdFrom(String.valueOf(LOGISTIC_POINT_ID_FROM))
                        .version(version)
                        .build()
        );
    }

    @NotNull
    private DoCargoUnitStatusEvent buildEvent(DoUnitStatusEventType type, long version) {
        var cargoUnit = new CargoUnit(BARCODE, REF, CargoUnitType.BOX, LOGISTIC_POINT_ID_FROM, LOGISTIC_POINT_ID_TO);
        return new DoCargoUnitStatusEvent(cargoUnit, version, type);
    }
}
