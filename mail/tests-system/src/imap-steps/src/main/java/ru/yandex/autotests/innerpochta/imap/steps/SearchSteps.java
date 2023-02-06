package ru.yandex.autotests.innerpochta.imap.steps;

import java.util.List;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.SearchRequest;
import ru.yandex.autotests.innerpochta.imap.responses.SearchResponse;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.matchers.SearchMatcher.shouldSeeNotEmptySearch;
import static ru.yandex.autotests.innerpochta.imap.matchers.WaitMatcher.withWaitFor;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.05.14
 * Time: 21:54
 */
public class SearchSteps {

    private final ImapClient client;

    private SearchSteps(ImapClient imap) {
        this.client = imap;
    }

    public static SearchSteps with(ImapClient imapClient) {
        return new SearchSteps(imapClient);
    }

    @Step("Получаем все сообщения")
    public List<String> allMessages() {
        return client.request(search().all()).shouldBeOk().getMessages();
    }

    @Step("Получаем uid всех сообщений")
    public List<String> uidAllMessages() {
        return client.request(search().uid(true).all()).shouldBeOk().getMessages();
    }

    @Step("Получаем uid сообщений: {0}")
    public List<String> uidMessages(String sequence) {
        return client.request(search(sequence).uid(true)).shouldBeOk().getMessages();
    }

    @Step("Должны найти сообщения по запросу {0}")
    public SearchResponse shouldSearch(SearchRequest searchRequest) {
        client.request(searchRequest).shouldBeOk().getMessages();
        assertThat("Должны что-то найти", client, withWaitFor(shouldSeeNotEmptySearch(searchRequest), 10, SECONDS));
        return (SearchResponse) client.getLastResponse();
    }
}
