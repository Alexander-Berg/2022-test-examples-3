package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.lambdaj.function.convert.StringConverter;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.MatcherAssert;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.ImapRequest;
import ru.yandex.qatools.allure.annotations.Step;

import static ch.lambdaj.collection.LambdaCollections.with;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.cthul.matchers.object.ContainsPattern.matchesPattern;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.matchers.WaitMatcher.withWaitFor;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

public abstract class ImapResponse<T extends ImapResponse<T>> {

    //общие ошибки для всех комманд
    //MAILPROTO-2360
    public static final String COMMAND_SYNTAX_ERROR = "Command syntax error.";
    public static final String BACKEND_ERROR = "backend error";
    private final List<String> responseLines = new ArrayList<String>();
    private ImapRequest<?> request = null;
    private Status status = Status.UNKNOWN;
    private String tag = null;
    private String statusLineAdditionalText = null;

    public static <T extends ImapResponse<T>> T newResponseInstance(Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(String line) {
        isTrue(!isComplete(), "Баг в тестах: ответ на запрос уже закончился, но мы прочитали ещё одну строку");

        responseLines.add(line);
        parseLastLine(line);
        parse(line);
    }

    protected abstract void parse(String line);

    protected abstract void validate();

    private void parseLastLine(String line) {
        //FIXME: Баг ли тестов, если приходящая строка - null?
        Matcher matcher = Pattern.compile("(?i)^(\\S*) (OK|NO|BAD)(.*)$").matcher(defaultIfEmpty(line, ""));
        if (matcher.matches()) {
            if (!matcher.group(1).equals("*")) {
                tag = matcher.group(1);
                status = Status.valueOf(matcher.group(2));
                statusLineAdditionalText = matcher.group(3).substring(1);
            }
        } else {
            assertThat("Строка с тегом запроса, но без OK|NO|BAD", line,
                    not(matchesPattern("(?i)^" + request.getTag())));
        }
    }

    public ImapRequest<? extends ImapResponse> getRequest() {
        return request;
    }

    public void setRequest(ImapRequest<?> request) {
        this.request = request;
    }

    public int numberOfLines() {
        return responseLines.size();
    }

    public List<String> lines() {
        return Collections.unmodifiableList(responseLines);
    }

    public boolean isComplete() {
        return status != Status.UNKNOWN;
    }

    public String getTag() {
        return tag;
    }

    public Status getStatus() {
        return status;
    }

    public T shouldBeOk() {
        return statusShouldBe(Status.OK);
    }

    public T shouldBeNo() {
        return statusShouldBe(Status.NO);
    }

    public T shouldBeBad() {
        return statusShouldBe(Status.BAD);
    }

    public T repeatUntilOk(ImapClient client) {
        return repeatUntil(client, Status.OK);
    }

    public T repeatUntilNo(ImapClient client) {
        return repeatUntil(client, Status.NO);
    }

    public T repeatUntilBad(ImapClient client) {
        return repeatUntil(client, Status.BAD);
    }

    @Override
    public String toString() {
        return String.format("Ответ для %s со статусом %s", getRequest().toString(), getStatus());
    }

    @Step("Ожидаем получить статус <{0}>")
    public T statusShouldBe(Status value) {
        assertThat(format("Для запроса %s \n(ответ ->) %s", getRequest(), statusLineAdditionalText),
                status, equalTo(value));
        return (T) this;
    }

    @Step("Повторяем запрос до тех пор, пока не будет статус <{1}>")
    private T repeatUntil(ImapClient client, Status status) {
        final ImapRequest req = this.getRequest();
        org.hamcrest.Matcher<? super ImapClient> matcher = new FeatureMatcher<ImapClient, Status>(
                equalTo(status), "", "") {
            @Override
            protected Status featureValueOf(ImapClient actual) {
                return actual.request(req).getStatus();
            }
        };
        assertThat("Клиенту вернулся неожиданный статус", client, withWaitFor(matcher, 10, SECONDS));
        return (T) client.request(req);
    }

    @Step("Проверяем наличие OK|NO|BAD")
    public T validateForOkNoBad() {
        assertThat("В запросе нет строки с OK|NO|BAD", status, is(not(Status.UNKNOWN)));
        assertThat("Теги запроса и ответа не совпадают", tag, equalTo(request.getTag()));
//        validate();    //todo: Вернуть с условием, что если есть то проверять   (>1 строчка)
        return (T) this;
    }

    @Step("Ожидаем приглашение к продолжению")
    public T shouldBeContinuationRequest() {
        assertThat(tag, equalTo("+"));
        return (T) this;
    }

    @Step("Ожидаем, что ответ будет таким же как <{0}>")
    public T shouldBeEqualTo(T responseToCompare) {
        assertThat("Ответы не совпадают", with(this.lines()).convert(removeTag(this.getTag())),
                hasSameItemsAsList(with(responseToCompare.lines()).convert(removeTag(responseToCompare.getTag()))));
        return (T) this;
    }

    @Step("Последняя строка должна содержать: {0}")
    public T statusLineContains(String additionalText) {
        MatcherAssert.assertThat(statusLineAdditionalText, containsString(additionalText));
        return (T) this;
    }

    private StringConverter<String> removeTag(final String tag) {
        return new StringConverter<String>() {
            @Override
            public String convert(String from) {
                return from.replaceFirst(String.format("^%s\\s", tag), "");
            }
        };
    }

    @Step("В ответе не должно быть ничего")
    public ImapResponse shouldBeEmpty() {
        MatcherAssert.assertThat(lines().size(), equalTo(1));
        return (T) this;
    }

    public static enum Status {
        UNKNOWN,
        OK,
        NO,
        BAD
    }
}
