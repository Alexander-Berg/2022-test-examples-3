package ru.yandex.mail.tests.sendbernar;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.api.RequestTraits;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.properties.TvmProperties;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.mops.Mops;
import ru.yandex.mail.tests.sendbernar.generated.SaveDraftResponse;
import ru.yandex.mail.tests.sendbernar.generated.SendMessageResponse;
import ru.yandex.mail.tests.sendbernar.generated.savedraft.ApiSaveDraft;
import ru.yandex.mail.tests.sendbernar.models.DiskAttachHelper;
import ru.yandex.mail.tests.sendbernar.models.Message;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.common.utils.Files.downloadFile;
import static ru.yandex.mail.tests.hound.Folders.folders;
import static ru.yandex.mail.tests.sendbernar.SendbernarApi.apiSendbernar;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.noSuchParam400;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.storageError400;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.wrongUid400;
import static ru.yandex.mail.things.matchers.CountMessages.hasCountMsgsIn;
import static ru.yandex.mail.things.matchers.Items.listsAreEqual;
import static ru.yandex.mail.things.matchers.MidsWithLabel.hasMsgWithLidInFolder;
import static ru.yandex.mail.things.matchers.StringDiffer.notDiffersWith;

@Aqua.Test
@Title("Ручка save_draft. Сохраняем черновик письма")
@Description("Проверяем работу ручки save_draft" +
        "Для всех параметров проверяем позитивные кейсы + " +
        "дублируем кейсы про сохранение черновиков ручкой mail_send")
