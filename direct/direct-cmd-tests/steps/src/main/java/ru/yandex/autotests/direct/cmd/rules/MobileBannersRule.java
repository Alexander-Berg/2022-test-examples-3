package ru.yandex.autotests.direct.cmd.rules;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent.EditAdGroupsMobileContentRequest;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

import static ru.yandex.autotests.direct.cmd.util.CampaignHelper.deleteAdGroupMobileContent;

public class MobileBannersRule extends BannersRule {

    private ImageUploadHelper imageUploadHelper;

    //use new MobileBannersRule()
    @Deprecated
    public static MobileBannersRule mobileBannersRuleNewType() {
        return new MobileBannersRule();
    }

    public MobileBannersRule() {
        super(CmdBeans.COMMON_REQUEST_GROUP_MOBILE_DEFAULT2);
        withMediaType(CampaignTypeEnum.MOBILE);
    }

    @Override
    public <T extends NeedsCmdSteps> T withDirectCmdSteps(DirectCmdSteps directCmdSteps) {
        if (imageUploadHelper != null) {
            imageUploadHelper.withBannerImageSteps(directCmdSteps.bannerImagesSteps());
        }
        return super.withDirectCmdSteps(directCmdSteps);
    }

    public ImageUploadHelper getImageUploadHelper() {
        return imageUploadHelper;
    }

    public MobileBannersRule withImageUploader(ImageUploadHelper imageUploadHelper) {
        this.imageUploadHelper = imageUploadHelper;
        return this;
    }

    @Override
    public void createGroup() {
        Group group = getGroup();
        group.setCampaignID(campaignId.toString());
        group.getBanners().forEach(b -> {
            b.withAdType("text");
            b.withCid(campaignId);
        });

        group.setStoreContentHref(getAppData().getStoreContentHref());
        group.getMobileContent().setMobileContentId(getAppData().getMobileContentId().toString());

        uploadImageIfNecessary();

        GroupsParameters groupRequest = GroupsParameters.forNewCamp(ulogin, campaignId, group);

        saveGroup(groupRequest);
    }

    @Override
    public void saveGroup(GroupsParameters request) {
        getDirectCmdSteps().groupsSteps().postSaveMobileAdGroups(request);
    }

    @Override
    public Group getCurrentGroup() {
        return getDirectCmdSteps().groupsSteps().getEditAdGroupsMobileContent(EditAdGroupsMobileContentRequest
                .forSingleBanner(ulogin, campaignId, groupId, bannerId))
                .getCampaign().getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"));
    }

    private void uploadImageIfNecessary() {
        if (imageUploadHelper != null) {
            imageUploadHelper.forCampaign(campaignId);
            imageUploadHelper.uploadAndResize();
            imageUploadHelper.fillBannerByUploadedImage(getGroup().getBanners().get(0));
        }
    }

    @Override
    protected void finish() {
        deleteAdGroupMobileContent(campaignId, ulogin);
        super.finish();
    }

    public Long getMobileAppId() {
        return getAppData().getMobileAppId();
    }

    public Long getMobileContentId() {
        return getAppData().getMobileContentId();
    }
}
