package ru.yandex.market.adv.b2bmonetization.campaign.yt.repository.campaign;

import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.model.campaign.Action;
import ru.yandex.market.adv.b2bmonetization.campaign.model.campaign.CampaignType;
import ru.yandex.market.adv.b2bmonetization.campaign.model.campaign.Color;
import ru.yandex.market.adv.b2bmonetization.campaign.model.campaign.Status;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.CampaignHistory;
import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.yt.client.YtClientProxy;

class CampaignHistoryRepositoryTest extends AbstractMonetizationTest {

    private static final long FIRST_PARTNER_ID = 1L;
    private static final long SECOND_PARTNER_ID = 2L;
    private static final long FIRST_PARTNER_FIRST_CAMPAIGN_ID = 10L;

    private static final long NOT_EXIST_PARTNER_ID = 9L;

    @Autowired
    private CampaignHistoryRepository campaignHistoryRepository;
    @Autowired
    private TimeService timeService;
    @Autowired
    private YtClientProxy ytClient;

    @DisplayName("Получение информации об изменениях рекламных кампаниях " +
            "с существующими идентификаторами партнера и кампании")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/selectCampaignHistoryInfo_existIds_campaignHistoryInfo_" +
                            "adv_campaign_history"
            ),
            before = "CampaignRepository/json/campaignHistory/" +
                    "selectCampaignHistoryInfo_existIds_campaignHistoryInfo.before.json"
    )
    void selectCampaignHistoryInfo_existIds_campaignHistoryInfo() {
        run("selectCampaignHistoryInfo_existIds_campaignHistoryInfo_",
                () -> Assertions.assertThat(
                                campaignHistoryRepository
                                        .selectCampaignHistoryInfo(FIRST_PARTNER_ID, FIRST_PARTNER_FIRST_CAMPAIGN_ID)
                        )
                        .hasSize(2)
                        .isEqualTo(createListOfCampaignHistory())
        );
    }

    @DisplayName("Получение информации об изменениях рекламных кампаний с несуществующими идентификаторами партнера")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/selectCampaignHistoryInfo_doNotExistPartnerId_emptyList_" +
                            "adv_campaign_history"
            ),
            before = "CampaignRepository/json/campaignHistory/" +
                    "selectCampaignHistoryInfo_doNotExistPartnerId_emptyList.before.json"
    )
    void selectCampaignHistoryInfo_doNotExistPartnerId_emptyList() {
        run("selectCampaignHistoryInfo_doNotExistPartnerId_emptyList_",
                () -> Assertions.assertThat(
                                campaignHistoryRepository.selectCampaignHistoryInfo(NOT_EXIST_PARTNER_ID,
                                        FIRST_PARTNER_FIRST_CAMPAIGN_ID)
                        )
                        .isEmpty()
        );
    }

    @DisplayName("Получение списка истории изменения кампаний существующего партнера")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/selectCampaignsHistoryWithPartnerId_existId_listOfCampaignHistoryInfo_" +
                            "adv_campaign_history"
            ),
            before = "CampaignRepository/json/campaignHistory/" +
                    "selectCampaignsHistoryWithPartnerId_existId_listOfCampaignHistoryInfo.before.json"
    )
    void selectCampaignsHistoryWithPartnerId_existId_listOfCampaignHistoryInfo() {
        run("selectCampaignsHistoryWithPartnerId_existId_listOfCampaignHistoryInfo_",
                () -> Assertions.assertThat(
                                campaignHistoryRepository.selectCampaignsHistoryWithPartnerId(FIRST_PARTNER_ID)
                        )
                        .hasSize(2)
                        .isEqualTo(createListOfCampaignHistory())
        );
    }

    @DisplayName("Добавление новой записи об изменении рекламной кампании")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/insertCampaignHistoryInfo_correctlyInserted_newRowInserted_" +
                            "adv_campaign_history"
            ),
            before = "CampaignRepository/json/campaignHistory/" +
                    "insertCampaignHistoryInfo_correctlyInserted_newRowInserted.before.json",
            after = "CampaignRepository/json/campaignHistory/" +
                    "insertCampaignHistoryInfo_correctlyInserted_newRowInserted.after.json"
    )
    void insertCampaignHistoryInfo_correctlyInserted_newRowInserted() {
        run("insertCampaignHistoryInfo_correctlyInserted_newRowInserted_",
                () -> ytClient.execInTransaction(tx ->
                        campaignHistoryRepository.insert(tx,
                                CampaignHistory.builder()
                                        .partnerId(SECOND_PARTNER_ID)
                                        .id(21L)
                                        .actionId(4L)
                                        .name("Название кампании 4")
                                        .uid(SECOND_PARTNER_ID)
                                        .offerCount(500L)
                                        .priority(1L)
                                        .activation(true)
                                        .status(Status.ACTIVE)
                                        .minBid(50)
                                        .maxBid(10000)
                                        .updatedAt(timeService.get())
                                        .color(Color.WHITE)
                                        .action(Action.CREATED)
                                        .type(CampaignType.EXCEL)
                                        .build()
                        )
                )
        );
    }

    @Nonnull
    private List<CampaignHistory> createListOfCampaignHistory() {
        return List.of(
                createCampaignHistory(1, Action.CREATED),
                createCampaignHistory(2, Action.UPDATED)
        );
    }

    @Nonnull
    private CampaignHistory createCampaignHistory(long actionId, Action action) {
        return CampaignHistory.builder()
                .partnerId(FIRST_PARTNER_ID)
                .id(FIRST_PARTNER_FIRST_CAMPAIGN_ID)
                .actionId(actionId)
                .name("Название кампании " + actionId)
                .uid(FIRST_PARTNER_ID)
                .offerCount(500L)
                .priority(1L)
                .activation(true)
                .status(Status.ACTIVE)
                .minBid(50)
                .maxBid(10000)
                .updatedAt(timeService.get())
                .color(Color.WHITE)
                .action(action)
                .type(CampaignType.EXCEL)
                .build();
    }
}
