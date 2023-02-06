package ru.yandex.autotests.innerpochta.wmi.attach;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.filter.VDirectCut;
import ru.yandex.autotests.innerpochta.wmi.core.obj.UploadAttachmentXmlObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.UploadAttachmentXml;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.hamcrest.Matchers.hasXPath;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.FileCompareMatcher.hasSameMd5As;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 26.01.15
 * Time: 20:21
 */
@Aqua.Test
@Title("Загрузка файлов в аттач в имени которых есть спец. символы")
@Description("Проверяем загрузку аттачей ручками: " +
        "write_attachment и upload_attachment.xml с различными именами файлов.\n " +
        "Проверяем, что в выдачи заэнкоженное имя и файл по ссылке можно скачать")
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Credentials(loginGroup = "AttachNameTest")
@RunWith(Parameterized.class)
@Issue("DARIA-43699")
public class AttachNameTest extends BaseTest {

    @Rule
    public CleanMessagesRule clean = with(authClient).all().allfolders();

    private String fileName;
    private File bmp;

    @Parameterized.Parameters(name = "NAME-{0}")
    public static Collection<Object[]> data() throws Exception {
        List<Object[]> attachName = new ArrayList<>();

        attachName.add(new Object[]{"Arşiv.bmp"});
        attachName.add(new Object[]{"####.bmp"});
        attachName.add(new Object[]{"!!!!.bmp"});
        attachName.add(new Object[]{"????.bmp"});
        attachName.add(new Object[]{"@@@@.bmp"});
        attachName.add(new Object[]{"$$$$.bmp"});
        attachName.add(new Object[]{"%%%%.bmp"});
        attachName.add(new Object[]{"^^^^.bmp"});
        attachName.add(new Object[]{"&&&&.bmp"});
        attachName.add(new Object[]{"****.bmp"});
        attachName.add(new Object[]{"((((.bmp"});
        attachName.add(new Object[]{")))).bmp"});
        attachName.add(new Object[]{"[[[[.bmp"});
        attachName.add(new Object[]{"]]]].bmp"});
        attachName.add(new Object[]{"{{{{.bmp"});
        attachName.add(new Object[]{"}}}}.bmp"});
        attachName.add(new Object[]{"----.bmp"});
        attachName.add(new Object[]{"++++.bmp"});
        attachName.add(new Object[]{"====.bmp"});
        attachName.add(new Object[]{"____.bmp"});
        attachName.add(new Object[]{"~~~~.bmp"});
        attachName.add(new Object[]{"::::.bmp"});
        attachName.add(new Object[]{",,,.bmp"});
        attachName.add(new Object[]{";;;;;.bmp"});
        attachName.add(new Object[]{"||||.bmp"});
        attachName.add(new Object[]{".....bmp"});
        attachName.add(new Object[]{"±±±±±.bmp"});
        attachName.add(new Object[]{">>>>.bmp"});
        attachName.add(new Object[]{"<<<<.bmp"});
        attachName.add(new Object[]{".bmp"});
        attachName.add(new Object[]{"The%20attachment.bmp"});
        attachName.add(new Object[]{"русский.bmp"});

        return attachName;
    }

    public AttachNameTest(String fileName) {
        this.fileName = fileName;
    }


    @Before
    public void prepareFile() throws IOException {
        bmp = File.createTempFile(fileName, null);
        asByteSink(bmp).write(asByteSource(getResource("img/1x1.bmp")).read());
    }

    @Test
    @Title("Ручка attach с всевозможными именами файлов")
    @Issue("DARIA-43699")
    @Description("Проверяем ручку write_attachment с всевозможными именами файлов. \n" +
            "Проверяем, что корректно энкодятся")
    public void writeAttachementWithEncodedName() throws Exception {
        UploadAttachmentXml resp = jsx(UploadAttachmentXml.class)
                .filters(new VDirectCut())
                .params(UploadAttachmentXmlObj.getObjToUploadFile(bmp))
                .post().via(hc);
        shouldSeeAttach(resp);
    }

    @Test
    @Title("Ручка attach с всевозможными именами файлов")
    @Issue("DARIA-43699")
    @Description("Проверяем ручку upload_attachment.xml с всевозможными именами файлов.\n" +
            "Проверяем, что корректно энкодятся")
    public void uploadAttachmentXmlWithEncodedName() throws Exception {
        UploadAttachmentXml resp = api(UploadAttachmentXml.class)
                .filters(new VDirectCut())
                .params(UploadAttachmentXmlObj.getObjToUploadFile(bmp))
                .post().via(hc).withDebugPrint();
        shouldSeeAttach(resp);
    }

    public void shouldSeeAttach(UploadAttachmentXml resp) throws IOException {
        Assert.assertThat("[DARIA-16859]", resp.toDocument(), hasXPath("//viewLargeUrl"));
        Assert.assertThat("[DARIA-16859]", resp.toDocument(), hasXPath("//previewUrl"));
        String url = resp.getDownloadUrl();

        File deliveredAttach = downloadFile(url, bmp.getName(), hc);

        Assert.assertThat("md5 хэши загруженного и скачанного аттачей не совпадают",
                deliveredAttach, hasSameMd5As(bmp));
    }
}
