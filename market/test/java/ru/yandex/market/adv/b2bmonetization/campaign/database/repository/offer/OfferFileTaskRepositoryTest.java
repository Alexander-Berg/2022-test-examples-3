package ru.yandex.market.adv.b2bmonetization.campaign.database.repository.offer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.database.entity.offer.file.FileTaskType;
import ru.yandex.market.adv.b2bmonetization.campaign.database.entity.offer.file.OfferFileEntity;
import ru.yandex.market.adv.b2bmonetization.campaign.database.entity.offer.file.OfferFileTaskEntity;
import ru.yandex.market.adv.b2bmonetization.campaign.database.entity.offer.file.PartnerType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 16.02.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
class OfferFileTaskRepositoryTest extends AbstractMonetizationTest {

    @Autowired
    private OfferFileTaskRepository offerFileTaskRepository;

    @DisplayName("Вернули только первую запись при limit 1.")
    @DbUnitDataSet(
            before = "OfferFileTaskRepository/csv/findActiveTasks_limitOneRow_success.csv"
    )
    @Test
    void findActiveTasks_limitOneRow_success() {
        Assertions.assertThat(offerFileTaskRepository.findActiveTasks(1L))
                .containsExactly(
                        OfferFileTaskEntity.builder()
                                .taskId(5L)
                                .type(FileTaskType.REMOVED)
                                .fileId(4L)
                                .campaignId(532L)
                                .offerFile(
                                        OfferFileEntity.builder()
                                                .id(4L)
                                                .systemName("rew3.xlsm")
                                                .name("r3.xlsm")
                                                .size(421L)
                                                .partnerId(523L)
                                                .partnerType(PartnerType.DBS)
                                                .updateAt(toInstant(LocalDateTime.of(2021, 10, 20, 13, 42, 53)))
                                                .used(true)
                                                .offerCount(562L)
                                                .minBid(new BigDecimal("4.20"))
                                                .maxBid(new BigDecimal("10"))
                                                .campaignId(532L)
                                                .build()
                                )
                                .build()
                );
    }

    @DisplayName("Выбрали все записи из таблицы.")
    @DbUnitDataSet(
            before = "OfferFileTaskRepository/csv/findActiveTasks_readAllRow_success.csv"
    )
    @Test
    void findActiveTasks_readAllRow_success() {
        Assertions.assertThat(offerFileTaskRepository.findActiveTasks(5L))
                .containsExactly(
                        OfferFileTaskEntity.builder()
                                .taskId(4L)
                                .type(FileTaskType.UPDATED)
                                .fileId(4L)
                                .campaignId(532L)
                                .offerFile(
                                        OfferFileEntity.builder()
                                                .id(4L)
                                                .systemName("rew3.xlsm")
                                                .name("r3.xlsm")
                                                .size(421L)
                                                .partnerId(523L)
                                                .partnerType(PartnerType.DBS)
                                                .updateAt(toInstant(LocalDateTime.of(2021, 10, 20, 13, 42, 53)))
                                                .used(true)
                                                .offerCount(562L)
                                                .minBid(new BigDecimal("4.20"))
                                                .maxBid(new BigDecimal("10"))
                                                .campaignId(532L)
                                                .build()
                                )
                                .build(),
                        OfferFileTaskEntity.builder()
                                .taskId(7L)
                                .type(FileTaskType.CREATED)
                                .fileId(6L)
                                .campaignId(893L)
                                .offerFile(
                                        OfferFileEntity.builder()
                                                .id(6L)
                                                .systemName("rew5.xlsm")
                                                .name("r5.xlsm")
                                                .size(421L)
                                                .partnerId(524L)
                                                .partnerType(PartnerType.FB)
                                                .updateAt(toInstant(LocalDateTime.of(2021, 7, 21, 13, 42, 53)))
                                                .used(true)
                                                .offerCount(952L)
                                                .minBid(new BigDecimal("4.32"))
                                                .maxBid(new BigDecimal("5.354"))
                                                .campaignId(893L)
                                                .build()
                                )
                                .build()
                );
    }

    @DisplayName("Обновление записи прошло успешно.")
    @DbUnitDataSet(
            before = "OfferFileTaskRepository/csv/setCompleteTrue_competeTrue_success.before.csv",
            after = "OfferFileTaskRepository/csv/setCompleteTrue_competeTrue_success.after.csv"
    )
    @Test
    void setCompleteTrue_competeTrue_success() {
        Assertions.assertThat(offerFileTaskRepository.setCompleteTrue(4175L))
                .isEqualTo(1L);
    }

    @DisplayName("Ничего не обновили по неизвестному id.")
    @Test
    void setCompleteTrue_unknownId_nothing() {
        Assertions.assertThat(offerFileTaskRepository.setCompleteTrue(693L))
                .isEqualTo(0L);
    }

    @DisplayName("Новая запись корректно создалась.")
    @DbUnitDataSet(
            before = "OfferFileTaskRepository/csv/insert_createNew_success.before.csv",
            after = "OfferFileTaskRepository/csv/insert_createNew_success.after.csv"
    )
    @Test
    void insert_createNew_success() {
        offerFileTaskRepository.insert(6L, 893L, FileTaskType.UPDATED);
    }

    @Nonnull
    private Instant toInstant(@Nonnull LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneOffset.systemDefault())
                .toInstant();
    }
}
