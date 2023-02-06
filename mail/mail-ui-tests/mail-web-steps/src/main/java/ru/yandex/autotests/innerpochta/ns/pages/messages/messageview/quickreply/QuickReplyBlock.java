package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.quickreply;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface QuickReplyBlock extends MailElement {

    @Name("Текстовое поле квикреплая")
    @FindByCss(".cke_editable")
    MailElement replyText();

    @Name("Кнопка «Отправить»")
    @FindByCss(".qa-QuickReply-SendButton")
    MailElement sendButton();

    @Name("Ссылка Перейти в полную форму ответа")
    @FindByCss(".qa-QuickReply-FullComposeButton")
    MailElement openCompose();

    @Name("Блок с основным адресам")
    @FindByCss(".qa-QuickReply-Recipients")
    MailElement yabbleMain();

    @Name("Блок «и ещё n получателей»")
    @FindByCss(".qa-QuickReply-Recipients-More")
    MailElement yabbleMore();

    @Name("Добавить аттач с компьютера")
    @FindByCss(".qa-Compose-FileInput2")
    MailElement localAttachInput();

    @Name("Добавить аттач с диска")
    @FindByCss(".qa-QuickReply-AttachDiskFileButton")
    MailElement addDiskAttach();

    @Name("Добавить аттач из почты")
    @FindByCss(".qa-QuickReply-AttachMailFileButton")
    MailElement addMailAttach();

    @Name("Крестик - закрыть QR")
    @FindByCss(".qa-QuickReply-CloseButton")
    MailElement closeQrBtn();
}
