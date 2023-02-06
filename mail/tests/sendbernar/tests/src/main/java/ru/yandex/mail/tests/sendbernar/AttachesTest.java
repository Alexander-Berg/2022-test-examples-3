package ru.yandex.mail.tests.sendbernar;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.tests.sendbernar.generated.SaveDraftResponse;
import ru.yandex.mail.tests.sendbernar.generated.WriteAttachmentResponse;
import ru.yandex.mail.tests.sendbernar.generated.savedraft.ApiSaveDraft;
import ru.yandex.mail.tests.sendbernar.generated.sendmessage.ApiSendMessage;
import ru.yandex.mail.tests.sendbernar.models.DiskAttachHelper;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.net.URLDecoder;

import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.tests.sendbernar.models.MessagesWithInlines.getInlineHtml;
import static ru.yandex.mail.tests.sendbernar.models.MessagesWithInlines.getSmile;
import static ru.yandex.mail.tests.sendbernar.models.MessagesWithInlines.getSmileWithHtml;

@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляет всевозможные аттачи")
@Stories({"mail send", "attach"})
@Issue("MAILADM-4048")
public class AttachesTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.attaches;
    }

    private static final String RESIZE_MAIL = "resize.yandex.net/mailservice";

    private void checkSmilesProxy(String mid) {
        assertThat("Смайл не проксируется через кешер resize.yandex.net",
                byMid(mid).content(),
                containsString(RESIZE_MAIL));
    }

    @Test
    @Description("Отправка смайла в html письме")
    public void shouldSendWithSmile() throws Exception {
        sendMessage()
                .withTo(authClient.account().email())
                .withHtml(ApiSendMessage.HtmlParam.YES)
                .withText(getSmile())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();


        checkSmilesProxy(mid);
    }

    @Test
    @Issue("DARIA-43044")
    @Description("Сохраняем инлайновый аттач в черновики и потом отправляем его")
    public void shouldSaveAsDraftWithSmileAndSendIt() throws Exception {
        String smile = getSmile();


        String draftMid = saveDraft()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .withHtml(ApiSaveDraft.HtmlParam.YES)
                .withText(smile)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        checkSmilesProxy(draftMid);


        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .withSourceMid(draftMid)
                .withHtml(ApiSendMessage.HtmlParam.YES)
                .withText(smile)
                .post(shouldBe(ok200()));

        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();


        checkSmilesProxy(mid);
    }

    @Test
    @Description("Отправка смайла и дополнительного html")
    public void shouldSendWithSmileAndAdditionalHtml() throws Exception {
        sendMessage()
                .withTo(authClient.account().email())
                .withHtml(ApiSendMessage.HtmlParam.YES)
                .withText(getSmileWithHtml())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();


        checkSmilesProxy(mid);
    }

    @Test
    @Description("Отсылаем письмо с инлайновым аттачем и проверяем cid")
    public void shouldSendWithInlineAttach() throws IOException {
        byte[] content = asByteSource(getResource("img/not_rotate.jpg")).read();
        String id = writeAttachment()
                .withFilename("not_rotate.jpg")
                .withReq((req) -> req.setBody(content))
                .post(shouldBe(ok200()))
                .as(WriteAttachmentResponse.class)
                .getId();


        String viewLargeUrl = SendbernarProperties.properties().messagePartReal(id);


        sendMessage()
                .withTo(authClient.account().email())
                .withHtml(ApiSendMessage.HtmlParam.YES)
                .withText(getInlineHtml(id, viewLargeUrl))
                .withSubj(subj)
                .post(shouldBe(ok200()));

        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();
        String cid = byMid(mid).getAttachments().get(0).getBinaryTransformerResult().getCid();


        assertThat("Разные cid у аттачей",
                cid,
                is(URLDecoder.decode(id, "UTF-8")));
    }


    @Test
    @Description("Отсылаем письмо с загруженным аттачем")
    public void shouldSendWithUploadedAttaches() {
        String id = uploadedId();


        sendMessage()
                .withSubj(subj)
                .withTo(authClient.account().email())
                .withUploadedAttachStids(id)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();
        String name = byMid(mid).getAttachments().get(0).getBinaryTransformerResult().getTypeInfo().getName();

        assertThat("Имя аттача и имя загруженного файла не совпадают",
                "not_rotate.jpg",
                is(name));
    }

    @Test
    @Description("Отсылаем письмо с дисковым аттачем")
    public void shouldSendWithDiskAttach() throws IOException {
        DiskAttachHelper attach = new DiskAttachHelper();


        sendMessage()
                .withTo(authClient.account().email())
                .withDiskAttaches(attach.getHtml())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();


        attach.assertAttachInMessageIsSame(byMid(mid));
    }
}