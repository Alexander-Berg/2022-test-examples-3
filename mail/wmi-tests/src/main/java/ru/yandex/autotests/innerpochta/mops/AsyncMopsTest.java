package ru.yandex.autotests.innerpochta.mops;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBodyData;
import lombok.val;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.*;
import org.postgresql.ds.PGSimpleDataSource;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol;
import ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv;
import ru.yandex.autotests.innerpochta.beans.yplatform.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreSshTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark.StatusParam;
import ru.yandex.autotests.innerpochta.wmi.core.mops.remove.ApiRemove;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.ThreadsByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.*;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.FidSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.TabSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.TidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.autotests.testpers.ssh.SSHAuthRule;
import ru.yandex.qatools.allure.annotations.*;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.function.Function.identity;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static ru.yandex.autotests.innerpochta.beans.tskv.TargetTskv.MESSAGE;
import static ru.yandex.autotests.innerpochta.beans.tskv.WmiTskv.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasFolderWithFid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.CountLidsMatcher.hasCountLabelsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.IsThereLabel.hasLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasCountMsgsInFid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasCountMsgsInTab;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasMsgsWithLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasNewMsgsInFid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasNewMsgsInTab;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.ssh.TSKVLogMatcher.logEntryShouldMatch;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSyncOrAsync;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okAsync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsByFolderCommand.threadsByFolder;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;
import static ru.yandex.autotests.testpers.ssh.SSHAuthRule.sshOn;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;
import static ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder.should;

@Title("[MOPS] Тесты на асинхронные операции в сервисе mops.")
@Description("Делаем различные операции с большим числом писем, проверяем что они выполняются.\n" +
    "Операции должны разбивать на чанки и выполняться через некоторое время.")
@Aqua.Test
@Features(MyFeatures.MOPS)
@Stories(MyStories.MOPS)
@Credentials(loginGroup = "AsyncMopsTest")
@Issue("DARIA-26997")
@IgnoreForPg
public class AsyncMopsTest extends MopsBaseTest {
    private static final int MIDS_COUNT = props().mopsThreshold() + 1;
    private static final int TIDS_COUNT = props().mopsThreshold() + 1;
    private static final long WAIT_TIME = MINUTES.toMillis(10);

    private static Logger logger = LogManager.getLogger("AsyncMopsTest");
    private static List<String> mids;
    private static final Set<String> tids = new HashSet<>();
    private static final String CUSTOM_LABEL_NAME = "mops_async_test_label";
    private static final String TSKV_LABEL_NAME = "mops_async_tskv_label";
    private static final String TSKV_MOPS_LOG_PATH = "/var/log/mops/user_journal.tskv";
    private static final DataSource mopsDbDataSource = makeMopsDbDataSource();

