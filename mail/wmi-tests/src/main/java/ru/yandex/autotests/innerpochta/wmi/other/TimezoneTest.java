package ru.yandex.autotests.innerpochta.wmi.other;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.GetUserParameters;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageBody;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.qatools.allure.annotations.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj.xmlVerDaria2;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj.getMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList.mailboxListJsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MessageBody.messageBody;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.DeleteUtils.deleteOneMessageBySubjFromInboxOutbox;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 22.05.15
 * Time: 15:47
 * <p/>
 * У юзеров стоят различные часовые пояса
 */
@Aqua.Test
@Title("Проверка таймзоны входящего письма")
@Description("Параметризованный тест проверяем timestamp и utc_timestamp для различных таймзон")
@Features(MyFeatures.WMI)
@Stories(MyStories.TIMESTAMP)
@RunWith(Parameterized.class)
public class TimezoneTest extends BaseTest {

    @Parameterized.Parameter
    public String login;

    @Parameterized.Parameter(1)
    public String pwd;

    @Parameterized.Parameter(2)
    public int tz;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> tzData() throws Exception {
        return asList(new Object[]{"moscowTz", "testqa", getOffset("Europe/Moscow")},
                new Object[]{"alaskaTz", "testqa", getOffset("America/Anchorage")},
                new Object[]{"greenwichTz", "testqa", getOffset("Europe/London")},
                new Object[]{"brazilTz", "testqa", getOffset("America/Sao_Paulo")},
                new Object[]{"arizonaTz", "testqa", getOffset("US/Arizona")});
    }

    private static int getOffset(String zoneId) {
        return LocalDateTime.now().atZone(ZoneId.of(zoneId)).getOffset().getTotalSeconds() / (60 * 60);
    }

    @Test
    @Issue("DARIA-48189")
    @Title("Время в письме и списке писем должны совпадать")
    @Description("Отправляем письмо, проверяем таймзоны для mailbox_list :" +
            "<utc_timestamp>1432297721</utc_timestamp>\n" +
            "<timestamp>1432258121000</timestamp> и message_body")
    public void timezoneTest() throws Exception {
        HttpClientManagerRule auth = authClient.with(login, pwd).login();
        DefaultHttpClient hc = auth.authHC();

        SendUtils sendUtils = new SendUtils(auth).waitDeliver().send();
        String mid = sendUtils.getMid();

        MailBoxList mailBoxList = mailboxListJsx(xmlVerDaria2()).post().via(hc);

        long utcTimestamp = mailBoxList.getUtcTimestamp(mid);
        long timestamp = mailBoxList.getTimestamp(mid);

        //у верстки кривая обработка этого поля, и они у себя в верстке к нему ещё добавляют МСК-оффсет
        assertThat(String.format("<timestamp> и <utc_timestamp> не отличаются на %s час(а)", tz), timestamp,
                equalTo((utcTimestamp + HOURS.toSeconds(tz)) - HOURS.toSeconds(3)));

        MessageBody messageBody = messageBody(getMsg(mid)).via(hc);

        long timestampUtcMessageBody = messageBody.getTimestamp() / 1000;
        long timestampMessageBody = Long.parseLong(messageBody.getUserTimestamp()) / 1000;

        // делим на 10 из-за погрешности
        assertThat("<utc_timestamp> mailbox_list и message_body различаются",
                utcTimestamp / 100, equalTo(timestampUtcMessageBody / 100));

        assertThat("<timestamp> и <user_timestamp> mailbox_list и message_body различаются", timestamp / 100,
                equalTo(timestampMessageBody / 100));

        deleteOneMessageBySubjFromInboxOutbox(sendUtils.getSubj(), hc);
    }
}
