package ru.yandex.autotests.innerpochta.hound.v2.negative;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.specification.ResponseSpecification;
import org.junit.Test;
import org.seleniumhq.jetty7.http.HttpStatus;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.MailboxRevisionResponse;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.changelog.ApiChangelog;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.InvalidArguments.UNEXISTING_UID;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;

@Aqua.Test
@Title("[HOUND] Ручка v2/changelog")
@Description("Тесты на ручку v2/changelog")
@Features(MyFeatures.HOUND)
@Stories(MyStories.CHANGLOG)
@Credentials(loginGroup = "HoundV2ChangelogTest")
public class ChangelogNegativeTest extends BaseHoundTest {

    @Test
    @Title("Проверка вызова без uid'а")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400WithoutUid() {
        apiHoundV2().changelog()
                .withRevision("1")
                .withMaxCount(Integer.toString(USUAL_CHANGELOG_COUNT))
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.STORE)
                .get(shouldBe(badRequest400()));
    }

    @Test
    @Title("Проверка вызова с неизвестным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForUnknownUid() {
        apiHoundV2().changelog()
                .withUid(UNEXISTING_UID)
                .withRevision("1")
                .withMaxCount(Integer.toString(USUAL_CHANGELOG_COUNT))
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.STORE)
                .get(shouldBe(badRequest400()));
    }

    @Test
    @Title("Проверка вызова с некорректным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForIncorrectUid() {
        apiHoundV2().changelog()
                .withUid("abacaba")
                .withRevision("1")
                .withMaxCount(Integer.toString(USUAL_CHANGELOG_COUNT))
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.STORE)
                .get(shouldBe(badRequest400()));
    }

    @Test
    @Title("Проверка вызова без revision")
    public void shouldReceive400WithoutRevision() {
        apiHoundV2().changelog()
                .withUid(uid())
                .withMaxCount(Integer.toString(USUAL_CHANGELOG_COUNT))
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.STORE)
                .get(shouldBe(badRequest400()));
    }

    @Test
    @Title("Проверка вызова с некорректным revision")
    public void shouldReceive400ForIncorrectRevision() {
        apiHoundV2().changelog()
                .withUid(uid())
                .withRevision("abacaba")
                .withMaxCount(Integer.toString(USUAL_CHANGELOG_COUNT))
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.STORE)
                .get(shouldBe(badRequest400()));
    }

    @Test
    @Title("Проверка вызова без max_count")
    public void shouldReceive400WithoutMaxCount() {
        apiHoundV2().changelog()
                .withUid(uid())
                .withRevision("1")
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.STORE)
                .get(shouldBe(badRequest400()));
    }

    @Test
    @Title("Проверка вызова с нулевым max_count")
    public void shouldReceive400ForZeroMaxCount() {
        apiHoundV2().changelog()
                .withUid(uid())
                .withRevision("1")
                .withMaxCount("0")
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.STORE)
                .get(shouldBe(badRequest400()));
    }

    @Test
    @Title("Проверка вызова со слишком большим max_count")
    public void shouldReceive400ForBigMaxCount() {
        apiHoundV2().changelog()
                .withUid(uid())
                .withRevision("1")
                .withMaxCount(Integer.toString(MAX_CHANGELOG_COUNT + 1))
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.STORE)
                .get(shouldBe(badRequest400()));
    }

    @Test
    @Title("Проверка вызова с некорректным max_count")
    public void shouldReceive400ForIncorrectMaxCount() {
        apiHoundV2().changelog()
                .withUid(uid())
                .withRevision("1")
                .withMaxCount("abacaba")
                .withChangelogTypes(ApiChangelog.ChangelogTypesParam.STORE)
                .get(shouldBe(badRequest400()));
    }

    @Test
    @Title("Должен вернуть 400 для несуществующей ревизии когда check_revision_policy=strict")
    public void shouldReceive400ForBigRevision() {
        Long revision = apiHound().mailboxRevision().withUid(uid()).get(shouldBe(ok200()))
                .as(MailboxRevisionResponse.class)
                .getMailboxRevision();

        apiHoundV2().changelog()
                .withUid(uid())
                .withRevision(String.valueOf(revision + 1))
                .withMaxCount(Integer.toString(USUAL_CHANGELOG_COUNT))
                .withCheckRevisionPolicy(ApiChangelog.CheckRevisionPolicyParam.STRICT)
                .get(shouldBe(badRequest400()));
    }

    public static ResponseSpecification badRequest400() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.BAD_REQUEST_400)
                .build();
    }

    private static int MAX_CHANGELOG_COUNT = 1000;
    private static int USUAL_CHANGELOG_COUNT = 100;
}
