package ru.yandex.autotests.innerpochta.api;

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
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.FileCompareMatcher.hasSameMd5As;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;

@Aqua.Test
@Title("[API] Загрузка аттача")
@Description("Скачиваем, проверяем верность загрузки")
@Features(MyFeatures.API_WMI)
@Stories(MyStories.ATTACH)
@Credentials(loginGroup = "Attaupload")
public class UploadAttachXmlTest extends BaseTest {

    @Test
    public void testSendAttachWithSpecifiedName() throws Exception {
        String attachName = "sometestAtt.txt";
        logger.warn("Attach name = " + attachName);
        File attach = Util.generateRandomShortFile(attachName, 64);
        UploadAttachmentXml resp = api(UploadAttachmentXml.class)
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

        UploadAttachmentXml resp = api(UploadAttachmentXml.class)
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

        UploadAttachmentXml resp = api(UploadAttachmentXml.class)
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

}