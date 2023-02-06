package ru.yandex.direct.core.testing.steps;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;

public class RetargetingSteps {

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private RetargetingRepository retargetingRepository;

    public RetargetingInfo createDefaultRetargetingInActiveTextAdGroup() {
        return createDefaultRetargetingInActiveTextAdGroup(
                new CampaignInfo().withCampaign(activeTextCampaign(null, null)));
    }

    public RetargetingInfo createDefaultRetargetingInActiveTextAdGroup(ClientInfo clientInfo) {
        return createDefaultRetargetingInActiveTextAdGroup(new CampaignInfo()
                .withCampaign(activeTextCampaign(null, null))
                .withClientInfo(clientInfo));
    }

    public RetargetingInfo createDefaultRetargetingInActiveTextAdGroup(CampaignInfo campaignInfo) {
        return createRetargeting(new RetargetingInfo()
                .withAdGroupInfo(new AdGroupInfo().withAdGroup(activeTextAdGroup(null)))
                .withCampaignInfo(campaignInfo));
    }

    public RetargetingInfo createDefaultRetargeting() {
        return createRetargeting((Retargeting) null);
    }

    public RetargetingInfo createDefaultRetargeting(AdGroupInfo adGroupInfo) {
        return createRetargeting(null, adGroupInfo);
    }

    public RetargetingInfo createDefaultRetargeting(CampaignInfo campaignInfo) {
        return createRetargeting(null, campaignInfo);
    }

    public RetargetingInfo createRetargeting(Retargeting retargeting) {
        return createRetargeting(new RetargetingInfo().withRetargeting(retargeting));
    }

    public RetargetingInfo createRetargeting(Retargeting retargeting, ClientInfo clientInfo) {
        return createRetargeting(retargeting, new CampaignInfo().withClientInfo(clientInfo));
    }

    public RetargetingInfo createRetargeting(Retargeting retargeting, CampaignInfo campaignInfo) {
        return createRetargeting(retargeting, new AdGroupInfo().withCampaignInfo(campaignInfo));
    }

    public RetargetingInfo createRetargeting(Retargeting retargeting, AdGroupInfo adGroupInfo) {
        return createRetargeting(new RetargetingInfo()
                .withAdGroupInfo(adGroupInfo)
                .withRetargeting(retargeting));
    }

    public RetargetingInfo createRetargeting(Retargeting retargeting,
                                             ClientInfo clientInfo, RetConditionInfo retConditionInfo) {
        return createRetargeting(retargeting, new CampaignInfo().withClientInfo(clientInfo), retConditionInfo);
    }

    public RetargetingInfo createRetargeting(Retargeting retargeting,
                                             CampaignInfo campaignInfo, RetConditionInfo retConditionInfo) {
        return createRetargeting(retargeting, new AdGroupInfo().withCampaignInfo(campaignInfo), retConditionInfo);
    }

    public RetargetingInfo createRetargeting(Retargeting retargeting,
                                             AdGroupInfo adGroupInfo, RetConditionInfo retConditionInfo) {
        return createRetargeting(new RetargetingInfo()
                .withAdGroupInfo(adGroupInfo)
                .withRetConditionInfo(retConditionInfo)
                .withRetargeting(retargeting));
    }

    public RetargetingInfo createRetargeting(RetargetingInfo retargetingInfo) {
        if (retargetingInfo.getRetargeting() == null) {
            retargetingInfo.withRetargeting(defaultRetargeting());
        }
        if (retargetingInfo.getRetargetingId() == null) {
            if (retargetingInfo.getAdGroupInfo().getClientInfo() !=
                    retargetingInfo.getRetConditionInfo().getClientInfo()) {
                throw new IllegalArgumentException(
                        "невалидный RetargetingInfo: в AdGroupInfo и RetConditionInfo "
                                + "содержаться разные объекты ClientInfo");
            }
            adGroupSteps.createAdGroup(retargetingInfo.getAdGroupInfo());
            retConditionSteps.createRetCondition(retargetingInfo.getRetConditionInfo());
            retargetingInfo.getRetargeting()
                    .withCampaignId(retargetingInfo.getCampaignId())
                    .withAdGroupId(retargetingInfo.getAdGroupId())
                    .withRetargetingConditionId(retargetingInfo.getRetConditionId());
            retargetingRepository.add(retargetingInfo.getShard(), singletonList(retargetingInfo.getRetargeting()));
        }
        return retargetingInfo;
    }

    public RetargetingInfo addRetargeting(AdGroupInfo adGroupInfo, Retargeting retargeting) {
        RetargetingInfo retargetingInfo = new RetargetingInfo()
                .withAdGroupInfo(adGroupInfo)
                .withRetargeting(retargeting);
        retargetingRepository.add(retargetingInfo.getShard(), singletonList(retargetingInfo.getRetargeting()));
        return retargetingInfo;
    }

    public Long addRetargeting(int shard, Retargeting retargeting) {
        return retargetingRepository.add(shard, singletonList(retargeting)).get(0);
    }

    /**
     * временное решение, будем думать что делать с этим в DIRECT-108812
     */
    public void createRetargetingRaw(Integer shard, Retargeting retargeting, RetConditionInfo retConditionInfo) {
        if (retargeting == null) {
            retargeting = defaultRetargeting();
        }
        retConditionSteps.createRetCondition(retConditionInfo);
        retargetingRepository.add(shard, singletonList(retargeting));
    }

    @NotNull
    public List<Retargeting> getRetargetingsByAdGroupId(int shard, long adGroupId) {
        return retargetingRepository.getRetargetingsByAdGroups(shard, List.of(adGroupId));
    }
}
