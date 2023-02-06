package ru.yandex.autotests.direct.cmd.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ImageAd;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

public class MCBannerRule extends BannersRule {
    public static final int IMAGE_WIDTH = 240;
    public static final int IMAGE_HEIGHT = 400;
    public final static ImageParams DEFAULT_MCBANNER_IMG_PARAMS = getDefaultImageParams();
    private Iterator<ImageAd> campImages;  //изображения загруженные клиентом и соответствующие данному типу кампании
    private ImageAd lastUsedImageAd;       //хранит последнее привязанное к баннеру изображение. даже если оно не загружалось

    private NewImagesUploadHelper imageUploadHelper;
    private CampaignTypeEnum campaignType;

    public static ImageParams getDefaultImageParams() {
        return new ImageParams().
                withFormat(ImageUtils.ImageFormat.JPG).
                withWidth(240).
                withHeight(400);
    }

    public MCBannerRule() {
        super(CmdBeans.COMMON_REQUEST_GROUP_MCBANNER_DEFAULT);
        getGroup().setBanners(new ArrayList<>(Collections.singletonList(BannersFactory.getDefaultMCBanner())));
        withMediaType(CampaignTypeEnum.MCBANNER);
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

    public ImageAd getLastUsedImageAd() {
        return lastUsedImageAd;
    }

    /**
     * @param imageUploadHelper ожидаем, что пользователь передаст imageUploadHelper c проинициалзирированными BannerImagesSteps
     * В противном случае пробуем "подстраховать": используем bannerImagesSteps из directCmdSteps. Но это возможно только после инициализации рулы.
     * @return
     */
    public MCBannerRule withImageUploader(NewImagesUploadHelper imageUploadHelper) {
        this.imageUploadHelper = imageUploadHelper;
        if (getDirectCmdSteps() != null) {
            this.imageUploadHelper.withBannerImageSteps(getDirectCmdSteps().bannerImagesSteps());
        }
        return this;
    }

    private MCBannerRule withDefaultImageUploader(String client) {
        this.withImageUploader((NewImagesUploadHelper) new NewImagesUploadHelper()
                .withImageParams(DEFAULT_MCBANNER_IMG_PARAMS)
                .withClient(client)
                .withUploadType(ImageUploadHelper.UploadType.FILE));
        return this;
    }

    /**
     * Проверяем инициализацию загрузчика изображений для mcbanner'a.
     * Если не проинициализирован, пробуем создать с параметрами по-умолчанию.
     *
     * @return созданый загрузчик (NewImagesUploadHelper), либо null
     */
    private NewImagesUploadHelper getOrInitDefaultImageUploader() {
        if (imageUploadHelper == null && ulogin != null) {
            withDefaultImageUploader(ulogin);
            imageUploadHelper.withBannerImageSteps(getDirectCmdSteps().bannerImagesSteps());
        }
        return this.imageUploadHelper;

    }

    @Override
    public void createGroup() {
        Group group = getGroup()
                .withRetargetings(null)
                .withShownBids(null);
        group.getKeywords()
                .stream()
                .forEach(k ->
                        k.withFixation(null)
                                .withGuarantee(null)
                                .withPremium(null)
                                .withVerdicts(null)
                );

        uploadImageIfNecessary();

        GroupsParameters groupRequest = GroupsParameters.forNewCamp(ulogin, campaignId, group);
        saveGroup(groupRequest);
    }

    @Override
    public void saveGroup(GroupsParameters request) {
        request.setAutoPrice("1");
        getDirectCmdSteps().groupsSteps().postSaveMcbannerAdGroups(request);
    }

    @Override
    public Group getCurrentGroup() {
        return getDirectCmdSteps().campaignSteps().getShowCampMultiEdit(ulogin, campaignId, groupId, bannerId)
                .getCampaign().getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"));
    }

    /**
     * Меняем на баннере изображение
     * Используем ранее загруженные, если больше нет - загружаем новое.
     * @param banner
     */
    public void changeBannerImage(Banner banner) {
        if (campImages.hasNext()) {
            lastUsedImageAd = campImages.next();
            banner.withImageAd(lastUsedImageAd);
        } else {
            uploadNewImageForBanner(banner);
        }
    }

    private void uploadNewImageForBanner(Banner banner) {
        if (getOrInitDefaultImageUploader() != null) {
            imageUploadHelper.withClient(ulogin);
            imageUploadHelper.upload();
            imageUploadHelper.fillBannerByUploadedImage(banner);
        }
    }

    private void uploadImageIfNecessary() {
        List<ImageAd> images = getDirectCmdSteps().groupsSteps()
                .getAddMcBannerGroups(campaignId, ulogin).getCampaign().getImageAds();
        campImages = images.stream().filter( i -> i.getHeight() == IMAGE_HEIGHT && i.getWidth() == IMAGE_WIDTH).collect(
                Collectors.toList()).iterator();
        if (campImages.hasNext()) { //если у пользователя уже загружены изображения, используем их.
            lastUsedImageAd = campImages.next();
            getGroup().getBanners().get(0).withImageAd(lastUsedImageAd);
        } else {
            uploadNewImageForBanner(getGroup().getBanners().get(0));
        }
    }
}

