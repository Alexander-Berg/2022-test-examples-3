package ru.yandex.market.adv.b2bmonetization.campaign.yt.repository.campaign;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.model.campaign.CampaignType;
import ru.yandex.market.adv.b2bmonetization.campaign.model.campaign.Color;
import ru.yandex.market.adv.b2bmonetization.campaign.model.campaign.Status;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.Campaign;
import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.yt.client.YtClientProxy;

class CampaignRepositoryTest extends AbstractMonetizationTest {

    private static final long EXIST_PARTNER_ID = 1L;
    private static final long EXIST_CAMPAIGN_ID = 10L;

    private static final long NOT_EXIST_PARTNER_ID = 9L;

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private TimeService timeService;
    @Autowired
    private YtClientProxy ytClient;

    @DisplayName("Получение информации о рекламных кампаниях с существующими идентификаторами партнера и кампании")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/selectCampaignInfo_existIds_campaignInfo_adv_campaign"
            ),
            before = "CampaignRepository/json/campaign/selectCampaignInfo_existIds_campaignInfo.before.json"
    )
    void selectCampaignInfo_existIds_campaignInfo() {
        run("selectCampaignInfo_existIds_campaignInfo_",
                () -> Assertions.assertThat(
                                campaignRepository.selectByPartnerIdAndCampaignId(EXIST_PARTNER_ID, EXIST_CAMPAIGN_ID)
                        )
                        .contains(createCampaignInfo())
        );
    }

    @DisplayName("Получение информации о рекламных кампаниях с несуществующими идентификаторами партнера")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/selectCampaignInfo_doNotExistPartnerId_emptyData_adv_campaign"
            ),
            before = "CampaignRepository/json/campaign/selectCampaignInfo_doNotExistPartnerId_emptyData.before.json"
    )
    void selectCampaignInfo_doNotExistPartnerId_emptyData() {
        run("selectCampaignInfo_doNotExistPartnerId_emptyData_",
                () -> Assertions.assertThat(
                                campaignRepository.selectByPartnerIdAndCampaignId(NOT_EXIST_PARTNER_ID,
                                        EXIST_CAMPAIGN_ID)
                        )
                        .isNotPresent()
        );
    }

    @DisplayName("Получение списка кампаний существующего партнера")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/adv_unittest/adv_campaign"
            ),
            before = "CampaignRepository/json/campaign/" +
                    "selectCampaignsWithPartnerId_existId_campaignInfoList.before.json"
    )
    void selectCampaignsWithPartnerId_existId_campaignInfoList() {
        Assertions.assertThat(
                        campaignRepository.selectCampaignsWithPartnerId(EXIST_PARTNER_ID)
                )
                .hasSize(2)
                .contains(createCampaignInfo());
    }

    @DisplayName("Добавление новой записи о рекламной кампании")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/insertCampaignInfo_correctlyInserted_newRowInserted_adv_campaign"
            ),
            before = "CampaignRepository/json/campaign/" +
                    "insertCampaignInfo_correctlyInserted_newRowInserted.before.json",
            after = "CampaignRepository/json/campaign/" +
                    "insertCampaignInfo_correctlyInserted_newRowInserted.after.json"
    )
    void insertCampaignInfo_correctlyInserted_newRowInserted() {
        run("insertCampaignInfo_correctlyInserted_newRowInserted_",
                () -> ytClient.execInTransaction(tx ->
                        campaignRepository.insert(tx,
                                Campaign.builder()
                                        .partnerId(2L)
                                        .id(10L)
                                        .name("Другое название")
                                        .uid(2L)
                                        .offerCount(600)
                                        .priority(1L)
                                        .status(Status.ACTIVE)
                                        .color(Color.WHITE)
                                        .updatedAt(timeService.get())
                                        .activation(true)
                                        .minBid(50)
                                        .maxBid(10000)
                                        .type(CampaignType.EXCEL)
                                        .build()
                        )
                )
        );
    }

    private Campaign createCampaignInfo() {
        return Campaign.builder()
                .partnerId(EXIST_PARTNER_ID)
                .id(EXIST_CAMPAIGN_ID)
                .name("Название кампании")
                .uid(EXIST_PARTNER_ID)
                .offerCount(500L)
                .priority(1L)
                .activation(true)
                .status(Status.ACTIVE)
                .minBid(50)
                .maxBid(10000)
                .updatedAt(timeService.get())
                .color(Color.WHITE)
                .type(CampaignType.EXCEL)
                .build();
    }
}
