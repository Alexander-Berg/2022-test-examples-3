package ru.yandex.autotests.innerpochta.webattach.preview;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.BeforeClass;
import org.junit.Test;

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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.getFileFromPath;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.02.15
 * Time: 17:08
 */
@Aqua.Test
@Title("Проверка параметра thumb_size, max_size с exif_rotate")
@Description("Проверяем, что у картинки правильная ориентация, при запросе с превьюшках")
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Issues({@Issue("DARIA-44402")})
@Credentials(loginGroup = ExifRotateTest.LOGIN_GROUP)
public class ExifRotateTest extends BaseWebattachTest {

    public static final String LOGIN_GROUP = "ExifRotateTest";

    private static final String ATTACH = "img/exif_rotate.jpg";

    private static String attacheName;
    private static String mid;

    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @BeforeClass
    public static void prepare() throws Exception {
        File attach = getFileFromPath(ATTACH, ".jpg");
        attacheName = attach.getName();
        mid = sendWith(authClient).viaProd()
                .addAttaches(attach)
                .send()
                .waitDeliver()
                .getMid();
    }

    @Test
    @Title("Параметр exif_rotate")
    @Description("Проверяем, что exif_rotate работает")
    public void simpleTestExifRotate() throws Exception {
        String urlPreview = urlOfAttach(mid, attacheName) + "&exif_rotate=y";
        File preview = downloadFile(urlPreview, attacheName, authClient.authHC());
        BufferedImage image = ImageIO.read(preview);
        assertThat("Неверная высота (height) у картинки", image.getHeight(), equalTo(2592));
        assertThat("Неверная ширина (width) у картинки c exif=y", image.getWidth(), equalTo(1944));
    }

    @Test
    @Title("Параметр exif_rotate с resize")
    @Description("Проверяем, что наличие max_size не ломает exif_rotate")
    public void testMaxSize() throws Exception {
        String urlPreview = urlOfAttach(mid, attacheName)
                + "&exif_rotate=y&resize=y&max_size=100x200";
        File preview = downloadFile(urlPreview, attacheName, authClient.authHC());
        BufferedImage image = ImageIO.read(preview);
        assertThat("Неверная высота (height) у картинки", image.getHeight(), equalTo(133));
        assertThat("Неверная ширина (width) у картинки", image.getWidth(), equalTo(100));
    }

    @Test
    @Title("Параметр exif_rotate с thumb")
    @Description("Проверяем, что наличие thumb не ломает exif_rotate")
    public void testThumbSize() throws Exception {
        String urlPreview = urlOfAttach(mid, attacheName)
                + "&exif_rotate=y&thumb=y&thumb_size=100x200";
        File preview = downloadFile(urlPreview, attacheName, authClient.authHC());
        BufferedImage image = ImageIO.read(preview);
        assertThat("Неверная высота (height) у картинки", image.getHeight(), equalTo(200));
        assertThat("Неверная ширина (width) у картинки", image.getWidth(), equalTo(100));
    }
}
