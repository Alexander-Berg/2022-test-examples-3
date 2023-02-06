package ru.yandex.autotests.directintapi.tests.mirrors;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by pavryabov on 27.04.15.
 * https://st.yandex-team.ru/TESTIRT-5328
 * Почему часть кейсов закомментирована:
 * https://st.yandex-team.ru/TESTIRT-9464
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.MIRRORS)
@Description("Тесты ручки Mirrors.get_domain_filter_for_bs")
@Issue("https://st.yandex-team.ru/DIRECT-41139")
@RunWith(Parameterized.class)
public class GetDomainFilterForBsTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public String[] inputDomains;

    @Parameterized.Parameter(value = 2)
    public String[] expectedDomains;

    @Parameterized.Parameters(name = "{0}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                {"yandex.ru", new String[]{"yandex.ru"}, new String[]{"yandex.ru"}},
                {"ya.ru", new String[]{"ya.ru"}, new String[]{"ya.ru"}},
                {"www.yandex.ru", new String[]{"www.yandex.ru"}, new String[]{"www.yandex.ru"}},
                {"www.ya.ru", new String[]{"www.ya.ru"}, new String[]{"www.ya.ru"}},
                {"Yandex.ru", new String[]{"Yandex.ru"}, new String[]{"yandex.ru"}},
                {"домен.рф", new String[]{"домен.рф"}, new String[]{"xn--d1acufc.xn--p1ai"}},
                {"xn--d1acufc.xn--p1ai", new String[]{"xn--d1acufc.xn--p1ai"}, new String[]{"xn--d1acufc.xn--p1ai"}},
                {"ya.ru, mail.ru", new String[]{"ya.ru", "mail.ru"}, new String[]{"ya.ru", "mail.ru"}},
                {"ya.ru, ya.ru", new String[]{"ya.ru", "ya.ru"}, new String[]{"ya.ru", "ya.ru"}},
//                {"http://yandex.ru", new String[]{"http://yandex.ru"}, new String[]{null}},
//                {"https://yandex.ru", new String[]{"https://yandex.ru"}, new String[]{null}},
//                {"ssh://yandex.ru", new String[]{"ssh://yandex.ru"}, new String[]{null}},
//                {"123", new String[]{"123"}, new String[]{null}},
//                {"yandex.ru/404", new String[]{"yandex.ru/404"}, new String[]{null}},
        };
        return Arrays.asList(data);
    }

    @Test
    public void getDomainFilterForBs() {
        String[] result = api.userSteps.getDarkSideSteps().getMirrorsSteps().getDomainFilterForBs(inputDomains);
        assertThat("ручка вернула правлиьный список доменов", result, equalTo(expectedDomains));
    }
}
