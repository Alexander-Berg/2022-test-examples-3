package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.spam.ApiSpam.WithSentParam;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.*;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 14.10.14
 * Time: 17:26
 * login = "unspam.mopsovitch",
 * pwd = "testqa123"
 * <p/>
 * https://wiki.yandex-team.ru/users/shelkovin/asyncoperations/http-interface
 */
@Aqua.Test
@Title("[MOPS] Ручка spam и unspam. Новый сервис mops на 8814 порту")
@Description("Помечаем письма спамом по mid, tid (переносим в папку \"Спам\"")
@Features(MyFeatures.MOPS)
@Stories({MyStories.MOPS_SPAM, MyStories.MOPS_UNSPAM})
@Issues({@Issue("AUTOTESTPERS-142"), @Issue("DARIA-43705")})
@Credentials(loginGroup = "SpamUnspamMopsTest")
public class SpamUnspamTest extends MopsBaseTest {
    private static final int COUNT_OF_LETTERS = 2;

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    @Test
    @Title("SPAM без mid-ов и tid-ов")
    @Description("Делаем запрос spam без mid и tid")
    public void spamWithoutMidsAndTids() throws Exception {
        val expectedError = "invalid arguments: mids or tids or fid or lid or tab not found";
        spam(new EmptySource()).post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Issue("DARIA-42393")
    @Title("SPAM с несуществующим mid")
    @Description("Делаем запрос spam с несуществующим mid [DARIA-41183].\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void spamWithNotExistMid() throws Exception {
        spam(new MidsSource(NOT_EXIST_MID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("SPAM с невалидным mid")
    @Description("Делаем запрос spam с невалидным mid")
    public void spamWithInvalidMid() throws Exception {
        spam(new MidsSource(INVALID_MID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("SPAM с существующим и несуществующим mid")
    @Description("Делаем запрос spam с существующим и несуществующим mid")
    public void spamWithExistAndNotExistMid() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        spam(new MidsSource(context.firstMid(), NOT_EXIST_MID)).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient,
                not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется в \"Спам\"", authClient,
                hasMsgIn(subject, folderList.spamFID()));
    }

    @Test
    @Title("SPAM с несуществующим tid")
    @Description("Делаем запрос spam с несуществующим tid [DARIA-41183].\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void spamWithNotExistTid() throws Exception {
        spam(new TidsSource(NOT_EXIST_TID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("SPAM с невалидным tid")
    @Description("Делаем запрос spam с невалидным tid")
    public void spamWithInvalidTid() throws Exception {
        spam(new TidsSource(INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("SPAM с существующим и несуществующим tid")
    @Description("Делаем запрос spam с существующим и несуществующим tid")
    public void spamWithExistAndNotExistTid() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        spam(new TidsSource(context.firstTid(), NOT_EXIST_TID)).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient,
                not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется из \"Исходящих\"", authClient,
                not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщения перенесутся в \"Спам\"", authClient,
                hasMsgsIn(subject, 2, folderList.spamFID()));
    }

    @Test
    @Title("SPAM с несуществующим fid")
    @Description("Делаем запрос spam с несуществующим fid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void spamWithNotExistFid() throws Exception {
        spam(new FidSource(NOT_EXIST_FID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("SPAM с невалидным fid")
    @Description("Делаем запрос spam с невалидным fid")
    public void spamWithInvalidFid() throws Exception {
        spam(new FidSource(INVALID_FID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("SPAM/UNSPAM с невалидным tid и невалидным mid")
    @Description("Делаем запросы spam/unspam с невалидным tid и невалидным mid")
    public void spamUnspamWithInvalidTidAndInvalidMid() throws Exception {
        val source = new MidsWithTidsSource(INVALID_MID, INVALID_TID);
        spam(source).post(shouldBe(invalidRequest()));
        unspam(source).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("SPAM/UNSPAM с невалидным tid и несуществующим mid")
    @Description("Делаем запросы spam/unspam с невалидным tid и несуществующим mid")
    public void spamUnspamWithInvalidTidAndNotExistsMid() throws Exception {
        val source = new MidsWithTidsSource(NOT_EXIST_MID, INVALID_TID);
        spam(source).post(shouldBe(invalidRequest()));
        unspam(source).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("SPAM/UNSPAM с невалидным tid и mid")
    @Description("Делаем запросы spam/unspam с невалидным tid и mid")
    public void spamUnspamWithInvalidTidAndMid() throws Exception {
        val mid = sendMail().firstMid();
        val source = new MidsWithTidsSource(mid, INVALID_TID);
        spam(source).post(shouldBe(invalidRequest()));
        unspam(source).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("SPAM/UNSPAM с несуществующим tid и невалидным mid")
    @Description("Делаем запросы spam/unspam с несуществующим tid и невалидным mid")
    public void spamUnspamWithNotExistsTidAndInvalidMid() throws Exception {
        val source = new MidsWithTidsSource(INVALID_MID, NOT_EXIST_TID);
        spam(source).post(shouldBe(invalidRequest()));
        unspam(source).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("SPAM/UNSPAM с несуществующим tid и mid")
    @Description("Делаем запросы spam/unspam с несуществующим tid и mid")
    public void spamUnspamWithNotExistsTidAndMid() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val mid = context.firstMid();

        spam(new MidsWithTidsSource(mid, NOT_EXIST_TID)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется в \"Спам\"",
                authClient, hasMsgIn(subject, folderList.spamFID()));

        //неспамом из папки спам по tid-у пометить письма нельзя
        unspam(new MidsSource(mid)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется во \"Входящие\"",
                authClient, hasMsgIn(subject, folderList.defaultFID()));
        assertThat("Ожидалось, что сообщение перенесется из \"Спам\"",
                authClient, not(hasMsgIn(subject, folderList.spamFID())));
    }

    @Test
    @Title("SPAM/UNSPAM с tid и невалидным mid")
    @Description("Делаем запросы spam/unspam с tid и невалидным mid")
    public void spamUnspamWithTidAndInvalidMid() throws Exception {
        val source = new MidsWithTidsSource(INVALID_MID, sendMail().firstTid());
        spam(source).post(shouldBe(invalidRequest()));
        unspam(source).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("SPAM/UNSPAM с tid и несуществующим mid")
    @Description("Делаем запросы spam/unspam с tid и несуществующим mid")
    public void spamUnspamWithTidAndNotExistsMid() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val tid = context.firstTid();

        spam(new MidsWithTidsSource(NOT_EXIST_MID, tid)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"",
                authClient, not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения перенесутся в \"Спам\"",
                authClient, hasMsgsIn(subject, 2, folderList.spamFID()));

        //неспамом из папки спам по tid-у пометить письма нельзя
    }

    @Test
    @Title("SPAM/UNSPAM с tid и mid")
    @Description("Делаем запросы spam/unspam с tid и mid")
    public void spamUnspamWithTidAndMid() throws Exception {
        val tidContext = sendMail();
        val tidSubject = tidContext.subject();
        val midContext = sendMail();
        val midSubject = midContext.subject();

        spam(new MidsWithTidsSource(midContext.firstMid(), tidContext.firstTid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(tidSubject, folderList.defaultFID())));
        assertThat("Ожидалось, что второе сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(midSubject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"",
                authClient, not(hasMsgIn(tidSubject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения перенесутся в \"Спам\"",
                authClient, hasMsgsIn(tidSubject, 2, folderList.spamFID()));
        assertThat("Ожидалось, что второе сообщение перенесется в \"Спам\"",
                authClient, hasMsgIn(midSubject, folderList.spamFID()));

        //неспамом из папки спам по tid-у пометить письма нельзя
    }

    @Test
    @Title("SPAM/UNSPAM с tid и mid из tid")
    @Description("Делаем запросы spam/unspam с tid и mid из tid")
    public void spamUnspamWithTidAndMidFromTid() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        spam(new MidsWithTidsSource(context.firstMid(), context.firstTid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"",
                authClient, not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения перенесутся в \"Спам\"",
                authClient, hasMsgsIn(subject, 2, folderList.spamFID()));

        //неспамом из папки спам по tid-у пометить письма нельзя
    }

    @Test
    @Title("UNSPAM без mid-ов и tid-ов")
    @Description("Делаем запрос unspam без mid и tid")
    public void unspamWithoutMidsAndTids() throws Exception {
        val expectedError = "invalid arguments: mids or tids or fid or lid or tab not found";
        unspam(new EmptySource()).post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Issue("DARIA-42393")
    @Title("UNSPAM с несуществующим mid")
    @Description("Делаем запрос unspam с несуществующим mid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void unspamWithNotExistMid() throws Exception {
        unspam(new MidsSource(NOT_EXIST_MID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("UNSPAM с невалидным mid")
    @Description("Делаем запрос unspam с невалидным mid")
    public void unspamWithInvalidMid() throws Exception {
        unspam(new MidsSource(INVALID_MID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("UNSPAM с несуществующим tid")
    @Description("Делаем запрос unspam с несуществующим tid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void unspamWithNotExistTid() throws Exception {
        unspam(new TidsSource(NOT_EXIST_TID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("UNSPAM с невалидным tid")
    @Description("Делаем запрос unspam с невалидным tid")
    public void unspamWithInvalidTid() throws Exception {
        unspam(new TidsSource(INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("UNSPAM с невалидным fid")
    @Description("Делаем запрос unspam с невалидным fid")
    public void unspamWithInvalidFid() throws Exception {
        unspam(new FidSource(INVALID_FID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Description("Помечаем спамом, затем неспамом письмо по mid")
    @Title("SPAM\\UNSPAM с параметром mid")
    public void spamUnspamMid() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val source = new MidsSource(context.firstMid());

        spam(source).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не перенесется из \"Отправленных\"", authClient, hasMsgIn(subject,
                folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в папке \"Спам\"", authClient, hasMsgIn(subject,
                folderList.spamFID()));

        unspam(source).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение по умолчанию перенесется во \"Входящие\"", authClient,
                hasMsgIn(subject, folderList.defaultFID()));
        assertThat("Ожидалось, что сообщение перенесется из папки \"Спам\"", authClient,
                not(hasMsgIn(subject, folderList.spamFID())));
    }

    @Test
    @Description("Помечаем спамом, затем неспамом письмо по mid")
    @Title("SPAM\\UNSPAM с параметром mid")
    public void spamUnspamWithFilterSubject() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        spam(new FidSource(folderList.defaultFID()).withSubject(subject)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не перенесется из \"Отправленных\"", authClient, hasMsgIn(subject,
                folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в папке \"Спам\"", authClient, hasMsgIn(subject,
                folderList.spamFID()));

        unspam(new FidSource(folderList.spamFID()).withSubject(subject)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение по умолчанию перенесется во \"Входящие\"", authClient,
                hasMsgIn(subject, folderList.defaultFID()));
        assertThat("Ожидалось, что сообщение перенесется из папки \"Спам\"", authClient,
                not(hasMsgIn(subject, folderList.spamFID())));
    }

    @Test
    @Description("Помечаем письма спамом, затем неспамом по тиду")
    @Title("SPAM\\UNSPAM с параметром tid")
    public void spamTid() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val subject = context.subject();

        spam(new TidsSource(context.firstTid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщения перенесутся из \"Входящих\"", authClient,
                not(hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщения перенесутся из \"Отправленных\"", authClient,
                not(hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в папке \"Спам\"", authClient,
                hasMsgsIn(subject, COUNT_OF_LETTERS * 2, folderList.spamFID()));

        //неспамом из папки спам по tid-у пометить письма нельзя
    }

    @Test
    @Title("UNSPAM с пользовательской папкой и тидом DARIA-41183")
    @Description("Помечаем письмо НЕспамом из папки \"Входящие\" (так исторически сложилось)\n " +
            "с пользовательской папкой и тидами")
    public void unspamTidUserFolder() throws Exception {
        val folderName = getRandomString();
        val fid = newFolder(folderName);
        val context = sendMail(COUNT_OF_LETTERS);
        val subject = context.subject();

        unspam(new TidsSource(context.firstTid())).withDestFid(fid).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщения перенесутся из \"Входящих\"", authClient,
                not(hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщения перенесутся из \"Отравленных\"", authClient,
                not(hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.sentFID())));

        assertThat(String.format("Ожидалось, что сообщения перенесутся во \"%s\" (%s)", folderName, fid), authClient,
                hasMsgsIn(subject, COUNT_OF_LETTERS * 2, fid));

    }

    @Test
    @Issue("DARIA-43705")
    @Description("Проверяем SPAM & UNSPAM с новым параметром nomove. Письмо должно остаться во входящих")
    public void testNoMoveSpamTest() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        spam(new MidsSource(context.firstMid())).withNomove("1").post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение не перенесется из \"Входящих\"", authClient, hasMsgIn(subject, folderList.defaultFID()));
        assertThat("Ожидалось, что сообщение не перенесется из \"Отправленных\"", authClient, hasMsgIn(subject,
                folderList.sentFID()));
        assertThat("Ожидалось, что сообщение не появится в папке \"Спам\"", authClient, not(hasMsgIn(subject,
                folderList.spamFID())));
    }

    @Test
    @Issues({@Issue("DARIA-44073"), @Issue("DARIA-43705")})
    @Description("Проверяем SPAM & UNSPAM с новым параметром nomove. Письмо должно остаться в папке спам")
    public void testNoMoveUnSpamTest() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val source = new MidsSource(context.firstMid());

        spam(source).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не перенесется из \"Отправленных\"", authClient, hasMsgIn(subject,
                folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в папке \"Спам\"", authClient, hasMsgIn(subject,
                folderList.spamFID()));

        unspam(source).withNomove("1").post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение не перенесется в папку \"Входящие\"", authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение останется в папке \"Отправленных\"", authClient, hasMsgIn(subject,
                folderList.sentFID()));
        assertThat("Ожидалось, что сообщение останется в папке \"Спам\"", authClient, hasMsgIn(subject,
                folderList.spamFID()));
    }

    @Test
    @Issue("MAILDEV-292")
    @Title("SPAM без параметра with_sent")
    @Description("Помечаем цепочку писем по tid без параметра with_sent как спам." +
            "\nПроверяем, что письмо из папки \"отправленные\" также помечено как спам.")
    public void spamWithoutParameterWithSent() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        spam(new TidsSource(context.firstTid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"", authClient,
                not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в папке \"Спам\"", authClient,
                hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.spamFID()));
    }

    @Test
    @Issue("MAILDEV-292")
    @Title("SPAM с параметром with_sent")
    @Description("Помечаем цепочку писем по tid с параметром with_sent=0 как спам." +
            "\nПроверяем, что письмо из папки \"отправленные\" не помечено как спам.")
    public void spamUsingParameterWithSentFalse() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        spam(new TidsSource(context.firstTid())).withWithSent(WithSentParam._0).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не перенесется из \"Отправленных\"", authClient,
                hasMsgIn(subject, folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в папке \"Спам\"", authClient,
                hasMsgsIn(subject, 1, folderList.spamFID()));
    }

    @Test
    @Issue("MAILDEV-292")
    @Title("SPAM с параметром with_sent")
    @Description("Помечаем цепочку писем по tid с параметром with_sent=1 как спам." +
                 "\nПроверяем, что письмо из папки \"отправленные\" также помечено как спам.")
    public void spamUsingParameterWithSentTrue() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        spam(new TidsSource(context.firstTid())).withWithSent(WithSentParam._1).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"", authClient,
                not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в папке \"Спам\"", authClient,
                hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.spamFID()));
    }
}
