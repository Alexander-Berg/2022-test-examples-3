package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark.StatusParam;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.qatools.allure.annotations.*;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.MopsApi.apiMops;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.*;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.FAKE_SEEN_LBL;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 14.10.14
 * Time: 17:25
 */
@Aqua.Test
@Title("[MOPS] Ручка mark. Тест с разными статусами. Новый сервис mops на 8814 порту")
@Description("Помечаем письма прочитанными, непрочитанными по mid, tid")
@Features(MyFeatures.MOPS)
@Stories(MyStories.MOPS_MARK)
@Issue("AUTOTESTPERS-142")
@Credentials(loginGroup = "MarkMopsTest")
public class MarkTest extends MopsBaseTest {
    private static final int COUNT_OF_LETTERS = 2;

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    @Rule
    public DeleteLabelsMopsRule deleteLabels = new DeleteLabelsMopsRule(authClient);

    @Test
    @Title("MARK без mid-ов, tid-ов, fid-а, lid-а и tab-а")
    @Description("Делаем запрос mark без mid, tid, fid и lid")
    public void markWithoutMidsAndTidsAndFidAndLid() throws Exception {
        val expectedError = "invalid arguments: mids or tids or fid or lid or tab not found";
        apiMops(authClient.account().userTicket()).mark()
                .withUid(authClient.account().uid())
                .withStatus(StatusParam.READ)
                .post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Title("MARK с несуществующим mid")
    @Description("Делаем запрос mark с несуществующим mid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void markWithNotExistMid() throws Exception {
        mark(new MidsSource(NOT_EXIST_MID), StatusParam.READ).post(shouldBe(okSync()));
    }

    @Test
    @Title("MARK с невалидным mid")
    @Description("Делаем запрос mark с невалидным mid")
    public void markWithInvalidMid() throws Exception {
        mark(new MidsSource(INVALID_MID), StatusParam.READ).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("MARK с несуществующим tid")
    @Description("Делаем запрос mark с несуществующим tid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void markWithNotExistTid() throws Exception {
        mark(new TidsSource(NOT_EXIST_TID), StatusParam.READ).post(shouldBe(okSync()));
    }

    @Test
    @Title("MARK с невалидным tid")
    @Description("Делаем запрос mark с невалидным tid")
    public void markWithInvalidTid() throws Exception {
        mark(new TidsSource(INVALID_TID), StatusParam.READ).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("MARK с несуществующим fid")
    @Description("Делаем запрос mark с несуществующим fid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void markWithNotExistFid() throws Exception {
        mark(new FidSource(NOT_EXIST_FID), StatusParam.READ).post(shouldBe(okSync()));
    }

    @Test
    @Title("MARK с невалидным fid")
    @Description("Делаем запрос mark с невалидным fid")
    public void markWithInvalidFid() throws Exception {
        mark(new FidSource(INVALID_FID), StatusParam.READ).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("MARK с невалидным tid и невалидным mid")
    @Description("Делаем запрос mark с невалидным tid и невалидным mid.\n")
    public void markWithInvalidTidAndInvalidMid() throws Exception {
        mark(new MidsWithTidsSource(INVALID_MID, INVALID_TID), StatusParam.READ)
                .post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("MARK с невалидным tid и несуществующим mid")
    @Description("Делаем запрос mark с невалидным tid и несуществующим mid.\n")
    public void markWithInvalidTidAndNotExistsMid() throws Exception {
        mark(new MidsWithTidsSource(NOT_EXIST_MID, INVALID_TID), StatusParam.READ)
                .post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("MARK с невалидным tid и mid")
    @Description("Делаем запрос mark с невалидным tid и mid.\n")
    public void markWithInvalidTidAndMid() throws Exception {
        val mid = sendMail().firstMid();
        mark(new MidsWithTidsSource(mid, INVALID_TID), StatusParam.READ)
                .post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("MARK с несуществующим tid и невалидным mid")
    @Description("Делаем запрос mark с несуществующим tid и невалидным mid.\n")
    public void markWithNotExistsTidAndInvalidMid() throws Exception {
        mark(new MidsWithTidsSource(INVALID_MID, NOT_EXIST_TID), StatusParam.READ)
                .post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("MARK с несуществующим tid и mid")
    @Description("Делаем запрос mark с несуществующим tid и mid.\n")
    public void markWithNotExistsTidAndMid() throws Exception {
        val mid = sendMail().firstMid();
        mark(new MidsWithTidsSource(mid, NOT_EXIST_TID), StatusParam.READ)
                .post(shouldBe(okSync()));
        assertThat("Ожидалось, что письмо будет помечено как прочитанное",
                authClient, hasMsgWithLid(mid, FAKE_SEEN_LBL));
    }

    @Test
    @Title("MARK с tid и невалидным mid")
    @Description("Делаем запрос mark с tid и невалидным mid.\n")
    public void markWithTidAndInvalidMid() throws Exception {
        val tid = sendMail().firstTid();
        mark(new MidsWithTidsSource(INVALID_MID, tid), StatusParam.READ)
                .post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("MARK с tid и несуществующим mid")
    @Description("Делаем запрос mark с tid и несуществующим mid.\n")
    public void markWithTidAndNotExistsMid() throws Exception {
        val context = sendMail();
        val sentMid = context.firstSentMid(folderList.sentFID());

        mark(new MidsSource(sentMid), StatusParam.NOT_READ).post(shouldBe(okSync()));
        assertThat("Ожидалось, что письмо из \"Отправленных\" будет отмечено как непрочитанное",
                authClient, not(hasMsgWithLidInFolder(sentMid, folderList.sentFID(), FAKE_SEEN_LBL)));

        mark(new MidsWithTidsSource(NOT_EXIST_MID, context.firstTid()), StatusParam.READ)
                .post(shouldBe(okSync()));
        assertThat("Ожидалось, что письмо из \"Входящих\" будет отмечено как прочитанное",
                authClient, hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL));
        assertThat("Ожидалось, что письмо из \"Отправленных\" будет отмечено как прочитанное",
                authClient, hasMsgWithLidInFolder(sentMid, folderList.sentFID(), FAKE_SEEN_LBL));
    }

    @Test
    @Title("MARK с tid и mid")
    @Description("Делаем запрос mark с tid и mid.\n")
    public void markWithTidAndMid() throws Exception {
        val context = sendMail();
        val sentMid = context.firstSentMid(folderList.sentFID());

        mark(new MidsSource(sentMid), StatusParam.NOT_READ).post(shouldBe(okSync()));

        assertThat("Ожидалось, что письмо из \"Отправленных\" будет отмечено как непрочитанное",
                authClient, not(hasMsgWithLidInFolder(sentMid, folderList.sentFID(), FAKE_SEEN_LBL)));

        val mid = sendMail().firstMid();
        mark(new MidsWithTidsSource(mid, context.firstTid()), StatusParam.READ)
                .post(shouldBe(okSync()));
        assertThat("Ожидалось, что письмо из \"Входящих\" будет отмечено как прочитанное",
                authClient, hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL));
        assertThat("Ожидалось, что письмо из \"Отправленных\" будет отмечено как прочитанное",
                authClient, hasMsgWithLidInFolder(sentMid, folderList.sentFID(), FAKE_SEEN_LBL));
        assertThat("Ожидалось, что второе письмо из \"Входящих\" будет отмечено как прочитанное",
                authClient, hasMsgWithLid(mid, FAKE_SEEN_LBL));
    }

    @Test
    @Title("MARK с tid и mid из этого tid")
    @Description("Делаем запрос mark с tid и mid из этого tid.\n")
    public void markWithTidAndMidFromTid() throws Exception {
        val context = sendMail();
        val sentMid = context.firstSentMid(folderList.sentFID());

        mark(new MidsSource(sentMid), StatusParam.NOT_READ).post(shouldBe(okSync()));
        assertThat("Ожидалось, что письмо из \"Отправленных\" будет отмечено как непрочитанное",
                authClient, not(hasMsgWithLidInFolder(sentMid, folderList.sentFID(), FAKE_SEEN_LBL)));

        mark(new MidsWithTidsSource(context.firstMid(), context.firstTid()), StatusParam.READ)
                .post(shouldBe(okSync()));
        assertThat("Ожидалось, что письмо из \"Входящих\" будет отмечено как прочитанное",
                authClient, hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL));
        assertThat("Ожидалось, что письмо из \"Отправленных\" будет отмечено как прочитанное",
                authClient, hasMsgWithLidInFolder(sentMid, folderList.sentFID(), FAKE_SEEN_LBL));
    }

    @Test
    @Title("MARK с одним существующим mid и одним несуществующим")
    @Description("Помечаем письма определенным статусом по миду")
    public void markReadUnreadMidWithInvalidMidShouldSeeFakeSeenLabel() throws Exception {
        val context = sendMail();
        val connectionId = getRandomString();
        val source = new MidsSource(context.firstMid(), NOT_EXIST_MID);

        mark(source, StatusParam.READ).withOraConnectionId(connectionId).post(shouldBe(okSync()));
        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", authClient,
                hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL));

        mark(source, StatusParam.NOT_READ).withOraConnectionId(connectionId).post(shouldBe(okSync()));
        assertThat("Письмо осталось помечено прочитанным", authClient,
                Matchers.not(hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL)));
    }

    @Test
    @Title("MARK с одним существующим tid и одним несуществующим")
    @Description("Помечаем письма определенным статусом по тиду")
    public void markReadUnreadTidWithNotExistTidShouldSeeFakeSeenLabel() throws Exception {
        val context = sendMail();
        val source = new TidsSource(context.firstTid(), NOT_EXIST_TID);

        mark(source, StatusParam.READ).post(shouldBe(okSync()));
        assertThat("Ожидалось что письмо в папке \"Входящие\" будет помечено как прочитанное," +
                   "но письмо оказалось без пометки", authClient,
                hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL));

        mark(source, StatusParam.NOT_READ).post(shouldBe(okSync()));
        assertThat("Письмо в папке \"Входящие\" осталось помечено прочитанным", authClient,
                Matchers.not(hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL)));
    }

    @Test
    @Title("MARK с mid")
    @Description("Помечаем письмо определенным статусом по миду")
    public void markReadUnreadMidShouldSeeFakeSeenLabel() throws Exception {
        val context = sendMail();
        //DARIA-49596
        val connectionId = getRandomString();
        val source = new MidsSource(context.firstMid());

        mark(source, StatusParam.READ).withOraConnectionId(connectionId).post(shouldBe(okSync()));
        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", authClient,
                hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL));

