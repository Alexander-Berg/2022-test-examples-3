package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface TranslateHeaderBlock extends MailElement {

    @Name("Перевести НА")
    @FindByCss(".ComposeReact-TranslateLanguagesPanel-LanguageBubble")
    ElementsCollection<MailElement> translateToLink();

    @Name("Ссылка «Справка»")
    @FindByCss(".ComposeReact-TranslateLanguagesPanel-HelpButton")
    MailElement translateHelp();

    @Name("Кнопка «Редактировать»")
    @FindByCss(".ComposeReact-TranslateLanguagesPanel-EditButton")
    MailElement editTranslateBtn();
}
