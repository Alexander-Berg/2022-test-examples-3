package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 25.05.15.
 */
public interface ComposeToolbarBlock extends MailElement {

    @Name("Кнопка “Оформить письма“")
    @FindByCss(".cke_button__switchmode.cke_button_on")
    MailElement turnOnFormattingBtn();

    @Name("Кнопка “Отключить оформление“")
    @FindByCss(".cke_button__switchmode.cke_button_off")
    MailElement turnOffFormattingBtn();

    @Name("Жирный шрифт")
    @FindByCss(".cke_button__bold")
    MailElement bold();

    @Name("Задизейбленная кнопка «Жирный шрифт»")
    @FindByCss(".cke_button__bold.cke_button_disabled")
    MailElement disabledBold();

    @Name("Курсивный шрифт")
    @FindByCss(".cke_button__italic")
    MailElement italic();

    @Name("Подчёркнутый шрифт")
    @FindByCss(".cke_button__underline")
    MailElement underline();

    @Name("Зачёркнутый шрифт")
    @FindByCss(".cke_button__strike")
    MailElement strike();

    @Name("Цитата")
    @FindByCss(".cke_button__blockquote")
    MailElement blockquote();

    @Name("Цвет текста")
    @FindByCss(".cke_button__mailtextcolor")
    MailElement mailtextcolor();

    @Name("Цвет заливки")
    @FindByCss(".cke_button__mailbgcolor")
    MailElement mailbgcolor();

    @Name("Тип шрифта")
    @FindByCss(".cke_button__mailfont")
    MailElement mailfont();

    @Name("Размер шрифт")
    @FindByCss(".cke_button__mailfontsize")
    MailElement mailfontsize();

    @Name("Нумерованный список")
    @FindByCss(".cke_button__numberedlist")
    MailElement numberedlist();

    @Name("Маркированный список")
    @FindByCss(".cke_button__bulletedlist")
    MailElement bulletedlist();

    @Name("Выпадушка выравнивания")
    @FindByCss(".cke_button__menualignment")
    MailElement alignment();

    @Name("Добавить ссылку")
    @FindByCss(".cke_button__link")
    MailElement addLinkBtn();

    @Name("Добавить смайлик")
    @FindByCss(".cke_button__emoticons")
    MailElement addSmileBtn();

    @Name("Добавить аттач с диска")
    @FindByCss(".cke_button__attachmentdisk")
    MailElement addDiskAttach();

    @Name("Добавить аттач из почты")
    @FindByCss(".cke_button__attachmentmail")
    MailElement addMailAttach();

    @Name("Добавить аттач с компьютера")
    @FindByCss(".mail-Compose-Attach-Popup-Item_computer-controller")
    MailElement localAttachInput();

    @Name("Отменить действие")
    @FindByCss(".cke_button__undo")
    MailElement undo();

    @Name("Убрать форматирование")
    @FindByCss(".cke_button__removeformat")
    MailElement removeFormatting();

    @Name("Добавить изображение")
    @FindByCss(".cke_button__addimage")
    MailElement addImage();
}
