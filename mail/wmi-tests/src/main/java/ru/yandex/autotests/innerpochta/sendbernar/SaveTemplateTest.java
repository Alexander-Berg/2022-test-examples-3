package ru.yandex.autotests.innerpochta.sendbernar;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SaveTemplateResponse;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FoldersObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.savetemplate.ApiSaveTemplate;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.DiskAttachHelper;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiSendbernar;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.StringDiffer.notDiffersWith;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.CountMessagesMatcher.hasCountMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Folders.folders;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.*;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

@Aqua.Test
@Title("Ручка save_template. Сохраняем шаблон письма")
@Description("Проверяем работу ручки save_template")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.TEMPLATE)
@Issues(@Issue("MAILPG-773"))
@Credentials(loginGroup = "SaveTemplate")
public class SaveTemplateTest extends BaseSendbernarClass {
    private int numberOfTemplates;

    @Before
    public void setup() {
        numberOfTemplates = folders(
                    FoldersObj.empty().setUid(authClient.account().uid())
                )
                .get()
                .via(authClient)
                .count(folderList.templateFID());
    }

    @Test
    @Title("Сохраняем в шаблоны письмо только с необходимыми параметрами: uid, caller, connection_id")
    public void shouldSaveTemplateWithRequiredParams() {
        String lid = lidByTitle("draft_label");

        String mid = saveTemplate()
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Шаблон не сохранился",
                authClient,
                hasCountMsgsIn(equalTo(numberOfTemplates + 1), folderList.templateFID()));
        assertThat("Шаблон не пометился прочитанным", authClient,
                hasMsgWithLidInFolder(mid, folderList.templateFID(), lidByTitle("seen_label")));
        assertThat("Шаблон не пометился черновиком", authClient,
                hasMsgWithLidInFolder(mid, folderList.templateFID(), lid));
    }

    @Test
    @Title("Дергаем save_template вообще без параметров")
    @IgnoreForPg("MAILPG-2767")
    public void shouldGetErrorWithoutParams() {
        apiSendbernar(getUserTicket()).saveTemplate()
                .post(shouldBe(noSuchParam400()));
    }

    @Test
    @Title("Дергаем save_template с неправильным uid")
    @IgnoreForPg("MAILPG-2767")
    public void shouldGetErrorOnWrongUid() {
        apiSendbernar(getUserTicket()).saveTemplate()
                .withUid(unexistingUid())
                .withCaller(caller)
                .post(shouldBe(wrongUid400()));
    }

    @Test
    @Title("Сохраняем шаблон с аттачем, загруженным через uploaded_attach_stids")
    public void shouldSaveTemplateWithUploadedAttach() {
        String id = uploadedId();

        String templateAttid = saveTemplate()
                .withUploadedAttachStids(id)
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getAttachments()
                .get(0)
                .getAttId();

        waitWith.subj(subj).template().waitDeliver();
        assertThat("id аттача из параметра и ответа save_template не совпадают", templateAttid, equalTo(id));
    }

    @Test
    @Title("Не должны сохранять шаблон с несуществующим аттачем")
    public void shouldNotSaveTemplateWithWrongUploadedAttach() throws Exception {
        String id = uploadedId();

        saveTemplate()
                //btw, если добавить текст в конец айдишника, аттач сохранится
                .withUploadedAttachStids(getRandomString() + id)
                .post(shouldBe(storageError400()));

        assertThat("Шаблон с аттачем с несуществующим stid сохранился",
                authClient,
                hasCountMsgsIn(equalTo(numberOfTemplates), folderList.templateFID()));
    }

    @Test
    @Title("Сохраняем шаблон с аттачем с диска")
    public void shouldSaveTemplateWithDiskAttach() throws IOException {
        DiskAttachHelper attach = new DiskAttachHelper();


        String mid = saveTemplate()
                .withDiskAttaches(attach.getHtml())
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        waitWith.subj(subj).template().waitDeliver();


        attach.assertAttachInMessageIsSame(byMid(mid));
    }

    @Test
    @Title("Сохраняем шаблон с вложениями parts_json")
    public void shouldSaveTemplateWithPartsJson() throws Exception {
        File imageJpeg = downloadFile(IMG_URL_JPG, getRandomString(), authClient.notAuthHC());

        String mid = saveTemplate()
                .withSubj(subj)
                .withPartsJson(getPartsJson(imageJpeg))
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        waitWith.template().subj(subj).waitDeliver();

        shouldSeeImageInMessage(imageJpeg, mid, authClient);
    }

    @Test
    @Title("Сохраняем шаблон с вложенным как eml письмом")
    public void shouldSaveTemplateWithForwardedMids() throws Exception {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String forwardedLetter = waitWith.subj(subj).inbox().waitDeliver().getMid();


        String mid = saveTemplate()
                .withForwardMids(forwardedLetter)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Вложенное как аттач письмо не сохранилось", authClient,
                hasMsgWithLidInFolder(mid, folderList.templateFID(), WmiConsts.FAKE_ATTACHED_LBL));
    }

    @Test
    @Title("Сохраняем шаблон с подтвержденным альтернативным адресом отправителя")
    public void shouldChangeFromMailbox() throws IOException {
        String altFrom = String.format("%s@%s", authClient.acc().getLogin(), "ya.ru");

        String mid = saveTemplate()
                .withFromMailbox(altFrom)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Отправитель сообщения не сохранился",
                byMid(mid).fromEmail(),
                equalTo(altFrom));
    }

    @Test
    @Title("Не сохраняем у шаблона неподтвержденный адрес отправителя")
    public void shouldNotSaveTemplateWithIncorrectFrom() throws IOException {
        String altFrom = randomAddress();

        String mid = saveTemplate()
                .withFromMailbox(altFrom)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Сохранился неподтвержденный альтернативный ящик отправителя",
                byMid(mid).fromEmail(),
                equalTo(authClient.acc().getSelfEmail()));
    }

    @Test
    @Title("Сохраняем шаблон с другим именем отправителя")
    public void shouldChangeFromName() throws IOException {
        String name = getRandomString();
        String mid = saveTemplate()
                .withFromName(name)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Имя отправителя не изменилось",
                byMid(mid).fromName(),
                equalTo(name));
    }

    @Test
    @Title("Сохраняем в шаблоны письмо с to, сс и bcc. Проверяем всех адресатов")
    public void shouldSaveToCcAndBcc() throws IOException {
        String to = randomAddress();
        String cc = randomAddress();
        String bcc = randomAddress();

        String mid = saveTemplate()
                .withTo(to)
                .withCc(cc)
                .withBcc(bcc)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        Message savedTemplate = byMid(mid);

        assertThat("Неправильный адресат to", savedTemplate.toEmail(), is(to));
        assertThat("Неправильный адресат cc", savedTemplate.ccEmail(), is(cc));
        assertThat("Неправильный адресат bcc", savedTemplate.bccEmail(), is(bcc));
    }

    @Test
    @Title("Сохраняем письмо с несколькими адресатами")
    public void shouldSaveMultipleAdressees() throws IOException {
        String to1 = randomAddress();
        String to2 = randomAddress();
        String to3 = randomAddress();
        String to4 = randomAddress();

        String mid = saveTemplate()
                .withTo(String.format("%s,%s,%s,%s", to1, to2, to3, to4))
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Не все адресаты to сохранились",
                byMid(mid).toEmailList(),
                hasSameItemsAsList(Arrays.asList(to1, to2, to3, to4)));
    }

    @Test
    @Title("Сохраняем шаблон себе")
    public void shouldSaveTemplateToSelf() throws IOException {
        String selfEmail = authClient.acc().getSelfEmail();

        String mid = saveTemplate()
                .withTo(selfEmail)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();


        assertThat("Шаблон себе не сохраняется",
                byMid(mid).toEmail(),
                equalTo(selfEmail));
    }

    @Test
    @Title("Сохраняем шаблон unicode-адресату")
    public void shouldSaveTemplateForUnicodeAddressee() throws IOException {
        String unicodeTo = "jøran@blåbærsyltetøy.gulbrandsen.priv.no";

        String templateMid = saveTemplate()
                .withTo(unicodeTo)
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Поломался unicode-адресат",
                byMid(templateMid).toEmail(),
                equalTo(unicodeTo));
    }

    @Test
    @Title("Проверяем, что сохраняем тему письма")
    public void shouldSaveSubject() throws IOException {
        String mid = saveTemplate()
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Тема шаблона не сохранилась",
                byMid(mid).subject(),
                is(subj));
    }

    @Test
    @Title("Проверяем, что у письма без темы после сохранения тема No subject")
    public void shouldSaveNoSubjectLetter() throws IOException {
        String mid = saveTemplate()
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Не проставилась дефолтная тема No subject",
                byMid(mid).subject(),
                is(WmiConsts.NO_SUBJECT_TITLE));
    }

    @Test
    @Title("Сохраняем шаблон с html-разметкой")
    public void shouldSaveHtmlTemplate() throws IOException {
        String content = "html content";
        String html = String.format("<html><body><b>%s</b></body></html>", content);

        String mid = saveTemplate()
                .withHtml(ApiSaveTemplate.HtmlParam.YES)
                .withText(html)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Неправильный текст при сохранении html",
                byMid(mid).text(),
                equalTo(content));
    }

    @Test
    @Title("Сохраняем шаблон с html-разметкой, не указывая флаг html=yes")
    public void shouldSaveTemplateWithHtmlSyntax() throws Exception {
        String content = "html content";
        String html = String.format("<html><body><b>%s</b></body></html>", content);

        String mid = saveTemplate()
                .withText(html)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();


        assertThat("Неправильный текст при сохранении html без флага html=yes",
                html, notDiffersWith(byMid(mid).sourceContent())
                        .exclude("\r")
                        .exclude("\n"));
    }

    @Test
    @Title("Проверяем, сохраняем ли неанглийский текст письма")
    public void shouldSaveNonEnglishText() throws Exception {
        String nonEnglish = "Русские и китайские символы 时间是最好的稀释剂，新舆论热点的出现，" +
                "不断转移公众的视线，掩盖了旧闻的解决。但是，一" +
                "个成熟的社会不会因为新热点的出现而习惯性地遗忘“旧闻”";

        String mid = saveTemplate()
                .withText(nonEnglish)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Ломается текст при сохранении юникода",
                byMid(mid).sourceContent(),
                notDiffersWith(nonEnglish)
                        .exclude(" ")
                        .exclude("\r")
                        .exclude("\n"));
    }

    @Test
    @Title("Проверяем, что на шаблон ставится произвольная метка")
    public void shouldSaveTemplateWithLabel() {
        String lid = Mops.newLabelByName(authClient, getRandomString());

        String mid = saveTemplate()
                .withLids(lid)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        assertThat("Метка на шаблоне не сохранилась",
                authClient,
                hasMsgWithLidInFolder(mid, folderList.templateFID(), lid));
    }

    @Test
    @Title("Проверяем, что можем перезаписать исходный шаблон")
    public void shouldNotOverwriteSourceTemplate() throws IOException {
        String sourceText = getRandomString();
        String sourceTo = randomAddress();

        String sourceTemplateMid = saveTemplate()
                .withTo(sourceTo)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        String newTemplateMid = saveTemplate()
                .withTo(randomAddress())
                .withSourceMid(sourceTemplateMid)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class)
                .getStored()
                .getMid();

        Message savedTemplate = byMid(newTemplateMid);

        Integer nubmerOfMessages =folders(
                    FoldersObj.empty().setUid(authClient.account().uid())
                )
                .get()
                .via(authClient)
                .count(folderList.templateFID());

        assertThat("После сохранения нового шаблона поверх старого в папке шаблонов 2 письма",
                nubmerOfMessages == numberOfTemplates+1);
        assertThat("После сохранения нового шаблона поверх старого остался старый текст",
                savedTemplate.content(), not(containsString(sourceText)));
        assertThat("После сохранения нового шаблона поверх старого остался старый адресат",
                savedTemplate.toEmail(), not(is(sourceTo)));
    }


    @Test
    @Title("Проверяем, сохраняем ли параметр message_id")
    public void shouldSaveMessageId() {
        String messageId = "<1121537786231@wmi5-qa.yandex.ru>";

        SaveTemplateResponse resp = saveTemplate()
                .withMessageId(messageId)
                .post(shouldBe(ok200()))
                .as(SaveTemplateResponse.class);

        assertThat("message_id в ответе сендбернара не совпадает с переданным", resp.getMessageId(), equalTo(messageId));

        String messageIdHeader = byMid(resp.getStored().getMid()).getHeader("message-id");

        assertThat("message_id в ответе mbody не совпадает с переданным в sendbernar", messageIdHeader, equalTo(messageId));
    }
}
