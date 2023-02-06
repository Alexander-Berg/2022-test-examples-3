package ru.yandex.autotests.direct.cmd.images.text;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.darkside.model.ImageType;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.direct.cmd.util.ImageUtils.ImageFormat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Создание текстовой группы с картинкой")
@Stories(TestFeatures.BannerImages.SAVE_BANNER_WITH_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@RunWith(Parameterized.class)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.TEXT)
@Tag(SmokeTag.YES)
public class SaveTextAdGroupsCreatingImageTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final ImageFormat FORMAT = ImageFormat.JPG;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private ImageType imageType;
    private ImageUploadHelper imageUploadHelper;
    private TextBannersRule bannersRule;

    public SaveTextAdGroupsCreatingImageTest(String authLogin, ImageType imageType, ImageParams imageParams) {
        this.imageType = imageType;

        imageUploadHelper = (ImageUploadHelper) new ImageUploadHelper()
                .withImageParams(imageParams)
                .withClient(CLIENT)
                .withUploadType(ImageUploadHelper.UploadType.FILE);

        bannersRule = new TextBannersRule().
                withImageUploader(imageUploadHelper).
                withMediaType(CampaignTypeEnum.TEXT).
                withUlogin(CLIENT);

        cmdRule = DirectCmdRule.defaultRule().as(authLogin).withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Тип картинки: {1}; Под пользователем: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CLIENT, ImageType.REGULAR, new ImageParams().
                        withFormat(FORMAT).
                        withWidth(450).
                        withHeight(450).
                        withResizeX1(0).
                        withResizeX2(450).
                        withResizeY1(0).
                        withResizeY2(450)},
                {CLIENT, ImageType.WIDE, new ImageParams().
                        withFormat(FORMAT).
                        withWidth(1080).
                        withHeight(607).
                        withResizeX1(0).
                        withResizeX2(1080).
                        withResizeY1(0).
                        withResizeY2(607)}
        });
    }

    @Test
    @Description("Создание текстовой группы с картинкой (bannersMultiSave)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9886")
    public void testCreatingImageAtBannersMultiSave() {
        Group createdGroup = getGroup();
        Banner createdBanner = createdGroup.getBanners().get(0);

        Banner expectedBanner = new Banner();
        expectedBanner.setImage(imageUploadHelper.getResizeResponse().getImage());
        expectedBanner.setImageType(imageType.getName());
        expectedBanner.setImageName(imageUploadHelper.getFileImageName());
        expectedBanner.setImageStatusModerate("New");

        assertThat("в сохраненном баннере присутствует картинка",
                createdBanner,
                beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    private Group getGroup() {
        ShowCampMultiEditRequest request = ShowCampMultiEditRequest.forSingleBanner(
                CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId(), bannersRule.getBannerId());
        ShowCampMultiEditResponse response = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(request);
        return response.getCampaign().getGroups().get(0);
    }
}
