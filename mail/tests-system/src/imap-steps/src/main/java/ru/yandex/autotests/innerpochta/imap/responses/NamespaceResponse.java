package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.matchers.IsNotExtended.not;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 17.04.14
 * Time: 16:07
 */
public final class NamespaceResponse extends ImapResponse<NamespaceResponse> {

    public static final String NIL = "NIL";

    private String rootsNamespace;
    private String usersNamespace;
    private String sharedNamespace;

    @Override
    protected void parse(String line) {
        parseNamespace(line);
    }

    @Override
    protected void validate() {
        assertThat("В ответе больше одной строки NAMESPACE", lines().size(), equalTo(1));
        assertThat("В ответе нет корневого namespace-а", rootsNamespace, not(nullValue()));
        assertThat("В ответе нет пользовательского namespace-а", usersNamespace, not(nullValue()));
        assertThat("В ответе нет общего namespace-а", sharedNamespace, not(nullValue()));
    }

    private void parseNamespace(String line) {
        String namespacePattern = String.format("(\\(\\(.*\\)\\)|%s)", NIL);
        Matcher matcher = Pattern.compile(String.format("(?i)^\\* NAMESPACE %s %s %s",
                namespacePattern, namespacePattern, namespacePattern)).matcher(line);
        if (matcher.matches()) {
            rootsNamespace = matcher.group(1);
            usersNamespace = matcher.group(2);
            sharedNamespace = matcher.group(3);
        }
    }

    @Step("response should contain root namespace: {0}")
    public NamespaceResponse rootNamespaceShouldBe(String expected) {
        assertThat("Неверный корневой namespace", rootsNamespace, equalTo(expected));
        return this;
    }

    @Step("response should contain user namespace: {0}")
    public NamespaceResponse userNamespaceShouldBe(String expected) {
        assertThat("Неверный пользовательский namespace", usersNamespace, equalTo(expected));
        return this;
    }

    @Step("response should contain shared namespace: {0}")
    public NamespaceResponse sharedNamespaceShouldBe(String expected) {
        assertThat("Неверный шаренный namespace", sharedNamespace, equalTo(expected));
        return this;
    }
}
