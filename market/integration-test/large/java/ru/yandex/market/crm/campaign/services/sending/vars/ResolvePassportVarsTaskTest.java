package ru.yandex.market.crm.campaign.services.sending.vars;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.domain.sending.SystemVar;
import ru.yandex.market.crm.campaign.domain.sending.Variable;
import ru.yandex.market.crm.campaign.services.sending.context.VarResolvingContext;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUser;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.misc.thread.ThreadUtils;

import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;

/**
 * @author apershukov
 */
public class ResolvePassportVarsTaskTest extends AbstractServiceLargeTest {

    private static class ContextMock implements VarResolvingContext {

        @Override
        public YPath getVarsDirectory() {
            return VARS;
        }

        @Override
        public YPath getSourceTable() {
            return SOURCE_TABLE;
        }

        @Override
        public Collection<Variable> getUsedVars() {
            return Collections.singleton(SystemVar.LASTNAME);
        }

        @Override
        public String getKeyColumnName() {
            return "email";
        }

        @Override
        public UidType getKeyUidType() {
            return UidType.EMAIL;
        }
    }

    private static class ControlStub implements Control<ResolveSystemVarsTaskData> {

        private ResolveSystemVarsTaskData data;

        @Override
        public void saveData(ResolveSystemVarsTaskData data) {
            this.data = data;
        }

        ResolveSystemVarsTaskData getData() {
            return data;
        }
    }

    private static YTreeMapNode profile(String lastname) {
        return passportProfile(PUID, "m", null, lastname);
    }

    private static YTreeMapNode profileAvatar(String avatar) {
        return passportProfile(PUID, "m", null, null, "default_login", "login", avatar);
    }

    private static final long PUID = 111;
    private static final YPath DIR = YPath.cypressRoot().child(ResolvePassportVarsTaskTest.class.getSimpleName());
    private static final YPath SOURCE_TABLE = DIR.child("emails");
    private static final YPath VARS = DIR.child("vars");

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private YtClient ytClient;

    @Inject
    private ResolvePassportVarsTask resolveUserNamesTask;

    @BeforeEach
    public void setUp() {
        CampaignUser user = new CampaignUser()
                .setEmail("user@yandex.ru")
                .setOriginalUid(Uid.asPuid(PUID));

        ytClient.write(SOURCE_TABLE, CampaignUser.class, Collections.singletonList(user));
    }

    /**
     * Если имя и фамилия удовлетворяют критериям, значение переменной lastname резолвится
     */
    @Test
    public void testResolveLastname() throws Exception {
        preparePassportProfiles(
                profile("Пупкин")
        );

        List<YTreeMapNode> results = resolve();

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("Пупкин", results.get(0).getString("value"));
    }

    @Test
    public void testResolveLastnameInLatin() throws Exception {
        preparePassportProfiles(
                profile("Pupkin")
        );

        List<YTreeMapNode> results = resolve();

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("Pupkin", results.get(0).getString("value"));
    }

    /**
     * Если фамилия не указана, значение переменной lastname не резолвится
     */
    @Test
    public void testDoNotResolveUsernameIfLastnameIsNotSpecified() throws Exception {
        preparePassportProfiles(
                profile(null)
        );

        List<YTreeMapNode> results = resolve();
        Assertions.assertTrue(results.isEmpty());
    }

    /**
     * Если имя или фамилия содержат цифры, значение переменной lastname не резолвится
     */
    @Test
    public void testDoNotResolveLastnameIfItContainsDigit() throws Exception {
        preparePassportProfiles(
                profile("Pupk1n")
        );

        List<YTreeMapNode> results = resolve();
        Assertions.assertTrue(results.isEmpty());
    }

    /**
     * Если фамилия состоит всего из одной буквы, значение переменной lastname не резолвится
     */
    @Test
    public void testDoNotResolveLastnameIfItContainsSingleLetter() throws Exception {
        preparePassportProfiles(
                profile("P")
        );

        List<YTreeMapNode> results = resolve();
        Assertions.assertTrue(results.isEmpty());
    }

    /**
     * Если в фамилии есть символ "-", значение переменной lastname резолвится
     */
    @Test
    public void testResolveUsernameWithLastnameHavingMinusSymbol() throws Exception {
        preparePassportProfiles(
                profile("Бестужев-Рюмин")
        );

        List<YTreeMapNode> results = resolve();
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("Бестужев-Рюмин", results.get(0).getString("value"));
    }

    /**
     * Если в фамилии есть повторяющийся символ "-", значение переменной lastname не резолвится
     */
    @Test
    public void testDoNotResolveLastnameIfItContainsRepeatedMinusSymbol() throws Exception {
        preparePassportProfiles(
                profile("Бестужев--Рюмин")
        );

        List<YTreeMapNode> results = resolve();
        Assertions.assertTrue(results.isEmpty());
    }

    /**
     * Если аватарка есть, значение переменной default_avatar резолвится в ссылку
     */
    @Test
    public void testResolveUserAvatar() throws Exception {
        preparePassportProfiles(
            profileAvatar("49368/269450396-467701428")
        );

        List<YTreeMapNode> results = resolve(SystemVar.AVATAR);

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("https://avatars.mds.yandex.net/get-yapic/49368/269450396-467701428/islands-retina-50",
            results.get(0).getString("value"));
    }

    /**
     * Если аватарка null, значение переменной default_avatar резолвится в ссылку на пустую дефолтную аватарку
     */
    @Test
    public void testResolveUserAvatarWithNullValue() throws Exception {
        preparePassportProfiles(
            profileAvatar(null)
        );

        List<YTreeMapNode> results = resolve(SystemVar.AVATAR);

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("https://avatars.mds.yandex.net/get-yapic/0/0-0/islands-retina-50",
            results.get(0).getString("value"));
    }

    private void preparePassportProfiles(YTreeMapNode... profiles) {
        ytSchemaTestHelper.preparePassportProfilesTable();
        userTestHelper.addPassportProfiles(profiles);
    }

    private List<YTreeMapNode> resolve() throws Exception {
        return resolve(SystemVar.LASTNAME);
    }

    private List<YTreeMapNode> resolve(SystemVar systemVar) throws Exception {
        ResolveSystemVarsTaskData data = new ResolveSystemVarsTaskData(Collections.singleton(systemVar));
        ControlStub control = new ControlStub();
        ContextMock context = new ContextMock();

        while (true) {
            ExecutionResult result = resolveUserNamesTask.run(context, data, control);
            data = control.getData();

            TaskStatus status = result.getNextStatus();

            if (status == TaskStatus.COMPLETING) {
                return ytClient.read(context.getVarTable(systemVar), YTableEntryTypes.YSON);
            }

            Assertions.assertEquals(TaskStatus.WAITING, status, "Unexpected task status");

            ThreadUtils.sleep(1, TimeUnit.SECONDS);
        }
    }
}
