package ru.yandex.market.adv.b2bmonetization.programs.database.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.programs.database.entity.PartnerProgramEntity;
import ru.yandex.market.adv.b2bmonetization.programs.model.PartnerColor;
import ru.yandex.market.adv.b2bmonetization.programs.model.PartnerProgramStatus;
import ru.yandex.market.adv.b2bmonetization.programs.model.PartnerProgramType;
import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Проверяет работу с сущностью участника.
 */
@ParametersAreNonnullByDefault
public class PartnerProgramRepositoryTest extends AbstractMonetizationTest {

    @Autowired
    private PartnerProgramRepository partnerProgramRepository;
    @Autowired
    private TimeService timeService;

    @DisplayName("По идентификатору партнера, типа программы и бизнесу нашли участника")
    @DbUnitDataSet(
            before = "PartnerProgramRepository/csv" +
                    "/findByPartnerIdAndProgramTypeAndBusinessId_exist_paricipantData.before.csv"
    )
    @Test
    public void findByPartnerIdAndProgramTypeAndBusinessId_exist_paricipantData() {
        Assertions.assertThat(
                        partnerProgramRepository.findByPartnerIdAndProgramType(10L, PartnerProgramType.NEWBIE)
                )
                .hasSize(1)
                .containsExactly(
                        PartnerProgramEntity
                                .builder()
                                .programId(1023L)
                                .partnerId(10L)
                                .programType(PartnerProgramType.NEWBIE)
                                .businessId(1021L)
                                .uid(421L)
                                .color(PartnerColor.WHITE)
                                .status(PartnerProgramStatus.REFUSED)
                                .enabled(false)
                                .createdAt(toInstant(LocalDateTime.of(2021, 10, 21, 13, 42, 53)))
                                .updatedAt(toInstant(LocalDateTime.of(2021, 10, 21, 13, 42, 53)))
                                .build()
                );
    }

    @DisplayName("Активирует партнера с указанным идентификатором и типом программы")
    @DbUnitDataSet(
            before = "PartnerProgramRepository/csv/setActivate_wasFalse_thenTrue.before.csv",
            after = "PartnerProgramRepository/csv/setActivate_wasFalse_thenTrue.after.csv"
    )
    @Test
    public void setActivate_wasFalse_thenTrue() {
        partnerProgramRepository.setStatus(1023L, PartnerProgramStatus.ACTIVATED, timeService.get());
    }

    @Nonnull
    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneOffset.systemDefault())
                .toInstant();
    }
}
