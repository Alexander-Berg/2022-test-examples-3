package ru.yandex.market.adv.b2bmonetization.campaign.database.repository.offer;

import java.math.BigDecimal;
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
import ru.yandex.market.adv.b2bmonetization.campaign.database.entity.offer.file.OfferFileEntity;
import ru.yandex.market.adv.b2bmonetization.campaign.database.entity.offer.file.PartnerType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 16.02.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
class OfferFileRepositoryTest extends AbstractMonetizationTest {

    @Autowired
    private OfferFileRepository offerFileRepository;

    @DisplayName("По id нашли запись в базе.")
    @DbUnitDataSet(
            before = "OfferFileRepository/csv/findById_correctId_exist.csv"
    )
    @Test
    void findById_correctId_exist() {
        Assertions.assertThat(offerFileRepository.findById(1L))
                .contains(
                        OfferFileEntity.builder()
                                .id(1L)
                                .systemName("rew.xlsm")
                                .name("r.xlsm")
                                .size(421L)
                                .partnerType(PartnerType.DBS)
                                .partnerId(523)
                                .updateAt(toInstant(LocalDateTime.of(2021, 10, 21, 13, 42, 53)))
                                .used(true)
                                .build()
                );
    }

    @DisplayName("По id не нашли запись в базе.")
    @DbUnitDataSet(
            before = "OfferFileRepository/csv/findById_wrongId_empty.csv"
    )
    @Test
    void findById_wrongId_empty() {
        Assertions.assertThat(offerFileRepository.findById(12L))
                .isEmpty();
    }

    @DisplayName("По campaign_id и partner_id нашли запись в базе.")
    @DbUnitDataSet(
            before = "OfferFileRepository/csv/findByCampaignIdAndPartnerId_correctId_exist.csv"
    )
    @Test
    void findByCampaignIdAndPartnerId_correctId_exist() {
        Assertions.assertThat(offerFileRepository.findByCampaignIdAndPartnerId(412L, 523L))
                .contains(
                        OfferFileEntity.builder()
                                .id(2L)
                                .systemName("rew.xlsm")
                                .name("r.xlsm")
                                .size(421L)
                                .partnerType(PartnerType.DBS)
                                .partnerId(523)
                                .updateAt(toInstant(LocalDateTime.of(2021, 10, 21, 13, 42, 53)))
                                .used(true)
                                .offerCount(952L)
                                .minBid(new BigDecimal("4.32"))
                                .maxBid(new BigDecimal("5.354"))
                                .campaignId(412L)
                                .build()
                );
    }

    @DisplayName("По campaign_id и partner_id не нашли запись в базе.")
    @DbUnitDataSet(
            before = "OfferFileRepository/csv/findByCampaignIdAndPartnerId_wrongId_empty.csv"
    )
    @Test
    void findByCampaignIdAndPartnerId_wrongId_empty() {
        Assertions.assertThat(offerFileRepository.findByCampaignIdAndPartnerId(412L, 513L))
                .isEmpty();
    }

    @DisplayName("Нашли две старые записи, время хранения которых истекло.")
    @DbUnitDataSet(
            before = "OfferFileRepository/csv/findOld_containTwoOldRow_success.csv"
    )
    @Test
    void findOld_containTwoOldRow_success() {
        Assertions.assertThat(
                        offerFileRepository.findOld(
                                toInstant(LocalDateTime.of(2021, 8, 15, 13, 42, 53)),
                                toInstant(LocalDateTime.of(2021, 10, 21, 13, 42, 53)),
                                5L
                        )
                )
                .containsExactlyInAnyOrder(
                        OfferFileEntity.builder()
                                .id(4L)
                                .systemName("rew3.xlsm")
                                .name("r3.xlsm")
                                .size(421L)
                                .partnerType(PartnerType.DBS)
                                .partnerId(523)
                                .updateAt(toInstant(LocalDateTime.of(2021, 10, 20, 13, 42, 53)))
                                .used(false)
                                .offerCount(562L)
                                .minBid(new BigDecimal("4.20"))
                                .maxBid(new BigDecimal("10"))
                                .build(),
                        OfferFileEntity.builder()
                                .id(6L)
                                .systemName("rew5.xlsm")
                                .name("r5.xlsm")
                                .size(421L)
                                .partnerType(PartnerType.FB)
                                .partnerId(525)
                                .updateAt(toInstant(LocalDateTime.of(2021, 7, 21, 13, 42, 53)))
                                .used(true)
                                .offerCount(952L)
                                .minBid(new BigDecimal("4.32"))
                                .maxBid(new BigDecimal("5.354"))
                                .build()
                );
    }

