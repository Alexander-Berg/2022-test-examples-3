package ru.yandex.mail.tests.sendbernar;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;

@Aqua.Test
@Title("Ручки композа письма для Xeno")
@Description("Ручки /compose_message и /compose_draft")
public class ComposeTest extends BaseXenoClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.xeno;
    }

    @Test
    @Title("Проверка простого композа письма для отправки")
    public void shouldComposeMessage() throws Exception {
        String to = "to@yandex.ru";

        composeMessage()
                .withTo(to)
                .post(shouldBe(
                        okMessageComposed()
                                .and().spec(hasRecipient("to[0]", to))
                ));
    }

    @Test
    @Title("Проверка, что при указании Cc и Bcc они возвращаются непустые")
    public void shouldFillBccAndCc() throws Exception {
        String to = "to@yandex.ru";
        String cc = "cc@yandex.ru";
        String bcc = "bcc@yandex.ru";

        composeMessage()
                .withTo(to)
                .withCc(cc)
                .withBcc(bcc)
                .post(shouldBe(
                        okMessageComposed()
                                .and().spec(hasRecipient("to[0]", to))
                                .and().spec(hasRecipient("cc[0]", cc))
                                .and().spec(hasRecipient("bcc[0]", bcc))
                        )
                );

    }

    @Test
    @Title("Проверяем, что можно сохранить черновик без To")
    @Issues(@Issue("MAILPG-1689"))
    public void shouldComposeDraftWithoutRecipients() throws Exception {
        composeDraft()
                .post(shouldBe(okDraftComposed()));
    }

    @Test
    @Title("Проверяем, что аттачи возвращаются")
    public void shouldComposeDraftWithAttaches() throws Exception {
        composeDraft()
                .withUploadedAttachStids(uploadedId())
                .post(shouldBe(okDraftComposed()
                        .and()
                        .spec(nonEmptyAttaches())));
    }

    @Test
    @Title("Проверяем, сохраняем ли параметр message_id в тело ответа compose_draft")
    public void shouldComposeDraftSaveMessageId() throws Exception {
        String messageId = "<1121537786231@wmi5-qa.yandex.ru>";

        composeDraft()
                .withMessageId(messageId)
                .post(shouldBe(okDraftComposed()
                        .and()
                        .spec(hasMessageId(messageId))));
    }

    @Test
    @Title("Проверяем, сохраняем ли параметр message_id в тело ответа compose_message")
    public void shouldComposeMessageSaveMessageId() throws Exception {
        String messageId = "<1121537786231@wmi5-qa.yandex.ru>";

        composeMessage()
                .withTo("to@yandex.ru")
                .withMessageId(messageId)
                .post(shouldBe(okMessageComposed()
                        .and()
                        .spec(hasMessageId(messageId))));
    }
}

