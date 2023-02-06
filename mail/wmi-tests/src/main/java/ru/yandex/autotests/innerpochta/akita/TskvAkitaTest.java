package ru.yandex.autotests.innerpochta.akita;

import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.akita.auth.ApiAuth;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreSshTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.LogCollectRule;
import ru.yandex.autotests.testpers.ssh.SSHAuthRule;
import ru.yandex.qatools.allure.annotations.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.ssh.TSKVLogMatcher.logEntryShouldMatch;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.LogCollectRule.withGrepAllLogsFor;
import static ru.yandex.autotests.testpers.ssh.SSHAuthRule.sshOn;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;
import static ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder.should;

@Aqua.Test
@Title("[LOGS] TSKV лог для akita")
@Description("tskv лог - " + ru.yandex.autotests.innerpochta.akita.TskvAkitaTest.TSKV_LOG_PATH)
@Features({MyFeatures.AKITA, MyFeatures.LOGGING})
@Stories({"TSKV", MyStories.LOGS})
@Credentials(loginGroup = "AkitaAuth")
@IgnoreSshTest
public class TskvAkitaTest extends AkitaBaseTest {

    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(URI.create(props().akitaUri()), props().getRobotGerritWebmailTeamSshKey());

    @Rule
    public LogCollectRule logs = withGrepAllLogsFor(sshAuthRule);

    static final String TSKV_LOG_PATH = "/app/log/http_client.tskv";

    @Test
    @Title("При emails=yes получаем все адреса")
    public void shouldGetAllEmailsWithSpecialParam() {
        auth()
                .withEmails(ApiAuth.EmailsParam.YES)
                .get(shouldBe(okAuth()));


        logLineShouldMatch(allEmails());
    }

    @Test
    @Title("При emails=no получаем яндексовые адреса")
    public void shouldGetOnlyYandexEmailsWithSpecialParam() {
        auth()
                .withEmails(ApiAuth.EmailsParam.NO)
                .get(shouldBe(okAuth()));


        logLineShouldMatch(onlyYandexEmails());
    }

    @Test
    @Title("Без emails получаем яндексовые адреса")
    public void shouldGetOnlyYandexEmailsWithoutAnyParam() {
        auth()
                .get(shouldBe(okAuth()));


        logLineShouldMatch(onlyYandexEmails());
    }

    private Matcher<Map<? extends String, ? extends String>> onlyYandexEmails() {
        return hasEntry(is("uri"), containsString("emails=getyandex"));
    }

    private Matcher<Map<? extends String, ? extends String>> allEmails() {
        return hasEntry(is("uri"), containsString("emails=getall"));
    }

    @Step("[SSH]: Должны увидеть тип адресов из ЧЯ в логе")
    private static void logLineShouldMatch(Matcher<Map<? extends String, ? extends String>> matcher) {
        List<Matcher<Map<? extends String, ? extends String>>> logMatchers = Collections.singletonList(matcher);

        assertThat(sshAuthRule.ssh().conn(),
                should(logEntryShouldMatch(logMatchers, TSKV_LOG_PATH, props().getCurrentRequestId()))
                        .whileWaitingUntil(
                                timeoutHasExpired(3000)
                                        .withPollingInterval(500))
        );
    }
}

