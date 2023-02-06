package ru.yandex.market.core.partner.placement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataSet(before = "PartnerPlacementProgramServiceTest.before.csv")
class PartnerPlacementProgramServiceTest extends FunctionalTest {
    private static final long PARTNER_ID = 1001;
    private static final long FULL_PROGRAM_PARTNER_ID = 1002;
    private static final long DELIVERY_ID = 1005;

    @Autowired
    private PartnerPlacementProgramService partnerPlacementProgramService;

    @Test
    @DisplayName("Удаление существующей записи")
    @DbUnitDataSet(after = "PartnerPlacementProgramServiceTest.delete.after.csv")
    void testDelete() {
        assertThat(partnerPlacementProgramService.delete(
                PARTNER_ID,
                PartnerPlacementProgramType.DROPSHIP,
                100500L
        )).isTrue();
        assertThat(partnerPlacementProgramService.getPartnerPlacementProgram(
                PARTNER_ID,
                PartnerPlacementProgramType.DROPSHIP
        )).isEmpty();
    }

    @Test
    @DisplayName("Удаление несуществующей записи")
    @DbUnitDataSet(after = "PartnerPlacementProgramServiceTest.before.csv")
    void testNotFoundDelete() {
        assertThat(partnerPlacementProgramService.delete(
                PARTNER_ID,
                PartnerPlacementProgramType.CPC,
                100500L
        )).isFalse();
    }

