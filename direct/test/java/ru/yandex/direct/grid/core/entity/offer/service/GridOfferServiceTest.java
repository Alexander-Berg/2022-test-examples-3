package ru.yandex.direct.grid.core.entity.offer.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOfferFilter;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOfferId;
import ru.yandex.direct.grid.core.entity.offer.repository.GridOfferYtRepository;
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionGroupByDate;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@GridCoreTest
@RunWith(SpringRunner.class)
public class GridOfferServiceTest {
    private static final LocalDate STAT_TO = LocalDate.now();
    private static final LocalDate STAT_FROM = STAT_TO.minusDays(5);

    @Autowired
    private Steps steps;

    @Autowired
    private GridOfferYtRepository gridOfferYtRepository;

    @Autowired
    private GridOfferService gridOfferService;

    @Captor
    private ArgumentCaptor<GdiOfferFilter> filterArgumentCaptor;
    @Captor
    private ArgumentCaptor<Collection<GdiOfferId>> offerIdsArgumentCaptor;
    @Captor
    private ArgumentCaptor<Collection<Long>> orderIdsArgumentCaptor;
    @Captor
    private ArgumentCaptor<Map<Long, Long>> masterIdBySubIdArgumentCaptor;

    private ClientInfo clientInfo;

    private CampaignInfo campaignInfo;
    private CampaignInfo masterCampaignInfo;
    private CampaignInfo subCampaignInfo;
    private CampaignInfo otherCampaignInfo;

    private AdGroupInfo adGroupInfo;
    private AdGroupInfo otherAdGroupInfo;

    private GdiOfferId offerId;
    private GdiOfferId otherOfferId;

    @Before
    public void setup() {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientInfo otherClientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        masterCampaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        subCampaignInfo = steps.campaignSteps().createActiveSubCampaign(clientInfo, masterCampaignInfo.getCampaignId());
        otherCampaignInfo = steps.campaignSteps().createActiveCampaign(otherClientInfo);

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        otherAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(otherCampaignInfo);

        FeedInfo feedInfo = steps.feedSteps().createDefaultSyncedFeed(clientInfo);
        offerId = new GdiOfferId()
                .withBusinessId(feedInfo.getBusinessId())
                .withShopId(feedInfo.getShopId())
                .withOfferYabsId(RandomNumberUtils.nextPositiveLong());
        FeedInfo otherFeedInfo = steps.feedSteps().createDefaultSyncedFeed(otherClientInfo);
        otherOfferId = new GdiOfferId()
                .withBusinessId(otherFeedInfo.getBusinessId())
                .withShopId(otherFeedInfo.getShopId())
                .withOfferYabsId(RandomNumberUtils.nextPositiveLong());

        doReturn(List.of())
                .when(gridOfferYtRepository)
                .getOffers(any(), any(), eq(STAT_FROM), eq(STAT_TO), any());
        doReturn(Map.of())
                .when(gridOfferYtRepository)
                .getOfferById(anyCollection());
        doReturn(Map.of())
                .when(gridOfferYtRepository)
                .getOfferStatsByDateByOrderId(anyCollection(), anyMap(), eq(STAT_FROM), eq(STAT_TO), any());
    }

    @Test
    public void getOffers_onlyClientExisting() {
        GdiOfferFilter filter = new GdiOfferFilter()
                .withCampaignIdIn(Set.of(campaignInfo.getCampaignId(), otherCampaignInfo.getCampaignId()))
                .withAdGroupIdIn(Set.of(adGroupInfo.getAdGroupId(), otherAdGroupInfo.getAdGroupId()));
        gridOfferService.getOffers(Objects.requireNonNull(clientInfo.getClientId()), filter, emptyList(),
                STAT_FROM, STAT_TO);

        verify(gridOfferYtRepository)
                .getOffers(filterArgumentCaptor.capture(), eq(emptyList()), eq(STAT_FROM), eq(STAT_TO), any());
        assertThat(filterArgumentCaptor.getValue().getCampaignIdIn())
                .containsOnly(campaignInfo.getCampaignId());
        assertThat(filterArgumentCaptor.getValue().getAdGroupIdIn())
                .containsOnly(adGroupInfo.getAdGroupId());
    }

    @Test
    public void getOffers_withSubCampaigns() {
        GdiOfferFilter filter = new GdiOfferFilter()
                .withCampaignIdIn(Set.of(masterCampaignInfo.getCampaignId()));
        gridOfferService.getOffers(Objects.requireNonNull(clientInfo.getClientId()), filter, emptyList(),
                STAT_FROM, STAT_TO);

        verify(gridOfferYtRepository)
                .getOffers(filterArgumentCaptor.capture(), eq(emptyList()), eq(STAT_FROM), eq(STAT_TO), any());
        assertThat(filterArgumentCaptor.getValue().getCampaignIdIn())
                .containsOnly(masterCampaignInfo.getCampaignId(), subCampaignInfo.getCampaignId());
    }

    @Test
    public void getOfferById_onlyClientExisting() {
        gridOfferService.getOfferById(Objects.requireNonNull(clientInfo.getClientId()), List.of(offerId, otherOfferId));

        verify(gridOfferYtRepository)
                .getOfferById(offerIdsArgumentCaptor.capture());
        assertThat(offerIdsArgumentCaptor.getValue())
                .containsOnly(offerId);
    }

    @Test
    public void getOfferStatsByDateByCampaignId_onlyClientExisting() {
        gridOfferService.getOfferStatsByDateByCampaignId(Objects.requireNonNull(clientInfo.getClientId()),
                Set.of(campaignInfo.getCampaignId(), otherCampaignInfo.getCampaignId()),
                STAT_FROM, STAT_TO, ReportOptionGroupByDate.DAY);

        verify(gridOfferYtRepository)
                .getOfferStatsByDateByOrderId(orderIdsArgumentCaptor.capture(), eq(Map.of()),
                        eq(STAT_FROM), eq(STAT_TO), eq(ReportOptionGroupByDate.DAY));
        assertThat(orderIdsArgumentCaptor.getValue())
                .containsOnly(campaignInfo.getOrderId());
    }

    @Test
    public void getOfferStatsByDateByCampaignId_withSubCampaigns() {
        gridOfferService.getOfferStatsByDateByCampaignId(Objects.requireNonNull(clientInfo.getClientId()),
                Set.of(campaignInfo.getCampaignId(), masterCampaignInfo.getCampaignId()),
                STAT_FROM, STAT_TO, ReportOptionGroupByDate.DAY);

        verify(gridOfferYtRepository)
                .getOfferStatsByDateByOrderId(orderIdsArgumentCaptor.capture(), masterIdBySubIdArgumentCaptor.capture(),
                        eq(STAT_FROM), eq(STAT_TO), eq(ReportOptionGroupByDate.DAY));
        assertSoftly(softly -> {
            softly.assertThat(orderIdsArgumentCaptor.getValue())
                    .containsOnly(campaignInfo.getOrderId(), masterCampaignInfo.getOrderId());
            softly.assertThat(masterIdBySubIdArgumentCaptor.getValue())
                    .containsOnly(Map.entry(subCampaignInfo.getOrderId(), masterCampaignInfo.getOrderId()));
        });
    }
}
