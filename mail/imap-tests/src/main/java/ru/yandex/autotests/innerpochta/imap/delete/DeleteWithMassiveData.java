package ru.yandex.autotests.innerpochta.imap.delete;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 09.04.14
 * Time: 18:38
 */
@Aqua.Test
@Title("Команда DELETE. Большие данные.")
@Features({ImapCmd.DELETE})
@Stories(MyStories.BIG_DATA)
@Description("Удаляем много папок. Работаем с двумя сессиями")
public class DeleteWithMassiveData extends BaseTest {
    private static Class<?> currentClass = DeleteWithMassiveData.class;
}
