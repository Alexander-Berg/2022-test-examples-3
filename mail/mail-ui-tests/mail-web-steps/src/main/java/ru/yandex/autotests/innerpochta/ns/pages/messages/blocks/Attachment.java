package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;


/**
 * Created by a-zoshchuk
 */
public interface Attachment extends MailElement {

    @Name("Название аттача")
    @FindByCss(".qa-MessageViewer-Attachment-fileName")
    MailElement title();

    @Name("Кнопка для просмотра аттача")
    @FindByCss(".qa-MessageViewer-Attachment-ViewBtn")
    MailElement show();

    @Name("Кнопка для сохранения аттача на диск")
    @FindByCss(".qa-MessageViewer-Attachment-SaveDiskBtn")
    MailElement save();

    @Name("Превью картинки в аттачах")
    @FindByCss(".qa-Attachment-preview")
    MailElement imgPreview();

    @Name("Кнопка для скачивания аттача")
    @FindByCss(".qa-MessageViewer-Attachment-DownloadBtn")
    MailElement download();

    @Name("Расширение аттача")
    @FindByCss(".qa-MessageViewer-Attachment-fileExtensionName")
    MailElement ext();
}
