package ru.yandex.autotests.innerpochta.mbody;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreSshTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendmessage.ApiSendMessage;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.testpers.ssh.SSHAuthRule;
import ru.yandex.qatools.allure.annotations.*;

import static com.google.common.collect.Lists.newArrayList;

import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.*;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.ssh.TSKVLogMatcher.logEntryShouldMatch;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.MessagesWithInlines.getSmileHtml;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;
import static ru.yandex.autotests.testpers.ssh.SSHAuthRule.sshOn;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;
import static ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder.should;

@Aqua.Test
@Title("Тесты на логи mbody")
@Features(MyFeatures.MBODY)
@Stories(MyStories.MBODY)
@Credentials(loginGroup = "MbodyLogTest")
@IgnoreSshTest
public class LogTest extends MbodyBaseTest {
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(URI.create(props().mbodyUri()), props().getRobotGerritWebmailTeamSshKey());

    private static String stid;
    private static String mid;

    @BeforeClass
    public static void prepare() throws Exception {
        CleanMessagesMopsRule.with(authClient).allfolders();
        Envelope envelope = sendWith(authClient)
                .viaProd()
                .text(getSmileHtml(Util.getRandomShortInt()) + "\nhttp://ya.ru")
                .html(ApiSendMessage.HtmlParam.YES.value())
                .send()
                .waitDeliver()
                .getEnvelope().orElse(null);

        assertNotNull("Не нашли отправленное письмо", envelope);

        mid = envelope.getMid();
        stid = envelope.getStid();
    }

    @Before
    public void setUp() {
        Validate.notBlank(stid, "STID не был получен из mdbdir");
    }

    private Matcher<Map<? extends String, ? extends String>> entry(WmiTskv key, Matcher<String> value) {
        return hasEntry(is(key.toString()), value);
    }

    @Test
    @Issue("MAILDEV-670")
    @Title("Проверяем stid в mbody phishing.tskv")
    public void shouldSeeATimestampInPhishingLog() throws Exception {
        final String phishingPattern = sshAuthRule.ssh()
                .cmd("head -1 /app/config/phishing.regexp")
                .replace("\\", "");
        assertNotEquals("Паттерн фишинга не должен быть пустым", phishingPattern, "");

        Envelope envelope = sendWith(authClient)
                .viaProd()
                .text(phishingPattern)
                .saveDraft()
                .waitDeliver()
                .getEnvelope().orElse(null);

        assertNotNull("Не нашли отправленное письмо", envelope);

        mid = envelope.getMid();
        stid = envelope.getStid();

        apiMbody().message()
                .withMid(mid)
                .withUid(uid())
                .get(identity()).peek().as(Mbody.class);

        assertThat("Запись должна содержать stid",
                sshAuthRule.ssh().conn(), should(logEntryShouldMatch(getLogMatcher(STID, stid), "/app/log/phishing.tskv", stid, 0))
                .whileWaitingUntil(timeoutHasExpired(5000).withPollingInterval(500)));
    }

    public List<Matcher<Map<? extends String, ? extends String>>> getLogMatcher(WmiTskv header, String exp) {
        return newArrayList(entry(header, equalTo(exp)));
    }
}
