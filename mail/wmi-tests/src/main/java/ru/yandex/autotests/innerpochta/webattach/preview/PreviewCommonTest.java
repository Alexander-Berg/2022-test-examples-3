package ru.yandex.autotests.innerpochta.webattach.preview;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

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
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.genFile;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.getFileFromPath;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 28.01.15
 * Time: 18:45
 * Было:
 * thumb_size=300
 * Стало
 * thumb_size=200[x300][+10][+50]
 * Примеры:
 * thumb_size=200 - как и раньше, квадратная картинка 200x200
 * thumb_size=200x300 - превьюха 200х300, перед ресайзом картинка обрезается в соответствии с заданными пропорциями.
 * При обрезании центрируется.
 * thumb_size=200+30 - превьюха 200x200, при обрезании используются переданные оффсеты
 */
@Aqua.Test
@Title("Проверка параметра thumb и resize для webattach-ей")
@Description("Проверяем превьюхи с параметром thumb и resize")
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Issue("DARIA-42189")
@Credentials(loginGroup = "AttachThumbTest")
public class PreviewCommonTest extends BaseWebattachTest {

    private static final String DEFAULT_IMG = "img/thumb/150x150.jpeg";
    private static final String IMG = "img/imgrotated/not_rotate.jpg";

    private static File attach;
    private static String attacheName;

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();


    @BeforeClass
    public static void prepare() throws IOException {
        attach = downloadFile(MailSendWithPartsJson.IMAGE_URl_JPEG, Util.getRandomString() + ".jpg", authClient.authHC());
        attacheName = attach.getName();
    }

    @Test
    @Description("Скачиваем картинку из webattach-ей\n" +
            "Должны увидеть квадратную картинку")
    public void getPreviewWithThumbShouldSeeSquareImage() throws Exception {
        String mid = sendWith(authClient).viaProd()
                .addAttaches(attach)
                .send()
                .waitDeliver()
                .getMid();
        String urlPreview = urlOfAttach(mid, attacheName) + "&thumb=y";

        File expectedPreview = getFileFromPath(DEFAULT_IMG);
        shouldSeeImageFile(urlPreview, expectedPreview);
    }

    @Test
    @Description("Скачиваем картинку из webattach-ей\n" +
            "Должны увидеть квадратную картинку")
    public void getPreviewWithoutThumbShouldSeeSquareImage() throws Exception {
        String mid = sendWith(authClient).viaProd()
                .addAttaches(attach)
                .send()
                .waitDeliver()
                .getMid();
        String urlPreview = urlOfAttach(mid, attacheName);

        File expectedPreview = getFileFromPath(IMG);
        shouldSeeImageFile(urlPreview, expectedPreview);
    }

    @Test
    @Description("Скачиваем файл (НЕ картинку) из webattach-ей, с параметрами thumb_size=200x200\n")
    public void getPreviewNotImageFileWithThumb() throws Exception {
        File randomFile = genFile(1024 * 1024);
        String mid = sendWith(authClient).viaProd()
                .addAttaches(randomFile)
                .send()
                .waitDeliver()
                .getMid();
        String url = urlOfAttach(mid, randomFile.getName())
                + "&thumb=y&thumb_size=200x200";

        shouldSeeFile(url, randomFile);
    }

    @Test
    @Issue("DARIA-44172")
    @Description("Скачиваем файл (НЕ картинку) из webattach-ей, с параметрами max_size=200x200\n")
    public void getPreviewNotImageFileWithTResize() throws Exception {
        File randomFile = genFile(1024 * 1024);
        String mid = sendWith(authClient).viaProd()
                .addAttaches(randomFile)
                .send()
                .waitDeliver()
                .getMid();
        String url = urlOfAttach(mid, randomFile.getName())
                + "&resize=y&max_size=200x200";

        shouldSeeFile(url, randomFile);
    }
}
