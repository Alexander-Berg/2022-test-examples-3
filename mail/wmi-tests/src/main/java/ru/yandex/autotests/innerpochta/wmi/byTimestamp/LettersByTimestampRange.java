package ru.yandex.autotests.innerpochta.wmi.byTimestamp;

import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.data.TimestampData.timestampDataForLetters;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 18.03.14
 * Time: 18:10
 * <p/>
 * [DARIA-41540]
 * Любые операции над письмами(удаление/изменения)
 * ЗАПРЕЩЕНЫ!!!
 */
@Aqua.Test
@Title("Mailbox_list, messages_by_timestamp_range вместе с since и till")
@Description("Проверяем только соответствие количества писем")
@Features(MyFeatures.WMI)
@Stories(MyStories.BY_TIMESTAMP)
@RunWith(Parameterized.class)
@Credentials(loginGroup = "ThreadsParamsSince")
@Issue("MAILPG-379")
public class LettersByTimestampRange extends BaseTest {

    private String since;
    private String till;
    private int expected;

    private DefaultHttpClient hcWithOAuth = authClient.oAuth();

    public LettersByTimestampRange(Integer since, Integer till, int expected) {
        if (since == null) {
            this.since = "";
        } else {
            this.since = since.toString();
        }

        if (till == null) {
            this.till = "";
        } else {
            this.till = till.toString();
        }

        this.expected = expected;
    }

    @Parameterized.Parameters(name = "since-{0}-till-{1}-exp-{2}")
    public static Collection<Object[]> data() {
        return timestampDataForLetters();
    }


    @Test
    @Issues({@Issue("DARIA-32622"), @Issue("DARIA-34367")})
    @Description("Проверяем количество писем, которые возвращает ручка")
    public void mailboxListWithSinceAndTillCounterChecks() throws IOException, JSONException {
        List<String> mids = jsx(MailBoxList.class).params(MailBoxListObj.empty()
                .setSince(since)
                .setTill(till)).post().via(hc)
                .withDebugPrint().getMidsOfMessagesInFolder();
        assertThat(String.format("Количество писем в диапазоне " +
                        "since=<%s> till=<%s> не совпадает с ожидаемым ", since, till),
                mids.size(), equalTo(expected));
    }
}
