package ru.yandex.mail.tests.sendbernar;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.tests.sendbernar.generated.WriteAttachmentResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static ch.lambdaj.function.matcher.NotNullOrEmptyMatcher.notNullOrEmpty;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.utils.Files.downloadBytes;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.attachOk200;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.emptyFilename400;

@Aqua.Test
@Title("Ручка загрузки аттача write_attachment")
@Description("Проверяем работу ручки write_attachment")
@Stories("attach")
@Issues(@Issue("MAILPG-687"))
@RunWith(DataProviderRunner.class)
public class WriteAttachmentTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.writeAttachment;
    }

    public static byte[] byteArrayMd5(byte[] array) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(array);
        return md.digest();
    }

    @DataProvider
    public static Object[][] cases() throws Exception {
        return new Object[][] {
                { "smallFile",          256,        null },
                { "bigFile",            4194304,    null },
                { "s#m%a^l{l}F|i<l>e",  256,        null },
                { "picture.bmp",        0,          "img/not_rotate.jpg" },
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

        id = URLDecoder.decode(id, "UTF-8");

        assertThat("Id должен быть не пустым", id, notNullOrEmpty());

        String url = SendbernarProperties.properties().messagePartReal();

        byte[] downloaded = downloadBytes(url, id, attachName);

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
