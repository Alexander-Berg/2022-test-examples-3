package ru.yandex.direct.core.entity.banner.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BannerServiceGetLastChangedBannerIdsTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final Long CID = RandomNumberUtils.nextPositiveLong();
    private static final Long OTHER_CLIENT_CID = RandomNumberUtils.nextPositiveLong();
    private static final int SHARD = 1;

    @Mock
    private BannerRelationsRepository bannerRelationsRepository;

    @Mock
    private CampaignRepository campaignRepository;

    // Нужно для корректной инициализации конструктора в BannerService
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @InjectMocks
    private BannerService bannerService;

    @Before
    public void setUp() {
        doReturn(Set.of(CID)).when(campaignRepository).getExistingCampaignIds(anyInt(), eq(CLIENT_ID),
                eq(Set.of(CID, OTHER_CLIENT_CID)));
    }

    @Test
    public void getLastChangedBannerIds_checkCampaignIdsFiltering() {
        bannerService.getLastChangedBannerIds(SHARD, CLIENT_ID, Map.of(
                CID, List.of(BannersBannerType.text, BannersBannerType.cpc_video),
                OTHER_CLIENT_CID, List.of(BannersBannerType.cpc_video)
        ));

        verify(bannerRelationsRepository)
                .getLastChangedBannerIdsWithCampaignIds(eq(SHARD), eq(Set.of(CID)), anyCollection());
    }

    @Test
    public void getLastChangedBannerIds_checkBannerTypesFiltering() {
        bannerService.getLastChangedBannerIds(SHARD, CLIENT_ID, Map.of(
                CID, List.of(BannersBannerType.text),
                OTHER_CLIENT_CID, List.of(BannersBannerType.cpc_video)
        ));

        verify(bannerRelationsRepository)
                .getLastChangedBannerIdsWithCampaignIds(eq(SHARD), anyCollection(), eq(Set.of(BannersBannerType.text)));
    }

}
