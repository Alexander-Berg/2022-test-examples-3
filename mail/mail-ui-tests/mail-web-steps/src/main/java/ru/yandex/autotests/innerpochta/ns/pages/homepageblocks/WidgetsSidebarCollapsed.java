package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by eremin-n-s
 */

public interface WidgetsSidebarCollapsed extends MailElement {

    String IFRAME_WIDGETS = "iframe[class*=TavernPortal_iframe_]";

    @Name("IFrame")
    @FindByCss("iframe[class*=TavernPortal_iframe_]")
    MailElement iframe();

    @Name("Кнопка развернуть")
    @FindByCss(".ToggleWidget-Button")
    MailElement expandBtn();

    @Name("Виджет Телемоста")
    @FindByCss(".CollapsedWidgetContainer.TelemostWidget")
    MailElement telemost();

    @Name("Виджет Заметок")
    @FindByCss(".CollapsedWidgetContainer.NotesWidget")
    MailElement notes();

    @Name("Виджет Диска, кнопки создания документа")
    @FindByCss(".CollapsedWidgetContainer.DiskWidget .CollapsedIcon-Icon_type_base")
    ElementsCollection<MailElement> diskBtns();
}
