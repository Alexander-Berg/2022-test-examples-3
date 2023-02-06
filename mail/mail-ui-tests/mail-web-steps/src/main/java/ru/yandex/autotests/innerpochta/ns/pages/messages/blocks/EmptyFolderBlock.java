package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface EmptyFolderBlock extends MailElement {

    String TEXT_HEADER = "Не нашлось ни одного письма.\n" +
            "Попробуйте сформулировать запрос иначе";

    String TEXT = "убедитесь, что в запросе нет ошибок\n" +
            "попробуйте уменьшить длину запроса\n" +
            "если вы помните отправителя, получателя, названия файлов или их тип, попробуйте " +
            "воспользоваться расширенным поиском или языком запросов\n" +
            "попробуйте найти нужное письмо вручную";

    @Name("Поле ввода текста для поиска в всплывающем меню")
    @FindByCss(".js-search-input")
    MailElement searchInput();

    @Name("Ссылка перехода в инбокс")
    @FindByCss("[href='#inbox']")
    MailElement inboxLink();

    @Name("Заголовок страницы пустой папки")
    @FindByCss("[class*=messages-empty__header]")
    MailElement textHeader();

    @Name("Текст страницы пустой папки")
    @FindByCss("[class*=messages-empty__list]")
    MailElement textList();

}

