package ru.yandex.autotests.innerpochta.hound.v2.negative;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isOneOf;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.InvalidArguments.UNEXISTING_UID;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgument;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.unknownUid;

@Aqua.Test
@Title("[HOUND] Ручка v2/changes")
@Description("Тесты на ручку v2/changes")
@Features(MyFeatures.HOUND)
@Stories(MyStories.CHANGLOG)
@Credentials(loginGroup = "HoundV2Changes")
public class ChangesNegativeTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clearFolders = DeleteFoldersRule.with(authClient).all().before(true);

    @Test
    @Title("Проверка вызова без uid'а")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400WithoutUid() {
        apiHoundV2().changes()
                .get(shouldBe(invalidArgument(equalTo("uid parameter is required"))));
    }

    @Test
    @Title("Проверка вызова с неизвестным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForUnknownUid() {
        apiHoundV2().changes()
                .withUid(UNEXISTING_UID)
                .withRevision("1")
                .withMaxCount("1")
                .get(shouldBe(unknownUid()));
    }

    @Test
    @Title("Проверка вызова с без указания max_count")
    public void shouldReceive400WithoutMaxCount() {
        apiHoundV2().changes()
                .withUid(authClient.account().uid())
                .withRevision("0")
                .get(shouldBe(invalidArgument(equalTo("invalid max_count argument"))));
    }

    @Test
    @Title("Проверка вызова с без указания revision")
    public void shouldReceive400WithoutRevision() {
        apiHoundV2().changes()
                .withUid(authClient.account().uid())
                .withMaxCount("1000")
                .get(shouldBe(invalidArgument(equalTo("invalid revision argument"))));
    }

    @Test
    @Title("Проверка вызова с без указания max_count и revision")
    public void shouldReceive400WithoutRevisionAndMaxCount() {
        apiHoundV2().changes()
                .withUid(authClient.account().uid())
                .get(shouldBe(invalidArgument(isOneOf("invalid revision argument", "invalid max_count argument"))));
    }

    @Test
    @Title("Проверка вызова с max_count < 0")
    public void shouldReceive400WithMaxCountLess0() {
        apiHoundV2().changes()
                .withUid(authClient.account().uid())
                .withRevision("0")
                .withMaxCount("-1")
                .get(shouldBe(invalidArgument(equalTo("max_count can not be less or equal to 0"))));
    }

    @Test
    @Title("Проверка вызова с max_count = 0")
    public void shouldReceive400WithMaxCountEquals0() {
        apiHoundV2().changes()
                .withUid(authClient.account().uid())
                .withRevision("0")
                .withMaxCount("0")
                .get(shouldBe(invalidArgument(equalTo("max_count can not be less or equal to 0"))));
    }

    @Test
    @Title("Проверка вызова с некорректным max_count")
    public void shouldReceive400WithIncorrectMaxCount() {
        apiHoundV2().changes()
                .withUid(authClient.account().uid())
                .withRevision("0")
                .withMaxCount("ЧНЯ")
                .get(shouldBe(invalidArgument(equalTo("invalid max_count argument"))));
    }

    @Test
    @Title("Проверка вызова с некорректным revision")
    public void shouldReceive400WithIncorrectRevision() {
        apiHoundV2().changes()
                .withUid(authClient.account().uid())
                .withRevision("Y")
                .withMaxCount("100")
                .get(shouldBe(invalidArgument(equalTo("invalid revision argument"))));
    }
}
