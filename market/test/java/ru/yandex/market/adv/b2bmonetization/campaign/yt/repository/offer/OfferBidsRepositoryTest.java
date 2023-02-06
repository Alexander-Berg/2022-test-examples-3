package ru.yandex.market.adv.b2bmonetization.campaign.yt.repository.offer;

import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.OfferBids;
import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.yt.client.YtClientProxy;

public class OfferBidsRepositoryTest extends AbstractMonetizationTest {

    private static final long PARTNER_ID = 1L;
    private static final long NOT_EXIST_PARTNER_ID = 9L;

    private static final long CAMPAIGN_ID = 10L;

    private static final String FIRST_SKU = "100";
    private static final String SECOND_SKU = "101";

    @Autowired
    private OfferBidsRepository offerBidsRepository;
    @Autowired
    private TimeService timeService;
    @Autowired
    private YtClientProxy ytClient;

    @DisplayName("Получение списка ставок и офферов с айдишниками партнера и кампании")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBids.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_correctIds_offersBidsList_" +
                            "offer_bids"
            ),
            before = "CampaignRepository/json/offerBids/" +
                    "findByPartnerIdAndCampaignId_correctIds_offersBidsList.before.json"
    )
    public void findByPartnerIdAndCampaignId_correctIds_offersBidsList() {
        run("findByPartnerIdAndCampaignId_correctIds_offersBidsList_",
                () -> Assertions.assertThat(
                                offerBidsRepository.findByPartnerIdAndCampaignId(PARTNER_ID, CAMPAIGN_ID, 10L)
                        )
                        .isEqualTo(createOfferBidsList())
        );
    }

    @DisplayName("Получение списка ставок и офферов с несуществующим partner id")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBids.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_notExistPartnerId_emptyList_" +
                            "offer_bids"
            ),
            before = "CampaignRepository/json/offerBids/" +
                    "findByPartnerIdAndCampaignId_notExistPartnerId_emptyList.before.json"
    )
    public void findByPartnerIdAndCampaignId_notExistPartnerId_emptyList() {
        run("findByPartnerIdAndCampaignId_notExistPartnerId_emptyList_",
                () -> Assertions.assertThat(
                                offerBidsRepository.findByPartnerIdAndCampaignId(NOT_EXIST_PARTNER_ID, CAMPAIGN_ID, 1L)
                        )
                        .isEmpty()
        );
    }

    @DisplayName("Добавление новой записи о партнере")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = OfferBids.class,
                    path = "//tmp/insert_correctlyInserted_newRowInserted_" +
                            "offer_bids"
            ),
            before = "CampaignRepository/json/offerBids/insert_correctlyInserted_newRowInserted.before.json",
            after = "CampaignRepository/json/offerBids/insert_correctlyInserted_newRowInserted.after.json"
    )
    public void insert_correctlyInserted_newRowInserted() {
        run("insert_correctlyInserted_newRowInserted_",
                () -> ytClient.execInTransaction(txAsync ->
                        offerBidsRepository.insert(
                                txAsync,
                                List.of(
                                        createOfferBids(2L, 20L, "201", 1000)
                                )
                        )
                )
        );
    }

    @Nonnull
    private List<OfferBids> createOfferBidsList() {
        return List.of(
                createOfferBids(PARTNER_ID, CAMPAIGN_ID, FIRST_SKU, 1000),
                createOfferBids(PARTNER_ID, CAMPAIGN_ID, SECOND_SKU, 1100)
        );
    }

    private OfferBids createOfferBids(long partnerId, long campaignId, String sku, long bid) {
        return OfferBids
                .builder()
                .partnerId(partnerId)
                .advCampaignId(campaignId)
                .sku(sku)
                .bid(bid)
                .updatedAt(timeService.get())
                .build();
    }
}
