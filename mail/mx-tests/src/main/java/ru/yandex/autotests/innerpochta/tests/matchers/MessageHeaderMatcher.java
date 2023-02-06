package ru.yandex.autotests.innerpochta.tests.matchers;

import com.google.common.base.Joiner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.List;

import static ch.lambdaj.Lambda.*;
import static java.lang.String.format;
import static java.util.Collections.list;
import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.autotests.innerpochta.tests.headers.HeadersData.HeaderNames.AUTH_RESULTS;
import static ru.yandex.autotests.innerpochta.tests.headers.HeadersData.HeaderNames.DKIM_SIGNATURE;

/**
 * User: alex89
 * Date: 21.03.14
 */


public class MessageHeaderMatcher extends TypeSafeMatcher<Message> {
    private Logger log = LogManager.getLogger(this.getClass());
    private String errorMessage;

    private String headerName;
    private Matcher headerMatcher;

    public MessageHeaderMatcher(String headerName, Matcher headerMatcher) {
        this.headerName = headerName;
        this.headerMatcher = headerMatcher;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean matchesSafely(Message msg) {
        List<Header> foundHeadersWithExpectedName;
        try {
            foundHeadersWithExpectedName = select(list(msg.getAllHeaders()),
                    having(on(Header.class).getName(), equalTo(headerName)));
            if (foundHeadersWithExpectedName.size() == 0) {
                errorMessage = format("В письме нет заголовков '%s'", headerName);
                return false;
            }
            errorMessage = format("Найден заголовок %s: %s", headerName,
                    Joiner.on("|").join(msg.getHeader(headerName)));
        } catch (MessagingException e) {
            errorMessage = format("'%s' заголовок не найден!", headerName);
            return false;
        }


        log.info(errorMessage);
        return select(foundHeadersWithExpectedName,
                having(on(Header.class).getValue(), headerMatcher)).size() > 0;
    }

    @Override
    protected void describeMismatchSafely(Message msg, Description description) {
        description.appendText(errorMessage);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(
                format("'%s' удволетворяющий условию: %s.", headerName, headerMatcher));
    }

    public static MessageHeaderMatcher hasHeader(String name, Matcher<String> headerMatcher) {
        return new MessageHeaderMatcher(name, headerMatcher);
    }

    public static MessageHeaderMatcher hasHeader(String name) {
        return new MessageHeaderMatcher(name, matchesPattern(".*"));
    }

    public static MessageHeaderMatcher hasDkimSignature(Matcher headerMatcher) {
        return new MessageHeaderMatcher(DKIM_SIGNATURE.getName(), headerMatcher);
    }

    public static MessageHeaderMatcher hasDkimStatus(String status) {
        return new MessageHeaderMatcher(AUTH_RESULTS.getName(), containsString("dkim=" + status));
    }

    public static MessageHeaderMatcher hasAuthResultsHeader(Matcher<String> headerMatcher) {
        return new MessageHeaderMatcher(AUTH_RESULTS.getName(), headerMatcher);
    }
}