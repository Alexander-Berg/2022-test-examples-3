package ru.yandex.autotests.direct.cmd.images.upload;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.images.UploadBannerImageResponse;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

@RunWith(Parameterized.class)
public abstract class BaseUploadBannerImageTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public ImageUtils.ImageFormat format;
    @Parameterized.Parameter(1)
    public int width;
    @Parameterized.Parameter(2)
    public int height;
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
                        withFormat(format).
                        withWidth(width).
                        withHeight(height));
    }

    protected void test() {
        imageUploadHelper.upload();
        testResponse(imageUploadHelper.getUploadResponse());
    }

    protected abstract ImageUploadHelper.UploadType getUploadType();

    protected abstract void testResponse(UploadBannerImageResponse uploadImageResponse);
}
