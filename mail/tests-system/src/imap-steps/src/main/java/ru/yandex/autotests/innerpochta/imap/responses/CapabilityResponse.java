package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.lambdaj.function.convert.Converter;
import com.google.common.base.Splitter;

import ru.yandex.qatools.allure.annotations.Step;

import static ch.lambdaj.collection.LambdaCollections.with;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

public final class CapabilityResponse extends ImapResponse<CapabilityResponse> {
    private List<String> capabilities = null;

    @Override
    protected void parse(String line) {
        parseCapability(line);
    }

    @Override
    protected void validate() {
        assertThat("В ответе нет строки CAPABILITY", capabilities, is(notNullValue()));
    }

    private void parseCapability(String line) {
        Matcher matcher = Pattern.compile("(?i)^\\* CAPABILITY(.*)$").matcher(line);
        if (matcher.matches()) {
            assertThat("В ответе больше одной строки CAPABILITY", capabilities, is(nullValue()));
            capabilities = newArrayList(Splitter.on(' ').split(matcher.group(1).substring(1)));
        }
    }

    @Step("Ответ должен содержать следующие CAPABILITY: {0}")
    public void shouldContainCapabilities(List<Capabilities> cap) {
        assertThat("Ожидалось наличие других CAPABILITY", capabilities, hasSameItemsAsList(newArrayList(with(cap).convert(new Converter<Capabilities, String>() {
            @Override
            public String convert(Capabilities from) {
                return from.value();
            }
        }))));
    }

    public void shouldContainCapabilities(Capabilities... requiredCapabilities) {
        shouldContainCapabilities(newArrayList(requiredCapabilities));
    }

    public static enum Capabilities {
        IMAP4REV1("IMAP4rev1"),
        CHILDREN("CHILDREN"),
        UNSELECT("UNSELECT"),
        LITERAL_PLUS("LITERAL+"),
        NAMESPACE("NAMESPACE"),
        XLIST("XLIST"),
        BINARY("BINARY"),
        UIDPLUS("UIDPLUS"),
        ENABLE("ENABLE"),
        IDLE("IDLE"),  //до логина НЕ показываем MAILPROTO-2347
        AUTH_PLAIN("AUTH=PLAIN"),
        AUTH_OAUTH("AUTH=XOAUTH2"),
        MOVE("MOVE"),    //добавилось в MAILPROTO-346
        ID("ID");

        private String value;

        private Capabilities(String value) {
            this.value = value;
        }

        public static Capabilities[] beforeLogin() {
            return new Capabilities[]{IMAP4REV1,
                    CHILDREN,
                    AUTH_PLAIN,
                    AUTH_OAUTH,
                    UNSELECT,
                    LITERAL_PLUS,
                    NAMESPACE,
                    XLIST,
                    BINARY,
                    UIDPLUS,
                    ENABLE,
                    MOVE,
                    IDLE,
                    ID};
        }

        public static Capabilities[] afterLogin() {
            return new Capabilities[]{IMAP4REV1,
                    CHILDREN,
                    UNSELECT,
                    LITERAL_PLUS,
                    NAMESPACE,
                    XLIST,
                    BINARY,
                    UIDPLUS,
                    ENABLE,
                    MOVE,
                    IDLE,
                    ID};
        }

        public String value() {
            return value;
        }

    }
}
