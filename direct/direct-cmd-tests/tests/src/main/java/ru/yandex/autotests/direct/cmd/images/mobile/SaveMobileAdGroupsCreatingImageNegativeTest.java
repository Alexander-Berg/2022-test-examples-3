package ru.yandex.autotests.direct.cmd.images.mobile;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.util.ImageUtils.ImageFormat;

@Aqua.Test
@Description("Валидация типа картинки при создании мобильной группы с картинкой")
@Stories(TestFeatures.BannerImages.SAVE_BANNER_WITH_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.MOBILE)
public class SaveMobileAdGroupsCreatingImageNegativeTest {

    private static final String GROUP_TEMPLATE = CmdBeans.COMMON_REQUEST_GROUP_MOBILE_DEFAULT2;
    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final ImageParams IMAGE_PARAMS_REGULAR = new ImageParams().
            withFormat(ImageFormat.JPG).
            withWidth(500).
            withHeight(520).
            withResizeX1(0).
            withResizeX2(500).
            withResizeY1(0).
            withResizeY2(520);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();


    private ImageUploadHelper imageUploadHelper;

    private CampaignRule textCampaignRule = new CampaignRule().withMediaType(CampaignTypeEnum.TEXT);
    private CampaignRule mobileCampaignRule = new CampaignRule().withMediaType(CampaignTypeEnum.MOBILE);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.
            defaultRule().
            as(CLIENT).
            withRules(textCampaignRule, mobileCampaignRule);

    @Before
    public void before() {
        imageUploadHelper = (ImageUploadHelper) new ImageUploadHelper().
                forCampaign(textCampaignRule.getCampaignId()).
                withBannerImageSteps(cmdRule.cmdSteps().bannerImagesSteps()).
                withImageParams(IMAGE_PARAMS_REGULAR).
                withClient(CLIENT).
                withUploadType(ImageUploadHelper.UploadType.FILE);
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Валидация типа картинки при создании мобильной группы с картинкой (cmd = saveMobileAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9871")
    public void testNegativeCreateBannerImageAtSaveMobileAdGroups() {
        imageUploadHelper.uploadAndResize();

        Group group = getUpdateGroupBean();

        GroupsParameters groupRequest = GroupsParameters.forNewCamp(CLIENT, mobileCampaignRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveMobileAdGroups(groupRequest);
    }

    private Group getUpdateGroupBean() {
        Long campaignId = mobileCampaignRule.getCampaignId();
        Group group = BeanLoadHelper.loadCmdBean(GROUP_TEMPLATE, Group.class);
        group.setCampaignID(campaignId.toString());
        group.getBanners().stream().forEach(b -> b.withCid(campaignId));
        imageUploadHelper.fillBannerByUploadedImage(group.getBanners().get(0));
        return group;
    }
}
