package ru.yandex.autotests.innerpochta.ns.pages.lite;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 30.05.12
 * <p> Time: 15:51
 */
public interface MoreLitePage extends MailPage {

    // Селект по меткам
    @FindByCss("[name = 'lid")
    Select selectLid();

    // Деселект по меткам
    @FindByCss("[name = 'unlid")
    Select selectUnlid();

    // Кнопка "Выполнить"
    @FindByCss("[name = 'move")
    WebElement runButton();

    // Кнопка "Прочитано"
    @FindByCss("[name = 'mark")
    WebElement readButton();

    // Кнопка "Непрочитано"
    @FindByCss("[name = 'unmark")
    WebElement unreadButton();
}
