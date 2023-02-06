package ru.yandex.autotests.innerpochta.wmi.other;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxListNearestObj;
import ru.yandex.autotests.innerpochta.wmi.core.utils.WaitUtils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.b2b.MailboxListIdsTest.DEFAULT_DEVIATION;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxListNearest.mailboxListNearestJsx;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

@Aqua.Test
@Title("Проверка выдачи mailbox_list_nearest")
@Features(MyFeatures.WMI)
@Stories(MyStories.OTHER)
@Issue("DARIA-52894")
@Credentials(loginGroup = "CountersTest")
public class MailboxListNearestTest extends BaseTest {
    @Test
    @Issue("DARIA-52894")
    @Title("Следующее/предыдущее письмо в mailbox_list_nearest")
    public void testPrevNextMailboxListNearest() throws Exception {
        String mid1 = sendWith.viaProd().waitDeliver().send().getMid();
        WaitUtils.waitSmth(5, TimeUnit.SECONDS);
        String mid2 = sendWith.viaProd().waitDeliver().send().getMid();
        WaitUtils.waitSmth(5, TimeUnit.SECONDS);
        String mid3 = sendWith.viaProd().waitDeliver().send().getMid();

        List<String> mids = mailboxListNearestJsx(MailboxListNearestObj.empty()
                .setSetSortTypeDate()
                .setDeviation(DEFAULT_DEVIATION)
                .setCurrentFolder(folderList.defaultFID()).setIds(mid2)).post().via(hc)
                .countShouldBe(2 * Integer.parseInt(DEFAULT_DEVIATION) + 1).getMids();

        assertThat("Письма <Следующее> и <Предыдущее> перепутаны в ручке mailbox_list_nearest [DARIA-52894]", mids,
                hasSameItemsAsList(newArrayList(mid3, mid2, mid1)).sameSorted());
    }
}