    private static DataSource makeMopsDbDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[] {props().mopsdbHost()});
        dataSource.setPortNumbers(new int[] {props().mopsdbPort()});
        dataSource.setUser(props().mopsdbUser());
        dataSource.setPassword(props().mopsdbPassword());
        dataSource.setDatabaseName(props().mopsdbName());
        return dataSource;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        lazyPrepareInbox();
        cleanupUserLocks(authClient.account().uid());
        checkStateBetweenTests();

        mids = MessagesByFolder.messagesByFolder(
                MessagesByFolderObj.empty()
                    .setUid(authClient.account().uid())
                    .setFid(folderList.defaultFID())
                    .setCount("100")
                    .setFirst("0")
        )
                .get()
                .via(authClient)
                .mids();
    }

    @After
    public void tearDown() throws Exception {
        checkStateBetweenTests();
    }

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).deleted();

    private static boolean cleanupUserLocks(String uid) throws Exception {
        return executeBooleanQuery("select util.force_release_lock(?::bigint)", uid);
    }

    private static void lazyPrepareInbox() throws Exception {
        for (Symbol symbol : new Symbol[]{Symbol.SENT, Symbol.SPAM, Symbol.TRASH}) {
            val fid = folderList.fidBySymbol(symbol);
            val folderName = folderList.nameBySymbol(symbol);
            val count = folderList.count(fid);
            if (count != 0) {
                logger.info(String.format("В папке '%s' оказалось %d писем (должно быть пусто). Будем очищать.",
                    folderName, count));
                clearFolder(fid);
            }
        }

        val inboxCount = folderList.count(folderList.defaultFID());
        if (inboxCount != MIDS_COUNT) {
            logger.info(String.format("Неправильное количество писем в инбоксе: %d вместо %d. Будем наполнять.",
                inboxCount, MIDS_COUNT));
            prepareInbox();
        }

        collectTids();

        val unreadCount = new FolderList(authClient).newCount(folderList.defaultFID());
        if (unreadCount != MIDS_COUNT) {
            logger.info(String.format("Неправильное количество непрочитанных писем в инбоксе: %d вместо %d. " +
                "Будем помечать непрочитанными.", unreadCount, MIDS_COUNT));
            markUnread((inboxCount - unreadCount) >= MIDS_COUNT);
        }

        val labelCount = countByName(LabelSymbol.PRIORITY_HIGH.toString());
        if (labelCount != 0) {
            logger.info(String.format("Не должно быть писем с метками (а их %d). Будем снимать метку.",
                labelCount));
            unlabel();
        }
    }

    private static void prepareInbox() throws Exception {
        clearFolder(folderList.defaultFID());
        fillInbox();
    }

    private static void collectTids() {
        logger.info("Получаем список тредов.");

        val resp = threadsByFolder(ThreadsByFolderObj.empty()
                .setFirst("0").setCount(String.valueOf(TIDS_COUNT))
                .setFid(folderList.defaultFID()).setUid(authClient.account().uid())).get().via(authClient).resp();

        val inboxTids = resp.getThreadsByFolder().getEnvelopes()
                .stream()
                .map(Envelope::getThreadId)
                .collect(Collectors.toSet());
        tids.addAll(inboxTids);
    }

    private static void fillInbox() {
        sendMail(MIDS_COUNT);
        assertThat(authClient, withWaitFor(hasNewMsgsInFid(equalTo(MIDS_COUNT), folderList.defaultFID()), WAIT_TIME));
    }

    private static void clearFolder(String fid) throws Exception {
        Mops.purge(authClient, new FidSource(fid)).post(shouldBe(okSyncOrAsync()));
        assertThat(authClient, withWaitFor(hasCountMsgsInFid(equalTo(0), fid), WAIT_TIME));
    }

    private static void checkStateBetweenTests() throws Exception {
        shouldBeEmptyTasks();
        userShouldNotHaveLocks();

        assumeThat("Не хватает сообщений во \"Входящих\" ", authClient,
            hasCountMsgsInFid(equalTo(MIDS_COUNT), folderList.defaultFID()));
        assumeThat("Не хватает НЕПРОЧИТАННЫХ сообщений во \"Входящих\"", authClient,
                hasNewMsgsInFid(equalTo(MIDS_COUNT), folderList.defaultFID()));
    }

    private static Matcher<HttpClientManagerRule> hasTasks(Matcher<String> matcher) {
        return new FeatureMatcher<HttpClientManagerRule, String>(matcher,
            "Ожидали выдачу /stat (данные о текущих задачах в очереди)",
            "Получили выдачу /stat") {

            @Override
            protected String featureValueOf(HttpClientManagerRule authClient) {
                try {
                    return Mops.stat(authClient).get(ResponseBodyData::asString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

    private static void shouldBeTasksWithType(String expected) {
        val response = Mops.stat(authClient).get(identity());
        assertTaskWithType(response, expected);
    }

    private static void shouldBeEmptyTasks() {
        assertThat("Есть непустые задачи", authClient, withWaitFor(hasTasks(containsString(EMPTY_TASKS))));
    }

    private static void shouldBeTaskWithGroupId(String expectedTaskGroupId) {
        val response = Mops.stat(authClient).get(identity());
        assertTaskWithGroupId(response, expectedTaskGroupId);
    }

    private static void userShouldNotHaveLocks() throws Exception {
        assertFalse("", userHasLocks(authClient.account().uid()));
    }

    private static boolean userHasLocks(String uid) throws Exception {
        return executeBooleanQuery("select 0 < (select count(*) from operations.user_locks where uid = ?::bigint)", uid);
    }

    private static boolean executeBooleanQuery(String query, String uid) throws Exception {
        try (val conn = mopsDbDataSource.getConnection();
             val stmt = conn.prepareStatement(query)) {
            stmt.setBigDecimal(1, new BigDecimal(uid));
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    private static Response startMarkInbox(StatusParam status) throws Exception {
        return Mops.mark(authClient, new FidSource(folderList.defaultFID()), status)
                .post(shouldBe(okAsync()));
    }

    private static void startMarkUnreadInbox(boolean asyncExpected) throws Exception {
        val statusMatcher = asyncExpected ? okAsync() : okSync();
        Mops.mark(authClient, new FidSource(folderList.defaultFID()), StatusParam.NOT_READ)
                .post(shouldBe(statusMatcher));
    }

    private static void startMarkReadAllMids() throws Exception {
        Mops.mark(authClient, new MidsSource(mids), StatusParam.READ).post(shouldBe(okAsync()));
    }

    private void startLabelInbox() throws Exception {
        label(new FidSource(folderList.defaultFID()), priorityHigh()).post(shouldBe(okAsync()));
    }

    private static void startUnlabelInbox() throws Exception {
        Mops.unlabel(authClient, new FidSource(folderList.defaultFID()), singletonList(priorityHigh()))
                .post(shouldBe(okAsync()));
    }

    private void startLabelAllMids() throws Exception {
        label(new MidsSource(mids), priorityHigh()).post(shouldBe(okAsync()));
    }

    private void startUnlabelAllMids() throws Exception {
        unlabel(new MidsSource(mids), priorityHigh()).post(shouldBe(okAsync()));
    }

    private static void startSpamInbox() throws Exception {
        Mops.spam(authClient, new FidSource(folderList.defaultFID())).post(shouldBe(okAsync()));
    }

    private void startUnspamAllMids() throws Exception {
        Mops.unspam(authClient, new MidsSource(mids)).post(shouldBe(okAsync()));
    }

    private static void startSpamAllMids() throws Exception {
        Mops.spam(authClient, new MidsSource(mids)).post(shouldBe(okAsync()));
    }

    private static void startUnspamAll() throws Exception {
        Mops.unspam(authClient, new FidSource(folderList.spamFID())).post(shouldBe(okAsync()));
    }

    private static void startRemoveAllMids() throws Exception {
        Mops.remove(authClient, new MidsSource(mids)).post(shouldBe(okAsync()));
    }

    private static void startRemoveInbox() throws Exception {
        Mops.remove(authClient, new FidSource(folderList.defaultFID())).post(shouldBe(okAsync()));
    }

    private void startMoveAll(String fromFid, String toFid, String destTab) throws Exception {
        complexMove(toFid, destTab, new FidSource(fromFid)).post(shouldBe(okAsync()));
    }

    private void startMoveAllMids(String fid, String destTab) throws Exception {
        complexMove(fid, destTab, new MidsSource(mids)).post(shouldBe(okAsync()));
    }

    private static void markUnread(boolean asyncExpected) throws Exception {
        startMarkUnreadInbox(asyncExpected);
        if (asyncExpected) {
            shouldBeTasksWithType("mark");
        }
        assertThat(authClient, withWaitFor(hasNewMsgsInFid(equalTo(MIDS_COUNT), folderList.defaultFID()), WAIT_TIME));
    }

    private static void unlabel() throws Exception {
        startUnlabelInbox();
        shouldBeTasksWithType("unlabel");
        assertThat(authClient, withWaitFor(hasCountLabelsWithLid(equalTo(0), priorityHigh()), WAIT_TIME));
    }

    @Test
    @Title("Последовательная пометка Входящих прочитанной/непрочитанной")
    @Description("Помечаем папку Входящие последовательно сначала прочитанной, затем непрочитанной")
    public void testMarkByFid() throws Exception {
        startMarkInbox(StatusParam.READ);
        shouldBeTasksWithType("mark");
        assertThat(authClient, withWaitFor(hasNewMsgsInFid(equalTo(0), folderList.defaultFID()), WAIT_TIME));

        markUnread(true);
    }

    @Test
    @Title("Последовательная пометка Входящих статусом forwarded")
    @Description("Тестирование использования запроса Mids[Range]ByFolderWithoutLabel")
    public void testMarkForwardedByFid() throws Exception {
        startMarkInbox(StatusParam.FORWARDED);
        shouldBeTasksWithType("mark");
        val forwardedLid = forwarded();
        assertThat(authClient, withWaitFor(hasMsgsWithLabel(equalTo(mids.size()), forwardedLid), WAIT_TIME));
    }

    @Test
    @Title("Последовательная пометка писем по mids прочитанными/непрочитанными")
    @Description("Помечаем письма по mids последовательно сначала прочитанными, затем непрочитанными")
    public void testMarkByMids() throws Exception {
        startMarkReadAllMids();
        shouldBeTasksWithType("mark");
        assertThat(authClient, withWaitFor(hasNewMsgsInFid(equalTo(0), folderList.defaultFID()), WAIT_TIME));

        markUnread(true);
    }

    @Test
    @Title("Перенос всего из Входящих в Спам и обратно")
    @Description("Переносим из Входящих все письма в Спам, затем обратно во Входящие и помечаем непрочитанными")
    public void testSpamUnspamByFid() throws Exception {
        startSpamInbox();
        shouldBeTasksWithType("spam");
        assertThat(authClient, withWaitFor(hasCountMsgsInFid(equalTo(MIDS_COUNT), folderList.spamFID()), WAIT_TIME));
        assertThat(authClient, withWaitFor(hasNewMsgsInFid(equalTo(0), folderList.defaultFID()), WAIT_TIME));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(0), Tab.defaultTab)));

        startUnspamAll();
        shouldBeTasksWithType("unspam");
        assertThat(authClient, withWaitFor(hasCountMsgsInFid(equalTo(MIDS_COUNT), folderList.defaultFID()), WAIT_TIME));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(MIDS_COUNT), Tab.defaultTab)));

        // для восстановления состояния ящика
        markUnread(true);
    }

    @Test
    @Title("Перенос писем по mids из Входящих в Спам и обратно")
    @Description("Переносим из Входящих письма по mids в Спам, затем обратно во Входящие и помечаем непрочитанными")
    public void testSpamUnspamByMids() throws Exception {
        startSpamAllMids();
        shouldBeTasksWithType("spam");
        assertThat(authClient, withWaitFor(hasCountMsgsInFid(equalTo(MIDS_COUNT), folderList.spamFID()), WAIT_TIME));
        assertThat(authClient, withWaitFor(hasNewMsgsInFid(equalTo(0), folderList.defaultFID()), WAIT_TIME));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(0), Tab.defaultTab)));

        startUnspamAllMids();
        shouldBeTasksWithType("unspam");
        assertThat(authClient, withWaitFor(hasCountMsgsInFid(equalTo(MIDS_COUNT), folderList.defaultFID()), WAIT_TIME));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(MIDS_COUNT), Tab.defaultTab)));

        // для восстановления состояния ящика
        markUnread(true);
    }

    @Test
    @Title("Перенос всего из Входящих в Корзину и обратно")
    @Description("Последовательно переносим все письма из Входящих в Корзину, затем обратно")
    public void testRemoveComplexMoveByFid() throws Exception {
        startRemoveInbox();
        shouldBeTasksWithType("remove");
        assertThat(authClient, withWaitFor(hasCountMsgsInFid(equalTo(MIDS_COUNT), folderList.deletedFID()), WAIT_TIME));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(0), Tab.defaultTab)));

        startMoveAll(folderList.deletedFID(), folderList.defaultFID(), Tab.defaultTab);
        shouldBeTasksWithType("complex_move");
        assertThat(authClient, withWaitFor(hasCountMsgsInFid(equalTo(MIDS_COUNT), folderList.defaultFID()), WAIT_TIME));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(MIDS_COUNT), Tab.defaultTab)));
    }

    @Test
    @Title("Перенос писем по mids из Входящих в Корзину и обратно")
    @Description("Последовательно переносим письма по mids из Входящих в Корзину, затем обратно")
    public void testRemoveComplexMoveByMids() throws Exception {
        startRemoveAllMids();
        shouldBeTasksWithType("remove");
        assertThat(authClient, withWaitFor(hasCountMsgsInFid(equalTo(MIDS_COUNT), folderList.deletedFID()), WAIT_TIME));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(0), Tab.defaultTab)));

        startMoveAllMids(folderList.defaultFID(), Tab.defaultTab);
        shouldBeTasksWithType("complex_move");
        assertThat(authClient, withWaitFor(hasCountMsgsInFid(equalTo(MIDS_COUNT), folderList.defaultFID()), WAIT_TIME));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(MIDS_COUNT), Tab.defaultTab)));
    }

    @Test
    @Title("Простановка и снятие метки на всех Входящих")
    @Description("Последовательно проставляем метку priority_high на всех письмах из папки Входящие, затем снимаем")
    public void testLabelUnlabelByFid() throws Exception {
        startLabelInbox();
        shouldBeTasksWithType("label");
        assertThat(authClient, withWaitFor(hasCountLabelsWithLid(equalTo(MIDS_COUNT), priorityHigh()), WAIT_TIME));

        unlabel();
    }

    @Test
    @Title("Простановка и снятие метки на письмах по mids")
    @Description("Последовательно проставляем метку priority_high на письмах по mids, затем снимаем")
    public void testLabelUnlabelByMids() throws Exception {
        startLabelAllMids();
        shouldBeTasksWithType("label");
        assertThat(authClient, withWaitFor(hasCountLabelsWithLid(equalTo(MIDS_COUNT),
            priorityHigh()), WAIT_TIME));

        startUnlabelAllMids();
        shouldBeTasksWithType("unlabel");
        assertThat(authClient, withWaitFor(hasCountLabelsWithLid(equalTo(0), priorityHigh()), WAIT_TIME));
    }

    @Test
    @Title("Пометка писем по tids")
    @Description("Помечаем письма прочитанными и обратно по tids")
    public void testAsyncMarkByTid() throws Exception {
        val source = new TidsSource(new ArrayList<>(tids));

        mark(source, StatusParam.READ).post(shouldBe(okAsync()));
        shouldBeTasksWithType("mark");
        assertThat(authClient, withWaitFor(hasNewMsgsInFid(equalTo(0), folderList.defaultFID())));

        mark(source, StatusParam.NOT_READ).post(shouldBe(okAsync()));
        shouldBeTasksWithType("mark");
        assertThat(authClient, withWaitFor(hasNewMsgsInFid(equalTo(MIDS_COUNT), folderList.defaultFID())));
    }

    private void fillFolder(int countOfMids, String dstFid) throws Exception {
        val startCount = updatedFolderList().count(folderList.defaultFID());

        val subjHeader = getRandomString();
        val mids = new ArrayList<String>();
        IntStream.range(0, countOfMids).mapToObj(val -> subjHeader + val).forEach(subj -> {
            try {
                val mid = sendMail(subj).firstMid();
                mids.add(mid);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });


        val endCount = updatedFolderList().count(folderList.defaultFID());
        assertThat("Не все отправленные сообщения дошли", endCount, is(equalTo(startCount + countOfMids)));

        complexMove(dstFid, new MidsSource(mids)).post(shouldBe(okSyncOrAsync()));

        assertThat("Не удалось переместить сообщения", authClient,
                withWaitFor(hasCountMsgsInFid(equalTo(startCount), folderList.defaultFID()), WAIT_TIME));
    }

    @Test
    @Issue("MAILDEV-769")
    @Title("Удаление папки с потомком, содержащим большое количество сообщений")
    @Description("Удаляем папку, потомок которой содержит количество сообщений " +
                 "достаточное для асинхронного выполнения операции.")
    public void deleteFolderWithSignificantlyFilledChild() throws Exception {
        val parentMidsCount = 2;
        val parentName = getRandomString();
        val childName = getRandomString();

        val beforeTrashSize = folderList.count(folderList.deletedFID());

        val parentFid = newFolder(parentName);
        val childFid = newFolder(childName, parentFid);

        fillFolder(parentMidsCount, parentFid);
        fillFolder(MIDS_COUNT, childFid);
        deleteFolder(parentFid).post(shouldBe(okAsync()));

        assertThat(authClient, withWaitFor(not(hasFolderWithFid(parentFid)), WAIT_TIME));
        assertThat(authClient, withWaitFor(not(hasFolderWithFid(childFid)), WAIT_TIME));

        val afterTrashSize = new FolderList(authClient).count(folderList.deletedFID());
        val expectedTrashSize = beforeTrashSize + MIDS_COUNT + parentMidsCount;

        assertThat(String.format("Ожидалось %d писем в папке \"Удаленные\", имеется %d", expectedTrashSize, afterTrashSize),
                afterTrashSize, is(equalTo(expectedTrashSize)));

        remove(new FidSource(folderList.deletedFID())).withWithSent(ApiRemove.WithSentParam._1)
                .post(shouldBe(okSyncOrAsync()));
        assertThat(authClient, withWaitFor(hasCountMsgsInFid(equalTo(0), folderList.deletedFID()), WAIT_TIME));
    }

    @Test
    @Issue("MAILDEV-826")
    @Title("Удаление метки, которой помечено большое количество сообщений")
    public void testDeleteLabel() throws Exception {
        val lid = getCustomLabelLid(CUSTOM_LABEL_NAME);

        label(new MidsSource(mids), lid).post(shouldBe(okAsync()));
        assertThat("Ожидалось, что сообщения во \"Входящих\" будут помечены пользовательской меткой",
                authClient, withWaitFor(hasCountLabelsWithLid(equalTo(MIDS_COUNT), lid), WAIT_TIME));

        deleteLabel(lid).post(shouldBe(okAsync()));
        assertThat("Ожидалось, что с сообщений во \"Входящих\" будет снята пользовательская метка",
                authClient, withWaitFor(hasCountLabelsWithLid(equalTo(0), lid), WAIT_TIME));
        assertThat("Метка не удалилась", authClient, not(hasLabel(lid)));
    }

    @Test
    @Issue("MAILDEV-986")
    @Title("Проверяем, что асинхронные операции логирют данные в user_journal.tskv")
    @IgnoreSshTest
    public void testTskvLog() throws Exception {
        SSHAuthRule sshMopsAuthRule = sshOn(URI.create(props().mopsHost()), props().getRobotGerritWebmailTeamSshKey());
        sshMopsAuthRule.authenticate();

        val lid = getCustomLabelLid(TSKV_LABEL_NAME);
        val userAgent = getRandomString();
        val connectionId = getRandomString();
        val yandexUidCookie = getRandomString();
        val icookie = getRandomString();

        label(new MidsSource(mids), lid)
            .withUa(userAgent)
            .withOraConnectionId(connectionId)
            .withYandexuid(yandexUidCookie)
            .withIcookie(icookie)
            .post(shouldBe(okAsync()));

        shouldBeTasksWithType("label");
        assertThat(authClient, withWaitFor(hasCountLabelsWithLid(equalTo(MIDS_COUNT), lid), WAIT_TIME));
        deleteLabel(lid).post(shouldBe(okAsync()));

        val checkMid = mids.get(0);
        val greps = newArrayList(authClient.account().uid(), checkMid, props().getCurrentRequestId(),
            "operation=label");
        val matcher = getLogMatcher(checkMid, userAgent, connectionId, yandexUidCookie, icookie);
        shouldSeeLogLine(matcher, greps, 0, sshMopsAuthRule);
    }

    private String getCustomLabelLid(String name) {
        val lid = updatedLabels().lidByName(name);
        return lid != null ? lid : newLabelByName(name);
    }

    private static List<Matcher<Map<? extends String, ? extends String>>> getLogMatcher(
            String mid, String userAgent, String connectionId, String yandexUidCookie,
            String icookie) {
        return asList(
                entry(TSKV_FORMAT, is("mail-user-journal-tskv-log")),
                entry(UNIXTIME, notNullValue(String.class)),
                entry(USER_AGENT, equalTo(userAgent)),
                entry(CONNECTION_ID, equalTo(connectionId + "-async")),
                entry(YANDEXUID_COOKIE, equalTo(yandexUidCookie)),
                entry(I_COOKIE, equalTo(icookie)),
                entry(TARGET, containsString(MESSAGE.toString())),
                entry(MIDS, containsString(mid)),
                entry(STATE, containsString(mid)),
                entry(REQUEST_ID, containsString(props().getCurrentRequestId()))
        );
    }

    private static Matcher<Map<? extends String, ? extends String>> entry(WmiTskv key, Matcher<String> value) {
        return hasEntry(is(key.toString()), value);
    }

    @Step("[SSH]: Должны увидеть в логе {0}, грепая по {1} (номер записи {2})")
    private static void shouldSeeLogLine(List<Matcher<Map<? extends String, ? extends String>>> logMatchers,
                                         List<String> greps, Integer entry, SSHAuthRule sshMopsAuthRule) {
        assertThat(sshMopsAuthRule.ssh().conn(), should(logEntryShouldMatch(logMatchers, TSKV_MOPS_LOG_PATH, greps, entry))
            .whileWaitingUntil(timeoutHasExpired(60*1000).withPollingInterval(500)));
    }

    @Test
    @Title("Проверка соответствия taskGroupId")
    @Description("Проверяем соответствие taskGroupId в ответе и выдаче ручки stat")
    @Issue("MAILDEV-1050")
    public void testTaskGroupId() throws Exception {
        val response = startMarkInbox(StatusParam.READ);
        shouldBeTaskWithGroupId(response.jsonPath().get("taskGroupId"));
        assertThat(authClient, withWaitFor(hasNewMsgsInFid(equalTo(0), folderList.defaultFID()), WAIT_TIME));
        markUnread(true);
    }

    @Test
    @Title("MARK с tab")
    @Description("Помечаем письмо определенным статусом по tab")
    public void markTab() throws Exception {
        Tab tab = Tab.RELEVANT;
        val source = new TabSource(tab.getName());

        complexMove(folderList.inboxFID(), tab.getName(), new MidsSource(mids)).post(shouldBe(okAsync()));
        shouldBeTasksWithType("complex_move");
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(MIDS_COUNT), tab.getName())));

        mark(source, StatusParam.READ).post(shouldBe(okAsync()));
        shouldBeTasksWithType("mark");
        assertThat(authClient, withWaitFor(hasNewMsgsInTab(equalTo(0), tab.getName())));

        mark(source, StatusParam.NOT_READ).post(shouldBe(okAsync()));
        shouldBeTasksWithType("mark");
        assertThat(authClient, withWaitFor(hasNewMsgsInTab(equalTo(MIDS_COUNT), tab.getName())));
    }

    @Test
    @Title("Перенос всего в таб Рассылки и обратно")
    @Description("Последовательно переносим все письма из Входящих в таб Расслыки, затем обратно")
    public void complexMoveToTabByFid() throws Exception {
        startMoveAll(folderList.defaultFID(), folderList.defaultFID(), Tab.NEWS.getName());
        shouldBeTasksWithType("complex_move");
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(0), Tab.defaultTab)));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(MIDS_COUNT), Tab.NEWS.getName())));

        startMoveAll(folderList.defaultFID(), folderList.defaultFID(), Tab.defaultTab);
        shouldBeTasksWithType("complex_move");
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(MIDS_COUNT), Tab.defaultTab)));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(0), Tab.NEWS.getName())));
    }

    @Test
    @Title("Перенос всего в таб Рассылки и обратно")
    @Description("Последовательно переносим все письма из таба Входящих в таб Расслыки, затем обратно")
    public void complexMoveToTabByMids() throws Exception {
        startMoveAllMids(folderList.defaultFID(), Tab.NEWS.getName());
        shouldBeTasksWithType("complex_move");
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(0), Tab.defaultTab)));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(MIDS_COUNT), Tab.NEWS.getName())));

        startMoveAllMids(folderList.defaultFID(), Tab.defaultTab);
        shouldBeTasksWithType("complex_move");
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(MIDS_COUNT), Tab.defaultTab)));
        assertThat(authClient, withWaitFor(hasCountMsgsInTab(equalTo(0), Tab.NEWS.getName())));
    }

}
