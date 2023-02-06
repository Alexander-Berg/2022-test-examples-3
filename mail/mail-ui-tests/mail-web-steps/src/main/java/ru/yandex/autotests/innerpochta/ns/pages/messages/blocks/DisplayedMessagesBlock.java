package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.hamcrest.Matchers;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

import java.util.List;

import static ch.lambdaj.Lambda.filter;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

public interface DisplayedMessagesBlock extends MailElement {

    @Name("Список писем")
    @FindByCss(".ns-view-messages-item-wrap")
    ElementsCollection<MessageBlock> list();

    @Name("Первое письмо с темой {0}")
    default MessageBlock firstMessageWithSubject(String subject){
        List<? extends MessageBlock> list = filter(hasText(Matchers.containsString(subject)), list());
        return list.get(0);
    }

    @Name("Список писем-заголовков тредов")
    @FindByCss(".js-messages-list > .ns-view-messages-item-wrap .ns-view-messages-item[data-id^='t']")
    ElementsCollection<MessageBlock> threadMainMessagesList();

    @Name("Список выделенных писем")
    @FindByCss(".ns-view-messages-item-wrap.is-checked")
    ElementsCollection<MessageBlock> selectedMessages();

    @Name("Список писем в открытом треде")
    @FindByCss(".ns-view-messages-item-thread .ns-view-messages-item-wrap")
    ElementsCollection<MessageBlock> messagesInThread();

    @Name("Показать все письма в треде")
    @FindByCss(".b-messages__thread-more-link__caption")
    MailElement showAllLink();

    @Name("Загрузить ещё письма в треде")
    @FindByCss(".js-messages-load-more")
    MailElement loadMoreLink();

    @Name("Показать все письма в треде")
    @FindByCss(".b-messages__thread-more-link")
    MailElement showAllText();

    @Name("Все результаты поиска")
    @FindByCss(".mail-MessagesSearchInfo-Title")
    MailElement allResults();
}


