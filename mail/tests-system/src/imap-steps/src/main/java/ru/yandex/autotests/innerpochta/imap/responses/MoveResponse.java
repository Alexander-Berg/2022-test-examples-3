package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.Matchers;

import ru.yandex.qatools.allure.annotations.Step;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 11.08.14
 * Time: 20:07
 */
public final class MoveResponse extends ImapResponse<MoveResponse> {

    public static final String WRONG_SESSION_STATE = "[CLIENTBUG] MOVE Wrong session state for command";
    public static final String NO_MESSAGES = "[CLIENTBUG] MOVE Failed (no messages).";
    public static String NO_SUCH_FOLDER = "[TRYCREATE] MOVE No such folder.";
    public static String FOLDER_ENCODING_ERROR = "[CLIENTBUG] MOVE Folder encoding error.";
    //MAILPROTO-2360
    public static String CAN_NOT_MOVE_FROM_RO_FOLDER = "[CLIENTBUG] MOVE Can not move from read-only folder.";
    public static String UID_CLIENT_BUG_NO_MESSAGES = "[CLIENTBUG] UID MOVE Completed (no messages).";

    private final Collection<Integer> expunged = new ArrayList<>();

    private Integer uidValidity = null;
    private String fromUidsSeq = null;
    private String toUidsSeq = null;


    @Override
    protected void parse(String line) {
        parseMove(line);
    }

    @Override
    protected void validate() {
    }

    private void parseMove(String line) {
        Matcher matcherExpunged = Pattern.compile("(?i)^\\* ([0-9]*) EXPUNGE$").matcher(line);
        Matcher matcherCopyUid =
                Pattern.compile("(?i)^\\* OK \\[COPYUID ([0-9]*) (.*) (.*)\\]$").matcher(line);

        if (matcherExpunged.matches()) {
            expunged.add(Integer.valueOf(matcherExpunged.group(1)));
        }

        if (matcherCopyUid.matches()) {
            uidValidity = Integer.valueOf(matcherCopyUid.group(1));
            fromUidsSeq = matcherCopyUid.group(2);
            toUidsSeq = matcherCopyUid.group(3);
        }
    }

    @Step("В ответе не должно быть событий с <EXPUNGE>")
    public MoveResponse expungedShouldBeEmpty() {
        assertThat("В ответе не должно быть событий с <EXPUNGE>", expunged, is(empty()));
        return this;
    }

    @Step("В ответе должен быть <EXPUNGE> со значениями: {0}")
    public MoveResponse expungeShouldBe(Integer... values) {
        assertThat("В ответе должен быть <EXPUNGE> со значениями",
                newArrayList(expunged), hasSameItemsAsList(newArrayList(values)));
        return this;
    }

    @Step("В ответе должен быть <UIDVALIDITY> со значеним: {0}")
    public MoveResponse shouldBeUidValidity(Integer validity) {
        if (uidValidity != null) {
            assertThat("Неверный <UIDVALIDITY>",
                    uidValidity, Matchers.equalTo(validity));
        }
        return this;
    }

    @Step("В ответе должна быть последовательность перемещаемый сообщений: {0}")
    public MoveResponse shouldBeFromUids(String uidsSequence) {
        assertThat("В ответе нет последовательности перемещаемых сообщений", fromUidsSeq, Matchers.is(notNullValue()));
        assertThat("Неверная UID последовательность перемещаемых сообщений", fromUidsSeq, equalTo(uidsSequence));

        return this;
    }

    @Step("В ответе должна быть последовательность перемещенных сообщений: {0}")
    public MoveResponse shouldBeToUids(String uidsSequence) {
        assertThat("В ответе нет последовательности перемещенных сообщений", toUidsSeq, Matchers.is(notNullValue()));
        assertThat("Неверная UID последовательность перемещенных сообщений", toUidsSeq, equalTo(uidsSequence));
        return this;
    }
}
