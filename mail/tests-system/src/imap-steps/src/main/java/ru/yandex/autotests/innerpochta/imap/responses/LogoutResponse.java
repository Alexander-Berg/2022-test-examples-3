package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public final class LogoutResponse extends ImapResponse<LogoutResponse> {

    public static final String BYE_MESSAGE = "IMAP4rev1 Server logging out";

    private String byeMessage = null;

    @Override
    protected void parse(String line) {
        parseBye(line);
    }

    @Override
    protected void validate() {
        assertThat("В ответе нет строки BYE", byeMessage, is(notNullValue()));
    }


    private void parseBye(String line) {
        Matcher matcher = Pattern.compile("(?i)^\\* BYE(.*)$").matcher(line);
        if (matcher.matches()) {
            assertThat("В ответе больше одной строки BYE", byeMessage, is(nullValue()));
            byeMessage = matcher.group(1).substring(1);
        }
    }

    @Step("should see <BUY IMAP4rev1 Server logging out>")
    public void shouldSeeBuyMessage(String buy) {
        assertThat(byeMessage, containsString(buy));
    }

}
