package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.DariaMessagesObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.DariaMessages;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.RecentFolderMatcher.hasRecent;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.RecentFolderMatcher.notHasRecent;

@Aqua.Test
@Title("Папки. Проверка тега recent")
@Description("Получаем письмо, отмечаем папку прочитанной, смотрим все время на recent тег")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "RecentFolder")
public class RecentFolderTest extends BaseTest {

    @Before
    public void clearRecent() throws Exception {
        //обнуляем recent
        jsx(DariaMessages.class).params(DariaMessagesObj.getObjCurrFolder(folderList.defaultFID())).post().via(hc);
        assertThat("Значение recent не обнулилось", hc, withWaitFor(notHasRecent(folderList.defaultFID())));
        // Отправка письма
        sendWith.viaProd().send("RecentFolder::markFolderSeen()").send();
    }

    @Test
    @Issue("MAILPG-477")
    @Title("Таг recent")
    @Description("Тестирование функции \"смотрели ли на папку после получения в нее новых писем\"\n" +
            "Отправляем и получаем письмо\n" +
            "- Смотрим что тег recent стал равен 1\n" +
            "Помечаем папку прочитанной\n" +
            "- Смотрим что тег стал равен 0")
    public void shouldNotSeeRecentValue() throws Exception {
        assertThat("Значение recent у папки не изменилось до истечения таймаута. Либо письма не дошло. " +
                        "Recent не должен быть > 1 [MAILPG-477]",
                hc, withWaitFor(hasRecent(folderList.defaultFID()), SECONDS.toMillis(60)));

        //recent должен сбрасывать либо запросом списка писем или тредов
        jsx(DariaMessages.class).params(DariaMessagesObj.getObjCurrFolder(folderList.defaultFID())).post().via(hc);
        jsx(DariaMessages.class).params(DariaMessagesObj.getObjCurrFolder(folderList.defaultFID()).threaded()).post().via(hc);

        assertThat("Значение recent не обнулилось", hc, notHasRecent(folderList.defaultFID()));
    }
}
