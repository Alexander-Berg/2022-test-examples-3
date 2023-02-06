package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.complexmove.ApiComplexMove.WithSentParam;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.EmptySource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.FidSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsWithTidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.TidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.MopsApi.apiMops;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.*;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.10.14
 * Time: 18:33
 * <p/>
 * DARIA-35802
 */
@Aqua.Test
@Title("[MOPS] Ручка complex_move. Новый сервис mops на 8814 порту")
@Description("Переносим письма по mid, tid в разные папки")
@Features(MyFeatures.MOPS)
@Stories(MyStories.MOPS_COMPLEX_MOVE)
@Issue("AUTOTESTPERS-142")
@Credentials(loginGroup = "ComplexMoveMopsTest")
public class ComplexMoveTest extends MopsBaseTest {
    private static final long WAIT_TIME = SECONDS.toMillis(60);
    private static final int COUNT_OF_LETTERS = 2;
    private static final String USER_FOLDER_NAME = getRandomString();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).before(true).all();

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("COMPLEX_MOVE без mid-ов и tid-ов")
    @Description("Делаем запрос complex_move без mid и tid")
    public void complexMoveWithoutMidsAndTids() throws Exception {
        val expectedError = "invalid arguments: mids or tids or fid or lid or tab not found";
        complexMove(folderList.deletedFID(), new EmptySource())
                .post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Title("COMPLEX_MOVE с несуществующим mid")
    @Description("Делаем запрос complex_move с несуществующим mid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void complexMoveWithNotExistMid() throws Exception {
        complexMove(folderList.defaultFID(), new MidsSource(NOT_EXIST_MID))
                .post(shouldBe(okSync()));
    }

    @Test
    @Title("COMPLEX_MOVE с невалидным mid")
    @Description("Делаем запрос complex_move с невалидным mid")
    public void complexMoveWithInvalidMid() throws Exception {
        complexMove(folderList.defaultFID(), new MidsSource(INVALID_MID))
                .post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("COMPLEX_MOVE с существующими и несуществующими mids")
    @Description("Переносим письма из папки \"Входящие\" в пользовательскую папку по mids." +
                 "\nПроверяем, что валидные сообщения перенеслись.")
    @Issue("MAILDEV-292")
    public void complexMoveWithExistsAndNotExistsMids() throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val context = sendMail();

        complexMove(fid, new MidsSource(context.firstMid(), NOT_EXIST_MID))
                .post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient,
                not(hasMsgIn(context.subject(), folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется в пользовательскую папку", authClient,
                hasMsgIn(context.subject(), fid));
    }

    @Test
    @Title("COMPLEX_MOVE с несуществующим tid")
    @Description("Делаем запрос complex_move с несуществующим tid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void complexMoveWithNotExistTid() throws Exception {
        complexMove(folderList.defaultFID(), new TidsSource(NOT_EXIST_TID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("COMPLEX_MOVE с невалидным tid")
    @Description("Делаем запрос complex_move с невалидным tid")
    public void complexMoveWithInvalidTid() throws Exception {
        complexMove(folderList.defaultFID(), new TidsSource(INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("COMPLEX_MOVE с существующими и несуществующими tids")
    @Description("Переносим письма из папки \"Входящие\" в пользовательскую папку по tids." +
                 "\nПроверяем, что валидные сообщения перенеслись.")
    @Issue("MAILDEV-292")
    public void complexMoveWithExistsAndNotExistsTids() throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val context = sendMail();
        val subject = context.subject();

        complexMove(fid, new TidsSource(context.firstTid(), NOT_EXIST_TID))
                .post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient,
                not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется в пользовательскую папку", authClient,
                hasMsgIn(subject, fid));
    }

    @Test
    @Title("COMPLEX_MOVE с несуществующим fid")
    @Description("Делаем запрос complex_move с несуществующим fid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void complexMoveWithNotExistsFid() throws Exception {
        complexMove(folderList.defaultFID(), new FidSource(NOT_EXIST_FID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("COMPLEX_MOVE с невалидным fid")
    @Description("Делаем запрос complex_move с невалидным fid")
    public void complexMoveWithInvalidFid() throws Exception {
        complexMove(folderList.defaultFID(), new FidSource(INVALID_FID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("COMPLEX_MOVE с параметром mid")
    @Description("Переносим одно письмо по mid в пользователькую папку")
    public void complexMoveMid() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val userFid = newFolder(USER_FOLDER_NAME);

        complexMove(userFid, new MidsSource(context.firstMid())).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение останется в \"Отправленных\"",
                authClient, hasMsgIn(subject, folderList.sentFID()));
        assertThat(String.format("Ожидалось, что сообщение появится в \"%s\"", USER_FOLDER_NAME),
                authClient, hasMsgIn(subject, userFid));
    }

    @Test
    @Title("COMPLEX_MOVE с параметром tid")
    @Description("Переносим одно письмо по tid в пользователькую папку")
    public void complexMoveTid() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val subject = context.subject();
        val userFid = newFolder(USER_FOLDER_NAME);

        complexMove(userFid, new TidsSource(context.firstTid())).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщения перенесется из \"Входящих\"",
                authClient, not(hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщения НЕ перенесутся из \"Отправленных\"",
                authClient, hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.sentFID()));
        assertThat(String.format("Ожидалось, что сообщения появится в \"%s\"", USER_FOLDER_NAME),
                authClient, hasMsgsIn(subject, COUNT_OF_LETTERS, userFid));
    }

    @Test
    @Issue("DARIA-42692")
    @Title("COMPLEX_MOVE с фильтром subject")
    @Description("Переносим одно письмо по теме в пользователькую папку")
    public void complexMoveWithFilterSubject() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val userFid = newFolder(USER_FOLDER_NAME);

        val source = new FidSource(folderList.defaultFID()).withSubject(subject);
        complexMove(userFid, source).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"", authClient,
                withWaitFor(not(hasMsgIn(subject, folderList.defaultFID())), WAIT_TIME));
        assertThat("Ожидалось, что сообщение НЕ перенесется из \"Отправленных\"", authClient, hasMsgIn(subject,
                folderList.sentFID()));
        assertThat(String.format("Ожидалось, что сообщение появится в \"%s\"", USER_FOLDER_NAME), authClient,
                hasMsgIn(subject, userFid));
    }

    @Test
    @Title("COMPLEX_MOVE с несуществующей папкой")
    @Description("Переносим одно письмо по mid в несуществующую папку: проверяем код и ответ {\n" +
            "   \"result\" : \"invalid request\",\n" +
            "   \"error\" : \"invalid arguments: move to non-existent folder 1234567891234567891\"\n" +
            "}")
    public void moveInNotExistFidShouldSeeInvalidRequest() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        val expectedError = String.format("invalid arguments: move to non-existent folder %s", NOT_EXIST_FID);
        complexMove(NOT_EXIST_FID, new TidsSource(context.firstTid()))
                .post(shouldBe(invalidRequest(equalTo(expectedError))));

        assertThat("Ожидалось, что сообщения останутся в \"Входящих\"",
                authClient, hasMsgIn(subject, folderList.defaultFID()));
        assertThat("Ожидалось, что сообщения останутся в \"Отправленных\"",
                authClient, hasMsgIn(subject, folderList.sentFID()));
    }

    @Test
    @Title("COMPLEX_MOVE с без параметра fid")
    @Description("Делаем запрос без параметра fid, проверяем код и ответ:" +
            "{\n" +
            "   \"result\" : \"invalid request\",\n" +
            "   \"error\" : \"invalid arguments: dest_fid must not be empty\"\n" +
            "}")
    public void moveInNoWithoutFidShouldSeeInvalidRequest() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val source = new TidsSource(context.firstTid());

        val apiComplexMove = apiMops(authClient.account().userTicket()).complexMove()
                .withUid(authClient.account().uid());
        source.fill(apiComplexMove);

        apiComplexMove.post(shouldBe(invalidRequest(equalTo("invalid arguments: dest_fid must not be empty"))));

        assertThat("Ожидалось, что сообщения останутся в \"Входящих\"",
                authClient,
                hasMsgIn(subject, folderList.defaultFID()));
        assertThat("Ожидалось, что сообщения останутся в \"Отправленных\"",
                authClient,
                hasMsgIn(subject, folderList.sentFID()));
    }

    @Test
    @Title("COMPLEX_MOVE с невалидным tid и невалидным mid")
    @Description("Делаем запрос complex_move с невалидным tid и невалидным mid")
    public void complexMoveWithInvalidTidAndInvalidMid() throws Exception {
        val source = new MidsWithTidsSource(INVALID_MID, INVALID_TID);
        complexMove(folderList.defaultFID(), source).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("COMPLEX_MOVE с невалидным tid и несуществующим mid")
    @Description("Делаем запрос complex_move с невалидным tid и несуществующим mid")
    public void complexMoveWithInvalidTidAndNotExistsMid() throws Exception {
        val source = new MidsWithTidsSource(NOT_EXIST_MID, INVALID_TID);
        complexMove(folderList.defaultFID(), source).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("COMPLEX_MOVE с невалидным tid и mid")
    @Description("Делаем запрос complex_move с невалидным tid и mid")
    public void complexMoveWithInvalidTidAndMid() throws Exception {
        val context = sendMail();
        val source = new MidsWithTidsSource(context.firstMid(), INVALID_TID);
        complexMove(folderList.deletedFID(), source).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("COMPLEX_MOVE с несуществующим tid и невалидным mid")
    @Description("Делаем запрос complex_move с несуществующим tid и невалидным mid")
    public void complexMoveWithNotExistsTidAndInvalidMid() throws Exception {
        val source = new MidsWithTidsSource(INVALID_MID, NOT_EXIST_TID);
        complexMove(folderList.deletedFID(), source).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("COMPLEX_MOVE с несуществующим tid и mid")
    @Description("Делаем запрос complex_move с несуществующим tid и mid")
    public void complexMoveWithNotExistsTidAndMid() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val source = new MidsWithTidsSource(context.firstMid(), NOT_EXIST_TID);
        complexMove(folderList.deletedFID(), source).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется в \"Удаленные\"",
                authClient, hasMsgIn(subject, folderList.deletedFID()));
    }

    @Test
    @Title("COMPLEX_MOVE с tid и невалидным mid")
    @Description("Делаем запрос complex_move с tid и невалидным mid")
    public void complexMoveWithTidAndInvalidMid() throws Exception {
        val context = sendMail();
        val source = new MidsWithTidsSource(INVALID_MID, context.firstTid());
        complexMove(folderList.deletedFID(), source).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("COMPLEX_MOVE с tid и несуществующим mid")
    @Description("Делаем запрос complex_move с tid и несуществующим mid")
    public void complexMoveWithTidAndNotExistsMid() throws Exception {
        val subj = Util.getRandomString();
        val context = sendMail(subj);
        val source = new MidsWithTidsSource(NOT_EXIST_MID, context.firstTid());

        complexMove(folderList.deletedFID(), source).post(shouldBe(okSync()));

        assertThat("Ожидалось, что цепочка сообщений перенесется в \"Удаленные\"",
                authClient, hasMsgsIn(subj, 2, folderList.deletedFID()));
    }

    @Test
    @Title("COMPLEX_MOVE с tid и mid")
    @Description("Делаем запрос complex_move с tid и mid")
    public void complexMoveWithTidAndMid() throws Exception {
        val subj = Util.getRandomString();
        val context = sendMail(subj);

        val anotherSubj = Util.getRandomString();
        val mid = sendMail(anotherSubj).firstMid();
        val source = new MidsWithTidsSource(mid, context.firstTid());

        complexMove(folderList.deletedFID(), source).post(shouldBe(okSync()));

        assertThat("Ожидалось, что цепочка сообщений перенесется в \"Удаленные\"",
                authClient, hasMsgsIn(subj, 2, folderList.deletedFID()));
        assertThat("Ожидалось, что одиночное сообщение перенесется в \"Удаленные\"",
                authClient, hasMsgIn(anotherSubj, folderList.deletedFID()));
    }

    @Test
    @Title("COMPLEX_MOVE с tid и mid из этого tid")
    @Description("Делаем запрос complex_move с tid и mid из этого tid")
    public void complexMoveWithTidAndMidFromTid() throws Exception {
        val subj = Util.getRandomString();
        val context = sendMail(subj);
        val source = new MidsWithTidsSource(context.firstMid(), context.firstTid());

        complexMove(folderList.deletedFID(), source).post(shouldBe(okSync()));

        assertThat("Ожидалось, что цепочка сообщений перенесется в \"Удаленные\"",
                authClient, hasMsgsIn(subj, 2, folderList.deletedFID()));
    }

    @Test
    @Title("COMPLEX_MOVE без параметра with_sent по tid")
    @Description("Переносим цепочку писем по tid в архив без параметра with_sent." +
            "\nПроверяем, что письмо из папки \"отправленные\" не переместилось в архив.")
    @Issues({@Issue("MAILDEV-292"), @Issue("MAILDEV-803")})
    public void complexMoveWithoutParameterWithSent() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val archiveFid = folderList.getOrCreateArchiveFid();

        complexMove(archiveFid, new TidsSource(context.firstTid())).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не перенесется из \"Отправленных\"", authClient,
                hasMsgIn(subject, folderList.sentFID()));
        assertThat("Ожидалось, что сообщения появятся в \"Архиве\"", authClient,
                hasMsgsIn(subject, 1, archiveFid));
    }

    @Test
    @Title("COMPLEX_MOVE с параметром with_sent по tid")
    @Description("Переносим цепочку писем по tid в архив с параметром with_sent=0." +
            "\nПроверяем, что письмо из папки \"отправленные\" не переместилось в архив.")
    @Issue("MAILDEV-292")
    public void complexMoveUsingParameterWithSentFalse() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val archiveFid = folderList.getOrCreateArchiveFid();

        complexMove(archiveFid, new TidsSource(context.firstTid())).withWithSent(WithSentParam._0)
                .post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не перенесется из \"Отправленных\"", authClient,
                hasMsgIn(subject, folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в \"Архиве\"", authClient,
                hasMsgsIn(subject, 1, archiveFid));
    }

    @Test
    @Title("COMPLEX_MOVE с параметром with_sent по tid")
    @Description("Переносим цепочку писем по tid в архив с параметром with_sent=1." +
                 "\nПроверяем, что письмо из папки \"отправленные\" также переместилось в архив.")
    @Issue("MAILDEV-292")
    public void complexMoveUsingParameterWithSentTrue() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val archiveFid = folderList.getOrCreateArchiveFid();

        complexMove(archiveFid, new TidsSource(context.firstTid())).withWithSent(WithSentParam._1)
                .post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"", authClient,
                not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в \"Архиве\"", authClient,
                hasMsgsIn(subject, COUNT_OF_LETTERS, archiveFid));
    }

    @Test
    @Title("COMPLEX_MOVE без параметра with_sent по tid")
    @Description("Переносим цепочку писем по tid в пользовательскую папку без параметра with_sent." +
            "\nПроверяем, что письмо из папки \"отправленные\" не переместилось в пользовательскую папку.")
    @Issue("MAILDEV-292")
    public void complexMoveToUserFolderWithoutParameterWithSent() throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val context = sendMail();
        val subject = context.subject();

        complexMove(fid, new TidsSource(context.firstTid())).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не перенесется из \"Отправленных\"", authClient,
                hasMsgIn(subject, folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в пользовательской папке", authClient,
                hasMsgsIn(subject, 1, fid));
    }

    @Test
    @Title("COMPLEX_MOVE с параметром with_sent по tid")
    @Description("Переносим цепочку писем по tid в пользовательскую папку c параметром with_sent=0." +
                 "\nПроверяем, что письмо из папки \"отправленные\" не переместилось в пользовательскую папку.")
    @Issue("MAILDEV-292")
    public void complexMoveToUserFolderUsingParameterWithSentFalse() throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val context = sendMail();
        val subject = context.subject();

        complexMove(fid, new TidsSource(context.firstTid())).withWithSent(WithSentParam._0)
                .post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не перенесется из \"Отправленных\"", authClient,
                hasMsgIn(subject, folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в пользовательской папке", authClient,
                hasMsgsIn(subject, 1, fid));
    }

    @Test
    @Title("COMPLEX_MOVE с параметром with_sent по tid")
    @Description("Переносим цепочку писем по tid в пользовательскую папку c параметром with_sent=1." +
            "\nПроверяем, что письмо из папки \"отправленные\" переместилось в пользовательскую папку.")
    @Issue("MAILDEV-292")
    public void complexMoveToUserFolderUsingParameterWithSentTrue() throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val context = sendMail();
        val subject = context.subject();

        complexMove(fid, new TidsSource(context.firstTid())).withWithSent(WithSentParam._1)
                .post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"", authClient,
                not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в пользовательской папке", authClient,
                hasMsgsIn(subject, COUNT_OF_LETTERS, fid));
    }

    @Test
    @Title("COMPLEX_MOVE из папки \"Отправленные\" по mid")
    @Description("Переносим письмо из папки \"Отправленные\" по mid" +
                 "\nПроверяем, что письмо перенеслось.")
    @Issue("MAILDEV-292")
    public void complexMoveFromSentByMid() throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val context = sendMail();
        val subject = context.subject();
        val sentMid = context.firstSentMid(folderList.sentFID());

        complexMove(fid, new MidsSource(sentMid)).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение не перенесется из \"Входящих\"",
                authClient, hasMsgIn(subject, folderList.defaultFID()));
        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"", authClient,
                not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в пользовательской папке", authClient,
                hasMsgsIn(subject, 1, fid));
    }

    @Test
    @Title("COMPLEX_MOVE из папки \"Отправленные\" по mid с параметром with_sent=0")
    @Description("Переносим письмо из папки \"Отправленные\" по mid." +
                 "\nПроверяем, что письмо перенеслось.")
    @Issue("MAILDEV-292")
    public void complexMoveFromSentByMidUsingWithSent() throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val context = sendMail();
        val subject = context.subject();
        val sentMid = context.firstSentMid(folderList.sentFID());

        complexMove(fid, new MidsSource(sentMid)).withWithSent(WithSentParam._0).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"", authClient,
                not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщение появится в пользовательской папке", authClient,
                hasMsgsIn(subject, 1, fid));
    }

    @Test
    @Title("COMPLEX_MOVE из папки \"Отправленные\" по fid")
    @Description("Переносим письмо из папки \"Отправленные\" по fid." +
                 "\nПроверяем, что письмо перенеслось.")
    @Issue("MAILDEV-292")
    public void complexMoveFromSentByFid() throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val context = sendMail();
        val subject = context.subject();

        complexMove(fid, new FidSource(folderList.sentFID()).withSubject(subject)).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"", authClient,
                withWaitFor(not(hasMsgIn(subject, folderList.sentFID())), WAIT_TIME));
        assertThat("Ожидалось, что сообщение появится в пользовательской папке", authClient,
                hasMsgsIn(subject, 1, fid));
    }

    @Test
    @Title("COMPLEX_MOVE из папки \"Отправленные\" по fid с параметром with_sent=0")
    @Description("Переносим письмо из папки \"Отправленные\" по fid." +
                 "\nПроверяем, что письмо перенеслось.")
    @Issue("MAILDEV-292")
    public void complexMoveFromSentByFidUsingWithSent() throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val context = sendMail();
        val subject = context.subject();

        complexMove(fid, new FidSource(folderList.sentFID()).withSubject(subject))
                .withWithSent(WithSentParam._0).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение перенесется из \"Отправленных\"", authClient,
                withWaitFor(not(hasMsgIn(subject, folderList.sentFID())), WAIT_TIME));
        assertThat("Ожидалось, что сообщение появится в пользовательской папке", authClient,
                hasMsgsIn(subject, 1, fid));
    }
}
