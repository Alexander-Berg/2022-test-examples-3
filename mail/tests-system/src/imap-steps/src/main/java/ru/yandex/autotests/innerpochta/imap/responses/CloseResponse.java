package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 11.06.14
 * Time: 21:00
 */
public final class CloseResponse extends ImapResponse<CloseResponse> {

    public static final String WRONG_SESSION_STATE = "[CLIENTBUG] CLOSE Wrong session state for command";

    private int expunged;
    private int of;

    @Override
    protected void parse(String line) {
        parseExpunge(line);
    }

    @Override
    protected void validate() {
    }

    private void parseExpunge(String line) {
        Matcher matcher = Pattern.compile("(?i)^\\* OK CLOSE expunged ([0-9]*) of ([0-9]*) messages so far$")
                .matcher(line);
        if (matcher.matches()) {
            expunged = Integer.valueOf(matcher.group(1));
            of = Integer.valueOf(matcher.group(2));
        }
    }

    @Step("Не должны ничего удалять")
    public CloseResponse shouldBeEmpty() {
        expungeShouldBe(0);
        ofShouldBe(0);
        return this;
    }

    @Step("<expunge> должен быть равен {0}")
    public CloseResponse expungeShouldBe(Integer value) {
        assertThat("Неверное логирование команды CLOSE: expunged не соответствует ожидаему", expunged, equalTo(value));
        return this;
    }

    @Step("<of> должен быть равен {0}")
    public CloseResponse ofShouldBe(Integer value) {
        assertThat("Неверное логирование команды CLOSE: expunged не соответствует ожидаему", of, equalTo(value));
        return this;
    }
}
