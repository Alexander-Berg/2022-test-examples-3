package ru.yandex.autotests.innerpochta.wmi.attach;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.filter.VDirectCut;
import ru.yandex.autotests.innerpochta.wmi.core.obj.UploadAttachmentXmlObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.UploadAttachmentXml;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.File;

import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.hamcrest.Matchers.hasXPath;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.FileCompareMatcher.hasSameMd5As;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 26.01.15
 * Time: 20:14
 */
@Aqua.Test
@Title("Загрузка аттача write_attachment")
@Description("Скачиваем, проверяем верность загрузки")
@Features(MyFeatures.WMI)
@Stories(MyStories.ATTACH)
@Credentials(loginGroup = "WriteAttachmentTest")
public class WriteAttachmentTest extends BaseTest {

    @Test
    public void testSendAttachWithSpecifiedName() throws Exception {
        String attachName = "sometestAtt.txt";
        logger.warn("Attach name = " + attachName);
        File attach = Util.generateRandomShortFile(attachName, 64);
        UploadAttachmentXml resp = jsx(UploadAttachmentXml.class)
                .params(UploadAttachmentXmlObj.getObjToUploadFile(attach))
                .post().via(hc);

        String url = resp.getDownloadUrl();

        File deliveredAttach = downloadFile(url, attach.getName(), hc);

        assertThat("md5 хэши загруженного и скачанного аттачей не совпадают",
                deliveredAttach, hasSameMd5As(attach));

    }

    @Test
    @Issue("DARIA-16859")
    public void testSendBMPImg() throws Exception {
        logger.warn("[DARIA-16859]");
        File bmp = File.createTempFile("1x1.bmp", null);
        asByteSink(bmp).write(asByteSource(getResource("img/1x1.bmp")).read());

        UploadAttachmentXml resp = jsx(UploadAttachmentXml.class)
                .filters(new VDirectCut())
                .params(UploadAttachmentXmlObj.getObjToUploadFile(bmp))
                .post().via(hc).withDebugPrint();

        assertThat("[DARIA-16859]", resp.toDocument(), hasXPath("//viewLargeUrl"));
        assertThat("[DARIA-16859]", resp.toDocument(), hasXPath("//previewUrl"));
        String url = resp.getDownloadUrl();

        File deliveredAttach = downloadFile(url, bmp.getName(), hc);

        assertThat("md5 хэши загруженного и скачанного аттачей не совпадают",
                deliveredAttach, hasSameMd5As(bmp));
    }

    @Test
    @Issue("DARIA-43699")
    @Description("Пытаемся зааплодить аттач с % в имене файла")
    public void testSendEncodeAttachTest() throws Exception {
        File bmp = File.createTempFile("fi%e.bmp", null);
        asByteSink(bmp).write(asByteSource(getResource("img/1x1.bmp")).read());

        UploadAttachmentXml resp = jsx(UploadAttachmentXml.class)
                .filters(new VDirectCut())
                .params(UploadAttachmentXmlObj.getObjToUploadFile(bmp))
                .post().via(hc);

        String url = resp.getDownloadUrl();

        File deliveredAttach = downloadFile(url, bmp.getName(), hc);

        assertThat("md5 хэши загруженного и скачанного аттачей не совпадают",
                deliveredAttach, hasSameMd5As(bmp));
    }
}