@Stories("draft")
@Issues(@Issue("MAILPG-772"))
public class SaveDraftTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.saveDraft;
    }

    private int numberOfDrafts;

    @Before
    public void setup() {
        numberOfDrafts = folders(
                HoundApi.apiHound(
                        HoundProperties.properties()
                                .houndUri(),
                        props().getCurrentRequestId()
                )
                .folders()
                .withUid(authClient.account().uid())
                .post(shouldBe(HoundResponses.ok200()))
        )
                .count(folderList.draftFID());
    }

    @Test
    @Title("Сохраняем в черновики письмо только с необходимыми параметрами: uid, caller, connection_id")
    public void shouldSaveDraftWithRequiredParams() {
        String draftLid = lidByTitle("draft_label");

        String mid = saveDraft()
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("Черновик не сохранился",
                authClient,
                hasCountMsgsIn(equalTo(numberOfDrafts + 1), folderList.draftFID()));

        assertThat("Черновик не пометился черновиком", authClient,
                hasMsgWithLidInFolder(mid, folderList.draftFID(), draftLid));

        assertThat("Черновик не пометился прочитанным", authClient,
                hasMsgWithLidInFolder(mid, folderList.draftFID(), lidByTitle("seen_label")));
    }

    @Test
    @Title("Дергаем save_draft вообще без параметров")
    public void shouldGetErrorWithoutParams() {
        apiSendbernar()
                .saveDraft()
                .post(shouldBe(noSuchParam400()));
    }

    @Test
    @Title("Дергаем save_draft с неправильным uid")
    public void shouldGetErrorOnWrongUid() {
        apiSendbernar()
                .saveDraft()
                .withUid(unexistingUid())
                .withCaller(caller)
                .post(shouldBe(wrongUid400()));
    }

    @Test
    @Title("Сохраняем черновик с аттачем, загруженным через uploaded_attach_stids")
    public void shouldSaveDraftWithUploadedAttach() throws Exception {
        String id = uploadedId();


        String draftAttid = saveDraft()
                .withUploadedAttachStids(id)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getAttachments()
                .get(0)
                .getAttId();


        waitWith.draft().waitDeliver();
        assertThat("id аттача из параметра и ответа save_draft не совпадают",
                draftAttid,
                equalTo(id));
    }

    @Test
    @Title("Не должны сохранять черновик с несуществующим аттачем")
    public void shouldNotSaveDraftWithWrongUploadedAttach() throws Exception {
        saveDraft()
                //btw, если добавить текст в конец айдишника, аттач сохранится
                .withUploadedAttachStids(Random.string() + uploadedId())
                .post(shouldBe(storageError400()));

        assertThat("Черновик с аттачем с несуществующим stid сохранился",
                authClient,
                hasCountMsgsIn(equalTo(numberOfDrafts), folderList.draftFID()));
    }

    @Test
    @Title("Сохраняем черновик с аттачем с диска")
    public void shouldSaveDraftWithDiskAttach() throws IOException {
        DiskAttachHelper attach = new DiskAttachHelper();


        String mid = saveDraft()
                .withDiskAttaches(attach.getHtml())
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        waitWith.draft().waitDeliver();


        attach.assertAttachInMessageIsSame(byMid(mid));
    }

    @Test
    @Title("Сохраняем черновик с вложениями parts_json")
    public void shouldSaveDraftWithPartsJson() throws Exception {
        File imageJpeg = downloadFile(IMG_URL_JPG, Random.string());

        String mid = saveDraft()
                .withSubj(subj)
                .withPartsJson(getPartsJson(imageJpeg))
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        waitWith.draft().subj(subj).waitDeliver();

        shouldSeeImageInMessage(imageJpeg, mid);
    }

    @Test
    @Title("Сохраняем черновик с вложенным как eml письмом")
    public void shouldSaveDraftWithForwardedMids() throws Exception {
        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String forwardedLetter = waitWith.subj(subj).inbox().waitDeliver().getMid();


        String mid = saveDraft()
                .withForwardMids(forwardedLetter)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("Вложенное как аттач письмо не сохранилось", authClient,
                hasMsgWithLidInFolder(mid, folderList.draftFID(), attachedLid()));
    }

    @Test
    @Title("Сохраняем черновик с подтвержденным альтернативным адресом отправителя")
    public void shouldChangeFromMailbox() throws IOException {
        String altFrom = String.format("%s@%s", authClient.account().login(), "ya.ru");

        String mid = saveDraft()
                .withFromMailbox(altFrom)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        assertThat("Отправитель сообщения не сохранился",
                byMid(mid).fromEmail(),
                equalTo(altFrom));
    }

    @Test
    @Title("Не сохраняем у черновика неподтвержденный адрес отправителя")
    public void shouldNotSaveDraftWithIncorrectFrom() throws IOException {
        String altFrom = randomAddress();

        String mid = saveDraft()
                .withFromMailbox(altFrom)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        assertThat("Сохранился неподтвержденный альтернативный ящик отправителя",
                byMid(mid).fromEmail(),
                equalTo(authClient.account().email()));
    }

    @Test
    @Title("Сохраняем черновик с другим именем отправителя")
    public void shouldChangeFromName() throws IOException {
        String name = Random.string();
        String mid = saveDraft()
                .withFromName(name)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        assertThat("Имя отправителя не изменилось",
                byMid(mid).fromName(),
                equalTo(name));
    }

    @Test
    @Title("Сохраняем в черновики письмо с to, сс и bcc. Проверяем всех адресатов")
    public void shouldSaveToCcAndBcc() throws IOException {
        String to = randomAddress();
        String cc = randomAddress();
        String bcc = randomAddress();

        String mid = saveDraft()
                .withTo(to)
                .withCc(cc)
                .withBcc(bcc)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        Message savedDraft = byMid(mid);

        assertThat("Неправильный адресат to", savedDraft.toEmail(), is(to.toLowerCase()));
        assertThat("Неправильный адресат cc", savedDraft.ccEmail(), is(cc.toLowerCase()));
        assertThat("Неправильный адресат bcc", savedDraft.bccEmail(), is(bcc.toLowerCase()));
    }

    @Test
    @Title("Сохраняем письмо с несколькими адресатами")
    public void shouldSaveMultipleAdressees() throws IOException {
        String to1 = randomAddress();
        String to2 = randomAddress();
        String to3 = randomAddress();
        String to4 = randomAddress();

        String mid = saveDraft()
                .withTo(String.format("%s,%s,%s,%s", to1, to2, to3, to4))
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        assertThat("Не все адресаты to сохранились",
                byMid(mid).toEmailList(),
                listsAreEqual(Arrays.asList(to1, to2, to3, to4)));
    }

    @Test
    @Title("Сохраняем черновик себе")
    public void shouldSaveDraftToSelf() throws IOException {
        String selfEmail = authClient.account().email();

        String mid = saveDraft()
                .withTo(selfEmail)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        assertThat("Черновик себе не сохраняется",
                byMid(mid).toEmail(),
                equalTo(selfEmail));
    }

    @Test
    @Title("Сохраняем черновик unicode-адресату")
    public void shouldSaveDraftForUnicodeAddressee() throws IOException {
        String unicodeTo = "jøran@blåbærsyltetøy.gulbrandsen.priv.no";

        String draftMid = saveDraft()
                .withTo(unicodeTo)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("Поломался unicode-адресат",
                byMid(draftMid).toEmail(),
                equalTo(unicodeTo));
    }

    @Test
    @Title("Проверяем, что сохраняем тему письма")
    public void shouldSaveSubject() throws IOException {
        String mid = saveDraft()
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("Тема черновика не сохранилась",
                byMid(mid).subject(),
                is(subj));
    }

    @Test
    @Title("Проверяем, что у письма без темы после сохранения тема No subject")
    public void shouldSaveNoSubjectLetter() throws IOException {
        String mid = saveDraft()
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("Не проставилась дефолтная тема No subject",
                byMid(mid).subject(),
                is(NO_SUBJECT_TITLE));
    }

    @Test
    @Title("Сохраняем черновик с html-разметкой")
    public void shouldSaveHtmlDraft() throws IOException {
        String content = "html content";
        String html = String.format("<html><body><b>%s</b></body></html>", content);

        String mid = saveDraft()
                .withHtml(ApiSaveDraft.HtmlParam.YES)
                .withText(html)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("Неправильный текст при сохранении html",
                byMid(mid).text(),
                equalTo(content));

    }

    @Test
    @Title("Сохраняем черновик с html-разметкой, не указывая флаг html=yes")
    public void shouldSaveDraftWithHtmlSyntax() throws IOException {
        String content = "html content";
        String html = String.format("<html><body><b>%s</b></body></html>", content);

        String mid = saveDraft()
                .withText(html)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("Неправильный текст при сохранении html без флага html=yes",
                html, notDiffersWith(byMid(mid).content())
                        .exclude("\r")
                        .exclude("\n"));
    }

    @Test
    @Title("Проверяем, сохраняем ли неанглийский текст письма")
    public void shouldSaveNonEnglishText() throws IOException {
        String nonEnglish = "Русские и китайские символы 时间是最好的稀释剂，新舆论热点的出现，" +
                "不断转移公众的视线，掩盖了旧闻的解决。但是，一" +
                "个成熟的社会不会因为新热点的出现而习惯性地遗忘“旧闻”";

        String mid = saveDraft()
                .withText(nonEnglish)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("Ломается текст при сохранении юникода",
                byMid(mid).text(),
                notDiffersWith(nonEnglish)
                        .exclude(" ")
                        .exclude("\r")
                        .exclude("\n"));
    }

    @Test
    @Title("Проверяем, что на черновик ставится произвольная метка")
    public void shouldSaveDraftWithLabel() {
        String lid = Mops.newLabelByName(authClient, Random.string());

        String mid = saveDraft()
                .withLids(lid)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("Метка на черновике не сохранилась",
                authClient,
                hasMsgWithLidInFolder(mid, folderList.draftFID(), lid));
    }

    @Test
    @Title("Проверяем, что можем перезаписать исходный черновик")
    public void shouldOverwriteSourceDraft() throws IOException {
        String sourceText = Random.string();
        String sourceTo = randomAddress();

        String sourceDraftMid = saveDraft()
                .withTo(sourceTo)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        String newDraftMid = saveDraft()
                .withTo(randomAddress())
                .withSourceMid(sourceDraftMid)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        Message savedDraft = byMid(newDraftMid);

        assertThat("После сохранения нового черновика поверх старого в папке черновиков 2 письма",
                authClient,
                hasCountMsgsIn(equalTo(numberOfDrafts + 1), folderList.draftFID()));
        assertThat("После сохранения нового черновика поверх старого остался старый текст",
                savedDraft.text(), not(containsString(sourceText)));
        assertThat("После сохранения нового черновика поверх старого остался старый адресат",
                savedDraft.toEmail(), not(is(sourceTo)));
    }

    @Test
    @Title("Проверяем, сохраняем ли параметр inreplyto_mid")
    public void shouldSaveInReplyToMid() throws IOException {
        String messageId = sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();


        String draftMid = saveDraft()
                .withInreplyto(messageId)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        String draftMessageId = byMid(draftMid).getHeader("in-reply-to");


        assertThat("inreplyto не сохранился", messageId, equalTo(draftMessageId));
    }

    @Test
    @Title("Проверяем, что сохраняем параметр references")
    public void shouldSaveReferences() throws Exception {
        String newSubj = Random.string();


        String reference = sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();
        String secondReference = sendMessage()
                .withTo(authClient.account().email())
                .withSubj(newSubj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();


        String mid = saveDraft()
                .withReferences(String.format("%s,%s", reference, secondReference))
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        String referenceHeader = byMid(mid).getHeader("references");


        assertThat("Не сохранился references черновика",
                referenceHeader,
                both(containsString(reference)).and(containsString(secondReference)));
    }

    @Test
    @Title("Проверяем, сохраняем ли параметр message_id")
    public void shouldSaveMessageId() {
        String messageId = "<1121537786231@wmi5-qa.yandex.ru>";

        SaveDraftResponse resp = saveDraft()
                .withMessageId(messageId)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class);

        assertThat("message_id в ответе сендбернара не совпадает с переданным", resp.getMessageId(), equalTo(messageId));

        String messageIdHeader = byMid(resp.getStored().getMid()).getHeader("message-id");

        assertThat("message_id в ответе mbody не совпадает с переданным в sendbernar", messageIdHeader, equalTo(messageId));
    }
}
