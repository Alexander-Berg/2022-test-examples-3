package ru.yandex.autotests.innerpochta.sendbernar;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SaveDraftResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.beans.sendbernar.WriteAttachmentResponse;


import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.*;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

@Aqua.Test
@Title("Подсчет хешей аттачей")
@Description("Проверяем работу подсчета хеша аттачей")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.ATTACH)
@Issue("MAILPG-2398")
@Credentials(loginGroup = "HashAttachesSendbernarTest")
@RunWith(DataProviderRunner.class)
public class HashAttachesTest extends BaseSendbernarClass {

    public static final int ATTACH_SIZE = 256;

    @Test
    @Title("Загружаем два раза один и тот же аттач, проверяем, что хеши совпали")
    public void WriteEqualAttaches() {
        byte[] body;
        body = randomAlphanumeric(ATTACH_SIZE).getBytes();
        String name = "file name";

        String hash1 = WriteAttachAndGetHash(name, body);
        String hash2 = WriteAttachAndGetHash(name, body);

        assertThat("Хеши одинаковых аттачей должны совпадать", hash1, equalTo(hash2));
    }

    @Test
    @Title("Загружаем два аттача с одинаковым именем и разными телами, проверяем, что хеши не совпали")
    public void WriteAttachesWithDifferentBodies() {
        byte[] body1;
        body1 = randomAlphanumeric(ATTACH_SIZE).getBytes();
        byte[] body2;
        body2 = randomAlphanumeric(ATTACH_SIZE).getBytes();
        String name = "file name";

        String hash1 = WriteAttachAndGetHash(name, body1);
        String hash2 = WriteAttachAndGetHash(name, body2);

        assertThat("Хеши аттачей с разными телами должны различаться", hash1, not(equalTo(hash2)));
    }

    @Test
    @Title("Загружаем два аттача с одинаковым телом и разными именами, проверяем, что хеши не совпали")
    public void WriteAttachesWithDifferentNames() {
        byte[] body;
        body = randomAlphanumeric(ATTACH_SIZE).getBytes();
        String name1 = "file name";
        String name2 = "another name";

        String hash1 = WriteAttachAndGetHash(name1, body);
        String hash2 = WriteAttachAndGetHash(name2, body);

        assertThat("Хеши аттачей с разными именами должны различаться", hash1, not(equalTo(hash2)));
    }

    @Test
    @Title("Загружаем аттач через write_attachment, вызываем save_draft с загруженым аттачем." +
            "Проверяем, что хеши совадают")
    public void SaveDraftAndWriteAttachmentShouldReturnEqualHashes() {
        byte[] body;
        body = randomAlphanumeric(ATTACH_SIZE).getBytes();
        String name = "file name";

        WriteAttachmentResponse resp = writeAttachment()
                .withFilename(name)
                .withReq((req) ->
                        req.setBody(body))
                .post(shouldBe(attachOk200()))
                .as(WriteAttachmentResponse.class);

        String sid = resp.getId();
        String hashWriteAttachment = resp.getHash();

        String hashSaveDraft = saveDraft()
                .withUploadedAttachStids(sid)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getAttachments()
                .get(0)
                .getHash();

        assertThat("Хеши одного аттача в выдаче write_attachment и save_draft должны совпать", hashWriteAttachment, equalTo(hashSaveDraft));
    }

    private String WriteAttachAndGetHash(String name, byte[] body) {
        String hash =  writeAttachment()
                .withFilename(name)
                .withReq((req) ->
                        req.setBody(body))
                .post(shouldBe(attachOk200()))
                .as(WriteAttachmentResponse.class)
                .getHash();

        assertThat("Хеш должен быть ненулевой", hash, not(nullValue()));
        return hash;
    }
}