    @Test
    @DisplayName("Получение всех типов записей")
    void testGetPartnerPlacementProgram() {
        var programs = partnerPlacementProgramService.getPartnerPlacementPrograms(FULL_PROGRAM_PARTNER_ID);
        assertThat(programs)
                .usingRecursiveComparison()
                .isEqualTo(Map.of(
                        PartnerPlacementProgramType.FULFILLMENT, PartnerPlacementProgram.builder()
                                .partnerId(FULL_PROGRAM_PARTNER_ID)
                                .program(PartnerPlacementProgramType.FULFILLMENT)
                                .status(PartnerPlacementProgramStatus.DISABLED)
                                .everActivated(true)
                                .createAt(LocalDateTime.of(2019, 10, 10, 12, 0, 12))
                                .updateAt(LocalDateTime.of(2019, 11, 10, 15, 10, 15))
                                .build(),
                        PartnerPlacementProgramType.DROPSHIP, PartnerPlacementProgram.builder()
                                .partnerId(FULL_PROGRAM_PARTNER_ID)
                                .program(PartnerPlacementProgramType.DROPSHIP)
                                .status(PartnerPlacementProgramStatus.SUCCESS)
                                .everActivated(true)
                                .createAt(LocalDateTime.of(2019, 10, 10, 12, 0, 12))
                                .updateAt(LocalDateTime.of(2019, 11, 10, 15, 10, 15))
                                .build(),
                        PartnerPlacementProgramType.CLICK_AND_COLLECT, PartnerPlacementProgram.builder()
                                .partnerId(FULL_PROGRAM_PARTNER_ID)
                                .program(PartnerPlacementProgramType.CLICK_AND_COLLECT)
                                .status(PartnerPlacementProgramStatus.FAIL)
                                .everActivated(false)
                                .createAt(LocalDateTime.of(2019, 10, 10, 12, 0, 12))
                                .updateAt(LocalDateTime.of(2019, 11, 10, 15, 10, 15))
                                .build(),
                        PartnerPlacementProgramType.CROSSDOCK, PartnerPlacementProgram.builder()
                                .partnerId(FULL_PROGRAM_PARTNER_ID)
                                .program(PartnerPlacementProgramType.CROSSDOCK)
                                .status(PartnerPlacementProgramStatus.DISABLED)
                                .everActivated(false)
                                .createAt(LocalDateTime.of(2019, 10, 10, 12, 0, 12))
                                .updateAt(LocalDateTime.of(2019, 11, 10, 15, 10, 15))
                                .build(),
                        PartnerPlacementProgramType.TURBO_PLUS, PartnerPlacementProgram.builder()
                                .partnerId(FULL_PROGRAM_PARTNER_ID)
                                .program(PartnerPlacementProgramType.TURBO_PLUS)
                                .status(PartnerPlacementProgramStatus.TESTED)
                                .everActivated(false)
                                .createAt(LocalDateTime.of(2019, 10, 10, 12, 0, 12))
                                .updateAt(LocalDateTime.of(2019, 11, 10, 15, 10, 15))
                                .build(),
                        PartnerPlacementProgramType.DROPSHIP_BY_SELLER, PartnerPlacementProgram.builder()
                                .partnerId(FULL_PROGRAM_PARTNER_ID)
                                .program(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                                .status(PartnerPlacementProgramStatus.TESTED)
                                .everActivated(false)
                                .createAt(LocalDateTime.of(2019, 10, 10, 12, 0, 12))
                                .updateAt(LocalDateTime.of(2019, 11, 10, 15, 10, 15))
                                .build(),
                        PartnerPlacementProgramType.CPC, PartnerPlacementProgram.builder()
                                .partnerId(FULL_PROGRAM_PARTNER_ID)
                                .program(PartnerPlacementProgramType.CPC)
                                .status(PartnerPlacementProgramStatus.SUCCESS)
                                .everActivated(false)
                                .createAt(LocalDateTime.of(2019, 10, 10, 12, 0, 12))
                                .updateAt(LocalDateTime.of(2019, 11, 10, 15, 10, 15))
                                .build()
                ));
    }

    @Test
    @DisplayName("Обновление существующей записи")
    @DbUnitDataSet(after = "PartnerPlacementProgramServiceTest.update.after.csv")
    void testSaveUpdate() {
        partnerPlacementProgramService.save(
                PARTNER_ID,
                PartnerPlacementProgramType.FULFILLMENT,
                PartnerPlacementProgramStatus.DISABLED,
                100500L
        );
        var program = partnerPlacementProgramService.getPartnerPlacementProgram(
                PARTNER_ID,
                PartnerPlacementProgramType.FULFILLMENT
        );
        assertThat(program).hasValueSatisfying(p ->
                assertThat(p).isEqualToIgnoringGivenFields(
                        PartnerPlacementProgram.builder()
                                .partnerId(PARTNER_ID)
                                .program(PartnerPlacementProgramType.FULFILLMENT)
                                .status(PartnerPlacementProgramStatus.DISABLED)
                                .everActivated(true)
                                .createAt(LocalDateTime.of(2019, 10, 10, 12, 0, 12))
                                .updateAt(LocalDateTime.now())
                                .build(),
                        "updateAt"
                )
        );
    }

    @Test
    @DisplayName("Вставка новой записи")
    @DbUnitDataSet(after = "PartnerPlacementProgramServiceTest.insert.after.csv")
    void testSaveInsert() {
        partnerPlacementProgramService.save(PARTNER_ID,
                PartnerPlacementProgramType.CLICK_AND_COLLECT,
                PartnerPlacementProgramStatus.CONFIGURE,
                100500L);
        Optional<PartnerPlacementProgram> program =
                partnerPlacementProgramService.getPartnerPlacementProgram(
                        PARTNER_ID, PartnerPlacementProgramType.CLICK_AND_COLLECT);
        assertThat(program).hasValueSatisfying(p ->
                assertThat(p).isEqualToIgnoringGivenFields(
                        PartnerPlacementProgram.builder()
                                .partnerId(PARTNER_ID)
                                .program(PartnerPlacementProgramType.CLICK_AND_COLLECT)
                                .status(PartnerPlacementProgramStatus.CONFIGURE)
                                .everActivated(false)
                                .createAt(LocalDateTime.now())
                                .updateAt(LocalDateTime.now())
                                .build(),
                        "updateAt",
                        "createAt"
                )
        );
    }

    @Test
    @DisplayName("Попытка вставки новой записи для доставки")
    @DbUnitDataSet(after = "PartnerPlacementProgramServiceTest.before.csv")
    void testSaveForDelivery() {
        partnerPlacementProgramService.save(DELIVERY_ID,
                PartnerPlacementProgramType.CLICK_AND_COLLECT,
                PartnerPlacementProgramStatus.CONFIGURE,
                100500L);
    }

    @Test
    @DbUnitDataSet(after = "PartnerPlacementProgramServiceTest.before.csv")
    void saveIfPresentShouldNotSaveDisabledPrograms() {
        partnerPlacementProgramService.saveIfProgramPresent(
                DELIVERY_ID,
                PartnerPlacementProgramType.CROSSDOCK,
                PartnerPlacementProgramStatus.DISABLED,
                100500L
        );
    }

    @Test
    @DbUnitDataSet(
            after = "PartnerPlacementProgramServiceTest" +
                    ".saveIfPresentShouldRemoveDisabledProgramsWithFalseEverActivated.after.csv"
    )
    void saveIfPresentShouldRemoveDisabledProgramsWithFalseEverActivated() {
        partnerPlacementProgramService.saveIfProgramPresent(
                PARTNER_ID,
                PartnerPlacementProgramType.CROSSDOCK,
                PartnerPlacementProgramStatus.DISABLED,
                100500L
        );
        partnerPlacementProgramService.saveIfProgramPresent(
                FULL_PROGRAM_PARTNER_ID,
                PartnerPlacementProgramType.CROSSDOCK,
                PartnerPlacementProgramStatus.DISABLED,
                100500L
        );
    }

    @Test
    void testFindPartnersByProgramTypesAndStatus() {
        assertEquals(Set.of(),
                partnerPlacementProgramService.findPartnersByProgramTypesAndStatus(
                        List.of(PartnerPlacementProgramType.FULFILLMENT, PartnerPlacementProgramType.DROPSHIP),
                        PartnerPlacementProgramStatus.TESTED
                ));
        assertEquals(Set.of(1002L),
                partnerPlacementProgramService.findPartnersByProgramTypesAndStatus(
                        List.of(PartnerPlacementProgramType.FULFILLMENT, PartnerPlacementProgramType.DROPSHIP),
                        PartnerPlacementProgramStatus.DISABLED
                ));
        assertEquals(Set.of(1001L, 1002L),
                partnerPlacementProgramService.findPartnersByProgramTypesAndStatus(
                        List.of(PartnerPlacementProgramType.FULFILLMENT, PartnerPlacementProgramType.DROPSHIP),
                        PartnerPlacementProgramStatus.SUCCESS
                ));
    }
}
