package ru.yandex.autotests.innerpochta.webattach.preview;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.webattach.BaseWebattachTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.getFileFromPath;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 29.01.15
 * Time: 18:12
 */
@Aqua.Test
@Title("Проверка параметра thumb_size для различных форматов")
@Description("Проверяем показ превьюшек для форматов png/gif/анимационный gif/psd/tiff")
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Issues({@Issue("DARIA-42189"), @Issue("DARIA-38978")})
@Credentials(loginGroup = "ImageFormatTest")
@RunWith(Parameterized.class)
public class ImageFormatTest extends BaseWebattachTest {

    private String attachPath;
    private String expectedPath;


    public static File attach;
    public static String mid;

    public ImageFormatTest(String attachPath, String expectedPath) {
        this.attachPath = attachPath;
        this.expectedPath = expectedPath;
    }

    @Parameterized.Parameters(name = "thumb_size = {0}")
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<Object[]>();

        data.add(new Object[]{
                "img/not_rotate.png",
                "img/thumb/100x120.png",
        });


        data.add(new Object[]{
                "img/kotenok.gif",
                "img/thumb/100x120.gif",
        });

        data.add(new Object[]{
                "img/not_rotate.bmp",
                "img/thumb/100x120.bmp",
        });

        data.add(new Object[]{
                "img/not_rotate.gif",
                "img/thumb/100x120(2).gif",
        });

        return data;
    }


    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Description("Отсылаем письмо с заготовленной картинкой.\n" +
            "Для каждого thumb_size, скачиваем превью. \n" +
            "Сверяем с заранее заготовленной картинкой.")
    public void getPreviewForImgFormatTest() throws Exception {
        attach = getFileFromPath(attachPath);
        mid = sendWith(authClient).viaProd()
                .addAttaches(attach)
                .send()
                .waitDeliver()
                .getMid();
        String urlPreview = urlOfAttach(mid, attach.getName())
                + "&thumb=y&thumb_size=100x120";
        File previewImage = getFileFromPath(expectedPath);
        shouldSeeImageFile(urlPreview, previewImage);
    }
}
