package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mariya-murm
 */
public interface ViewEventSidebar extends MailElement {

    @Name("Кнопка «Пойду»")
    @FindByCss(".qa-EventDecision-ButtonYes")
    MailElement buttonYes();

    @Name("Кнопка решения в попапе принятой встречи")
    @FindByCss(".qa-EventDecision-ButtonDecision")
    MailElement buttonDecision();

    @Name("Крестик в сайдбаре просмотра события")
    @FindByCss(".qa-EventFormPreview-Closer")
    MailElement closeEventPreviewShedule();

    @Name("Кнопка удаления события")
    @FindByCss(".qa-EventFormDelete-Button")
    MailElement eventDeleteButton();
}
