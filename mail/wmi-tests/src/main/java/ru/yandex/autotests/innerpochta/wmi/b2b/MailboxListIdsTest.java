package ru.yandex.autotests.innerpochta.wmi.b2b;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.hound.HoundB2BTest;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxListNearestObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.allure.annotations.*;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxListIdsObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList.mailboxListJsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxListIds.mailboxListIdsJsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxListNearest.mailboxListNearestJsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.NearestMessages.nearestMessages;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 16.09.15
 * Time: 15:02
 *
 * Используем зоопарк. Нельзя удалять письма.
 */
@Aqua.Test
@Title("[B2B] Тестирование mailbox_list_ids и mailbox_list_nearest")
@Description("Сравнение с выводом mailbox_list_ids")
@Features(MyFeatures.WMI)
@Stories({MyStories.B2B, MyStories.MESSAGES_LIST})
@Credentials(loginGroup = "ZooNew")
@Issue("MAILPG-128")
public class MailboxListIdsTest extends BaseTest {

    public static final String DEVIATION = "2";
    public static final String DEFAULT_DEVIATION = "1";
    public static final String NOT_EXIST_MID = "";

    @Test
    @Title("Сравниваем выдачу mailbox_list_ids с пустыми параметрами")
    public void compareMailboxListIdsWithoutParams() {
        Oper respTest = mailboxListIdsJsx().post().via(hc);
        respTest.setHost(props().b2bUri().toString());
        Oper respProd = respTest.post().via(hc);
        assertThat(respTest.toDocument(), equalToDoc(respProd.toDocument()));
    }

    @Test
    @Title("Сравниваем выдачу mailbox_list_ids с одним мидом")
    public void compareMailboxListIdsWithOneIds() {
        String mid = mailboxListJsx().post().via(hc).getMidOfFirstMessage();
        Oper respTest = mailboxListIdsJsx(empty().setIds(mid)).post().via(hc).withDebugPrint();

        respTest.setHost(props().b2bUri().toString());
        Oper respProd = respTest.post().via(hc).withDebugPrint();
        assertThat(respTest.toDocument(), equalToDoc(respProd.toDocument()));
    }

    @Test
    @Title("Сравниваем выдачу mailbox_list_ids с одним мидами")
    public void compareMailboxListIdsWithMoreIds() {
        List<String> mids = mailboxListJsx().post().via(hc).getMidsOfMessagesInFolder().subList(0, 2);
        Oper respTest = mailboxListIdsJsx(empty().setIds(mids)).post().via(hc);


        respTest.setHost(props().b2bUri().toString());
        Oper respProd = respTest.post().via(hc);
        assertThat(respTest.toDocument(), equalToDoc(respProd.toDocument()));
    }


    @Test
    @Title("Ручка mailbox_list_ids с tid-ом")
    @Description("В логе должна быть такая ошибка:" +
            "[16/09/2015:14:24:03 +0300] mail.wmi[140249484580608]: (137754803:406546739:mdb170) " +
            "MailboxListIdsTest.a5Ion mailbox_list_ids status=31 hash=99912df73e643d1cbebc48ff6def075d " +
            "reason=[Internal error (Argument 'tids' not supported anymore)]")
    public void mailboxListIdsWithTidShouldSeeError() {
        mailboxListIdsJsx(empty().setTids(String.valueOf(Util.getRandomShortInt()))).post().via(hc)
                .errorcode(WmiConsts.WmiErrorCodes.INTERNAL_ERROR_31);
    }

    @Test
    @Title("Ручка mailbox_list_ids с current_folder")
    @Description("В логе должна быть такая ошибка:" +
            "[16/09/2015:14:31:28 +0300] mail.wmi[140249485108992]: (137754803:406546739:mdb170) " +
            "MailboxListIdsTest.102IV mailbox_list_ids status=31 hash=d4076272d175b846cec03ea326cb1825 " +
            "reason=[Internal error (Argument 'current_folder' not supported anymore)]")
    public void mailboxListIdsWithCurrentFolderShouldSeeError() {
        mailboxListIdsJsx(empty().setCurrentFolder(String.valueOf(Util.getRandomShortInt()))).post().via(hc)
                .errorcode(WmiConsts.WmiErrorCodes.INTERNAL_ERROR_31);
    }

    @Test
    @Title("Ручка mailbox_list_ids с sort_type")
    @Description("В логе должна быть такая ошибка:" +
            "[16/09/2015:14:32:02 +0300] mail.wmi[140249514698496]: (137754803:406546739:mdb170) " +
            "MailboxListIdsTest.bNilI mailbox_list_ids status=31 hash=14702082eff9068c6e7b03bb74ceb1f4 " +
            "reason=[Internal error (Argument 'sort_type' not supported anymore)]")
    public void mailboxListIdsWithSortTypeShouldSeeError() {
        mailboxListIdsJsx(empty().setSortType(String.valueOf(Util.getRandomShortInt()))).post().via(hc)
                .errorcode(WmiConsts.WmiErrorCodes.INTERNAL_ERROR_31);
    }

    @Test
    @Title("Ручка mailbox_list_nearest без письма после")
    @Issue("DARIA-52838")
    public void mailboxListNearestWithFistMidTest() {
        String mid = mailboxListJsx().post().via(hc).getMidOfFirstMessage();
        Oper respTest = mailboxListNearestJsx(MailboxListNearestObj.empty()
                .setSetSortTypeDate()
                .setDeviation(DEFAULT_DEVIATION)
                .setCurrentFolder(folderList.defaultFID()).setIds(mid)).post().via(hc)
                //нет письма, который после
                .countShouldBe(2);

        respTest.setHost(props().b2bUri().toString());
        Oper respProd = respTest.post().via(hc);

        assertThat(respTest.toDocument(), equalToDoc(respProd.toDocument()));
    }

