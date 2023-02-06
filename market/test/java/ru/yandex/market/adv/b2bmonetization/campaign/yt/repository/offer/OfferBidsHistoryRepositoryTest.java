package ru.yandex.market.adv.b2bmonetization.campaign.yt.repository.offer;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.model.campaign.Action;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.OfferBidsHistory;
import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.yt.client.YtClientProxy;

@ParametersAreNonnullByDefault
class OfferBidsHistoryRepositoryTest extends AbstractMonetizationTest {

    private static final long PARTNER_ID = 1L;
    private static final long NOT_EXIST_PARTNER_ID = 9L;

    private static final long CAMPAIGN_ID = 10L;

    private static final String FIRST_SKU = "100";
    private static final String SECOND_SKU = "101";

    @Autowired
    private OfferBidsHistoryRepository repository;
    @Autowired
    private TimeService timeService;
    @Autowired
    private YtClientProxy ytClient;

    @DisplayName("Получение списка событий, произошедших со ставкой")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBidsHistory.class,
                    path = "//tmp/findByPartnerIdCampaignIdAndSku_correctIds_correctlyGettingOfferBidsHistory_" +
                            "offer_bids_history"
            ),
            before = "CampaignRepository/json/offerBidsHistory/" +
                    "findByPartnerIdCampaignIdAndSku_correctIds_correctlyGettingOfferBidsHistory.before.json"
    )
    void findByPartnerIdCampaignIdAndSku_correctIds_correctlyGettingOfferBidsHistory() {
        run("findByPartnerIdCampaignIdAndSku_correctIds_correctlyGettingOfferBidsHistory_",
                () -> Assertions.assertThat(
                                repository.findByPartnerIdCampaignIdAndSku(PARTNER_ID, CAMPAIGN_ID, FIRST_SKU)
                        )
                        .isEqualTo(
                                List.of(
                                        create(PARTNER_ID, CAMPAIGN_ID, FIRST_SKU, 1L, 1000)
                                )
                        )
        );
    }

    @DisplayName("Получение списка событий, произошедших со ставкой с несуществующим partnerId")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBidsHistory.class,
                    path = "//tmp/findByPartnerIdCampaignIdAndSku_notExistPartnerId_emptyList_" +
                            "offer_bids_history"
            ),
            before = "CampaignRepository/json/offerBidsHistory/" +
                    "findByPartnerIdCampaignIdAndSku_notExistPartnerId_emptyList.before.json"
    )
    void findByPartnerIdCampaignIdAndSku_notExistPartnerId_emptyList() {
        run("findByPartnerIdCampaignIdAndSku_notExistPartnerId_emptyList_",
                () -> Assertions.assertThat(
                                repository.findByPartnerIdCampaignIdAndSku(NOT_EXIST_PARTNER_ID, CAMPAIGN_ID, FIRST_SKU)
                        )
                        .isEmpty()
        );
    }

    @DisplayName(" Получение списка офферов со ставками в разрезе партера и кампании")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBidsHistory.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_correctIds_listWithTwoObjects_" +
                            "offer_bids_history"
            ),
            before = "CampaignRepository/json/offerBidsHistory/" +
                    "findByPartnerIdAndCampaignId_correctIds_listWithTwoObjects.before.json"
    )
    void findByPartnerIdAndCampaignId_correctIds_listWithTwoObjects() {
        run("findByPartnerIdAndCampaignId_correctIds_listWithTwoObjects_",
                () -> Assertions.assertThat(
                                repository.findByPartnerIdAndCampaignId(PARTNER_ID, CAMPAIGN_ID)
                        )
                        .hasSize(2)
                        .isEqualTo(
                                List.of(
                                        create(PARTNER_ID, CAMPAIGN_ID, FIRST_SKU, 1L, 1000),
                                        create(PARTNER_ID, CAMPAIGN_ID, SECOND_SKU, 2L, 1100)
                                )
                        )
        );
    }

    @DisplayName("Получение списка офферов со ставками в разрезе партера и кампании с несуществующим partnerId")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBidsHistory.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_notExistPartnerId_emptyList_" +
                            "offer_bids_history"
            ),
            before = "CampaignRepository/json/offerBidsHistory/" +
                    "findByPartnerIdAndCampaignId_notExistPartnerId_emptyList.before.json"
    )
    void findByPartnerIdAndCampaignId_notExistPartnerId_emptyList() {
        run("findByPartnerIdAndCampaignId_notExistPartnerId_emptyList_",
                () -> Assertions.assertThat(
                                repository.findByPartnerIdAndCampaignId(NOT_EXIST_PARTNER_ID, CAMPAIGN_ID)
                        )
                        .isEmpty()
        );
    }

    @DisplayName("Добавление новой записи об изменении ставок рекламной кампании")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBidsHistory.class,
                    path = "//tmp/insert_correctlyInserted_newRowInserted_" +
                            "offer_bids_history"
            ),
            before = "CampaignRepository/json/offerBidsHistory/" +
                    "insert_correctlyInserted_newRowInserted.before.json",
            after = "CampaignRepository/json/offerBidsHistory/" +
                    "insert_correctlyInserted_newRowInserted.after.json"
    )
    void insert_correctlyInserted_newRowInserted() {
        run("insert_correctlyInserted_newRowInserted_",
                () -> ytClient.execInTransaction(txAsync ->
                        repository.insert(
                                txAsync,
                                List.of(
                                        create(2L, 20L, "201", 4L, 1000)
                                )
                        )
                )
        );
    }

    @Nonnull
    private OfferBidsHistory create(long partnerId,
                                    long campaignId,
                                    String sku,
                                    long actionId,
                                    long bid) {
        return OfferBidsHistory
                .builder()
                .partnerId(partnerId)
                .advCampaignId(campaignId)
                .sku(sku)
                .bid(bid)
                .actionId(actionId)
                .updatedAt(timeService.get())
                .action(Action.CREATED)
                .build();
    }
}
