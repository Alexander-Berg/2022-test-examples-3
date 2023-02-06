package ru.yandex.autotests.innerpochta.sendbernar;


import java.io.File;
import java.io.IOException;

import lombok.val;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SaveDraftResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.WriteAttachmentResponse;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.DiskAttachHelper;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.savedraft.ApiSaveDraft;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendmessage.ApiSendMessage;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.MessagesWithInlines.getInlineHtml;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.MessagesWithInlines.getRandomSmile;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.MessagesWithInlines.getRandomSmileWithHtml;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;


@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляет всевозможные аттачи")
@Features(MyFeatures.SENDBERNAR)
@Stories({MyStories.MAIL_SEND, MyStories.ATTACH})
@Issue("MAILADM-4048")
@Credentials(loginGroup = "InlineAttaSend")
public class AttachesTest extends BaseSendbernarClass {
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
                .withTo(authClient.acc().getSelfEmail())
                .withHtml(ApiSendMessage.HtmlParam.YES)
                .withText(getRandomSmile())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();


        checkSmilesProxy(mid);
    }

    @Test
    @Issue("DARIA-43044")
    @Description("Сохраняем инлайновый аттач в черновики и потом отправляем его")
    public void shouldSaveAsDraftWithSmileAndSendIt() throws Exception {
        String smile = getRandomSmile();


        String draftMid = saveDraft()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withHtml(ApiSaveDraft.HtmlParam.YES)
                .withText(smile)
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();


        checkSmilesProxy(draftMid);


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
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
                .withTo(authClient.acc().getSelfEmail())
                .withHtml(ApiSendMessage.HtmlParam.YES)
                .withText(getRandomSmileWithHtml())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();


        checkSmilesProxy(mid);
    }

    @Test
    @Description("Отсылаем письмо с инлайновым аттачем и проверяем md5sum")
    public void shouldSendWithInlineAttach() throws Exception {
        String filename = "not_rotate.jpg";
        String resourceName = "img/imgrotated/" + filename;

        File inline = File.createTempFile(getRandomString(), null);
        byte[] content = asByteSource(getResource(resourceName)).read();

        asByteSink(inline).write(content);

        String id = writeAttachment()
                .withFilename(filename)
                .withReq((req) -> req.setBody(content))
                .post(shouldBe(ok200()))
                .as(WriteAttachmentResponse.class)
                .getId();


        String viewLargeUrl = props().webattachHost() + "/message_part_real/?sid=" + id + "&no_disposition=y";


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withHtml(ApiSendMessage.HtmlParam.YES)
                .withText(getInlineHtml(id, viewLargeUrl))
                .withSubj(subj)
                .post(shouldBe(ok200()));

        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();

        shouldSeeImageInMessage(inline, mid, authClient, filename);
    }


    @Test
    @Description("Отсылаем письмо с загруженным аттачем")
    public void shouldSendWithUploadedAttaches() {
        String id = uploadedId();


        sendMessage()
                .withSubj(subj)
                .withTo(authClient.acc().getSelfEmail())
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
                .withTo(authClient.acc().getSelfEmail())
                .withDiskAttaches(attach.getHtml())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();


        attach.assertAttachInMessageIsSame(byMid(mid));
    }

    @Test
    @Description("Отсылаем письмо с дисковым аттачем в формате json")
    @Issue("MAILPG-2297")
    public void shouldSendWithDiskAttachJson() throws IOException {
        DiskAttachHelper attach = new DiskAttachHelper();

        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withDiskAttachesJson(attach.getJson())
                .withSubj(subj)
                .post(shouldBe(ok200()));
        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();

        attach.assertAttachInMessageIsSame(byMid(mid));
    }

    @Test
    @Description("Должны склеивать дисковые аттачи, переданные в html и json")
    @Issue("MAILPG-2297")
    public void shouldSendWithBothJsonAndHtmlDiskAttach() throws IOException {
        DiskAttachHelper attachJson = new DiskAttachHelper();
        DiskAttachHelper attachHtml = new DiskAttachHelper();

        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withDiskAttachesJson(attachJson.getJson())
                .withDiskAttaches(attachHtml.getHtml())
                .withSubj(subj)
                .post(shouldBe(ok200()));
        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();

        val message = byMid(mid);
        attachJson.assertAttachInMessageIsSame(message);
        attachHtml.assertAttachInMessageIsSame(message);
    }

    @Test
    @Description("Отправка письма с несколькими дисковыми аттачами в формате json")
    @Issue("MAILPG-2297")
    public void shouldSendWithMultipleDiskAttachJson() throws IOException {
        DiskAttachHelper attachFirst = new DiskAttachHelper();
        DiskAttachHelper attachSecond = new DiskAttachHelper();

        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withDiskAttachesJson(attachFirst.getJson())
                .withDiskAttachesJson(attachSecond.getJson())
                .withSubj(subj)
                .post(shouldBe(ok200()));
        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();

        val message = byMid(mid);
        attachFirst.assertAttachInMessageIsSame(message);
        attachSecond.assertAttachInMessageIsSame(message);
    }

    @Test
    @Description("Отсылаем письмо с дисковым аттачем в формате json и видим добавление заголовка")
    @Issue("MAILPG-4647")
    public void shouldAddTitleWithDiskAttach() throws Exception {
        DiskAttachHelper attach = new DiskAttachHelper();

        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withDiskAttachesJson(attach.getJson())
                .withSubj(subj)
                .post(shouldBe(ok200()));
        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();

        assertThat("Заголовок для аттачей не был добавлен",
            byMid(mid).sourceContent(),
            containsString(DiskAttachHelper.ruAttachesTitle));
    }

    @Test
    @Description("Отсылаем письмо без дисковых аттачей и смотрим что заголовка нет")
    @Issue("MAILPG-4647")
    public void shouldNotAddTitleWithoutDiskAttaches() throws Exception {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));
        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();

        assertThat("Заголовок для аттачей был добавлен",
                byMid(mid).sourceContent(),
                not(containsString(DiskAttachHelper.ruAttachesTitle)));
    }

    @Test
    @Description("Отсылаем письмо с дисковым аттачем в формате json и полем hash")
    @Issue("MAILPG-4694")
    public void shouldContainDataHashInAttach() throws Exception {
        DiskAttachHelper attach = new DiskAttachHelper();

        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withDiskAttachesJson(attach.getJson())
                .withSubj(subj)
                .post(shouldBe(ok200()));
        String mid = waitWith.subj(subj).inbox().waitDeliver().getMid();

        assertThat("У аттача есть аттрибут data-hash",
                byMid(mid).sourceContent(),
                containsString(String.format("data-hash=\"%s\"", attach.getHash())));
    }
}
