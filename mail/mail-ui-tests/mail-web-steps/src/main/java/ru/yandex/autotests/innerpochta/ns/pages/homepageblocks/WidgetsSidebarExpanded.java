package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by eremin-n-s
 */

public interface WidgetsSidebarExpanded extends MailElement {

    @Name("Кнопка свернуть")
    @FindByCss(".ToggleWidget-ExpandedContainer .ToggleWidget-Button")
    MailElement collapseBtn();

    @Name("Иконка виджета Телемоста")
    @FindByCss(".ExpandedWidgetContainer.TelemostWidget .ExpandedContainerButton")
    MailElement telemostIcon();

    @Name("Иконка виджета Диска")
    @FindByCss(".ExpandedWidgetContainer.DiskWidget .WidgetTitle-Icon")
    MailElement diskIcon();

    @Name("Иконка виджета Заметок")
    @FindByCss(".ExpandedWidgetContainer.NotesWidget .WidgetTitle-Icon")
    MailElement notesIcon();

    @Name("Виджет Телемоста, кнопка «Создать видеовстречу»")
    @FindByCss(".ExpandedWidgetContainer.TelemostWidget .Button2")
    MailElement telemostBtn();

    @Name("Виджет Заметок, кнопка-крестик")
    @FindByCss(".ExpandedWidgetContainer.NotesWidget .HeaderAction-PlusIcon")
    MailElement notesPlusBtn();

    @Name("Виджет Диска, кнопка-крестик")
    @FindByCss(".ExpandedWidgetContainer.DiskWidget .HeaderAction-PlusIcon")
    MailElement diskPlusBtn();

    @Name("Виджет Диска, кнопка создания документа")
    @FindByCss(".ExpandedWidgetContainer.DiskWidget [title='Создать документ']")
    MailElement diskDocBtn();

    @Name("Виджет Диска, кнопка создания таблицы")
    @FindByCss(".ExpandedWidgetContainer.DiskWidget [title='Создать таблицу']")
    MailElement diskXlsxBtn();

    @Name("Виджет Диска, кнопка создания презентации")
    @FindByCss(".ExpandedWidgetContainer.DiskWidget [title='Создать презентацию']")
    MailElement diskPptxBtn();
}
