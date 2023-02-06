package ru.yandex.autotests.direct.cmd.images.mobile;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.util.ImageUtils.ImageFormat;

@Aqua.Test
@Description("Валидация типа картинки при изменении картинки в мобильной группе")
@Stories(TestFeatures.BannerImages.SAVE_BANNER_WITH_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.MOBILE)
public class SaveMobileAdGroupsEditingImageNegativeTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final ImageParams IMAGE_PARAMS_REGULAR = new ImageParams().
            withFormat(ImageFormat.JPG).
            withWidth(500).
            withHeight(520).
            withResizeX1(0).
            withResizeX2(500).
            withResizeY1(0).
            withResizeY2(520);
    private static final ImageParams IMAGE_PARAMS_WIDE = new ImageParams().
            withFormat(ImageFormat.JPG).
            withWidth(1080).
            withHeight(607).
            withResizeX1(0).
            withResizeX2(1080).
            withResizeY1(0).
            withResizeY2(607);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private ImageUploadHelper imageUploadHelper = (ImageUploadHelper) new ImageUploadHelper()
            .withImageParams(IMAGE_PARAMS_WIDE)
            .withClient(CLIENT)
            .withUploadType(ImageUploadHelper.UploadType.FILE);

    private CampaignRule textCampaignRule = new CampaignRule()
            .withUlogin(CLIENT)
            .withMediaType(CampaignTypeEnum.TEXT);

    private MobileBannersRule mobileBannersRule = new MobileBannersRule().
            withImageUploader(imageUploadHelper).
            withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(textCampaignRule, mobileBannersRule);

    @Test(expected = HttpClientLiteException.class)
    @Description("Валидация типа картинки при изменении картинки в мобильной группе (cmd = saveMobileAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9873")
    public void testNegativeEditBannerImageAtSaveMobileAdGroups() {
        imageUploadHelper.withImageParams(IMAGE_PARAMS_REGULAR);
        imageUploadHelper.forCampaign(textCampaignRule.getCampaignId());
        imageUploadHelper.uploadAndResize();

        Group createdGroup = mobileBannersRule.getGroup();
        createdGroup.withAdGroupID(mobileBannersRule.getGroupId().toString());
        createdGroup.getBanners().get(0).withBid(mobileBannersRule.getBannerId());

        imageUploadHelper.fillBannerByUploadedImage(createdGroup.getBanners().get(0));

        cmdRule.cmdSteps().groupsSteps().postSaveMobileAdGroups(
                GroupsParameters.forExistingCamp(CLIENT, mobileBannersRule.getCampaignId(), createdGroup));
    }
}
