package ru.yandex.direct.core.entity.banner.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.banner.type.creative.BannerCreativeRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.ClientMeasurerSettings;
import ru.yandex.direct.core.entity.client.model.ClientMeasurerSystem;
import ru.yandex.direct.core.entity.client.service.ClientMeasurerSettingsService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.mediascope.MediascopeClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class MediascopePositionServiceTest {

    private static final int SHARD = 1;

    private CampaignRepository campaignRepository;
    private BannerRelationsRepository bannerRelationsRepository;
    private BannerCreativeRepository bannerCreativeRepository;
    private ClientMeasurerSettingsService clientMeasurerSettingsService;
    private CreativeRepository creativeRepository;

    private MediascopePositionService mediascopePositionService;

    private List<Long> campaignIds = List.of(44L);
    private List<Long> bannerIds = List.of(55L, 66L, 77L);
    private List<Long> creativeIds = List.of(88L, 99L, 101L);

    @Before
    public void before() {
        campaignRepository = mock(CampaignRepository.class);
        bannerRelationsRepository = mock(BannerRelationsRepository.class);
        bannerCreativeRepository = mock(BannerCreativeRepository.class);
        clientMeasurerSettingsService = mock(ClientMeasurerSettingsService.class);
        creativeRepository = mock(CreativeRepository.class);
        MediascopeClient mediascopeClient = mock(MediascopeClient.class);

        mediascopePositionService = new MediascopePositionService(
                campaignRepository,
                bannerRelationsRepository,
                bannerCreativeRepository,
                clientMeasurerSettingsService,
                creativeRepository,
                mediascopeClient);
    }

    @Test
    public void collectMediascopePositions_FewBanners_ReturnPositions() {
        Map<Long, Long> campaignIdsByBannerIds = Map.of(
                bannerIds.get(0), campaignIds.get(0),
                bannerIds.get(1), campaignIds.get(0),
                bannerIds.get(2), campaignIds.get(0));
        when(bannerRelationsRepository.getCampaignIdsByBannerIdsForShard(anyInt(), anyList()))
                .thenReturn(campaignIdsByBannerIds);

        Long clientId = 11L;
        List<Campaign> campaigns = List.of(
                new Campaign()
                        .withId(campaignIds.get(0))
                        .withClientId(clientId)
                        .withName("asfsaf")
                        .withStartTime(LocalDate.now()));

        when(campaignRepository.getCampaigns(anyInt(), anyCollection())).thenReturn(campaigns);

        Map<Long, ClientMeasurerSettings> clientMeasurerSettingsMap =
                Map.of(clientId, new ClientMeasurerSettings()
                        .withClientId(clientId)
                        .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                        .withSettings("{\"access_token\": \"1\", \"refresh_token\": \"2\"," +
                                " \"expires_at\": 1574471672, \"tmsec_prefix\": \"prefix\"}"));

        when(clientMeasurerSettingsService.getByClientIdsAndSystem(anyInt(), anyList(),
                eq(ClientMeasurerSystem.MEDIASCOPE))).thenReturn(clientMeasurerSettingsMap);

        Map<Long, Long> creativeIdsForBannerIds = Map.of(
                bannerIds.get(0), creativeIds.get(0),
                bannerIds.get(1), creativeIds.get(1),
                bannerIds.get(2), creativeIds.get(2));
        when(bannerCreativeRepository.getBannerIdToCreativeId(anyInt(), anyCollection())).thenReturn(creativeIdsForBannerIds);

        List<Creative> creatives = List.of(
                new Creative().withId(creativeIds.get(0)).withPreviewUrl("xxx"),
                new Creative().withId(creativeIds.get(1)).withPreviewUrl("yyy"),
                new Creative().withId(creativeIds.get(2)).withPreviewUrl("zzz"));
        when(creativeRepository.getCreatives(anyInt(), anyCollection())).thenReturn(creatives);

        var result = mediascopePositionService.collectPositions(SHARD, bannerIds);
        var position = result.get(bannerIds.get(0));

        assertThat(result.size(), is(3));
        assertThat(position.getPositionId(), is("prefix_5-44-55"));
        assertThat(position.getPositionName(), is("asfsaf-55"));
        assertThat(position.getCreativeId(), is("88"));
        assertThat(position.getCreativeUrl(), is("xxx"));
        assertThat(position.getFinishDate(), is(nullValue()));
        assertThat(position.getParams().get(0).getParamValue(), is("asfsaf"));
        assertThat(position.getAccessToken(), is("1"));
    }
}
