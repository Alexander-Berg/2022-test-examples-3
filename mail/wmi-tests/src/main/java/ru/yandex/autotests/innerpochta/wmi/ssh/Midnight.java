package ru.yandex.autotests.innerpochta.wmi.ssh;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;


/**
 * Нужен пользователь на машинке.
 * по умолчанию
 * useradd test
 * passwd test
 * qwerty
 */
@Aqua.Test
@Features(MyFeatures.WMI)
@Stories(MyStories.SSH)
@Title("[SSH] Проверка отсутствия перезагрузок wmi в полночь")
@Description("АХТУНГ! Меняет время на машине. " +
        "Ходит по ssh на машинку и узнает что перезагрузок в полночь не было")
public class Midnight extends BaseTest {

    @Test
    @Description(
            "Стопим синхронизатор времени\n" +
                    "Переводим время далеко в прошлое\n" +
                    "Смотрим аптайм wmi\n" +
                    "Ждем 2 минуты и сравнываем с новым аптаймом\n" +
                    "Новый аптайм должен быть больше или равен старому"
    )
    public void checkWmiNotRebootingAtMidnight() throws Exception {
        sshAuthRule.ssh().cmd("/etc/init.d/ntpd stop");

        sshAuthRule.ssh().cmd("date 0530235911");

        long upTimeBefore = getUpTime("ps ax | fgrep wmi");
        logger.info(upTimeBefore);
        Thread.sleep(120000);
        long upTimeAfter = getUpTime("ps ax | fgrep wmi");
        logger.info(upTimeAfter);

        sshAuthRule.ssh().cmd("ntpdate pool.ntp.org");
        sshAuthRule.ssh().cmd("/etc/init.d/ntpd start");

        assertThat("Похоже на то, что перезагрузка была", upTimeAfter, greaterThanOrEqualTo(upTimeBefore));
    }

    @Step("Парсим аптайм из команды {0}")
    private long getUpTime(String command) throws Exception {
        String upTime = sshAuthRule.ssh().cmd(command);
        int i = upTime.indexOf("/usr/");
        String test = upTime.substring(i - 6, i).trim().replaceAll(":", "");
        return Long.parseLong(test);
    }
}

