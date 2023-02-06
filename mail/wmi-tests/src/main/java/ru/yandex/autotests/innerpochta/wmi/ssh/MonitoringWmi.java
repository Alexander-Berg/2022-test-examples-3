package ru.yandex.autotests.innerpochta.wmi.ssh;

import gumi.builders.UrlBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.*;
import ru.yandex.qatools.allure.model.SeverityLevel;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.ssh.SSHCoresMatcher.hasNoCoreDumps;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 9/12/13
 * Time: 7:31 PM
 */
@Aqua.Test
@Title("[SSH] Тестирование состояния машинки")
@Description("Различные тесты для отслеживания состояния машинки и логов")
@Features(MyFeatures.WMI)
@Stories(MyStories.SSH)
public class MonitoringWmi extends BaseTest {
    public void mbodyStarted() throws IOException {
        assertThat("Message_body не запущен", sshAuthRule.ssh().cmd("ps auxf | grep -v 'grep' | grep mbody"),
                allOf(containsString("wmi"),
                        containsString("/etc/mbody/mbody.conf"),
                        containsString("/usr/bin/mbody")));
    }

    // TODO: оторвать rhel-специфичные темы
    // здесь это проверка, установлен ли simple_jsx через rpm и dpkg
    // в других тестах - то, что baida в /usr/sbin, а не в /usr/bin
    @Test
    @Title("Должен быть simple_jsx")
    @Severity(SeverityLevel.BLOCKER)
    public void simpleJsxShouldBeInstalled() throws Exception {
        String simpleJsxOnRhel = sshAuthRule.ssh().cmd("rpm -q simple_jsx");
        String simpleJsxOnUbuntu = sshAuthRule.ssh().cmd("dpkg -l | grep simple-jsx");
        String sshResults = String.format("%s %s", simpleJsxOnRhel, simpleJsxOnUbuntu);
        assertThat("simple-jsx не найден ",
                sshResults, anyOf(containsString("simple-jsx"), containsString("simple_jsx")));
    }

    @Test
    @Description("Проверяем, что запущен nginx")
    @Severity(SeverityLevel.BLOCKER)
    public void nginxShouldBeStarted() throws Exception {
        assertThat("Nginx не запущен ", sshAuthRule.ssh().cmd("ps auxf | grep -v 'grep' | grep nginx"),
                containsString("/usr/sbin/nginx"));
    }

    @Test
    @Description("Проверяем, что запущена baida\n" +
            "Проверяем также, что она запущена не под рутом.\n" +
            "DARIA-23866")
    @Issue("DARIA-23866")
    @Severity(SeverityLevel.BLOCKER)
    public void baidaShouldBeStarted() throws Exception {
        assertThat("Baida не запущена или запущена из-под root [DARIA-23866]",
                sshAuthRule.ssh().cmd("ps auxf | grep -v 'grep' | grep baida"),
                allOf(containsString("wmi"),
                        anyOf(containsString("/usr/bin/baida"), containsString("/usr/sbin/baida")),
                        not(containsString("root"))));
    }


    @Test
    @Description("Проверяем, что запущена hound\n" +
            "Проверяем также, что она запущена не под рутом.")
    @Issue("DARIA-53369")
    @Severity(SeverityLevel.BLOCKER)
    public void houndShouldBeStarted() throws Exception {
        assertThat("hound не запущена или запущена из-под root [DARIA-23866]",
                sshAuthRule.ssh().cmd("ps auxf | grep -v 'grep' | grep hound"),
                allOf(containsString("wmi"),
                        anyOf(containsString("/usr/bin/hound"), containsString("/usr/sbin/hound")),
                        not(containsString("root"))));
    }

    @Test
    @Title("Не должно быть корок")
    @Description("Корки лежат в /var/cores/")
    public void shouldBeNoCores() throws Exception {
        assertThat("Найдены корки ", sshAuthRule.ssh().conn(), hasNoCoreDumps());
    }

    @Test
    @Description("MBODY должен быть запущен")
    @Severity(SeverityLevel.BLOCKER)
    public void mbodyShouldBeStarted() throws Exception {
        mbodyStarted();
    }

    @Test
    @Issue("DARIA-27711")
    @Title("Должен быть пинг на [:9090] порту")
    public void pingYmodHttpServerShouldSeePong() throws Exception {
        logger.warn("[DARIA-27711]");
        assertThat("ymod_httpserver почему-то не отвечает ",
                sshAuthRule.ssh().cmd(String.format("wget -q -O - '%s/ping'", UrlBuilder.fromString(props().betaHost())
                        .withScheme("http").withPort(9090).toString())), CoreMatchers.containsString("pong"));
    }

    @Test
    @Issue("DARIA-42969")
    @Title("Проверка service wmi reload")
    @Description("2.11.14 RELOAD процесса wmi приводил к его остановке.\n" +
            "Релоудим процесс, проверяем, что он запущен")
    @Severity(SeverityLevel.BLOCKER)
    public void testReloadWmiShouldBeStarted() throws IOException {
        sshAuthRule.ssh().cmd("service wmi reload");
        //        exec("service mops reload");
        sshAuthRule.ssh().cmd("service mbody reload");

        assertThat("Baida не запущен", sshAuthRule.ssh().cmd("ps auxf | grep -v 'grep' | grep baida"),
                allOf(containsString("wmi"),
                        anyOf(containsString("/usr/bin/baida"), containsString("/usr/sbin/baida")),
                        not(containsString("root"))));

        mbodyStarted();
    }
}