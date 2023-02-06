package ru.yandex.autotests.innerpochta.akita;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.akita.Aliases;
import ru.yandex.autotests.innerpochta.beans.akita.AuthResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ch.lambdaj.function.matcher.NotNullOrEmptyMatcher.notNullOrEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;

@Aqua.Test
@Credentials(loginGroup = "AkitaAliasesAuth")
@Features(MyFeatures.AKITA)
@Stories(MyStories.AUTH)
public class AliasesAuthTest extends AkitaBaseTest {
    @Test
    @Issue("MAILPG-4087")
    @Title("Проверка наличия атрибута aliases у пользователя")
    public void shouldReturnAliasesAttributes() {
        Aliases aliases = auth()
                .get(shouldBe(okAuth()))
                .as(AuthResponse.class)
                .getAccountInformation()
                .getAccount()
                .getAliases();
        String pdd = aliases
                .get7();

        assertThat("Список полученных алиасов должен быть пустым",
                pdd, notNullOrEmpty());
    }
}
