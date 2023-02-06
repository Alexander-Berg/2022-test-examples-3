package ru.yandex.autotests.innerpochta.imap.matchers;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.SearchRequest;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * Created by kurau on 23.03.16.
 */
public class SearchMatcher extends TypeSafeMatcher<ImapClient> {

    private SearchRequest searchRequest;

    private Matcher matcher;

    private SearchMatcher(SearchRequest searchRequest, Matcher matcher) {
        this.searchRequest = searchRequest;
        this.matcher = matcher;
    }

    public static SearchMatcher shouldSeeNotEmptySearch(SearchRequest searchRequest) {
        return new SearchMatcher(searchRequest, hasSize(greaterThan(0)));
    }

    @Override
    protected boolean matchesSafely(ImapClient imapClient) {
        List<String> messages = imapClient.request(searchRequest).shouldBeOk().getMessages();
        return matcher.matches(messages);
    }

    @Override
    public void describeTo(Description description) {
        //TODO
    }
}
