package ru.yandex.autotests.direct.cmd.images.upload;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.commons.banner.BannerType;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.images.AbstractImageUploadHelper;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;

@RunWith(Parameterized.class)
public abstract class BaseUploadImageTest {

    protected static final String CLIENT = "at-direct-image-banner72";
    protected BannerType bannerType = null; //в cmd_uploadImage по-умолчанию banner_type = image_ad
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT);
    @Parameterized.Parameter(0)
    public ImageUtils.ImageFormat format;
    @Parameterized.Parameter(1)
    public int width;
    @Parameterized.Parameter(2)
    public int height;
    @Parameterized.Parameter(3)
    public AbstractImageUploadHelper.UploadType uploadType;
    protected DirectCmdSteps directCmdSteps;
    protected NewImagesUploadHelper imageUploadHelper;

    @Before
    public void before() {
        directCmdSteps = cmdRule.cmdSteps();
        imageUploadHelper = (NewImagesUploadHelper) new NewImagesUploadHelper().
                withBannerImageSteps(directCmdSteps.bannerImagesSteps()).
                withClient(CLIENT).
                withUploadType(uploadType).
                withImageParams(new ImageParams().
                        withFormat(format).
                        withWidth(width).
                        withHeight(height));
        imageUploadHelper.withBannerType(bannerType);
        imageUploadHelper.upload();
    }

}