    @Test
    @Title("Ручка mailbox_list_nearest")
    @Issue("DARIA-52838")
    public void mailboxListNearestTest() {
        List<String> mids = mailboxListJsx().post().via(hc).getMidsOfMessagesInFolder();
        String mid = mids.get(mids.size() / 2);

        Oper respTest = mailboxListNearestJsx(MailboxListNearestObj.empty().setSetSortTypeDate()
                .setDeviation(DEFAULT_DEVIATION)
                .setCurrentFolder(folderList.defaultFID()).setIds(mid)).post().via(hc)
                .countShouldBe(2 * Integer.parseInt(DEFAULT_DEVIATION) + 1);

        respTest.setHost(props().b2bUri().toString());

        Oper respProd = respTest.post().via(hc);

        assertThat(respTest.toDocument(), equalToDoc(respProd.toDocument()));
    }

    @Test
    @Title("Ручка mailbox_list_nearest c параметром deviation")
    @Issue("DARIA-52838")
    public void mailboxListNearestDeviationTest() {
        List<String> mids = mailboxListJsx().post().via(hc).getMidsOfMessagesInFolder();
        String mid = mids.get(mids.size() / 2);

        Oper respTest = mailboxListNearestJsx(MailboxListNearestObj.empty().setSetSortTypeDate().setDeviation(DEVIATION)
                .setCurrentFolder(folderList.defaultFID()).setIds(mid)).post().via(hc)
                .countShouldBe(2 * Integer.parseInt(DEVIATION) + 1);
        respTest.setHost(props().b2bUri().toString());
        Oper respProd = respTest.post().via(hc);

        assertThat(respTest.toDocument(), equalToDoc(respProd.toDocument()));
    }

    @Test
    @Stories(MyFeatures.HOUND)
    @Title("Ручка ywmi nearest_messages без письма после.")
    @Issue("DARIA-52908")
    public void ywmiNearestMessagesDeviationWithFistMidTest() {
        String mid = mailboxListJsx().post().via(hc).getMidOfFirstMessage();

        Oper respTest = nearestMessages(MailboxListNearestObj.empty()
                .setSetSortTypeDateAscending()
                .setUid(composeCheck.getUid())
                .setDeviation(DEFAULT_DEVIATION)
                .setCurrentFolder(folderList.defaultFID()).setMid(mid)).get().via(hc)
                //нет письма, который после
                .countShouldBe(2);
        respTest.setHost(props().houndUri(props().b2bUri().toString()));
        Oper respProd = respTest.get().via(hc);

        Map expectedMap = JsonUtils.getObject(respProd.toString(), Map.class);
        Map actualMap = JsonUtils.getObject(respTest.toString(), Map.class);

        assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }

    @Test
    @Stories(MyFeatures.HOUND)
    @Title("Ручка ywmi nearest_messages.")
    @Issue("DARIA-52908")
    public void ywmiNearestMessagesTest() {
        List<String> mids = mailboxListJsx().post().via(hc).getMidsOfMessagesInFolder();
        String mid = mids.get(mids.size() / 2);

        Oper respTest = nearestMessages(MailboxListNearestObj.empty()
                .setSetSortTypeDateAscending()
                .setUid(composeCheck.getUid())
                .setDeviation(DEFAULT_DEVIATION)
                .setCurrentFolder(folderList.defaultFID()).setMid(mid)).get().via(hc)
                .countShouldBe(2 * Integer.parseInt(DEFAULT_DEVIATION) + 1);
        respTest.setHost(props().houndUri(props().b2bUri().toString()));

        Oper respProd = respTest.get().via(hc);

        Map expectedMap = JsonUtils.getObject(respProd.toString(), Map.class);
        Map actualMap = JsonUtils.getObject(respTest.toString(), Map.class);

        assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }

    @Test
    @Stories(MyFeatures.HOUND)
    @Title("Ручка ywmi nearest_messages c параметром deviation.")
    @Issue("DARIA-52908")
    public void ywmiNearestMessagesDeviationTest() {
        List<String> mids = mailboxListJsx().post().via(hc).getMidsOfMessagesInFolder();
        String mid = mids.get(mids.size() / 2);

        Oper respTest = nearestMessages(MailboxListNearestObj.empty()
                .setUid(composeCheck.getUid())
                .setSetSortTypeDate().setDeviation(DEVIATION)
                .setCurrentFolder(folderList.defaultFID()).setMid(mid)).get().via(hc)
                .countShouldBe(2 * Integer.parseInt(DEVIATION) + 1);
        respTest.setHost(props().houndUri(props().b2bUri().toString()));
        Oper respProd = respTest.get().via(hc);

        Map expectedMap = JsonUtils.getObject(respProd.toString(), Map.class);
        Map actualMap = JsonUtils.getObject(respTest.toString(), Map.class);

        assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }


    @Test
    @Stories(MyFeatures.HOUND)
    @Title("Ручка ywmi nearest_messages c несуществующим mid.")
    @Issue("DARIA-52908")
    public void ywmiNearestMessagesNotExistMidShouldSeeError() {
        nearestMessages(MailboxListNearestObj.empty()
                .setUid(composeCheck.getUid())
                .setSetSortTypeDate()
                .setDeviation(DEFAULT_DEVIATION)
                .setCurrentFolder(folderList.defaultFID()).setMid(NOT_EXIST_MID)).get().via(hc)
        .code(equalTo(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001.code()))
                .message(equalTo("invalid argument")).reason(equalTo("mid parameter is required"));
    }
}
