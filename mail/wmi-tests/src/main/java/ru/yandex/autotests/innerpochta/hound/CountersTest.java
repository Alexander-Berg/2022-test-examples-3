package ru.yandex.autotests.innerpochta.hound;

import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.yplatform.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.b2b.MailboxListIdsTest.DEFAULT_DEVIATION;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.CountersObject.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Counters.counters;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.09.14
 * Time: 14:10
 * <p>
 * curl 'mail.yandex.ru:9090/counters?uid=3000340278'
 * {"counters":{"fresh":1,"unread":4}}
 * https://wiki.yandex-team.ru/users/jkennedy/ywmiapi#counters
 * <p>
 * [DARIA-40374]
 */
@Aqua.Test
@Title("[HOUND] Проверяем выдачу ручки counters на 9090 порту")
@Description("Проверяем отдельно для INBOX-а и различных папок.\n" +
        "Смотрим точное совпадение unread и наличие fresh")
@Features(MyFeatures.HOUND)
@Stories(MyStories.OTHER)
@Issues({@Issue("DARIA-40374"), @Issue("DARIA-48809"), @Issue("MAILPG-288"), @Issue("MAILPG-378")})
@Credentials(loginGroup = "CountersTest")
public class CountersTest extends BaseHoundTest {
    private static final int COUNT = 4;

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).before(true).allfolders();

    @Rule
    public DeleteFoldersRule deleteFolders = DeleteFoldersRule.with(authClient).all();

    private static String uid = authClient.account().uid();

    @Test
    @Title("Counters на пустом ящике")
    @Description("Проверяем ручку counters на пустом ящике")
    public void testWithEmptyMailbox() {
        counters(empty().setUid(uid)).get().via(authClient)
                .unreadShouldBe(equalTo(0)).freshShouldBe(greaterThanOrEqualTo(0));
    }

    @Test
    @Issue("MAILPG-378")
    @Title("Counters c inbox")
    @Description("Отсылаем себе пару писем. Проверяем ручку counters, только с inbox-ом.")
    public void withOnlyInboxTest() throws Exception {
        List<String> mids = sendWith(authClient).viaProd().count(COUNT).send().waitDeliver().getMids();
        counters(empty().setUid(uid)).get().via(authClient).unreadShouldBe(equalTo(COUNT));

        Mops.mark(authClient, new MidsSource(mids), ApiMark.StatusParam.READ)
                .post(shouldBe(okSync()));
        counters(empty().setUid(uid)).get().via(authClient).withDebugPrint()
                .unreadShouldBe(equalTo(0)).freshShouldBe(greaterThanOrEqualTo(COUNT));
    }

    @Test
    @Stories(MyFeatures.HOUND)
    @Title("Сбрасываем пришедшие(fresh) письма в inbox")
    @Issue("DARIA-48809")
    @Description("Отсылаем себе пару писем. Дергаем ручку counters до сброса и после")
    public void resetRecentMailCounterTest() throws Exception {
        sendWith(authClient).viaProd().send().waitDeliver().getMids();
        counters(empty().setUid(uid)).get().via(authClient).unreadShouldBe(equalTo(1)).freshShouldBe(greaterThanOrEqualTo(1));
        resetFreshCounter();
        counters(empty().setUid(uid)).get().via(authClient).unreadShouldBe(equalTo(1)).freshShouldBe(Matchers.equalTo(0));
    }

    @Test
    @Stories(MyFeatures.HOUND)
    @Title("Проверяем ручку mailbox_revision")
    @Issue("MAILPG-288")
    @Description("Аналог lcn для pg. " +
            "1. Проверяем revision, при немодифицирующих операциях" +
            "2. Проверяем revision, при модифицирующих операциях")
    public void mailboxRevisionTest() throws Exception {
        Integer revision = mailboxRevision();
        assertThat("Немодифицирующие операции должны не менять ревизию", revision, equalTo(mailboxRevision()));
        sendWith(authClient).viaProd().saveDraft().waitDeliver();
        Integer newRevision = mailboxRevision();
        assertThat("После получения письма revision не поменялся [MAILPG-288]", revision, not(Matchers.equalTo(newRevision)));

        String mid = api(MessagesByFolder.class).setHost(props().houndUri()).params(MessagesByFolderObj.empty()
                .setUid(uid).setFirst("0").setCount("1").setTill("2000000000")
                .setFid(folderList.draftFID()).setSortType("date1")).get().via(authClient)
                .resp().getEnvelopes().get(0).getMid();

        Mops.remove(authClient, new MidsSource(mid))
                .post(shouldBe(okSync()));
        assertThat("Возвращаем mailbox в первоначальное состояние: revision не должен быть равен старому",
                revision, not(equalTo(mailboxRevision())));
    }

    @Test
    @Title("Counters c пользовательской папкой")
    @Description("Проверяем, что если письмо есть в пользовательской папке, то оно с суммируется с тем что в inbox-е")
    public void testWithUserFolders() throws Exception {
        List<String> mids = sendWith(authClient).viaProd().count(COUNT).send().waitDeliver().getMids();

        String folderName = Util.getRandomString();
        String childName = Util.getRandomString();
        String fid = Mops.newFolder(authClient, folderName);
        String childFid = Mops.newFolder(authClient, childName, fid);

        Mops.complexMove(authClient, fid, new MidsSource(mids.get(0)))
                .post(shouldBe(okSync()));
        Mops.complexMove(authClient, childFid, new MidsSource(mids.get(1)))
                .post(shouldBe(okSync()));

        counters(empty().setUid(uid)).get().via(authClient)
                .unreadShouldBe(equalTo(COUNT)).freshShouldBe(greaterThanOrEqualTo(COUNT));

        Mops.mark(authClient, new MidsSource(mids.subList(0, 2)), ApiMark.StatusParam.READ)
                .post(shouldBe(okSync()));

        counters(empty().setUid(uid)).get().via(authClient)
                .unreadShouldBe(equalTo(COUNT - 2)).freshShouldBe(greaterThanOrEqualTo(COUNT - 2));
    }

    @Test
    @Title("Counters c системными папками")
    @Description("Проверяем, что если письмо есть в системной папке, оно не отображается в выдаче")
    public void testWithSystemFolders() throws Exception {
        List<String> mids = sendWith(authClient).viaProd().count(COUNT).send().waitDeliver().getMids();

        Mops.complexMove(authClient, folderList.spamFID(), new MidsSource(mids.get(0)))
                .post(shouldBe(okSync()));
        Mops.complexMove(authClient, folderList.deletedFID(), new MidsSource(mids.get(1)))
                .post(shouldBe(okSync()));
        Mops.complexMove(authClient, folderList.draftFID(), new MidsSource(mids.get(2)))
                .post(shouldBe(okSync()));

        counters(empty().setUid(uid)).get().via(authClient)
                .unreadShouldBe(equalTo(COUNT - 3)).freshShouldBe(greaterThanOrEqualTo(COUNT - 3));
    }

    @Test
    @Issue("DARIA-52894")
    @Title("Следующее/предыдущее письмо в nearest_messages")
    public void testPrevNextNearestMessages() {
        String mid1 = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String mid2 = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String mid3 = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        List<String> mids = nearestMessages(mid2, DEFAULT_DEVIATION).stream()
                .map(Envelope::getMid)
                .collect(Collectors.toList());

        assertThat("Письма <Следующее> и <Предыдущее> перепутаны в ручке nearest_messages [DARIA-52894]", mids,
                hasSameItemsAsList(newArrayList(mid3, mid2, mid1)).sameSorted());
    }
}
