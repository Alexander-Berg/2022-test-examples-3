package ru.yandex.autotests.directintapi.tests.bsfront.auth;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontAuthResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontRequest;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by pavryabov on 27.08.15.
 * https://st.yandex-team.ru/TESTIRT-6894
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка прав и шаблонов при запросе только с operator_uid")
@Issue("https://st.yandex-team.ru/DIRECT-43716")
@RunWith(Parameterized.class)
public class BsFrontAuthOnlyOperatorTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public String login;

    @Parameterized.Parameter(value = 2)
    public List<String> expectedActions;

    @Parameterized.Parameter(value = 3)
    public List<Integer> expectedTemplates;
    private BsFrontAuthResponse bsFrontAuthResponse;

    @Parameterized.Parameters(name = "{0}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                {"супер", Logins.SUPER_LOGIN, new ArrayList<String>(), new ArrayList<Integer>()},
                {"саппорт", Logins.SUPPORT, new ArrayList<String>(), new ArrayList<Integer>()},
                {"суперридер", Logins.SUPER_READER, new ArrayList<String>(), new ArrayList<Integer>()},
                {"вешальщик", Logins.PLACER, new ArrayList<String>(), new ArrayList<Integer>()},
                {"тимлидер", Logins.TEAMLEADER, new ArrayList<String>(), new ArrayList<Integer>()},
                {"медиа", Logins.MEDIA, new ArrayList<String>(), new ArrayList<Integer>()},
                {"менеджер", Logins.MANAGER_DEFAULT, new ArrayList<String>(), new ArrayList<Integer>()},
                {"агентство", Logins.AGENCY_YE_DEFAULT, new ArrayList<String>(), new ArrayList<Integer>()},
                {"старый клиент", Logins.CLIENT_FREE_YE_DEFAULT,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"представитель клиента", Logins.CLIENT_FREE_YE_DEFAULT_REP,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"клиент", Logins.CLIENT_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"сервисируемый клиент", Logins.LOGIN_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"легкий клиент", Logins.CLIENT_LIGHT,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"субклиент с правами редактирования", Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"субклиент без прав редактирования", Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        BsFrontAuthResponse.getActions, BsFrontAuthResponse.genericTemplates}
        };
        return Arrays.asList(data);
    }

    @Before
    public void callBsFront() {
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(login).getPassportID());
        bsFrontAuthResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().auth(new BsFrontRequest().withOperatorUid(uid));
    }

    @Test
    public void checkAvailableActions() {
        assertThat("список прав соответствует ожидаемому",
                bsFrontAuthResponse.getAvailableActions(),
                containsInAnyOrder(expectedActions.toArray(new String[expectedActions.size()])));
    }

    @Test
    public void checkAvailableTemplates() {
        assertThat("список шаблонов соответствует ожидаемому",
                bsFrontAuthResponse.getAvailableTemplates(), hasSize(sizeMatcher()));
    }

    private Matcher<Integer> sizeMatcher() {
        if (expectedTemplates.size() == 0) {
            return equalTo(0);
        } else {
            return greaterThanOrEqualTo(expectedTemplates.size());
        }
    }

}
