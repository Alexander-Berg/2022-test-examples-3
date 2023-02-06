package ru.yandex.autotests.innerpochta.webattach.preview;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 29.01.15
 * Time: 15:35
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.webattach.BaseWebattachTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.mailsend.MailSendWithPartsJson;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.getFileFromPath;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("Проверка работы webattach-ей с параметром thumb_size")
@Description("Проверяем показ превьюшек с различными параметрами thumb_size")
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Issue("DARIA-42189")
@Credentials(loginGroup = PreviewSizeTest.LOGIN_GROUP)
@RunWith(Parameterized.class)
public class PreviewSizeTest extends BaseWebattachTest {

    public static final String LOGIN_GROUP = "AttachThumbSize";

    private String size;
    private String path;
    private String resizePath;

    public static final String ATTACH = "img/imgrotated/not_rotate.jpg";

    public static final String RESIZE_150x200 = "img/resize/150x200.jpg";
    public static final String RESIZE_1x1 = "img/resize/1x1.jpg";
    public static final String RESIZE_10x8 = "img/resize/10x8.jpg";
    public static final String RESIZE_10x13 = "img/resize/10x13.jpg";

    public static final String DEFAULT_IMG = "img/thumb/150x150.jpeg";
    public static final String IMG_200x200 = "img/thumb/200x200.jpeg";
    public static final String IMG_1x200 = "img/thumb/1x200.jpeg";
    public static final String IMG_200x1 = "img/thumb/200x1.jpeg";
    public static final String IMG_10x120000 = "img/thumb/10x120000.jpeg";
    public static final String IMG_120000x10 = "img/thumb/120000x10.jpeg";
    public static final String IMG_120000x10000 = "img/thumb/120000x10000.jpeg";

    public static File attach;
    public static String attacheName;
    public static String mid;

    @ClassRule
    public static CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    public PreviewSizeTest(String size, String path, String resizePath) {
        this.size = size;
        this.path = path;
        this.resizePath = resizePath;
    }

    @Parameterized.Parameters(name = "thumb_size = {0}")
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<Object[]>();

        data.add(new Object[]{
                "",
                DEFAULT_IMG,
                ATTACH
        });

        data.add(new Object[]{
                "200x200",
                IMG_200x200,
                RESIZE_150x200
        });

        data.add(new Object[]{
                "200x1",
                IMG_200x1,
                RESIZE_1x1
        });

        data.add(new Object[]{
                "1x200",
                IMG_1x200,
                RESIZE_1x1
        });

        data.add(new Object[]{
                "100000x120000",
                IMG_120000x10000,
                ATTACH
        });

        data.add(new Object[]{
                "10x120000",
                IMG_10x120000,
                RESIZE_10x8
        });

        data.add(new Object[]{
                "120000x10",
                IMG_120000x10,
                RESIZE_10x13
        });

        return data;
    }

    @BeforeClass
    public static void prepare() throws Exception {
        attach = downloadFile(MailSendWithPartsJson.IMAGE_URl_JPEG, Util.getRandomString() + ".jpg", authClient.authHC());
        attacheName = attach.getName();
        mid = sendWith(authClient).viaProd()
                .addAttaches(attach)
                .send()
                .waitDeliver()
                .getMid();
    }

    @Test
    @Description("Отсылаем письмо с заготовленной картинкой.\n" +
            "Для каждого thumb_size, скачиваем превью. \n" +
            "Сверяем с заранее заготовленной картинкой.")
    public void getPreviewThumbSizeTest() throws Exception {
        String urlPreview = urlOfAttach(mid, attacheName)
                + "&thumb=y&thumb_size=" + size;
        File previewImage = getFileFromPath(path);
        shouldSeeImageFile(urlPreview, previewImage);
    }

    @Test
    @Issue("DARIA-44172")
    @Description("Отсылаем письмо с заготовленной картинкой.\n" +
            "Для каждого thumb_size, скачиваем превью. \n" +
            "Сверяем с заранее заготовленной картинкой.")
    public void getPreviewMaxSizeTest() throws Exception {
        String urlPreview = urlOfAttach(mid, attacheName)
                + "&resize=y&max_size=" + size;
        File previewImage = getFileFromPath(resizePath);
        shouldSeeImageFile(urlPreview, previewImage);
    }
}
