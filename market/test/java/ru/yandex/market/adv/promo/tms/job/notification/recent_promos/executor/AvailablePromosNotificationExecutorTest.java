package ru.yandex.market.adv.promo.tms.job.notification.recent_promos.executor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.utils.CommonTestUtils;
import ru.yandex.market.adv.promo.tms.job.notification.recent_promos.dao.AvailablePromosYTDao;
import ru.yandex.market.adv.promo.tms.job.notification.recent_promos.model.PartnerAvailablePromoInfo;
import ru.yandex.market.adv.promo.tms.yt.YtCluster;
import ru.yandex.market.adv.promo.tms.yt.YtTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationResponse;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationToPartnerRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvailablePromosNotificationExecutorTest extends FunctionalTest {
    private static final String CREATION_TIME_ATTRIBUTE = "creation_time";
    private static final String XML_WITH_ONE_PROMO =
            CommonTestUtils.getResource(AvailablePromosNotificationExecutorTest.class, "onePromo.xml");

    private static final String XML_WITH_TWO_PROMOS =
            CommonTestUtils.getResource(AvailablePromosNotificationExecutorTest.class, "twoPromos.xml");

    @Autowired
    private AvailablePromosNotificationExecutor executor;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private YtTemplate promoYtTemplate;

    @Autowired
    private AvailablePromosYTDao availablePromosYTDao;

    @Test
    @DbUnitDataSet(
            before = "AvailablePromosNotificationExecutorTest/testNotChange/before.csv",
            after = "AvailablePromosNotificationExecutorTest/testNotChange/before.csv"
    )
    void testNotChange() {
        YtCluster cluster = promoYtTemplate.getClusters()[0];
        Yt yt = cluster.getYt();
        Cypress cypress = mock(Cypress.class);
        when(cypress.exists(any(YPath.class))).thenReturn(true);
        YTreeNode node = mock(YTreeNode.class);
        when(node.stringValue()).thenReturn(Instant.EPOCH.toString());
        Map<String, YTreeNode> tableAttributes = Map.of(CREATION_TIME_ATTRIBUTE, node);
        YTreeNode tableNode = mock(YTreeNode.class);
        when(tableNode.getAttributes()).thenReturn(tableAttributes);
        when(cypress.get(any(YPath.class), anyCollection())).thenReturn(tableNode);
        when(yt.cypress()).thenReturn(cypress);

        executor.doJob(null);
        verify(mbiApiClient, never()).sendNotificationsToPartners(anyList(), anyBoolean());
    }

    @Test
    void testUnavailable() {
        Cypress cypress = mock(Cypress.class);
        when(cypress.exists(any(YPath.class))).thenThrow(new RuntimeException());
        for (YtCluster cluster : promoYtTemplate.getClusters()) {
            Yt yt = cluster.getYt();
            when(yt.cypress()).thenReturn(cypress);
        }
        executor.doJob(null);
        verify(mbiApiClient, never()).sendNotificationsToPartners(anyList(), anyBoolean());
    }

    @Test
    void testFirstAvailable() throws Exception {
        YtCluster cluster = promoYtTemplate.getClusters()[0];
        Yt yt = cluster.getYt();
        Cypress cypress = mock(Cypress.class);
        when(cypress.exists(any(YPath.class))).thenReturn(true);
        YTreeNode node = mock(YTreeNode.class);
        when(node.stringValue()).thenReturn(Instant.now().toString());
        Map<String, YTreeNode> tableAttributes = Map.of(CREATION_TIME_ATTRIBUTE, node);
        YTreeNode tableNode = mock(YTreeNode.class);
        when(tableNode.getAttributes()).thenReturn(tableAttributes);
        when(cypress.get(any(YPath.class), anyCollection())).thenReturn(tableNode);
        when(yt.cypress()).thenReturn(cypress);

        when(availablePromosYTDao.getAvailableRecentPromos(
                matches(cluster.getSimpleName()),
                anyString(),
                eq(0),
                anyInt()
                )
        ).thenReturn(List.of(makeInfoWithShopId(0), makeInfoWithShopId(0), makeInfoWithShopId(1)));
        when(mbiApiClient.sendNotificationsToPartners(anyList(), eq(false))).thenReturn(
                List.of(
                        new SendNotificationResponse(1L)
                )
        );

        executor.doJob(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SendNotificationToPartnerRequest>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(mbiApiClient, times(2)).sendNotificationsToPartners(argumentCaptor.capture(), eq(false));
        List<List<SendNotificationToPartnerRequest>> requests = argumentCaptor.getAllValues();
        assertEquals(2, requests.size());
        List<SendNotificationToPartnerRequest> batchRequest1 = requests.get(0);
        assertEquals(1, batchRequest1.size());
        SendNotificationToPartnerRequest request1 = batchRequest1.get(0);
        assertEquals(0, request1.getPartnerId());
        assertEquals(XML_WITH_TWO_PROMOS.replaceAll("\\s+", ""), request1.getNotificationData());

        List<SendNotificationToPartnerRequest> batchRequest2 = requests.get(1);
        assertEquals(1, batchRequest2.size());
        SendNotificationToPartnerRequest request2 = batchRequest2.get(0);
        assertEquals(1, request2.getPartnerId());
        assertEquals(XML_WITH_ONE_PROMO.replaceAll("\\s+", ""), request2.getNotificationData());
    }

    @Test
    void testSecondAvailable() throws Exception {
        Cypress unavailableCypress = mock(Cypress.class);
        when(unavailableCypress.exists(any(YPath.class))).thenThrow(new RuntimeException());
        YtCluster firstCluster = promoYtTemplate.getClusters()[0];
        Yt firstYt = firstCluster.getYt();
        when(firstYt.cypress()).thenReturn(unavailableCypress);

        YtCluster secondCluster = promoYtTemplate.getClusters()[0];
        Yt secondYt = secondCluster.getYt();
        Cypress cypress = mock(Cypress.class);
        when(cypress.exists(any(YPath.class))).thenReturn(true);
        YTreeNode node = mock(YTreeNode.class);
        when(node.stringValue()).thenReturn(Instant.now().toString());
        Map<String, YTreeNode> tableAttributes = Map.of(CREATION_TIME_ATTRIBUTE, node);
        YTreeNode tableNode = mock(YTreeNode.class);
        when(tableNode.getAttributes()).thenReturn(tableAttributes);
        when(cypress.get(any(YPath.class), anyCollection())).thenReturn(tableNode);
        when(secondYt.cypress()).thenReturn(cypress);

        when(availablePromosYTDao.getAvailableRecentPromos(
                matches(secondCluster.getSimpleName()),
                anyString(),
                eq(0),
                anyInt()
                )
        ).thenReturn(List.of(makeInfoWithShopId(0), makeInfoWithShopId(0), makeInfoWithShopId(1)));
        when(mbiApiClient.sendNotificationsToPartners(anyList(), eq(false))).thenReturn(
                List.of(
                        new SendNotificationResponse(1L)
                )
        );

        executor.doJob(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SendNotificationToPartnerRequest>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(mbiApiClient, times(2)).sendNotificationsToPartners(argumentCaptor.capture(), eq(false));
        List<List<SendNotificationToPartnerRequest>> requests = argumentCaptor.getAllValues();
        assertEquals(2, requests.size());
        List<SendNotificationToPartnerRequest> batchRequest1 = requests.get(0);
        assertEquals(1, batchRequest1.size());
        SendNotificationToPartnerRequest request1 = batchRequest1.get(0);
        assertEquals(0, request1.getPartnerId());
        assertEquals(XML_WITH_TWO_PROMOS.replaceAll("\\s+", ""), request1.getNotificationData());

        List<SendNotificationToPartnerRequest> batchRequest2 = requests.get(1);
        assertEquals(1, batchRequest2.size());
        SendNotificationToPartnerRequest request2 = batchRequest2.get(0);
        assertEquals(1, request2.getPartnerId());
        assertEquals(XML_WITH_ONE_PROMO.replaceAll("\\s+", ""), request2.getNotificationData());
    }

    @Test
    void testManyPromosForOnePartner() throws Exception {
        YtCluster cluster = promoYtTemplate.getClusters()[0];
        Yt yt = cluster.getYt();
        Cypress cypress = mock(Cypress.class);
        when(cypress.exists(any(YPath.class))).thenReturn(true);
        YTreeNode node = mock(YTreeNode.class);
        when(node.stringValue()).thenReturn(Instant.now().toString());
        Map<String, YTreeNode> tableAttributes = Map.of(CREATION_TIME_ATTRIBUTE, node);
        YTreeNode tableNode = mock(YTreeNode.class);
        when(tableNode.getAttributes()).thenReturn(tableAttributes);
        when(cypress.get(any(YPath.class), anyCollection())).thenReturn(tableNode);
        when(yt.cypress()).thenReturn(cypress);

        int numberOfPromoInfos = 1000;
        List<PartnerAvailablePromoInfo> availablePromoInfos = new ArrayList<>(numberOfPromoInfos);
        for (int k = 0; k < numberOfPromoInfos; k++) {
            availablePromoInfos.add(makeInfoWithShopId(1));
        }
        when(availablePromosYTDao.getAvailableRecentPromos(
                        matches(cluster.getSimpleName()),
                        anyString(),
                        eq(0),
                        anyInt()
                )
        ).thenReturn(availablePromoInfos);
        when(mbiApiClient.sendNotificationsToPartners(anyList(), eq(false))).thenReturn(
                List.of(
                        new SendNotificationResponse(1L)
                )
        );

        executor.doJob(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SendNotificationToPartnerRequest>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(mbiApiClient, times(1)).sendNotificationsToPartners(argumentCaptor.capture(), eq(false));
        List<SendNotificationToPartnerRequest> request = argumentCaptor.getValue();
        assertEquals(1, request.size());
    }

    private PartnerAvailablePromoInfo makeInfoWithShopId(long shopId) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("y-MM-d");
        return new PartnerAvailablePromoInfo.Builder()
                .withPromoId("promoId")
                .withPromoName("Name")
                .withCategoriesCount(2)
                .withStartDate(simpleDateFormat.parse("2021-08-2").getTime() / 1000)
                .withEndDate(simpleDateFormat.parse("2021-08-5").getTime() / 1000)
                .withOffersCount(11)
                .withMinDiscount(11)
                .withMaxDiscount(22)
                .withMechanic(1)
                .withPublishDate(1L)
                .withPartnerId(shopId)
                .build();
    }
}
