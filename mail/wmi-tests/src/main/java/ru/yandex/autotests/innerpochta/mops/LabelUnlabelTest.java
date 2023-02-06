package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.*;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 09.12.14
 * Time: 20:55
 */
@Aqua.Test
@Title("[MOPS] Ручки label/unlabel. Новый сервис mops на 8814 порту")
@Description("Ставим и снимаем пользовательские метки на письма по mids, tids и в папке с определенным fid")
@Features(MyFeatures.MOPS)
@Stories(MyStories.MOPS_LABEL)
@Issue("DARIA-42355")
@Credentials(loginGroup = "MarkMops2Test")
public class LabelUnlabelTest extends MopsBaseTest {
    private static final String labelName = getRandomString();
    private static final int COUNT_OF_LETTERS = 2;
    private static final int COUNT_OF_LABELS = 5;

    private String lid;

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).inbox().outbox();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    @Rule
    public DeleteLabelsMopsRule deleteLabels = new DeleteLabelsMopsRule(authClient);

    @Before
    public void initLabel() throws IOException {
        lid = newLabelByName(labelName);
    }

    private List<String> createLids(int count) throws IOException {
        val lids = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            lids.add(newLabelByName(labelName + i));
        }
        return lids;
    }

    @Test
    @Title("LABEL без mid, tid и fid")
    @Description("Делаем запрос LABEL без mid, tid и fid")
    public void labelWithoutMidsAndTids() throws Exception {
        val expectedError = "invalid arguments: mids or tids or fid or lid or tab not found";
        label(new EmptySource(), lid).post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Issue("DARIA-42937")
    @Title("LABEL с несуществующим mid")
    @Description("Делаем запрос LABEL с несуществующим mid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void labelWithNotExistMid() throws Exception {
        label(new MidsSource(NOT_EXIST_MID), lid).post(shouldBe(okSync()));
    }

    @Test
    @Issue("DARIA-42937")
    @Title("LABEL с невалидным mid")
    @Description("Делаем запрос LABEL с невалидным mid")
    public void labelWithInvalidMid() throws Exception {
        label(new MidsSource(INVALID_MID), lid).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("LABEL с несуществующим tid")
    @Description("Делаем запрос LABEL с несуществующим tid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void labelWithNotExistTid() throws Exception {
        label(new TidsSource(NOT_EXIST_TID), lid).post(shouldBe(okSync()));
    }

    @Test
    @Title("LABEL с невалидным tid")
    @Description("Делаем запрос LABEL с невалидным tid")
    public void labelWithInvalidTid() throws Exception {
        label(new TidsSource(INVALID_TID), lid).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("LABEL с несуществующей меткой")
    @Description("Помечаем письмо несуществующей меткой")
    public void labelWithNotExistLidsShouldSeeInvalidRequest() throws Exception {
        val context = sendMail();
        val expectedError = String.format("no lid '%s': no such label", NOT_EXIST_LID);
        label(new MidsSource(context.firstMid()), NOT_EXIST_LID)
                .post(shouldBe(invalidRequest(containsString(expectedError))));
    }

    @Test
    @Title("LABEL с несуществующей папкой")
    @Description("Помечаем письма из несуществующей папкой")
    public void labelWithNotExistFid() throws Exception {
        label(new FidSource(NOT_EXIST_FID), lid).post(shouldBe(okSync()));
    }

    @Test
    @Title("LABEL с невалидной папкой")
    @Description("Помечаем письма из невалидной папкой")
    public void labelWithInvalidFid() throws Exception {
        label(new FidSource(INVALID_FID), lid).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("UNLABEL без mid, tid и fid")
    @Description("Делаем запрос LABEL без mid, tid и fid")
    public void ulabelWithoutMidsAndTids() throws Exception {
        val expectedError = "invalid arguments: mids or tids or fid or lid or tab not found";
        unlabel(new EmptySource(), lid).post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Title("UNLABEL с несуществующим tid")
    @Description("Делаем запрос LABEL с несуществующим tid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void unlabelWithNotExistTid() throws Exception {
        unlabel(new TidsSource(NOT_EXIST_TID), lid).post(shouldBe(okSync()));
    }

    @Test
    @Title("UNLABEL с несуществующей меткой")
    @Description("Помечаем письмо несуществующей меткой")
    public void unlabelWithNotExistLidsShouldSeeInvalidRequest() throws Exception {
        val context = sendMail();
        val expectedError = String.format("no lid '%s': no such label", NOT_EXIST_LID);
        unlabel(new MidsSource(context.firstMid()), NOT_EXIST_LID)
                .post(shouldBe(invalidRequest(containsString(expectedError))));
    }

    @Test
    @Title("UNLABEL с несуществующей папкой")
    @Description("Помечаем письма из несуществующей папки")
    public void unlabelWithNotExistFid() throws Exception {
        unlabel(new FidSource(NOT_EXIST_FID), lid).post(shouldBe(okSync()));
    }

    @Test
    @Title("LABEL/UNLABEL с невалидным tid и невалидным mid")
    @Description("Делаем запросы LABEL/UNLABEL с невалидным tid и невалидным mid")
    public void labelUnlabelWithInvalidTidAndInvalidMid() throws Exception {
        val source = new MidsWithTidsSource(INVALID_MID, INVALID_TID);
        label(source, lid).post(shouldBe(invalidRequest()));
        unlabel(source, lid).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("LABEL/UNLABEL с невалидным tid и несуществующим mid")
    @Description("Делаем запросы LABEL/UNLABEL с невалидным tid и несуществующим mid")
    public void labelUnlabelWithInvalidTidAndNotExistsMid() throws Exception {
        val source = new MidsWithTidsSource(NOT_EXIST_MID, INVALID_TID);
        label(source, lid).post(shouldBe(invalidRequest()));
        unlabel(source, lid).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("LABEL/UNLABEL с невалидным tid и mid")
    @Description("Делаем запросы LABEL/UNLABEL с невалидным tid и mid")
    public void labelUnlabelWithInvalidTidAndMid() throws Exception {
        val mid = sendMail().firstMid();
        val source = new MidsWithTidsSource(mid, INVALID_TID);
        label(source, lid).post(shouldBe(invalidRequest()));
        unlabel(source, lid).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("LABEL/UNLABEL с несуществующим tid и невалидным mid")
    @Description("Делаем запросы LABEL/UNLABEL с несуществующим tid и невалидным mid")
    public void labelUnlabelWithNotExistsTidAndInvalidMid() throws Exception {
        val source = new MidsWithTidsSource(INVALID_MID, NOT_EXIST_TID);
        label(source, lid).post(shouldBe(invalidRequest()));
        unlabel(source, lid).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("LABEL/UNLABEL с несуществующим tid и mid")
    @Description("Делаем запросы LABEL/UNLABEL с несуществующим tid и mid")
    public void labelUnlabelWithNotExistsTidAndMid() throws Exception {
        val mid = sendMail().firstMid();
        val source = new MidsWithTidsSource(mid, NOT_EXIST_TID);

        label(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение будет помечено пользовательской меткой",
                authClient, hasMsgWithLid(mid, lid));

        unlabel(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось, что с сообщения будет снята пользовательская метка",
                authClient, not(hasMsgWithLid(mid, lid)));
    }

    @Test
    @Title("LABEL/UNLABEL с tid и невалидным mid")
    @Description("Делаем запросы LABEL/UNLABEL с tid и невалидным mid")
    public void labelUnlabelWithTidAndInvalidMid() throws Exception {
        val source = new MidsWithTidsSource(INVALID_MID, sendMail().firstTid());
        label(source, lid).post(shouldBe(invalidRequest()));
        unlabel(source, lid).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("LABEL/UNLABEL с tid и несуществующим mid")
    @Description("Делаем запросы LABEL/UNLABEL с tid и несуществующим mid")
    public void labelUnlabelWithTidAndNotExistsMid() throws Exception {
        val subject = getRandomString();
        val context = sendMail(subject);
        val sentMid = context.firstSentMid(folderList.sentFID());
        val source = new MidsWithTidsSource(NOT_EXIST_MID, context.firstTid());

        label(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение во \"Входящих\" будет помечено пользовательской меткой",
                authClient, hasMsgWithLid(context.firstMid(), lid));
        assertThat("Ожидалось, что сообщение в \"Отправленных\" будет помечено пользовательской меткой",
                authClient, hasMsgWithLidInFolder(sentMid, folderList.sentFID(), lid));

        unlabel(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось, что с сообщения во \"Входящих\" будет снята пользовательская метка",
                authClient, not(hasMsgWithLid(context.firstMid(), lid)));
        assertThat("Ожидалось, что с сообщение в \"Отправленных\" будет снята пользовательская метка",
                authClient, not(hasMsgWithLidInFolder(sentMid, folderList.sentFID(), lid)));
    }

    @Test
    @Title("LABEL/UNLABEL с tid и mid")
    @Description("Делаем запросы LABEL/UNLABEL с tid и mid")
    public void labelUnlabelWithTidAndMid() throws Exception {
        val subject = getRandomString();
        val context = sendMail(subject);
        val sentMid = context.firstSentMid(folderList.sentFID());
        val mid = context.firstMid();
        val source = new MidsWithTidsSource(mid, context.firstTid());

        label(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение во \"Входящих\" будет помечено пользовательской меткой",
                authClient, hasMsgWithLid(mid, lid));
        assertThat("Ожидалось, что сообщение в \"Отправленных\" будет помечено пользовательской меткой",
                authClient, hasMsgWithLidInFolder(sentMid, folderList.sentFID(), lid));
        assertThat("Ожидалось, что второе сообщение во \"Входящих\" будет помечено пользовательской меткой",
                authClient, hasMsgWithLid(mid, lid));

        unlabel(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось, что с сообщения во \"Входящих\" будет снята пользовательская метка",
                authClient, not(hasMsgWithLid(mid, lid)));
        assertThat("Ожидалось, что с сообщения в \"Отправленных\" будет снята пользовательская метка",
                authClient, not(hasMsgWithLidInFolder(sentMid, folderList.sentFID(), lid)));
        assertThat("Ожидалось, что со второго сообщения во \"Входящих\" будет снята пользовательская метка",
                authClient, not(hasMsgWithLid(mid, lid)));
    }

    @Test
    @Title("LABEL/UNLABEL с tid и mid из этого tid")
    @Description("Делаем запросы LABEL/UNLABEL с tid и mid из этого tid")
    public void labelUnlabelWithTidAndMidFromTid() throws Exception {
        val subject = getRandomString();
        val context = sendMail(subject);
        val sentMid = context.firstSentMid(folderList.sentFID());
        val mid = context.firstMid();
        val source = new MidsWithTidsSource(mid, context.firstTid());

        label(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение во \"Входящих\" будет помечено пользовательской меткой",
                authClient, hasMsgWithLid(mid, lid));
        assertThat("Ожидалось, что сообщение в \"Отправленных\" будет помечено пользовательской меткой",
                authClient, hasMsgWithLidInFolder(sentMid, folderList.sentFID(), lid));
    }

    @Test
    @Issue("DARIA-49466")
    @Title("LABEL/UNLABEL с mids из папки Отправленные и Входящие")
    @Description("Помечаем письма пользовательскими метками по мидам из папки Отправленные и Входящие.\n" +
            "Снимаем метки")
    public void labelOutboxMidsShouldSeeLabels() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);

        val midsOutbox = context.mids(folderList.sentFID());
        val midsInbox = context.mids();

        val mids = newArrayList(midsInbox);
        mids.addAll(midsOutbox);

        val source = new MidsSource(mids);
        label(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма будут помечены пользовательской меткой, но письма оказалось без пометки", authClient,
                hasMsgsWithLid(midsInbox, lid));
        assertThat("Ожидалось что письма будут помечены пользовательской меткой в отправленных, " +
                        "но письма оказалось без пометки", authClient,
                hasMsgsWithLidInFolder(midsOutbox, folderList.sentFID(), lid));

        unlabel(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма не будут помечены пользовательской меткой, но письма оказалось без пометки", authClient,
                not(hasMsgsWithLid(midsInbox, lid)));
        assertThat("Ожидалось что письма не будут помечены пользовательской меткой в отправленных", authClient,
                not(hasMsgsWithLidInFolder(midsOutbox, folderList.sentFID(), lid)));
    }

    @Test
    @Title("LABEL/UNLABEL с mids")
    @Description("Помечаем письма пользовательскими метками по мидам.\n" +
            "Снимаем метки")
    public void labelMidsShouldSeeLabels() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val mids = context.mids();
        val source = new MidsSource(mids);

        label(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма будут помечены пользовательской меткаой, но письма оказалось без пометки", authClient,
                hasMsgsWithLid(mids, lid));

        unlabel(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма не будут помечены пользовательскими метками", authClient,
                not(hasMsgsWithLid(mids, lid)));
    }


    @Test
    @Issue("DARIA-42692")
    @Title("LABEL/UNLABEL с фильтром subject")
    @Description("Помечаем письма пользовательскими метками по subject в папке.\n" +
            "Снимаем метки")
    public void labelWithFilterSubject() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val mids = context.mids();
        val source = new FidSource(folderList.defaultFID()).withSubject(context.subject());

        label(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма будут помечены пользовательской меткаой, но письма оказалось без пометки", authClient,
                hasMsgsWithLid(mids, lid));

        unlabel(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма не будут помечены пользовательскими метками", authClient,
                not(hasMsgsWithLid(mids, lid)));
    }

    @Test
    @Title("LABEL/UNLABEL с tid")
    @Description("Помечаем письма пользовательскими метками по тиду.\n" +
            "Снимаем метки")
    public void labelTidShouldSeeLabels() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val tid = context.firstTid();
        val source = new TidsSource(tid);

        label(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма будут помечены пользовательской меткой, но письма оказалось без пометки", authClient,
                hasMsgsWithLid(context.mids(), lid));

        unlabel(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма не будут помечены пользовательскими метками", authClient,
                not(hasMsgsWithLid(context.mids(), lid)));
    }

    @Test
    @Title("LABEL/UNLABEL с fid")
    @Description("Помечаем письма пользовательскими метками из папки.\n" +
            "Снимаем метки")
    public void labelFidShouldSeeLabels() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val source = new FidSource(folderList.defaultFID());

        label(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма будут помечены пользовательской меткой, но письма оказалось без пометки", authClient,
                hasMsgsWithLid(context.mids(), lid));

        unlabel(source, lid).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма не будут помечены пользовательскими метками", authClient,
                not(hasMsgsWithLid(context.mids(), lid)));
    }

    @Test
    @Title("LABEL/UNLABEL с lids")
    @Description("Помечаем несколько писем метками.\n" +
            "Снимаем метки")
    public void labelMidsWithLids() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val source = new FidSource(folderList.defaultFID());
        val lids = createLids(COUNT_OF_LABELS);

        label(source, lids).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма будут помечены пользовательской метками, но письма оказались без пометки", authClient,
                hasMsgsWithLids(context.mids(), lids));

        unlabel(source, lids).post(shouldBe(okSync()));
        assertThat("Ожидалось что письма не будут помечены пользовательскими метками", authClient,
                notHasMsgsWithLids(context.mids(), lids));
    }

    @Test
    @Title("LABEL/UNLABEL с lids")
    @Description("Снимаем метки с письма без меток")
    public void unlabelMidsWithoutLids() throws Exception {
        sendMail(COUNT_OF_LETTERS);
        val source = new FidSource(folderList.defaultFID());
        val lids = createLids(COUNT_OF_LABELS);
        label(source, lids).post(shouldBe(okSync()));
    }

    @Test
    @Issue("MPROTO-1897")
    @Title("Пометка меткой удаленного письма")
    public void labelDeletedLetter() throws Exception {
        val context = sendMail();
        val mid = context.firstMid();
        remove(new MidsSource(mid)).post(shouldBe(okSync()));
        label(new MidsSource(mid), lid).post(shouldBe(okSync()));
    }
}
