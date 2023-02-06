package ru.yandex.direct.intapi.entity.display.canvas.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreative;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerCreativeRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.display.canvas.model.AuthResponse;
import ru.yandex.direct.intapi.entity.display.canvas.model.CreativeCampaignRequest;
import ru.yandex.direct.intapi.entity.display.canvas.model.CreativeCampaignResult;
import ru.yandex.direct.intapi.entity.display.canvas.validation.CreativeCampaignValidationService;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.intapi.entity.display.canvas.model.ActionType.CREATIVE_GET;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DisplayCanvasCampaignServiceTest {

    private Long creativeId1;
    private Long creativeId2;

    @Autowired
    private Steps steps;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CreativeCampaignValidationService creativeCampaignValidationService;

    @Autowired
    private OldBannerCreativeRepository bannerCreativeRepository;

    private RbacService rbacService;
    private DisplayCanvasAuthService displayCanvasAuthService;

    private DisplayCanvasCampaignService displayCanvasCampaignService;

    private ClientInfo clientInfo;
    private CreativeInfo canvasInfo;
    private TextBannerInfo bannerInfo;

    private int shard;

    @Before
    public void before() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner();
        clientInfo = bannerInfo.getClientInfo();
        shard = clientInfo.getShard();
        creativeId1 = steps.creativeSteps().getNextCreativeId();
        canvasInfo = steps.creativeSteps().addDefaultCanvasCreative(clientInfo, creativeId1);
        creativeId2 = steps.creativeSteps().getNextCreativeId();

        OldBannerCreative bannerCreative = new OldBannerCreative()
                .withBannerId(bannerInfo.getBannerId())
                .withCampaignId(bannerInfo.getCampaignId())
                .withAdGroupId(bannerInfo.getAdGroupId())
                .withCreativeId(canvasInfo.getCreativeId())
                .withStatusModerate(OldBannerCreativeStatusModerate.NEW);
        bannerCreativeRepository.addBannerCreatives(shard, singletonList(bannerCreative));

        displayCanvasAuthService = mock(DisplayCanvasAuthService.class);
        when(displayCanvasAuthService.auth(clientInfo.getUid(), clientInfo.getClientId()))
                .thenReturn(new AuthResponse(singletonList(CREATIVE_GET), Collections.emptyList()));

        rbacService = mock(RbacService.class);
        when(rbacService.getChiefByClientId(clientInfo.getClientId())).thenReturn(clientInfo.getUid());

        shardHelper = spy(shardHelper);
        Mockito.doReturn(clientInfo.getLogin()).when(shardHelper).getLoginByUid(clientInfo.getUid());

        displayCanvasCampaignService = new DisplayCanvasCampaignService(shardHelper, rbacService,
                displayCanvasAuthService, creativeCampaignValidationService, campaignRepository);
    }

    @Test
    public void getCampIdsCreatives() {
        var creativeIdToCampaigns = displayCanvasCampaignService.getCreativesCampaigns(
                clientInfo.getUid(), clientInfo.getClientId(),
                new CreativeCampaignRequest().withCreativeIds(asList(creativeId1, creativeId2)));
        List<Long> capmIds = getCampIdsList(creativeIdToCampaigns);
        Assertions.assertThat(capmIds).containsOnly(bannerInfo.getCampaignId());
    }

    @Test
    public void creativeWithoutCamp() {
        var creativeIdToCampaigns = displayCanvasCampaignService.getCreativesCampaigns(
                clientInfo.getUid(), clientInfo.getClientId(),
                new CreativeCampaignRequest().withCreativeIds(singletonList(creativeId2)));
        List<Long> capmIds = getCampIdsList(creativeIdToCampaigns);
        Assertions.assertThat(capmIds).isNullOrEmpty();
    }

    private List<Long> getCampIdsList(Map<Long, List<CreativeCampaignResult>> response) {
        return response.entrySet()
                .stream().map(Map.Entry::getValue)
                .collect(Collectors.toList())
                .stream().flatMap(List::stream)
                .map(CreativeCampaignResult::getCampaignId)
                .collect(Collectors.toList());
    }

}
