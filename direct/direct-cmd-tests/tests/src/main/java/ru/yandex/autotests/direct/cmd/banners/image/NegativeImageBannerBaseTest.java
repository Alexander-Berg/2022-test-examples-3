package ru.yandex.autotests.direct.cmd.banners.image;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ImageAd;
import ru.yandex.autotests.direct.cmd.data.images.UploadImageResponse;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@RunWith(Parameterized.class)
public abstract class NegativeImageBannerBaseTest {
    protected static final String CLIENT = "at-direct-image-banner71";
    protected static final String ANOTHER_CLIENT = "at-direct-image-banner72";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected CampaignRule campaignRule;
    protected NewImagesUploadHelper imagesUploadHelper;
    protected Long campaignId;
    protected Banner newBanner;
    protected CampaignTypeEnum campaignType;

    @Parameterized.Parameters(name = "Сохранение невалидных графических объявлений. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        campaignId = campaignRule.getCampaignId();
        newBanner = BannersFactory.getDefaultImageBanner(campaignType);
    }

    @Description("Нельзя сохранить/изменить картинку без picture_hash")
    public void imageBannerWithoutPictureHash() {
        newBanner.withImageAd(new ImageAd());
        ErrorResponse response = saveGroup();
        assertThat("Получили ошибку", response.getError(), equalTo(CommonErrorsResource.WRONG_INPUT_DATA.toString()));
    }

    @Description("Нельзя сохранить/изменить картинку с несуществующим picture_hash")
    public void imageBannerWithInvalidPictureHash() {
        newBanner.withImageAd(new ImageAd().withHash(RandomUtils.getString(10)));
        ErrorResponse response = saveGroup();
        assertThat("Получили ошибку", response.getError(), equalTo(UploadImageResponse.ERROR_IMG_OR_PERMISSION_DENIED));
    }

    @Description("Нельзя сохранить/изменить картинку без image_ad")
    public void imageBannerWithoutImageAd() {
        newBanner.withImageAd(null);
        ErrorResponse response = saveGroup();
        assertThat("Получили ошибку", response.getError(), equalTo(CommonErrorsResource.WRONG_INPUT_DATA.toString()));
    }

    @Description("Нельзя сохранить/изменить картинку без типа баннера (picture_hash заполнен)")
    public void imageBannerWithoutAdType() {
        newBanner.withAdType(null);
        imagesUploadHelper.fillBannerByUploadedImage(newBanner);
        ErrorResponse response = saveGroup();
        checkError(response.getError());
    }

    @Description("Нельзя сохранить/изменить картинку, загруженную другим пользователем")
    public void imageBannerWithAnotherLoginImageHash() {
        imagesUploadHelper.withClient(ANOTHER_CLIENT)
                .withBannerImageSteps(cmdRule.cmdSteps().bannerImagesSteps()
                );
        imagesUploadHelper.upload();
        imagesUploadHelper.fillBannerByUploadedImage(newBanner);
        ErrorResponse response = saveGroup();
        assertThat("Получили ошибку", response.getError(), equalTo(UploadImageResponse.ERROR_IMG_OR_PERMISSION_DENIED));
    }

    protected ErrorResponse saveGroup() {
        switch (campaignType) {
            case TEXT:
                return cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(getTextGroupParameters());
            case MOBILE:
                return cmdRule.cmdSteps().groupsSteps().postSaveMobileAdGroupsInvalidData(getMobileGroupParameters());
            default:
                throw new BackEndClientException("Не указан тип кампании");
        }

    }

    protected abstract GroupsParameters getMobileGroupParameters();

    protected abstract GroupsParameters getTextGroupParameters();

    public void checkError(String error) {
        switch (campaignType) {
            case TEXT:
                assertThat("Получили ошибку", error, containsString(UploadImageResponse.ERROR_IMG_INVALID_TITLE));
                assertThat("Получили ошибку", error, containsString(UploadImageResponse.ERROR_IMG_INVALID_TEXT));
                break;
            case MOBILE:
                assertThat("Получили ошибку", error, equalTo(UploadImageResponse.ERROR_IMG_INVALID_AD_TYPE));
                break;
            default:
                throw new BackEndClientException("Не указан тип кампании");
        }
    }
}
