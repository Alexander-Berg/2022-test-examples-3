package ru.yandex.autotests.innerpochta.wmi.ssh;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


@Aqua.Test
@Title("[SSH] Тестирование сервиса wmi по ssh, используя команду @devamax")
@Description("Тест по ssh, командой от devamax@ - выводит ок или bad")
@Features(MyFeatures.WMI)
@Stories(MyStories.SSH)
public class TestWMIService extends BaseTest {

    public static final String LOG_NAME = "~/testWMIservise.log";

    @Test
    @IgnoreForPg
    @Issue("WMI-839")
    @Description("Проверка статистики на порту 8081")
    public void testStats() throws Exception {
        logger.warn("[WMI-839]");
        String command = "echo -ne \"m\" | nc localhost 8081";
        String resp = sshAuthRule.ssh().cmd(command);
        assertThat("Информация о DBPOOL не отображается или отображается неправильно ",
                resp, containsString("=== DBPOOL info =="));
        assertThat("Сломалась статистика на порту 8081 ", resp, containsString("STATS"));
    }

    @Test
    @Issues({@Issue("WMI-311"), @Issue("DARIA-30507"), @Issue("DARIA-53369")})
    @Description("Выполняет команду command, проверяет что нет ответа \"bad\"\n" +
            "Автор команды devamax@\n" +
            "Если bad находит, то тест провален\n" +
            "Заменили все вхождения fgrep -c 'null' на egrep -c \"null|error.log\" в конфиге 194*\n" +
            "DARIA-30507\n" +
            "WMI-311")
    public void isWMIServiceOk() throws Exception {
        String command = "pid=`sudo ps axufww| sudo fgrep baida| sudo fgrep hound| sudo fgrep wmi| sudo fgrep -v fgrep| sudo awk '{print $2}'`; " +
                "num=`sudo ls -la /proc/$pid/fd/{0,1,2}| sudo egrep -c \"null|error.log\"`; if [ \"$num\" -ne \"3\" ]; " +
                "then echo bad; " +
                "else echo ok; fi >" + LOG_NAME;

        sshAuthRule.ssh().cmd(command);
        //waitUntilWritingLog();
        String command2 = "sudo cat " + LOG_NAME;
        assertTrue("Something BAD. All questions to devamax@ or ognemuh@ about cmd " + command, isAllOk(command2));
    }

    /**
     * Ищет bad в ответе
     *
     * @param command команда
     * @return true если не нашел bad
     * @throws Exception всё
     */
    private boolean isAllOk(String command) throws Exception {
        String resp = sshAuthRule.ssh().cmd(command);
        return !resp.contains("bad");
    }
}

