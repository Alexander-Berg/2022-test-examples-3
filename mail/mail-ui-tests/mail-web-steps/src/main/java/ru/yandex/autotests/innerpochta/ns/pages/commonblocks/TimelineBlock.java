package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author yaroslavna
 */
public interface TimelineBlock extends MailElement {

    @Name("Текущее время")
    @FindByCss(".is-current > span")
    MailElement currentTime();

    @Name("Первое дело")
    @FindByCss(".is-grey")
    MailElement event();

    @Name("Дела")
    @FindByCss(".is-grey")
    ElementsCollection<MailElement> events();

    @Name("Создать событие")
    @FindByCss(".mail-Timeline-NewEvent")
    MailElement newEvent();

    @Name("Свернуть таймлайн")
    @FindByCss(".mail-Timeline-Toggle-Collapse")
    MailElement collapse();

    @Name("Развернуть таймлайн")
    @FindByCss(".mail-Timeline-Toggle-Expand")
    MailElement expand();

    @Name("Содержание таймлайна")
    @FindByCss(".mail-Timeline-List-Item-Content")
    MailElement content();
}
