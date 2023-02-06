package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark.StatusParam;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.*;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static java.util.function.Function.identity;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.02.15
 * Time: 16:00
 */
@Aqua.Test
@Title("[MOPS] RO и MOPS")
@Description("Проеряем, что в случае RO оракла активируется асинхронные операции")
@Features(MyFeatures.MOPS)
@Stories(MyStories.RO)
@Issues({@Issue("DARIA-42195"), @Issue("DARIA-49322")})
@Ignore("MAILDEV-905")
@Credentials(loginGroup = "ROMopsTest")
public class ROMopsTest extends MopsBaseTest {
    private static String mid = "2170000000023104543";

    @BeforeClass
    public static void prepare() throws IOException {
//        assumeThat("mdb000 не в RO режиме", api(YamailStatus.class).post().via(authClient.authHC()).getDbStatus(),
//                equalTo("ro"));
    }

    @Test
    @Description("Делаем в RO пометку прочитанным непрочитанным, смотрим STAT")
    public void testReadUnreadRo() throws Exception {
        mark(new MidsSource(mid), StatusParam.NOT_READ).post(shouldBe(okSync()));
        shouldSeeTaskInStat("mark");
    }

    @Test
    @Description("Делаем в RO пометку спамом неспамом, смотрим STAT")
    public void testSpamUnspamRo() throws Exception {
        val source = new MidsSource(mid);

        spam(source).post(shouldBe(okSync()));
        shouldSeeTaskInStat("spam");

        unspam(source).post(shouldBe(okSync()));
        shouldSeeTaskInStat("unspam");
    }

    @Test
    @Description("Делаем в RO перемещение в черновики смотрим STAT")
    public void testMoveRo() throws Exception {
        complexMove(folderList.draftFID(), new MidsSource(mid)).post(shouldBe(okSync()));
        shouldSeeTaskInStat("move");
    }

    @Test
    @Description("Делаем в RO удаление смотрим STAT")
    public void testRemoveRo() throws Exception {
        remove(new MidsSource(mid)).post(shouldBe(okSync()));
        shouldSeeTaskInStat("trash");
    }

    private void shouldSeeTaskInStat(String type) throws IOException {
        val response = Mops.stat(authClient).get(identity());
        assertTaskWithType(response, type);
    }

    @Test
    @Ignore
    public void shouldSeeStat() throws IOException {
        Mops.stat(authClient).get(identity());
    }
}