    @DisplayName("Нашли одну старую запись, при limit 1.")
    @DbUnitDataSet(
            before = "OfferFileRepository/csv/findOld_containTwoOldRowAndLimitOne_success.csv"
    )
    @Test
    void findOld_containTwoOldRowAndLimitOne_success() {
        Assertions.assertThat(
                        offerFileRepository.findOld(
                                toInstant(LocalDateTime.of(2021, 8, 15, 13, 42, 53)),
                                toInstant(LocalDateTime.of(2021, 10, 21, 13, 42, 53)),
                                1L
                        )
                )
                .hasSize(1);
    }

    @DisplayName("Удаление по id прошло успешно.")
    @DbUnitDataSet(
            before = "OfferFileRepository/csv/delete_oneRow_success.before.csv",
            after = "OfferFileRepository/csv/delete_oneRow_success.after.csv"
    )
    @Test
    void delete_oneRow_success() {
        offerFileRepository.delete(
                OfferFileEntity.builder()
                        .id(2L)
                        .build()
        );
    }

    @DisplayName("Удаление по id прошло успешно. Связанные записи удалили каскадно.")
    @DbUnitDataSet(
            before = "OfferFileRepository/csv/delete_oneRow_deleteCascade.before.csv",
            after = "OfferFileRepository/csv/delete_oneRow_deleteCascade.after.csv"
    )
    @Test
    void delete_oneRow_deleteCascade() {
        offerFileRepository.delete(
                OfferFileEntity.builder()
                        .id(2L)
                        .build()
        );
    }

    @DisplayName("Новая запись корректно создалась.")
    @DbUnitDataSet(
            before = "OfferFileRepository/csv/save_createNew_success.before.csv",
            after = "OfferFileRepository/csv/save_createNew_success.after.csv"
    )
    @Test
    void save_createNew_success() {
        Assertions.assertThat(
                        offerFileRepository.save(
                                        OfferFileEntity.builder()
                                                .systemName("rew1.xlsm")
                                                .name("r1.xlsm")
                                                .size(421L)
                                                .partnerType(PartnerType.FB)
                                                .partnerId(542)
                                                .updateAt(toInstant(LocalDateTime.of(2021, 10, 21, 13, 42, 53)))
                                                .used(false)
                                                .build()
                                )
                                .getId()
                )
                .isGreaterThanOrEqualTo(1L);
    }

    @DisplayName("Обновление записи прошло успешно.")
    @DbUnitDataSet(
            before = "OfferFileRepository/csv/save_updateExist_success.before.csv",
            after = "OfferFileRepository/csv/save_updateExist_success.after.csv"
    )
    @Test
    void save_updateExist_success() {
        offerFileRepository.save(
                OfferFileEntity.builder()
                        .id(1590L)
                        .systemName("rew2.xlsm")
                        .name("r2.xlsm")
                        .size(422L)
                        .partnerId(545)
                        .partnerType(PartnerType.DBS)
                        .updateAt(toInstant(LocalDateTime.of(2021, 10, 21, 16, 42, 53)))
                        .used(true)
                        .offerCount(952L)
                        .minBid(new BigDecimal("4.34"))
                        .maxBid(new BigDecimal("5.354"))
                        .campaignId(532L)
                        .build()
        );
    }

    @Nonnull
    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneOffset.systemDefault())
                .toInstant();
    }
}
