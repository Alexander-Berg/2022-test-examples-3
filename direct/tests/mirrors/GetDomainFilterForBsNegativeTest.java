package ru.yandex.autotests.directintapi.tests.mirrors;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.exceptions.DarkSideException;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by pavryabov on 29.04.15.
 * https://st.yandex-team.ru/TESTIRT-5328
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.MIRRORS)
@Description("Проверка ошибочного вызова Mirrors.get_domain_filter_for_bs")
@Issue("https://st.yandex-team.ru/DIRECT-41139")
public class GetDomainFilterForBsNegativeTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Test
    public void getDomainFilterForBsNegativeTest() {
        try {
            api.userSteps.getDarkSideSteps().getMirrorsSteps().getDomainFilterForBs(null);
            throw new AssertionError("успешный вызов Mirrors.get_domain_filter_for_bs с null вместо массива строк");
        } catch (DarkSideException e) {
            assertThat("вернулось правильное исключение",
                    e.getCause().getClass().toString(), equalTo(JsonRpcClientException.class.toString()));
        }
    }
}
