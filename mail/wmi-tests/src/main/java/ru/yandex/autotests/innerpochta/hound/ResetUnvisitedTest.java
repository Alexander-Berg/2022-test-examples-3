package ru.yandex.autotests.innerpochta.hound;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.ResetUnvisitedObj;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.innerpochta.hound.data.InvalidArgumentConsts.NOT_EXIST_PARAM;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.RecentFoldersMatcher.hasRecent;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.UnvisitedFoldersMatcher.notHasUnvisited;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ResetUnvisited.resetUnvisitedFlag;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 28.09.15
 * Time: 19:18
 */
@Aqua.Test
@Title("[HOUND] Ручка сброса флага recent у папки reset_recent_flag")
@Description("Различные кейсы на ручку")
@Features(MyFeatures.HOUND)
@Stories(MyStories.OTHER)
@Issues({@Issue("DARIA-51493"), @Issue("MAILPG-477")})
@Credentials(loginGroup = "Mobcorpava")
public class ResetUnvisitedTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Issue("MAILPG-477")
    @Title("Сброс флага в папке Входящие")
    public void resetRecentFlagWithDefaultFid() throws Exception {
        sendWith(authClient).viaProd().text("ResetRecentFlagTest:resetRecentFlagWithDefaultFid").send();
        Assert.assertThat("Значение recent у папки не изменилось до истечения таймаута. Либо письмо не дошло",
                authClient, withWaitFor(hasRecent(folderList.defaultFID()), SECONDS.toMillis(60)));

        resetUnvisitedFlag(ResetUnvisitedObj.empty().setUid(uid()).setFid(folderList.defaultFID())).get().via(authClient)
                .ok();

        Assert.assertThat("Значение recent обнулилось", authClient, hasRecent(folderList.defaultFID()));
        Assert.assertThat("Значение unvisited не обнулилось", authClient, notHasUnvisited(folderList.defaultFID()));
    }

    @Test
    @Title("Запрос reset_recent_flag с несуществующим фидом")
    public void resetRecentFlagWithWrongFid() {
        resetUnvisitedFlag(ResetUnvisitedObj.empty().setUid(uid())
                .setFid(NOT_EXIST_PARAM)).get().via(authClient).codeShouldBe(WmiConsts.WmiErrorCodes.NO_SUCH_FOLDER_5002)
                .messageShouldBe(equalTo("no such folder")).reasonShouldBe(equalTo("no such folder fid=" + NOT_EXIST_PARAM));
    }

    @Test
    @Title("Запрос reset_recent_flag без фида")
    public void resetRecentFlagWithoutFid() {
        resetUnvisitedFlag(ResetUnvisitedObj.empty().setUid(uid())).get().via(authClient)
                .codeShouldBe(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001)
                .messageShouldBe(equalTo("invalid argument")).reasonShouldBe(equalTo("fid parameter is required"));
    }
}
