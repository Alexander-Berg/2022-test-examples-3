package ru.yandex.autotests.innerpochta.hound.v2.positive;

import lombok.val;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.Change;
import ru.yandex.autotests.innerpochta.beans.hound.V2ChangelogResponse;
import ru.yandex.autotests.innerpochta.beans.hound.MailboxRevisionResponse;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.changelog.ApiChangelog;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops.label;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops.newLabelByName;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/changelog")
@Description("Тесты на ручку v2/changelog")
@Features(MyFeatures.HOUND)
@Stories(MyStories.CHANGLOG)
@Credentials(loginGroup = "HoundV2ChangelogTest")
public class ChangelogPositiveTest extends BaseHoundTest {

    @ClassRule
    public static CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @ClassRule
    public static DeleteLabelsMopsRule deleteLabels = new DeleteLabelsMopsRule(authClient);

    @ClassRule
    public static DeleteFoldersRule deleteFoldersRule = DeleteFoldersRule.with(authClient).all();


    @Before
    @Description("Добавлят запись в базу, чтобы mailboxRevision не выдавал ошибку, когда записей нет. " +
            "Запоманиет последнюю ревизию. Отсылает письмо сам себе - 2 записи store в базе. Создает метку. Помечает письмо. Всего 4 изменения")
    public void Prepare() throws Exception {
        newFolder(authClient, Util.getRandomString());

        lastRevision = apiHound().mailboxRevision().withUid(uid()).get(shouldBe(ok200()))
                .as(MailboxRevisionResponse.class)
                .getMailboxRevision();

        String mid = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        val lid = newLabelByName(authClient, Util.getRandomString());
        label(authClient, new MidsSource(mid), asList(lid)).post(shouldBe(okSync()));
    }

    @Test
    @Title("Должен вернуть пустой список для слишком большой ревизии когда check_revision_policy=loyal")
    public void shouldReceiveEmptyListForBigRevision() {
        Long revision = apiHound().mailboxRevision().withUid(uid()).get(shouldBe(ok200()))
                .as(MailboxRevisionResponse.class)
                .getMailboxRevision();

        List<Change> changes = apiHoundV2().changelog()
                .withUid(uid())
                .withRevision(String.valueOf(revision + 1))
                .withMaxCount(Integer.toString(USUAL_CHANGELOG_COUNT))
                .withCheckRevisionPolicy(ApiChangelog.CheckRevisionPolicyParam.LOYAL)
                .get(shouldBe(ok200()))
                .as(V2ChangelogResponse.class)
                .getChanges();

        assertThat("Ожидали 0 изменений", changes.size(), equalTo(0));
    }

    @Test
    @Title("Должен вернуть все изменения с проверкой ревизии")
    public void shouldReceiveAllChanges() {
        List<Change> changes = apiHoundV2().changelog()
                .withUid(uid())
                .withRevision(String.valueOf(lastRevision))
                .withMaxCount(Integer.toString(USUAL_CHANGELOG_COUNT))
                .get(shouldBe(ok200()))
                .as(V2ChangelogResponse.class)
                .getChanges();

        assertThat("Ожидали 4 изменения", changes.size(), equalTo(4));
        assertThat("Ожидали изменение store", changes.get(0).getType(), equalTo("store"));
        assertThat("Ожидали изменение store", changes.get(1).getType(), equalTo("store"));
        assertThat("Ожидали изменение label-create", changes.get(2).getType(), equalTo("label-create"));
        assertThat("Ожидали изменение update", changes.get(3).getType(), equalTo("update"));
    }

    @Test
    @Title("Должен вернуть первые max_count изменений")
    public void shouldReceiveMaxCountChanges() {
        List<Change> changes =  apiHoundV2().changelog()
                .withUid(uid())
                .withRevision(String.valueOf(lastRevision))
                .withMaxCount(Integer.toString(EXPECTED_NUMBER_OF_CHANGES))
                .get(shouldBe(ok200()))
                .as(V2ChangelogResponse.class)
                .getChanges();

        assertThat("Ожидали 3 изменения", changes.size(), equalTo(EXPECTED_NUMBER_OF_CHANGES));
        assertThat("Ожидали изменение store", changes.get(0).getType(), equalTo("store"));
        assertThat("Ожидали изменение store", changes.get(1).getType(), equalTo("store"));
        assertThat("Ожидали изменение label-create", changes.get(2).getType(), equalTo("label-create"));
    }

    @Test
    @Title("Должен отфильтровать по типу и вернуть только изменения label-create и update")
    public void shouldReceiveFilteredChanges() {
        List<Change> changes =  apiHoundV2().changelog()
                .withUid(uid())
                .withRevision(String.valueOf(lastRevision))
                .withMaxCount(Integer.toString(USUAL_CHANGELOG_COUNT))
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.LABELCREATE)
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.UPDATE)
                .get(shouldBe(ok200()))
                .as(V2ChangelogResponse.class)
                .getChanges();

        assertThat("Ожидали 2 изменения", changes.size(), equalTo(2));
        assertThat("Ожидали изменение label-create", changes.get(0).getType(), equalTo("label-create"));
        assertThat("Ожидали изменение update", changes.get(1).getType(), equalTo("update"));
    }

    private static Long lastRevision;
    private static int USUAL_CHANGELOG_COUNT = 100;
    private static int EXPECTED_NUMBER_OF_CHANGES = 3;
}
