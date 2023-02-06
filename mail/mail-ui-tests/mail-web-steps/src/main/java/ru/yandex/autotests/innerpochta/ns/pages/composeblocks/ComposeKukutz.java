package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface ComposeKukutz extends MailElement {
    @Name("Список изменений в получателях")
    @FindByCss(".ComposeRecipientsDiff-Section")
    ElementsCollection<MailElement> diffList();

    @Name("Кнопка «Не показывать» кукутц")
    @FindByCss(".ComposeRecipientsDiff-CollapseControl")
    MailElement dontShowBtn();

    @Name("Кнопка «Добавить показ изменений в списке получателей»")
    @FindByCss(".ComposeRecipientsDiff-CollapsedSection")
    MailElement addBtn();
}
