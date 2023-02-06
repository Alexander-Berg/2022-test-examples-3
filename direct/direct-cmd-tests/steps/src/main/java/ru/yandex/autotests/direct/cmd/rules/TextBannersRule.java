package ru.yandex.autotests.direct.cmd.rules;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.VideoAddition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

public class TextBannersRule extends BannersRule {

    private ImageUploadHelper imageUploadHelper;
    private VideoAdditionCreativeRule videoAdditionCreativeRule = null;

    public TextBannersRule() {
        super(CmdBeans.COMMON_REQUEST_GROUP_TEXT_DEFAULT2);
        withMediaType(CampaignTypeEnum.TEXT);
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

    public TextBannersRule withImageUploader(ImageUploadHelper imageUploadHelper) {
        this.imageUploadHelper = imageUploadHelper;
        return this;
    }

    @Override
    public void createGroup() {
        Group group = getGroup();
        group.setCampaignID(campaignId.toString());
        group.getBanners().forEach(b -> b.withCid(campaignId));

        if (videoAdditionCreativeRule != null) {
            Long creativeId = videoAdditionCreativeRule.getCreativeId();

            group.getBanners().get(0).setVideoResources(
                    VideoAddition.getDefaultVideoAddition(creativeId)
            );
        }

        uploadImageIfNecessary();

        GroupsParameters groupRequest = GroupsParameters.forNewCamp(
                ulogin, campaignId, group);
        saveGroup(groupRequest);
    }

    @Override
    public void saveGroup(GroupsParameters request) {
        getDirectCmdSteps().groupsSteps().postSaveTextAdGroups(request);
    }

    @Override
    public Group getCurrentGroup() {
        return getDirectCmdSteps().campaignSteps().getShowCampMultiEdit(ulogin, campaignId, groupId, bannerId)
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

    public TextBannersRule withVideoAddition(VideoAdditionCreativeRule videoAdditionCreativeRule) {
        this.videoAdditionCreativeRule = videoAdditionCreativeRule;
        return this;
    }
}
