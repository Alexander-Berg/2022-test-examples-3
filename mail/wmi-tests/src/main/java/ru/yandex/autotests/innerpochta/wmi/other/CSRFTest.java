package ru.yandex.autotests.innerpochta.wmi.other;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ComposeCheck;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.06.14
 * Time: 17:10
 * <p/>
 * <p/>
 * [DARIA-22595]
 */
@Aqua.Test
@Title("CSRF")
@Description("Не должны позволяет удалить письма авторизованного пользователя.(и др. модифиципующие операции)")
@Features(MyFeatures.WMI)
@Stories(MyStories.OTHER)
@Issue("DARIA-22595")
@Credentials(loginGroup = "CSRF")
public class CSRFTest extends BaseTest {

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().all();

    public static final String REFERER_HEADER = "Referer";
    public static final String ORIGIN_HEADER = "Origin";
    private String ckey;

    @Before
    public void getCkey() throws IOException {
        ckey = api(ComposeCheck.class).post().via(hc).getCKey();
    }

    @Test
    public void testWithoutHeaders() throws Exception {
        String mid = sendWith.waitDeliver().send().getMid();
        MailboxOperObj mbObj = MailboxOperObj.moveOneMsg(mid, folderList.defaultFID(), folderList.defaultFID());
        api(MailboxOper.class).params(mbObj).post().via(hc).resultIdOk();
    }

    @Description("Делаем модифиципующий запрос с двумя хедерами и c различными вариантами ckey и OAuth\n" +
            "Ожидаемый результат: должны увидеть запись в логе")
    @Test
    public void testWithHeaders() throws Exception {
        String mid = sendWith.waitDeliver().send().getMid();

        MailboxOperObj mbObj = MailboxOperObj.moveOneMsg(mid, folderList.defaultFID(), folderList.defaultFID());
        api(MailboxOper.class).header(REFERER_HEADER, Util.getRandomString()).header(ORIGIN_HEADER, Util.getRandomString())
                .params(mbObj).post().via(hc).yamail();

        //не должны увидеть запись в логе
        api(MailboxOper.class).header(REFERER_HEADER, Util.getRandomString()).header(ORIGIN_HEADER, Util.getRandomString())
                .params(mbObj.setCkey(ckey)).post().via(hc).resultIdOk();

        api(MailboxOper.class).header(REFERER_HEADER, Util.getRandomString()).header(ORIGIN_HEADER, Util.getRandomString())
                .params(mbObj).post().via(authClient.oAuth()).resultIdOk();

        api(MailboxOper.class).header(REFERER_HEADER, Util.getRandomString()).header(ORIGIN_HEADER, Util.getRandomString())
                .params(mbObj.setCkey(ckey)).post().via(authClient.oAuth()).resultIdOk();
    }

    @Description("Делаем модифиципующий запрос c Origin и c различными вариантами ckey и OAuth\n" +
            "Ожидаемый результат: должны увидеть запись в логе")
    @Test
    public void testWithOriginHeader() throws Exception {
        String mid = sendWith.waitDeliver().send().getMid();

        MailboxOperObj mbObj = MailboxOperObj.moveOneMsg(mid, folderList.defaultFID(), folderList.defaultFID());

        api(MailboxOper.class).header(ORIGIN_HEADER, Util.getRandomString())
                .params(mbObj).post().via(hc).yamail();

        //не должны увидеть запись в логе
        api(MailboxOper.class).header(ORIGIN_HEADER, Util.getRandomString())
                .params(mbObj.setCkey(ckey)).post().via(hc).resultIdOk();

        api(MailboxOper.class).header(ORIGIN_HEADER, Util.getRandomString())
                .params(mbObj).post().via(authClient.oAuth()).resultIdOk();

        api(MailboxOper.class).header(ORIGIN_HEADER, Util.getRandomString())
                .params(mbObj.setCkey(ckey)).post().via(authClient.oAuth()).resultIdOk();


    }

    @Description("Делаем модифиципующий запрос с двумя хедерами и c различными вариантами ckey и OAuth\n" +
            "Ожидаемый результат: должны увидеть запись в логе")
    @Test
    public void testWithRefererHeader() throws Exception {
        String mid = sendWith.waitDeliver().send().getMid();

        MailboxOperObj mbObj = MailboxOperObj.moveOneMsg(mid, folderList.defaultFID(), folderList.defaultFID());
        api(MailboxOper.class).header(REFERER_HEADER, Util.getRandomString())
                .params(mbObj).post().via(hc).yamail();

        //не должны увидеть запись в логе
        api(MailboxOper.class).header(REFERER_HEADER, Util.getRandomString())
                .params(mbObj.setCkey(ckey)).post().via(hc).resultIdOk();

        api(MailboxOper.class).header(REFERER_HEADER, Util.getRandomString())
                .params(mbObj).post().via(authClient.oAuth()).resultIdOk();

        api(MailboxOper.class).header(REFERER_HEADER, Util.getRandomString())
                .params(mbObj.setCkey(ckey)).post().via(authClient.oAuth()).resultIdOk();
    }
}