package ru.yandex.autotests.innerpochta.sendbernar;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.beans.sendbernar.WriteAttachmentResponse;
import ru.yandex.qatools.allure.annotations.*;

import java.net.URLEncoder;
import java.util.Arrays;

import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.*;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.byteArrayMd5;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFileBody;
import static ch.lambdaj.function.matcher.NotNullOrEmptyMatcher.notNullOrEmpty;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

@Aqua.Test
@Title("Ручка загрузки аттача write_attachment")
@Description("Проверяем работу ручки write_attachment")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.ATTACH)
@Issues(@Issue("MAILPG-687"))
@Credentials(loginGroup = "WriteAttachmentSendbernar")
@RunWith(DataProviderRunner.class)
public class WriteAttachmentTest extends BaseSendbernarClass {

    @DataProvider
    public static Object[][] cases() throws Exception {
        return new Object[][] {
                { "smallFile",          256,        null},
                { "bigFile",            4194304,    null },
                { "s#m%a^l{l}F|i<l>e",  256,        null },
                { "picture.bmp",        0,          "img/not_rotate.bmp" },
                { "archive.zip",        0,          "attach/archive.zip" },
                { "document.docx",      0,          "attach/document.docx" },
                { "document.pdf",       0,          "attach/document.pdf" }
        };
    }

    @Test
    @UseDataProvider("cases")
    @Title("Загружаем аттач и проверяем его загрузку")
    public void shouldWriteAttachWithParams(String attachName, int length, String resource) throws Exception {
        byte[] body;
        if (resource != null) {
            body = asByteSource(getResource(resource)).read();
        } else {
            body = randomAlphanumeric(length).getBytes();
        }
        String id = writeAttachment()
                .withFilename(attachName)
                .withReq((req) ->
                        req.setBody(body))
                .post(shouldBe(attachOk200()))
                .as(WriteAttachmentResponse.class)
                .getId();

        assertThat("Id должен быть не пустым", id, notNullOrEmpty());

        String downloadUrl = props().webattachHost() + "/message_part_real/?sid="
                + id + "&name=" + URLEncoder.encode(attachName, "UTF-8");

        byte[] downloaded = downloadFileBody(downloadUrl, hc);

        assertThat("md5 хэши загруженного и скачанного аттачей не совпадают",
                Arrays.equals(byteArrayMd5(body), byteArrayMd5(downloaded)));
    }

    @Test
    @Title("Не загружаем аттач при отсутствии имени файла")
    public void shouldNotWriteAttachmentWithoutFilename() {

        writeAttachment()
                .withReq((req) ->
                        req.setBody("This is test text!")
                           .removeQueryParam("filename"))
                .post(shouldBe(emptyFilename400()));
    }
}
