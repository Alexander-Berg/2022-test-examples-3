package ru.yandex.autotests.innerpochta.wmi.logs;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Headers;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.CountersObject;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.junitextensions.rules.retry.RetryRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.OPERATION;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.TEST_BUCKETS;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.*;
import static ru.yandex.autotests.innerpochta.beans.tskv.TargetTskv.*;
import static ru.yandex.autotests.innerpochta.beans.tskv.OperTskv.*;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.ssh.TSKVLogMatcher.logEntryShouldMatch;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ResetFreshCounter.resetFreshCounter;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;
import static ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder.should;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 11.02.16
 * Time: 15:25
 */
@Aqua.Test
@Title("[LOGS] TSKV лог для hound")
@Description("tskv hound лог - " + TskvHoundTest.TSKV_HOUND_LOG_PATH)
@Features({MyFeatures.HOUND, MyFeatures.LOGGING})
@Stories({MyStories.JOURNAL, "TSKV", MyStories.LOGS})
@Credentials(loginGroup = "TskvMopsTest")
@Issues({@Issue("DARIA-53895")})
public class TskvHoundTest extends BaseTest {

    @Rule
    public RetryRule retry = RetryRule.retry().ifException(IndexOutOfBoundsException.class)
            .times(5).every(5, TimeUnit.SECONDS);

    public static final String TSKV_HOUND_LOG_PATH = "/var/log/hound/user_journal.tskv";

    private Matcher<Map<? extends String, ? extends String>> entry(WmiTskv key, Matcher<String> value) {
        return hasEntry(is(key.toString()), value);
    }

    @Test
    @Issue("DARIA-53895")
    @Title("Пробрасываем X-Yandex-ExpBoxes в /var/log/hound/user_journal.tskv")
    public void shouldWriteExpInHound() throws IOException, InterruptedException {
        String exp = "experiment_" + Util.getRandomString();
        resetFreshCounter(CountersObject.empty().setUid(composeCheck.getUid())).header(Headers.EXP_BOXES, exp)
                .get().via(hc).shouldBe().ok();
        shouldSeeLogLine(getResetFreshLogMatcher(exp), exp, 0);
    }

    @Test
    @Issue("MAILDEV-331")
    @Title("Проверяем, что X-Yandex-ClientType прокидывается в hound")
    public void shouldWriteClientTypeInHound() {
        String exp = Util.getRandomString();
        resetFreshCounter(CountersObject.empty().setUid(composeCheck.getUid())).header(Headers.CLIENT_TYPE, exp)
                .get().via(hc).shouldBe().ok();

        shouldSeeLogLine(getHeadLogMatcher(CLIENT_TYPE, exp), exp, 0);
    }

    public List<Matcher<Map<? extends String, ? extends String>>> getResetFreshLogMatcher(String exp) {
        return newArrayList(entry(OPERATION, equalTo(RESET_FRESH.toString())),
                entry(TARGET, containsString(MAILBOX.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(TEST_BUCKETS, containsString(exp)));
    }

    public List<Matcher<Map<? extends String, ? extends String>>> getHeadLogMatcher(WmiTskv header, String exp) {
        return newArrayList(entry(OPERATION, equalTo(RESET_FRESH.toString())),
                entry(TARGET, containsString(MAILBOX.toString())),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId())),
                entry(header, containsString(exp)));
    }


    @Step("[SSH]: Должны увидеть в логе {0}, грепая по {1} (номер записи {2})")
    public static void shouldSeeLogLine(List<Matcher<Map<? extends String, ? extends String>>> logMatchers, String grep, Integer entry) {
        assertThat(sshAuthRule.ssh().conn(), should(logEntryShouldMatch(logMatchers, TSKV_HOUND_LOG_PATH, grep, entry))
                .whileWaitingUntil(timeoutHasExpired(3000).withPollingInterval(500)));
    }
}