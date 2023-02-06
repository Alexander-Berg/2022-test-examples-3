package ru.yandex.autotests.directintapi.tests.bsfront.changenotify;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontChangeNotifyResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.Creative;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by pavryabov on 04.09.15.
 * https://st.yandex-team.ru/TESTIRT-6894
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка прав доступа оператора к клиенту в BsFront.change_notify. Негативные проверки")
@Issue("https://st.yandex-team.ru/DIRECT-43716")
@RunWith(Parameterized.class)
public class BsFrontChangeNotifyEditRightsNegativeTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public String operatorLogin;

    @Parameterized.Parameter(value = 2)
    public String clientLogin;

    @Parameterized.Parameters(name = "{0}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                //Нет прав оператора на клиента
                {"менеджер - клиент", Logins.MANAGER_DEFAULT, Logins.CLIENT_FOR_RUB},
                {"агентство - сервисируемый клиент", Logins.AGENCY_CAMPAIGNS, Logins.LOGIN_FOR_RUB},
                {"агентство - клиент", Logins.AGENCY_CAMPAIGNS, Logins.CLIENT_FOR_RUB},
                {"агентство - чужой субклиент", Logins.AGENCY_YE_DEFAULT, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS},
                {"клиент - другой клиент", Logins.CLIENT_FOR_RUB, Logins.LOGIN_FOR_RUB},
                {"клиент - субклиент", Logins.CLIENT_FOR_RUB, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS},
                {"субклиент с правами редактирования - субклиент без прав редактирования",
                        Logins.SUB_CLIENT_WITH_EDIT_RIGHTS, Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS},
                {"субклиент без прав редактирования - субклиент с правами редактирования",
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS},
                //Есть права оператора на клиента, но нет прав на создание креативов
                {"супер без клиента", Logins.SUPER_LOGIN, null},
                {"супер - агентство", Logins.SUPER_LOGIN, Logins.AGENCY_YE_DEFAULT},
                {"суперридер - клиент", Logins.SUPER_READER, Logins.CLIENT_FOR_RUB},
                {"вешальщик - клиент", Logins.PLACER, Logins.CLIENT_FOR_RUB},
                {"медиа - клиент", Logins.MEDIA, Logins.CLIENT_FOR_RUB},
                {"медиа - сервисируемый клиент", Logins.MEDIA, Logins.LOGIN_FOR_RUB},
                {"тимлидер - клиент", Logins.TEAMLEADER, Logins.CLIENT_FOR_RUB},
                {"менеджер - менеджер", Logins.MANAGER_DEFAULT, Logins.MANAGER_DEFAULT},
                {"менеджер - агентство", Logins.MANAGER_DEFAULT, Logins.AGENCY_YE_DEFAULT},
                {"агентство - агентство", Logins.AGENCY_CAMPAIGNS, Logins.AGENCY_CAMPAIGNS},
                {"субклиент без прав редактирования - субклиент без прав редактирования",
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS, Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS},
                {"субклиент без прав редактирования - субклиент c правами редактирования",
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS}
        };
        return Arrays.asList(data);
    }

    private Integer uid;

    @Before
    public void getUid() {
        uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(operatorLogin).getPassportID());
    }

    @Test
    public void checkError() {
        //DIRECT-45922
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .changeNotifyExpectError(new BsFrontRequest()
                                .withOperatorUid(uid)
                                .withClientLogin(clientLogin)
                                .withCreatives(new Creative().withId(123l)),
                        BsFrontChangeNotifyResponse.OPERATOR_NOT_ALLOWED_CREATE_CREATIVES);
    }
}
