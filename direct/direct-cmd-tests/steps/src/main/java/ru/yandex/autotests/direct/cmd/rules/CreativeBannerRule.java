package ru.yandex.autotests.direct.cmd.rules;

import java.util.ArrayList;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.common.CreativeBanner;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.direct.cmd.util.CampaignHelper.deleteAdGroupMobileContent;

// таск:
public class CreativeBannerRule extends BannersRule {

    private CampaignTypeEnum campaignType;
    private Long creativeId;

    public CreativeBannerRule(CampaignTypeEnum campaignType) {
        super(CmdBeans.COMMON_REQUEST_GROUP_TEXT_DEFAULT2);
        this.campaignType = campaignType;
        switch (campaignType) {
            case TEXT:
                withGroupTemplate(CmdBeans.COMMON_REQUEST_GROUP_TEXT_DEFAULT2);
                break;
            case MOBILE:
                withGroupTemplate(CmdBeans.COMMON_REQUEST_GROUP_MOBILE_DEFAULT2);
                break;
        }
        getGroup().withBanners(new ArrayList<>(singletonList(BannersFactory.getDefaultImageBanner(campaignType))));
        withMediaType(campaignType);
    }

    public Long getCreativeId() {
        return creativeId;
    }

    @Override
    public void createGroup() {
        Group group = getGroup();
        createCreative();
        group.setCampaignID(campaignId.toString());
        group.getBanners().get(0)
                .withCreativeBanner(new CreativeBanner().withCreativeId(creativeId));
        group.getBanners().stream().forEach(b -> b.withCid(campaignId));

        if (isMobileCamp()) {
            getGroup().setStoreContentHref(getAppData().getStoreContentHref());
            getGroup().getMobileContent().setMobileContentId(getAppData().getMobileContentId().toString());
        }

        GroupsParameters groupRequest = GroupsParameters.forNewCamp(ulogin, campaignId, group);
        saveGroup(groupRequest);
    }

    @Override
    public void saveGroup(GroupsParameters request) {
        switch (campaignType) {
            case TEXT:
                getDirectCmdSteps().groupsSteps().postSaveTextAdGroups(request);
                break;
            case MOBILE:
                getDirectCmdSteps().groupsSteps().postSaveMobileAdGroups(request);
                break;
            default:
                throw new BackEndClientException("Не указан тип кампании");
        }
    }

    private void createCreative() {
        creativeId = TestEnvironment.newDbSteps().useShardForLogin(ulogin).perfCreativesSteps()
                .saveDefaultCanvasCreativesForClient(Long.parseLong(User.get(ulogin).getClientID()));
    }

    @Override
    public Group getCurrentGroup() {
        return getDirectCmdSteps().campaignSteps().getShowCampMultiEdit(ulogin, campaignId, groupId, bannerId)
                .getCampaign().getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"));
    }

    @Override
    protected void finish() {
        if (isMobileCamp()) {
            deleteAdGroupMobileContent(campaignId, ulogin);
        }
        super.finish();
    }
}