        mark(source, StatusParam.NOT_READ).withOraConnectionId(connectionId).post(shouldBe(okSync()));
        assertThat("Письмо осталось помечено прочитанным", authClient,
                Matchers.not(hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL)));
    }

    @Test
    @Issue("DARIA-42692")
    @Title("Проверка MARK с фильтром subject")
    @Description("Помечаем письма прочитанными/непрочитанными с фильтром subject")
    public void markWithFilterSubject() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val source = new FidSource(folderList.defaultFID()).withSubject(context.subject());

        mark(source, StatusParam.READ).post(shouldBe(okSync()));
        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", authClient,
                hasMsgsWithLid(context.mids(), FAKE_SEEN_LBL));

        mark(source, StatusParam.NOT_READ).post(shouldBe(okSync()));
        assertThat("Письмо осталось помечено прочитанным", authClient,
                not(hasMsgsWithLid(context.mids(), FAKE_SEEN_LBL)));
    }

    @Test
    @Title("MARK с tid")
    @Description("Помечаем письмо определенным статусом по тиду")
    public void markReadUnreadTidShouldSeeFakeSeenLabel() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val source = new TidsSource(context.firstTid());

        mark(source, StatusParam.READ).post(shouldBe(okSync()));
        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", authClient,
                hasMsgsWithLid(context.mids(), FAKE_SEEN_LBL));

        mark(source, StatusParam.NOT_READ).post(shouldBe(okSync()));
        assertThat("Письмо осталось помечено прочитанным", authClient,
                Matchers.not(hasMsgsWithLid(context.mids(), FAKE_SEEN_LBL)));
    }

    @Test
    @Title("MARK с неизвестным статусом")
    @Description("Помечаем письмо неизвестным статусом")
    public void markIncorrectStatusShouldSeeInternalError() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val expectedError = "invalid arguments: status not supported: wrong_status";
        apiMops(authClient.account().userTicket()).mark()
                .withUid(authClient.account().uid())
                .withStatus("wrong_status")
                .withTids(context.firstTid())
                .post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Title("MARK с forwarded")
    @Description("Помечаем одно письмо отвеченным")
    public void markForwardedShouldSeeFakeForwardedLabel() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        mark(new TidsSource(context.firstTid()), StatusParam.FORWARDED).post(shouldBe(okSync()));
        assertThat("Ожидалось что письмо будет помечено как письмо, на которое пересылали", authClient,
                hasMsgsWithLid(context.mids(), forwarded()));
    }

    @Test
    @Title("MARK с answered")
    @Description("Помечаем одно письмо отвеченным")
    public void markAnsweredShouldSeeFakeAnsweredLabel() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        mark(new TidsSource(context.firstTid()), StatusParam.REPLIED).post(shouldBe(okSync()));
        assertThat("Ожидалось что письмо будет помечено как письмо, на которое ответили", authClient,
                hasMsgsWithLid(context.mids(), answered()));
    }

    private void testMarkSync(String inboxMid, String sentMid, Source source) throws Exception {
        val sentFid = folderList.sentFID();

        assertThat("Ожидалось, что копия письма из \"Отправленных\" будет помечена прочитанным",
                authClient, hasMsgWithLidInFolder(sentMid, sentFid, FAKE_SEEN_LBL));
        mark(new MidsSource(sentMid), StatusParam.NOT_READ).post(shouldBe(okSync()));

        assertThat("Ожидалось, что входящее письмо не будет помечено прочитанным",
                authClient, not(hasMsgWithLid(inboxMid, FAKE_SEEN_LBL)));
        assertThat("Ожидалось, что копия письма из \"Отправленных\" не будет помечена прочитанным",
                authClient, not(hasMsgWithLidInFolder(sentMid, sentFid, FAKE_SEEN_LBL)));

        mark(source, StatusParam.READ).post(shouldBe(okSync()));

        assertThat("Ожидалось, что исходное письмо из \"Входящих\" будет помечено прочитанным",
                authClient, hasMsgWithLid(inboxMid, FAKE_SEEN_LBL));
        assertThat("Ожидалось, что копия исходного письма из \"Отправленных\" будет помечена прочитанным",
                authClient, hasMsgWithLidInFolder(sentMid, sentFid, FAKE_SEEN_LBL));
    }

    @Test
    @Title("Синхронизация помеченных писем")
    @Description("Создаем копию письма, помечаем исходное письмо прочитанным по mid, " +
                 "копия также должна пометиться прочитанным")
    @Issue("MAILDEV-861")
    public void testMarkSyncByMid() throws Exception {
        val context = sendMail();
        val inboxMid = context.firstMid();
        testMarkSync(inboxMid, context.firstSentMid(folderList.sentFID()), new MidsSource(inboxMid));
    }

    @Test
    @Title("Синхронизация помеченных писем")
    @Description("Создаем копию письма, помечаем исходное письмо прочитанным по tid, " +
                 "копия также должна пометиться прочитанным")
    @Issue("MAILDEV-861")
    public void testMarkSyncByTid() throws Exception {
        val context = sendMail();
        testMarkSync(context.firstMid(), context.firstSentMid(folderList.sentFID()), new TidsSource(context.firstTid()));
    }

    @Test
    @Title("MARK с tab")
    @Description("Помечаем письмо определенным статусом по tab")
    public void markTab() throws Exception {
        val context = sendMail();
        Tab tab = Tab.RELEVANT;
        val source = new TabSource(tab.getName());

        complexMove(folderList.inboxFID(), tab.getName(), new MidsSource(context.firstMid())).post(shouldBe(okSync()));

        mark(source, StatusParam.READ).post(shouldBe(okSync()));
        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", authClient,
                hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL));

        mark(source, StatusParam.NOT_READ).post(shouldBe(okSync()));
        assertThat("Письмо осталось помечено прочитанным", authClient,
                Matchers.not(hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL)));

        assertThat("Письмо в отправленных не должно пометиться непрочитанным", authClient,
                hasMsgWithLid(context.firstSentMid(folderList.sentFID()), FAKE_SEEN_LBL));
    }

    @Test
    @Title("MARK READ письма с mention")
    @Description("Отправляем письмо с mention. Проверяем, что писльмо пометилось метками mention_label и mention_unvisited_label" +
            "\nВызываем mark read, проверяем что снялась метки mention_label и mention_unvisited_label.")
    @Issue("MAILPG-2290")
    public void markReadMessageWithMention() throws Exception {
        val mid = sendMailWithMention(authClient.acc().getSelfEmail()).firstMid();
        assertThat("Ожидалось, что у письма появится метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма появится метка mention_unvisited_label", authClient,
                hasMsgWithLid(mid, mention_unvisited_label()));

        mark(new MidsSource(mid), StatusParam.READ).post(shouldBe(okSync()));

        assertThat("Ожидалось, что у письма останется метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма исчезнет метка mention_unvisited_label", authClient,
                Matchers.not(hasMsgWithLid(mid, mention_unvisited_label())));
    }

    @Test
    @Title("MARK NOT_READ, REPLIED, FORWARDED письма с mention. Метки должны остаться")
    @Description("Отправляем письмо с mention. Проверяем, что писльмо пометилось метками mention_label и mention_unvisited_label" +
            "\nВызываем mark со статусами NOT_READ, REPLIED, FORWARDED, проверяем что метки mention_label и mention_unvisited_label осталась.")
    @Issue("MAILPG-2290")
    public void markNotReadRepliedForwardedMessageWithMention() throws Exception {
        val mid = sendMailWithMention(authClient.acc().getSelfEmail()).firstMid();
        assertThat("Ожидалось, что у письма появится метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма появится метка mention_unvisited_label", authClient,
                hasMsgWithLid(mid, mention_unvisited_label()));

        mark(new MidsSource(mid), StatusParam.NOT_READ).post(shouldBe(okSync()));
        assertThat("Ожидалось, что у письма останется метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма останется метка mention_unvisited_label", authClient,
                hasMsgWithLid(mid, mention_unvisited_label()));

        mark(new MidsSource(mid), StatusParam.REPLIED).post(shouldBe(okSync()));
        assertThat("Ожидалось, что у письма останется метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма останется метка mention_unvisited_label", authClient,
                hasMsgWithLid(mid, mention_unvisited_label()));

        mark(new MidsSource(mid), StatusParam.FORWARDED).post(shouldBe(okSync()));
        assertThat("Ожидалось, что у письма останется метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма останется метка mention_unvisited_label", authClient,
                hasMsgWithLid(mid, mention_unvisited_label()));
    }

    @Test
    @Title("MARK NOT_READ, REPLIED, FORWARDED письма с mention. Метки должны не измениться")
    @Description("Отправляем письмо с mention. Проверяем, что писльмо пометилось метками mention_label и mention_unvisited_label" +
            "\nВызываем mark со статусами READ, проверяем что метка mention_unvisited_label исчезла." +
            "\nВызываем mark со статусами NOT_READ, REPLIED, FORWARDED, проверяем что метка mention_label осталась, mention_unvisited_label не появилась")
    @Issue("MAILPG-2290")
    public void markNotReadRepliedForwardedMessageWithRemovedMention() throws Exception {
        val mid = sendMailWithMention(authClient.acc().getSelfEmail()).firstMid();
        assertThat("Ожидалось, что у письма появится метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма появится метка mention_unvisited_label", authClient,
                hasMsgWithLid(mid, mention_unvisited_label()));


        mark(new MidsSource(mid), StatusParam.READ).post(shouldBe(okSync()));
        assertThat("Ожидалось, что у письма останется метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма исчезнет метка mention_unvisited_label", authClient,
                Matchers.not(hasMsgWithLid(mid, mention_unvisited_label())));

        mark(new MidsSource(mid), StatusParam.NOT_READ).post(shouldBe(okSync()));
        assertThat("Ожидалось, что у письма останется метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма не появится метка mention_unvisited_label", authClient,
                Matchers.not(hasMsgWithLid(mid, mention_unvisited_label())));

        mark(new MidsSource(mid), StatusParam.REPLIED).post(shouldBe(okSync()));
        assertThat("Ожидалось, что у письма останется метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма не появится метка mention_unvisited_label", authClient,
                Matchers.not(hasMsgWithLid(mid, mention_unvisited_label())));

        mark(new MidsSource(mid), StatusParam.FORWARDED).post(shouldBe(okSync()));
        assertThat("Ожидалось, что у письма останется метка mention_label", authClient,
                hasMsgWithLid(mid, mention_label()));
        assertThat("Ожидалось, что у письма не появится метка mention_unvisited_label", authClient,
                Matchers.not(hasMsgWithLid(mid, mention_unvisited_label())));
    }

    @Test
    @Issue("MAILPG-2448")
    @Title("MARK с несуществующим lid")
    @Description("Делаем запрос mark с несуществующим lid.\n" +
            "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void markwithUnexistingLid() throws Exception {
        mark(new LidSource(NOT_EXIST_LID), StatusParam.READ).post(shouldBe(okSync()));
    }

    @Test
    @Issue("MAILPG-2448")
    @Title("MARK с lid")
    @Description("Помечаем письмо определенным статусом по пользовательской метке")
    public void markwithUserLabel() throws Exception {
        val removed = sendMail();
        val context = sendMail();

        String lid = newLabelByName(getRandomString());
        label(new MidsSource(context.firstMid(), removed.firstMid()), lid).post(shouldBe(okSync()));
        remove(new MidsSource(removed.firstMid())).post(shouldBe(okSync()));

        val source = new LidSource(lid);

        mark(source, StatusParam.READ).post(shouldBe(okSync()));
        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", authClient,
                hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL));

        assertThat("Ожидалось что удаленное письмо не будет помечено как прочитанное", authClient,
                Matchers.not(hasMsgWithLidInFolder(removed.firstMid(), folderList.deletedFID(), FAKE_SEEN_LBL)));

        mark(source, StatusParam.NOT_READ).post(shouldBe(okSync()));
        assertThat("Письмо осталось помечено прочитанным", authClient,
                Matchers.not(hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL)));
    }

    @Test
    @Issue("MAILPG-2448")
    @Title("MARK с lid")
    @Description("Помечаем письмо определенным статусом по метке аттачей")
    public void markwithAttachLid() throws Exception {
        File attach = AttachUtils.genFile(1);
        val removed = sendMail(attach);
        val context = sendMail(attach);

        assertThat("Ожидалось что на письме будет метка вложения", authClient,
                hasMsgWithLid(context.firstMid(), attached()));

        assertThat("Ожидалось что на письме будет метка вложения", authClient,
                hasMsgWithLid(removed.firstMid(), attached()));

        remove(new MidsSource(removed.firstMid())).post(shouldBe(okSync()));

        val source = new LidSource(attached());

        mark(source, StatusParam.READ).post(shouldBe(okAsync()));
        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", authClient,
                withWaitFor(hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL)));

        assertThat("Ожидалось что удаленное письмо не будет помечено как прочитанное", authClient,
                withWaitFor(Matchers.not(hasMsgWithLidInFolder(removed.firstMid(), folderList.deletedFID(), FAKE_SEEN_LBL))));

        mark(source, StatusParam.NOT_READ).post(shouldBe(okAsync()));
        assertThat("Письмо осталось помечено прочитанным", authClient,
                withWaitFor(Matchers.not(hasMsgWithLid(context.firstMid(), FAKE_SEEN_LBL))));

        assertThat("Письмо в отправленных должно пометиться непрочитанным", authClient,
                withWaitFor(Matchers.not(hasMsgWithLid(context.firstSentMid(folderList.sentFID()), FAKE_SEEN_LBL))));
    }
}