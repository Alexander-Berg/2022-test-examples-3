package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;


/**
 * Created by a-zoshchuk
 */
public interface AttachmentInMessageList extends MailElement {

    @Name("Название аттача")
    @FindByCss(".mail-File-Name")
    MailElement title();

    @Name("Кнопка для просмотра аттача")
    @FindByCss(".mail-File-Actions-Item_secondary.js-preview-attachment")
    MailElement show();

    @Name("Кнопка для сохранения аттача на диск")
    @FindByCss(".js-show-save-popup")
    MailElement save();

    @Name("Превью картинки в списке писем")
    @FindByCss(".mail-File-Icon")
    MailElement imgPreview();

    @Name("Кнопка для скачивания аттача")
    @FindByCss(".js-download-attachment.mail-File-Actions-Item_secondary")
    MailElement download();

    @Name("Тултип аттача")
    @FindByCss(".mail-File-Actions")
    MailElement toolTip();

    @Name("Тултип аттача в правой колонке")
    @FindByCss(".mail-File-Actions-Item")
    MailElement toolTipInRightPanel();

    @Name("Кнопка просмотра eml в списке писем")
    @FindByCss(".mail-File-Actions-Item_secondary.js-eml-preview")
    MailElement emlPreviewBtn();

    @Name("Расширение аттача")
    @FindByCss(".mail-File-Extension")
    MailElement ext();
}
