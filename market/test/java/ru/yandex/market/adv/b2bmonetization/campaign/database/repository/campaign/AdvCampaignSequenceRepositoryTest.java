package ru.yandex.market.adv.b2bmonetization.campaign.database.repository.campaign;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;

/**
 * Date: 16.02.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
class AdvCampaignSequenceRepositoryTest extends AbstractMonetizationTest {

    @Autowired
    private AdvCampaignSequenceRepository advCampaignSequenceRepository;

    @DisplayName("Идентификатор следующей рекламной кампании получен успешно")
    @Test
    void getNextAdvCampaignId_nextId_success() {
        Assertions.assertThat(advCampaignSequenceRepository.getNextAdvCampaignId())
                .isGreaterThanOrEqualTo(1L);
    }

    @DisplayName("Идентификатор следующего действия над рекламной кампанией получен успешно")
    @Test
    void getNextAdvCampaignActionId_nextId_success() {
        Assertions.assertThat(advCampaignSequenceRepository.getNextAdvCampaignActionId())
                .isGreaterThanOrEqualTo(1L);
    }

    @DisplayName("Идентификатор следующего действия над товарной ставкой получен успешно")
    @Test
    void getNextOfferBidActionId_nextId_success() {
        Assertions.assertThat(advCampaignSequenceRepository.getNextOfferBidActionId())
                .isEqualTo(1L);
    }
}
