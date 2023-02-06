package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo;
import ru.yandex.direct.core.entity.promocodes.model.TearOffReason;
import ru.yandex.direct.core.entity.promocodes.service.PromocodesAntiFraudService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyPromocodeParameters;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestPromocodeInfo.createPromocodeInfo;

public class NotifYPromocodeServiceFilterPromocodesToSaveTest {
    private static final String DOMAIN = "yandex.ru";
    private static final NotifyPromocodeParameters UPDATE_REQUEST = new NotifyPromocodeParameters()
            .withCampaignId(1L).withServiceId(7);
    private final PromocodesAntiFraudService antiFraudService;
    private final NotifyPromocodeService service;

    @SuppressWarnings("ConstantConditions")
    public NotifYPromocodeServiceFilterPromocodesToSaveTest() {
        antiFraudService = mock(PromocodesAntiFraudService.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByCampaignId(anyLong())).thenReturn(1);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        when(campaignRepository.getCampaigns(anyInt(), anyList())).thenReturn(List.of(new Campaign()));
        when(antiFraudService.isAcceptableWallet(any())).thenReturn(false);
        service = new NotifyPromocodeService(null, antiFraudService, null, campaignRepository, shardHelper, null, null);
    }

    @Test
    public void domainWithoutStat_returnAllCodes() {
        when(antiFraudService.domainHasStat(DOMAIN)).thenReturn(false);
        var promocodeInfos = List.of(createPromocodeInfo(), createPromocodeInfo(), createPromocodeInfo());
        var result = service.filterPromocodesToSave(promocodeInfos, UPDATE_REQUEST, DOMAIN);
        assertThat("Should produce the same list", result, equalTo(promocodeInfos));
    }

    @Test
    public void domainWithStat_codesForNewClientsOnly_returnNoCodesAndTearOff() {
        when(antiFraudService.domainHasStat(DOMAIN)).thenReturn(true);
        var promocodeInfos = List.of(newClientPromocode(), newClientPromocode(), newClientPromocode());
        var result = service.filterPromocodesToSave(promocodeInfos, UPDATE_REQUEST, DOMAIN);
        assertThat("Should produce an empty list", result, equalTo(List.of()));
        verify(antiFraudService)
                .tearOffPromocodes(anyInt(), anyLong(), eq(promocodeInfos), eq(TearOffReason.HAS_IMPRESSIONS));
    }

    @Test
    public void domainWithStat_codesNotForNewClientsOnly_returnAllCodes() {
        when(antiFraudService.domainHasStat(DOMAIN)).thenReturn(true);
        var promocodeInfos = List.of(createPromocodeInfo(), createPromocodeInfo(), createPromocodeInfo());
        var result = service.filterPromocodesToSave(promocodeInfos, UPDATE_REQUEST, DOMAIN);
        assertThat("Should produce the same list", result, equalTo(promocodeInfos));
        verify(antiFraudService, never())
                .tearOffPromocodes(anyInt(), anyLong(), eq(promocodeInfos), any());
    }

    @Test
    public void domainWithStat_someCodesNotForNewClientsOnly_returnFilteredAndTearOff() {
        when(antiFraudService.domainHasStat(DOMAIN)).thenReturn(true);
        var p1 = createPromocodeInfo();
        var p2 = newClientPromocode();
        var p3 = createPromocodeInfo();
        var p4 = newClientPromocode();
        var promocodeInfos = List.of(p1, p2, p3, p4);
        var result = service.filterPromocodesToSave(promocodeInfos, UPDATE_REQUEST, DOMAIN);
        assertThat("Should produce the list of promocodes for old clients", result, equalTo(List.of(p1, p3)));
        verify(antiFraudService)
                .tearOffPromocodes(anyInt(), anyLong(), eq(List.of(p2, p4)), eq(TearOffReason.HAS_IMPRESSIONS));
    }

    private static PromocodeInfo newClientPromocode() {
        return createPromocodeInfo().withForNewClientsOnly(true);
    }
}
