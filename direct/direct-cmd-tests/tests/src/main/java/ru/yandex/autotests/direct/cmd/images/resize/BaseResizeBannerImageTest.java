package ru.yandex.autotests.direct.cmd.images.resize;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.images.AjaxResizeBannerImageResponse;
import ru.yandex.autotests.direct.cmd.data.images.UploadBannerImageResponse;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

@RunWith(Parameterized.class)
public abstract class BaseResizeBannerImageTest {

    protected static final ImageUtils.ImageFormat FORMAT = ImageUtils.ImageFormat.JPG;
    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public int width;
    @Parameterized.Parameter(1)
    public int height;
    @Parameterized.Parameter(2)
    public int resizeX1;
    @Parameterized.Parameter(3)
    public int resizeX2;
    @Parameterized.Parameter(4)
    public int resizeY1;
    @Parameterized.Parameter(5)
    public int resizeY2;
    protected ImageUploadHelper imageUploadHelper;
    protected CampaignRule campaignRule = new CampaignRule().
            withMediaType(CampaignTypeEnum.TEXT).
            withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(campaignRule);

    @Before
    public void before() {
        imageUploadHelper = (ImageUploadHelper) new ImageUploadHelper().
                forCampaign(campaignRule.getCampaignId()).
                withBannerImageSteps(cmdRule.cmdSteps().bannerImagesSteps()).
                withClient(CLIENT).
                withUploadType(getUploadType()).
                withImageParams(new ImageParams().
                        withFormat(FORMAT).
                        withWidth(width).
                        withHeight(height).
                        withResizeX1(resizeX1).
                        withResizeX2(resizeX2).
                        withResizeY1(resizeY1).
                        withResizeY2(resizeY2));
    }

    protected void test() {
        imageUploadHelper.uploadAndResize();
        testResponse(imageUploadHelper.getUploadResponse(), imageUploadHelper.getResizeResponse());
    }

    protected abstract void testResponse(UploadBannerImageResponse uploadImageResponse,
                                         AjaxResizeBannerImageResponse resizeImageResponse);

    protected abstract ImageUploadHelper.UploadType getUploadType();

    protected String getCurrentImgSizeAsString() {
        int width = resizeX2 - resizeX1;
        int height = resizeY2 - resizeY1;
        return width + "x" + height;
    }
}
