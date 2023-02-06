package ru.yandex.market.billing.cpa_auction.notification;

import java.time.Clock;
import java.time.ZoneId;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.cpa_auction.CpaAuctionBillingDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.CampaignDao;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequestJson;

public class NettingNotificationServiceTest extends FunctionalTest {
    @Autowired
    private CpaAuctionBillingDao cpaAuctionBillingDao;
    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private PartnerNotificationClient partnerNotificationClientMock;
    private NettingNotificationService nettingNotificationService;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(DateTimes.toInstantAtDefaultTz(2022, 6, 3, 13, 0, 0),
                ZoneId.systemDefault());
        nettingNotificationService = new NettingNotificationService(cpaAuctionBillingDao, campaignDao,
                partnerNotificationClientMock, clock);
    }

    @Test
    @DbUnitDataSet(before = "NettingNotificationServiceTest.before.csv")
    void testGetCorrectNotifications() {
        nettingNotificationService.sendNotifications();
        ArgumentCaptor<SendNotificationRequestJson> requestJsonArgumentCaptor =
                ArgumentCaptor.forClass(SendNotificationRequestJson.class);
        Mockito.verify(partnerNotificationClientMock, Mockito.times(2))
                .sendNotificationJson(requestJsonArgumentCaptor.capture());
        var notifiedShops =
                requestJsonArgumentCaptor.getAllValues().stream()
                        .map(requestJson -> requestJson.getDestination().getShopId())
                        .collect(Collectors.toList());
        Assertions.assertThat(notifiedShops).containsExactlyInAnyOrder(100L, 102L);

        Object dataActual = requestJsonArgumentCaptor.getAllValues().stream()
                .filter(requestJson -> requestJson.getDestination().getShopId() == 100L)
                .map(SendNotificationRequestJson::getData).findFirst().orElse(null);
        Assertions.assertThat(dataActual).isNotNull();
    }

}
