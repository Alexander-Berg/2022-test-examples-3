package ru.yandex.autotests.innerpochta.ns.pages.corp;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CorpPage extends MailPage {

    @Name("Лого «NDA»")
    @FindByCss("[class*=PSHeader__ndaStamp]")
    MailElement logoNDA();

    @Name("«Жук» для отправки багрепорта")
    @FindByCss(".YndxBug")
    MailElement mailBugReport();

    @Name("Форма обратной связи")
    @FindByCss("[class*=Bug__bug_expanded]")
    MailElement feedbackFormReport();

    @Name("Крестик в форме обратной связи")
    @FindByCss("[class*=Bug__close]")
    MailElement mailBugReportClose();

    @Name("Полоска «+ кукуц» в композе")
    @FindByCss(".mail-Compose-Recipients-Diff")
    MailElement kukutz();

    @Name("Кнопка «Закрыть» на полоске «+ кукуц»")
    @FindByCss(".mail-Compose-Recipients-Diff-close")
    MailElement closeKukutz();

}
