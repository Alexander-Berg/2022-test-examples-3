package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import junitparams.JUnitParamsRunner;
import one.util.streamex.EntryStream;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithFavorite;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class CampaignWithFavoriteTypeSupportTest {

    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public Steps steps;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Test
    public void oneCampaign_addSingleUidToFavorites() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();

        Set<Long> uidsToAdd = Set.of(campaignInfo.getUid());
        changeFavoriteUids(campaignInfo.getClientInfo(), Map.of(getActualCampaign(campaignInfo), uidsToAdd));

        CampaignWithFavorite modifiedCampaign = getActualCampaign(campaignInfo);
        assertThat(modifiedCampaign.getFavoriteForUids())
                .containsExactlyInAnyOrderElementsOf(uidsToAdd);
    }

    @Test
    public void oneCampaign_addManyUidToFavorites() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        ClientInfo otherClient = steps.clientSteps().createDefaultClient();

        Set<Long> uidsToAdd = Set.of(campaignInfo.getUid(), otherClient.getUid());
        changeFavoriteUids(campaignInfo.getClientInfo(), Map.of(getActualCampaign(campaignInfo), uidsToAdd));

        CampaignWithFavorite modifiedCampaign = getActualCampaign(campaignInfo);
        assertThat(modifiedCampaign.getFavoriteForUids())
                .containsExactlyInAnyOrderElementsOf(uidsToAdd);
    }

    @Test
    public void oneCampaign_removeOneUidFromFavorites() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        ClientInfo otherClient = steps.clientSteps().createDefaultClient();
        Set<Long> initialUids = Set.of(campaignInfo.getUid(), otherClient.getUid());
        changeFavoriteUids(campaignInfo.getClientInfo(), Map.of(getActualCampaign(campaignInfo), initialUids));

        Set<Long> uidsToRemove = Set.of(campaignInfo.getUid());
        Set<Long> leftUids = Sets.difference(initialUids, uidsToRemove);
        changeFavoriteUids(campaignInfo.getClientInfo(), Map.of(getActualCampaign(campaignInfo), leftUids));

        CampaignWithFavorite modifiedCampaign = getActualCampaign(campaignInfo);
        assertThat(modifiedCampaign.getFavoriteForUids())
                .containsExactlyInAnyOrderElementsOf(leftUids);
    }

    @Test
    public void oneCampaign_removeAllUidsFromFavorites() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        ClientInfo otherClient = steps.clientSteps().createDefaultClient();
        Set<Long> initialUids = Set.of(campaignInfo.getUid(), otherClient.getUid());
        changeFavoriteUids(campaignInfo.getClientInfo(), Map.of(getActualCampaign(campaignInfo), initialUids));

        changeFavoriteUids(campaignInfo.getClientInfo(), Map.of(getActualCampaign(campaignInfo),
                Collections.emptySet()));

        CampaignWithFavorite modifiedCampaign = getActualCampaign(campaignInfo);
        assertThat(modifiedCampaign.getFavoriteForUids())
                .isNullOrEmpty();
    }

    @Test
    public void twoCampaigns_addSingleUidToFavorites() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        List<CampaignInfo> campaigns = List.of(
                steps.campaignSteps().createActiveTextCampaign(clientInfo),
                steps.campaignSteps().createActiveTextCampaign(clientInfo)
        );

        Set<Long> uidsToAdd = Set.of(clientInfo.getUid());
        changeFavoriteUids(clientInfo, Map.of(
                getActualCampaign(campaigns.get(0)), uidsToAdd,
                getActualCampaign(campaigns.get(1)), uidsToAdd)
        );

        List<CampaignWithFavorite> modifiedCampaigns = List.of(
                getActualCampaign(campaigns.get(0)),
                getActualCampaign(campaigns.get(1))
        );
        assertThat(modifiedCampaigns)
                .extracting(CampaignWithFavorite::getFavoriteForUids)
                .containsOnly(uidsToAdd, uidsToAdd);
    }

    @Test
    public void twoCampaigns_removeAllUidsFromFavorites() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        List<CampaignInfo> campaigns = List.of(
                steps.campaignSteps().createActiveTextCampaign(clientInfo),
                steps.campaignSteps().createActiveTextCampaign(clientInfo)
        );
        Set<Long> initialUids = Set.of(clientInfo.getUid());
        changeFavoriteUids(clientInfo, Map.of(
                getActualCampaign(campaigns.get(0)), initialUids,
                getActualCampaign(campaigns.get(1)), initialUids
        ));

        changeFavoriteUids(clientInfo, Map.of(
                getActualCampaign(campaigns.get(0)), Collections.emptySet(),
                getActualCampaign(campaigns.get(1)), Collections.emptySet())
        );

        List<CampaignWithFavorite> modifiedCampaigns = List.of(
                getActualCampaign(campaigns.get(0)),
                getActualCampaign(campaigns.get(1))
        );
        assertThat(modifiedCampaigns)
                .extracting(CampaignWithFavorite::getFavoriteForUids)
                .containsOnlyNulls();
    }

    private void changeFavoriteUids(ClientInfo clientInfo, Map<CampaignWithFavorite, Set<Long>> uidsToChange) {
        List<AppliedChanges<CampaignWithFavorite>> appliedChanges = EntryStream.of(uidsToChange)
                .mapKeyValue((campaign, uids) -> {
                    ModelChanges<CampaignWithFavorite> campaignModelChanges =
                            new ModelChanges<>(campaign.getId(), CampaignWithFavorite.class)
                                    .process(uids, CampaignWithFavorite.FAVORITE_FOR_UIDS);
                    return campaignModelChanges.applyTo(campaign);
                })
                .toList();

        RestrictedCampaignsUpdateOperationContainer updateOperationContainer =
                RestrictedCampaignsUpdateOperationContainer.create(
                        clientInfo.getShard(),
                        clientInfo.getUid(),
                        clientInfo.getClientId(),
                        clientInfo.getUid(),
                        clientInfo.getUid()
                );
        campaignModifyRepository.updateCampaigns(updateOperationContainer, appliedChanges);
    }

    private CampaignWithFavorite getActualCampaign(CampaignInfo campaignInfo) {
        int shard = campaignInfo.getShard();
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());
        List<? extends BaseCampaign> campaigns = campaignTypedRepository.getTyped(shard, campaignIds);
        return (TextCampaign) campaigns.get(0);
    }
}
