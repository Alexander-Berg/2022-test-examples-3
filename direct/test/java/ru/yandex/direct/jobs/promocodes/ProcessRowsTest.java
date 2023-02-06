package ru.yandex.direct.jobs.promocodes;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.promocodes.model.CampPromocodes;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo;
import ru.yandex.direct.core.entity.promocodes.model.TearOffReason;
import ru.yandex.direct.core.entity.promocodes.repository.CampPromocodesRepository;
import ru.yandex.direct.core.entity.promocodes.service.PromocodesAntiFraudService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessRowsTest {
    private static final long INVOICE_ID = 123456L;
    private static final long PROMOCODE_ID = 987654L;
    private static final long CAMPAIGN_ID = 777L;
    private static final int SHARD = 2;
    private static final int SERVICE_ID = 7;
    private static final List<FraudPromoRedirectsInfo> ROWS = List.of(
            new FraudPromoRedirectsInfo(CAMPAIGN_ID, PROMOCODE_ID, INVOICE_ID, 0L));
    SafeSearchTearOffPromocodesJob safeSearchTearOffPromocodesJob;
    PromocodesAntiFraudService promocodesAntiFraudService;
    CampaignRepository campaignRepository;
    CampPromocodesRepository campPromocodesRepository;

    @BeforeEach
    void init() {
        campPromocodesRepository = mock(CampPromocodesRepository.class);
        campaignRepository = mock(CampaignRepository.class);

        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByCampaignId(any())).thenReturn(SHARD);

        promocodesAntiFraudService = mock(PromocodesAntiFraudService.class);

        safeSearchTearOffPromocodesJob = new SafeSearchTearOffPromocodesJob(SERVICE_ID,
                null,
                null,
                promocodesAntiFraudService,
                campaignRepository,
                campPromocodesRepository,
                shardHelper
        );
    }

    @Test
    void campaign_noPromocodes_doNothing() {
        when(campaignRepository.getCampaigns(eq(SHARD), anyList())).thenReturn(List.of(new Campaign().withWalletId(0L)));
        when(campPromocodesRepository.getCampaignPromocodes(eq(SHARD), eq(CAMPAIGN_ID)))
                .thenReturn(null);

        safeSearchTearOffPromocodesJob.processRows(ROWS);
        verify(promocodesAntiFraudService, never()).tearOffPromocodes(eq(SERVICE_ID), anyLong(), anyList(),
                any(TearOffReason.class));
        assertTrue(safeSearchTearOffPromocodesJob.processedItems.contains(0L));
    }

    @Test
    void campaign_hasWalletNoPromocodes_doNothing() {
        when(campaignRepository.getCampaigns(eq(SHARD), anyList())).thenReturn(List.of(new Campaign().withWalletId(34L)));
        when(campPromocodesRepository.getCampaignPromocodes(eq(SHARD), eq(34L)))
                .thenReturn(null);

        safeSearchTearOffPromocodesJob.processRows(ROWS);
        verify(promocodesAntiFraudService, never()).tearOffPromocodes(eq(SERVICE_ID), anyLong(), anyList(),
                any(TearOffReason.class));
        assertTrue(safeSearchTearOffPromocodesJob.processedItems.contains(0L));
    }

    @Test
    void campaign_hasWalletWithPromocodes_tearOff() {
        when(campaignRepository.getCampaigns(eq(SHARD), anyList())).thenReturn(List.of(new Campaign().withWalletId(34L)));
        List<PromocodeInfo> promocodeInfos = List.of(new PromocodeInfo().withId(PROMOCODE_ID));
        when(campPromocodesRepository.getCampaignPromocodes(eq(SHARD), eq(34L)))
                .thenReturn(new CampPromocodes().withPromocodes(promocodeInfos));

        safeSearchTearOffPromocodesJob.processRows(ROWS);
        verify(promocodesAntiFraudService).tearOffPromocodes(eq(SERVICE_ID), eq(34L), anyList(),
                eq(TearOffReason.ANTIVIR_ANALYTICS));
        assertTrue(safeSearchTearOffPromocodesJob.processedItems.contains(0L));
    }

    @Test
    void campaign_hasPromocodes_tearOff() {
        when(campaignRepository.getCampaigns(eq(SHARD), anyList())).thenReturn(List.of(new Campaign().withWalletId(0L)));
        List<PromocodeInfo> promocodeInfos = List.of(new PromocodeInfo().withId(PROMOCODE_ID));
        when(campPromocodesRepository.getCampaignPromocodes(eq(SHARD), eq(CAMPAIGN_ID)))
                .thenReturn(new CampPromocodes().withPromocodes(promocodeInfos));

        safeSearchTearOffPromocodesJob.processRows(ROWS);
        verify(promocodesAntiFraudService).tearOffPromocodes(eq(SERVICE_ID), eq(CAMPAIGN_ID), anyList(),
                eq(TearOffReason.ANTIVIR_ANALYTICS));
        assertTrue(safeSearchTearOffPromocodesJob.processedItems.contains(0L));
    }

    @Test
    void campaign0_noPromocodes_campaign1_hasPromocodes_tearOffFrom1() {
        when(campaignRepository.getCampaigns(eq(SHARD), anyList())).thenReturn(List.of(new Campaign().withWalletId(0L)));
        List<PromocodeInfo> promocodeInfos = List.of(new PromocodeInfo().withId(PROMOCODE_ID));
        when(campPromocodesRepository.getCampaignPromocodes(eq(SHARD), eq(CAMPAIGN_ID)))
                .thenReturn(new CampPromocodes().withPromocodes(promocodeInfos));

        long cid0 = 775L;
        List<FraudPromoRedirectsInfo> rows = List.of(
                new FraudPromoRedirectsInfo(cid0, PROMOCODE_ID + 1, INVOICE_ID + 1, 0L),
                new FraudPromoRedirectsInfo(CAMPAIGN_ID, PROMOCODE_ID, INVOICE_ID, 1L)
        );
        safeSearchTearOffPromocodesJob.processRows(rows);

        verify(promocodesAntiFraudService, never()).tearOffPromocodes(eq(SERVICE_ID), eq(cid0), anyList(),
                any(TearOffReason.class));
        verify(promocodesAntiFraudService).tearOffPromocodes(eq(SERVICE_ID), eq(CAMPAIGN_ID), anyList(),
                eq(TearOffReason.ANTIVIR_ANALYTICS));
        assertTrue(safeSearchTearOffPromocodesJob.processedItems.containsAll(List.of(0L, 1L)));
    }
}
