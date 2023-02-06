package ru.yandex.direct.logicprocessor.processors.campstatusmoderate;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesCampaignRepository;
import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusCampaign;
import ru.yandex.direct.core.entity.campaign.container.WalletsWithCampaigns;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.logicprocessor.processors.campstatusmoderate.handlers.BalanceResyncHandler;
import ru.yandex.direct.logicprocessor.processors.campstatusmoderate.handlers.CampaignToResync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CampaignStatusCalcServiceTest {
    private AggregatedStatusesCampaignRepository aggregatedStatusesCampaignRepository;
    private CampaignService campaignService;
    private CampaignStatusModerateRepository campaignStatusModerateRepository;
    private CampaignStatusCalcService campaignStatusCalcService;
    private PpcPropertiesSupport ppcPropertiesSupport;
    private DirectConfig directConfig;
    private CampaignRepository campaignRepository;
    private BalanceResyncHandler balanceResyncHandler;

    @BeforeEach
    void before() {
        aggregatedStatusesCampaignRepository = mock(AggregatedStatusesCampaignRepository.class);
        campaignService = mock(CampaignService.class);
        campaignStatusModerateRepository = mock(CampaignStatusModerateRepositoryImpl.class);
        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        directConfig = mock(DirectConfig.class);
        campaignRepository = mock(CampaignRepository.class);

        PpcProperty<Integer> mockedProperty = mock(PpcProperty.class);
        when(mockedProperty.getOrDefault(anyInt())).thenReturn(100);
        when(ppcPropertiesSupport.get(eq(PpcPropertyNames.AGGREGATED_CAMP_STATUS_MODERATE), any(Duration.class)))
                .thenReturn(mockedProperty);

        campaignStatusCalcService = new CampaignStatusCalcService(aggregatedStatusesCampaignRepository,
                campaignRepository, List.of(), campaignService, ppcPropertiesSupport, directConfig,
                balanceResyncHandler, null);
    }

    @Test
    void ignoredCampaignTypesTest() {
        assertThat(campaignStatusCalcService.isIgnoredCampaignType(CampaignType.TEXT)).isFalse();
        assertThat(campaignStatusCalcService.isIgnoredCampaignType(CampaignType.DYNAMIC)).isFalse();
        assertThat(campaignStatusCalcService.isIgnoredCampaignType(CampaignType.CPM_BANNER)).isFalse();
        assertThat(campaignStatusCalcService.isIgnoredCampaignType(CampaignType.INTERNAL_AUTOBUDGET)).isTrue();
        assertThat(campaignStatusCalcService.isIgnoredCampaignType(CampaignType.INTERNAL_DISTRIB)).isTrue();
        assertThat(campaignStatusCalcService.isIgnoredCampaignType(CampaignType.INTERNAL_FREE)).isTrue();
        assertThat(campaignStatusCalcService.isIgnoredCampaignType(CampaignType.PERFORMANCE)).isTrue();
    }

    @Test
    void getAllCampaignsWithSameWalletTest() {
        when(campaignRepository.getWalletsWithCampaignsByWalletCampaignIds(anyInt(), anySet(), anyBoolean()))
                .thenReturn(new WalletsWithCampaigns(
                        List.of(
                                new Campaign()
                                        .withId(777L)
                                        .withUserId(900L),
                                new Campaign()
                                        .withId(888L)
                                        .withUserId(800L)
                        ),
                        List.of(
                                new Campaign()
                                        .withId(1L)
                                        .withWalletId(777L)
                                        .withUserId(901L),
                                new Campaign()
                                        .withId(2L)
                                        .withWalletId(777L)
                                        .withUserId(902L),
                                new Campaign()
                                        .withId(5L)
                                        .withWalletId(888L)
                                        .withUserId(800L)
                        )
                ));
        List<CampaignToResync> result =
                campaignStatusCalcService.getAllCampaignsWithSameWallet(5, List.of(
                        new AggregatedStatusCampaign()
                                .withId(2L)
                                .withWalletId(777L)
                                .withUserId(902L),
                        new AggregatedStatusCampaign()
                                .withId(5L)
                                .withWalletId(888L)
                                .withUserId(800L),
                        new AggregatedStatusCampaign()
                                .withId(10L)
                                .withWalletId(null)
                                .withUserId(700L)
                        ));
        assertThat(result).containsExactlyInAnyOrder(
                new CampaignToResync(1L, 901L),
                new CampaignToResync(2L, 902L),
                new CampaignToResync(5L, 800L),
                new CampaignToResync(10L, 700L)
        );
    }
}
