package ru.yandex.autotests.innerpochta.hound;

import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.CountersObject.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FoldersCounters.foldersCounters;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Проверяем выдачу ручки folders_counters")
@Features(MyFeatures.HOUND)
@Stories(MyStories.FOLDERS)
@Issues({@Issue("MAILPG-1685")})
@Credentials(loginGroup = "FoldersCountersTest")
public class FoldersCountersTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient).before(true).allfolders();

    @Test
    @Title("FoldersCounters при операциях с письмами")
    @Description("Присылаем и читаем письма, проверяем при этом счётчики")
    public void testWithEmailLifeCycle() throws Exception {
        final String fid = folderList.defaultFID();
        checkFolderCounters(0, 0, fid);

        List<String> mids = sendWith(authClient).viaProd().count(2).send().waitDeliver().getMids();
        assertThat("Ожидаем два письма", mids.size(), equalTo(2));
        checkFolderCounters(2, 2, fid);

        Mops.mark(authClient, new MidsSource(mids.get(0)), ApiMark.StatusParam.READ)
                .post(shouldBe(okSync()));
        checkFolderCounters(1, 2, fid);

        Mops.purge(authClient, new MidsSource(mids.get(1)))
                .post(shouldBe(okSync()));
        checkFolderCounters(0, 1, fid);
    }

    @Test
    @Title("FoldersCounters с пустым uid")
    @Description("Проверяем ручку folders_counters с пустым переданным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void testWithEmptyUid() {
        foldersCounters(empty().setUid("")).get().via(authClient)
                .withDebugPrint()
                .statusCodeShouldBe(HttpStatus.SC_OK)
                .code(equalTo(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001.code()))
                .message(equalTo("invalid argument"))
                .reason(equalTo("uid parameter is required"));
    }

    private void checkFolderCounters(int newCount, int count, String fid) {
        foldersCounters(empty().setUid(uid())).get().via(authClient)
                .statusCodeShouldBe(HttpStatus.SC_OK)
                .expectNewMessagesInFolder(newCount, fid)
                .expectMessagesInFolder(count, fid);
    }
}
