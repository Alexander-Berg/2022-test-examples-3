package ru.yandex.autotests.innerpochta.api;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;

@Aqua.Test
@Title("[API] Удаление писем методом API")
@Description("Отсылаем несколько писем, удаляем их, передавая " +
                "список писем либо массивом идс, либо через запятую")
@Credentials(loginGroup = "DeleteByApi")
@Features(MyFeatures.API_WMI)
@Stories(MyStories.LETTERS)
public class DeleteMailsTest extends BaseTest {

    private String subj;

    private List<String> mids;

    private static final int COUNT_OF_MSG = 2;

    @Before
    public void prepare() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_MSG).send();
        subj = sendUtils.getSubj();
        mids = sendUtils.getMids();
    }

    @Test
    @Description("Проверяем ручку MailboxOper с удалением\n" +
            "Удаляем письма, используя несколько ids\n" +
            "с помощью API")
    public void testDeleteWithNotOneIds() throws Exception {
        logger.warn("Тестируем mailbox_oper на удаление, удаляем, используя несколько ids");
        MailboxOperObj mbObj = MailboxOperObj.deleteMsges(mids);
        api(MailboxOper.class).params(mbObj).post().via(hc);

        MailBoxList respAfter = api(MailBoxList.class)
                .post().via(hc);
        assertFalse("Похоже, письма не удалились", respAfter.isThereMessage(subj));
    }

    @Test
    @Description("Проверяем ручку MailboxOper с удалением\n" +
            "Удаляем письма, используя один ids\n" +
            "с помощью API")
    public void testDeleteWithOneIds() throws Exception {
        logger.warn("Тестируем mailbox_oper на удаление. Удаляем, используя один ids, " +
                "перечисляя миды через запятую");
        MailboxOperObj mbObj = MailboxOperObj
                .deleteOneMsg(mids.get(0) + "," + mids.get(1));
        api(MailboxOper.class).params(mbObj).post().via(hc);

        MailBoxList respAfter = api(MailBoxList.class)
                .post().via(hc);
        assertFalse("Похоже, письма не удалились", respAfter.isThereMessage(subj));
    }
}