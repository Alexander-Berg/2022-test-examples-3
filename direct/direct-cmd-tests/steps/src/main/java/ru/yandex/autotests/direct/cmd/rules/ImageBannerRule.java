package ru.yandex.autotests.direct.cmd.rules;

import java.util.ArrayList;
import java.util.Collections;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

import static ru.yandex.autotests.direct.cmd.util.CampaignHelper.deleteAdGroupMobileContent;

public class ImageBannerRule extends BannersRule {

    private NewImagesUploadHelper imageUploadHelper;
    private CampaignTypeEnum campaignType;

    public ImageBannerRule(CampaignTypeEnum campaignType) {
        //todo change
        super(CmdBeans.COMMON_REQUEST_GROUP_TEXT_DEFAULT2);
        this.campaignType = campaignType;
        switch (campaignType) {
            case TEXT:
                withGroupTemplate(CmdBeans.COMMON_REQUEST_GROUP_TEXT_DEFAULT2);
                getGroup().setBanners(
                        new ArrayList<>(Collections.singletonList(BannersFactory.getDefaultTextImageBanner())));
                break;
            case MOBILE:
                withGroupTemplate(CmdBeans.COMMON_REQUEST_GROUP_MOBILE_DEFAULT2);
                getGroup().setBanners(
                        new ArrayList<>(Collections.singletonList(BannersFactory.getDefaultMobileAppImageBanner())));
                break;
        }
        withMediaType(campaignType);

    }

    @Override
    public <T extends NeedsCmdSteps> T withDirectCmdSteps(DirectCmdSteps directCmdSteps) {
        if (imageUploadHelper != null) {
            imageUploadHelper.withBannerImageSteps(directCmdSteps.bannerImagesSteps());
        }
        return super.withDirectCmdSteps(directCmdSteps);
    }

    public NewImagesUploadHelper getImageUploadHelper() {
        return imageUploadHelper;
    }

    public ImageBannerRule withImageUploader(NewImagesUploadHelper imageUploadHelper) {
        this.imageUploadHelper = imageUploadHelper;
        return this;
    }

    @Override
    public void createGroup() {
        Group group = getGroup();
        group.setCampaignID(campaignId.toString());
        group.getBanners().stream().forEach(b -> b.withCid(campaignId));

        if (isMobileCamp()) {
            getGroup().setStoreContentHref(getAppData().getStoreContentHref());
            getGroup().getMobileContent().setMobileContentId(getAppData().getMobileContentId().toString());
        }

        uploadImageIfNecessary();

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

    @Override
    protected void finish() {
        if (isMobileCamp()) {
            deleteAdGroupMobileContent(campaignId, ulogin);
        }
        super.finish();
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
            imageUploadHelper.withClient(ulogin);
            imageUploadHelper.upload();
            imageUploadHelper.fillBannerByUploadedImage(getGroup().getBanners().get(0));
        }
    }
}
