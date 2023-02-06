package ru.yandex.autotests.direct.cmd.steps;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.group.Retargeting;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.steps.campaings.CampaignSteps;
import ru.yandex.autotests.direct.cmd.steps.groups.GroupsSteps;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopLevelSteps extends DirectBackEndSteps {

    public static class Ids {
        private Long campaignId;
        private List<Long> groupIds;
        private Map<Long, List<Long>> bannerIds;

        public Long getCampaignId() {
            return campaignId;
        }

        public List<Long> getGroupIds() {
            return groupIds;
        }

        public List<Long> getBannerIds(long groupId) {
            return bannerIds.get(groupId);
        }

        public Long getFirstGroupId() {
            return groupIds.get(0);
        }

        public Long getFirstBannerId() {
            return bannerIds.get(groupIds.get(0)).get(0);
        }
    }

    public Ids createTextCampaignWithRetargeting(long retCondId, String uLogin) {
        Retargeting retargeting = new Retargeting().
                withRetCondId(retCondId).
                withPriceContext("200");
        Group group = loadDefaultTextGroup().
                withRetargetings(Collections.singletonList(retargeting));
        return createDefaultTextCampaign(uLogin, null, group);
    }

    public Ids createDefaultTextCampaign(String uLogin) {
        return createDefaultTextCampaign(uLogin, null, null);
    }

    private Ids createDefaultTextCampaign(String uLogin, SaveCampRequest campRequest, Group group) {
        if (campRequest == null) {
            campRequest = loadDefaultSaveCampRequest();
        }
        if (group == null) {
            group = loadDefaultTextGroup();
        }

        Long campaignId = createCampaign(campRequest.
                withMediaType(CampaignTypeEnum.TEXT.getValue()).
                withUlogin(uLogin));
        createTextGroup(campaignId, uLogin, group);

        return getCreatedIds(campaignId, uLogin);
    }

    private Long createCampaign(SaveCampRequest campRequest) {
        return getInstance(CampaignSteps.class, getContext()).saveNewCampaign(campRequest);
    }

    private void createTextGroup(long campaignId, String ulogin, Group group) {
        group.setCampaignID(String.valueOf(campaignId));
        group.getBanners().forEach(b -> b.withCid(campaignId));
        GroupsParameters groupRequest = GroupsParameters.forNewCamp(
                ulogin, campaignId, group);

        getInstance(GroupsSteps.class, getContext()).postSaveTextAdGroups(groupRequest);
    }

    private Ids getCreatedIds(long campaignId, String uLogin) {
        Ids ids = new Ids();
        ids.campaignId = campaignId;

        ShowCampResponse showCamp = getInstance(CampaignSteps.class, getContext())
                .getShowCamp(uLogin, String.valueOf(campaignId));
        ids.groupIds = showCamp.getGroups().
                stream().
                map(Banner::getAdGroupId).
                collect(Collectors.toList());

        ids.bannerIds = new HashMap<>();
        ids.groupIds.forEach(groupId -> {
            Group group = getInstance(GroupsSteps.class, getContext()).getAdGroup(uLogin, groupId);
            List<Long> bannerIds = group.getBanners().
                    stream().
                    map(Banner::getBid).
                    collect(Collectors.toList());
            ids.bannerIds.put(groupId, bannerIds);
        });

        return ids;
    }

    private SaveCampRequest loadDefaultSaveCampRequest() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_NEW_TEXT_CAMP_DEFAULT, SaveCampRequest.class);
    }

    private Group loadDefaultTextGroup() {
        return GroupsFactory.getDefaultTextGroup();
    }
}
