package ru.yandex.autotests.direct.cmd.images.mobile;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent.EditAdGroupsMobileContentRequest;
import ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent.EditAdGroupsMobileContentResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.ImageType;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.util.ImageUtils.ImageFormat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Добавление картинки в мобильную группу")
@Stories(TestFeatures.BannerImages.SAVE_BANNER_WITH_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.MOBILE)
@Tag(SmokeTag.YES)
@RunWith(Parameterized.class)
public class SaveMobileAdGroupsEditingImageTest {

    public static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;

    private ImageUploadHelper imageUploadHelper = (ImageUploadHelper) new ImageUploadHelper()
            .withImageParams(new ImageParams().
                    withFormat(ImageFormat.JPG).
                    withWidth(1080).
                    withHeight(607).
                    withResizeX1(0).
                    withResizeX2(1080).
                    withResizeY1(0).
                    withResizeY2(607))
            .withClient(CLIENT)
            .withUploadType(ImageUploadHelper.UploadType.FILE);
    private MobileBannersRule bannersRule = new MobileBannersRule().
            withUlogin(CLIENT);

    public SaveMobileAdGroupsEditingImageTest(String authLogin) {
        cmdRule = DirectCmdRule.defaultRule().as(authLogin).withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Под пользователем: {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {CLIENT},
                {Logins.SUPER}
        });
    }

    @Before
    public void before() {
        imageUploadHelper.withBannerImageSteps(cmdRule.cmdSteps().bannerImagesSteps());
        imageUploadHelper.forCampaign(bannersRule.getCampaignId());
    }

    @Test
    @Description("Добавление картинки в мобильную группу (cmd = saveMobileAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9874")
    public void testSaveBannerImageAtSaveMobileAdGroups() {
        addImageToCreatedMobileBanner();

        Banner editedBanner = getEditedGroup().getBanners().get(0);

        Banner expectedBanner = new Banner();
        expectedBanner.setImage(imageUploadHelper.getResizeResponse().getImage());
        expectedBanner.setImageType(ImageType.WIDE.getName());
        expectedBanner.setImageName(imageUploadHelper.getFileImageName());
        expectedBanner.setImageStatusModerate("New");

        assertThat("в измененном мобильном баннере присутствует картинка",
                editedBanner,
                beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    private void addImageToCreatedMobileBanner() {
        imageUploadHelper.uploadAndResize();
        Group group = getUpdateGroupBean();
        cmdRule.cmdSteps().groupsSteps().postSaveMobileAdGroups(
                GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group));
    }

    private Group getEditedGroup() {
        EditAdGroupsMobileContentRequest request = EditAdGroupsMobileContentRequest.
                forSingleBanner(CLIENT, bannersRule.getCampaignId(),
                        bannersRule.getGroupId(), bannersRule.getBannerId());

        EditAdGroupsMobileContentResponse response =
                cmdRule.cmdSteps().groupsSteps().getEditAdGroupsMobileContent(request);
        return response.getCampaign().getGroups().get(0);
    }

    private Group getUpdateGroupBean() {
        Group group = bannersRule.getGroup();
        group.withAdGroupID(bannersRule.getGroupId().toString());
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        imageUploadHelper.fillBannerByUploadedImage(group.getBanners().get(0));
        return group;
    }
}
