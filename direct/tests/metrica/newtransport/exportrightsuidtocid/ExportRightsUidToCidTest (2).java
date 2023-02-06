package ru.yandex.autotests.directintapi.tests.metrica.newtransport.exportrightsuidtocid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.general.YesNoEnum;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.ExportRightsUidToCidRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.tsv.ExportRightsUidToCidResponse;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.model.Uids;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by pavryabov on 13.11.15.
 * https://st.yandex-team.ru/TESTIRT-7726
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.EXPORT_RIGHTS_UID_TO_CID)
@Description("Проверка маппинга uids - cid в ручке ExportRightsUidToCid")
@Issue("https://st.yandex-team.ru/DIRECT-47326")
@RunWith(Parameterized.class)
public class ExportRightsUidToCidTest {

    private static final String LOGIN_FOR_USD = ru.yandex.autotests.directapi.darkside.Logins.LOGIN_USD; //используем свой логин, чтобы тесты апи не мешали при создании кампании
    // менеджеры из IDM-группы с ID=1000,
    private static final Collection<Long> TEST_IDM_GROUP_MANAGERS =
            Arrays.asList(Uids.MANAGER_UID, Uids.TEAMLEADER_UID, Uids.SUPER_TEAMLEADER_UID, Uids.TRANSFER_MANAGER_UID);

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public String creator;

    @Parameterized.Parameter(value = 2)
    public String client;

    @Parameterized.Parameter(value = 3)
    public List<Long> expectedUids;

    @Parameterized.Parameter(value = 4)
    public YesNoEnum settingValue;

    @Parameterized.Parameters(name = "{0}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                {"самостоятельный клиент",
                        Logins.CLIENT_SELF, Logins.CLIENT_SELF, Arrays.asList(), YesNoEnum.NO},
                {"самостоятельный клиент, кампания создана супером",
                        Logins.SUPER_LOGIN, Logins.CLIENT_SELF, Arrays.asList(), YesNoEnum.NO},
                {"сервисируемый клиент, кампания создана клиентом",
                        LOGIN_FOR_USD, LOGIN_FOR_USD, Arrays.asList(), YesNoEnum.NO},
                {"сервисируемый клиент, кампания создана супером",
                        Logins.SUPER_LOGIN, LOGIN_FOR_USD, TEST_IDM_GROUP_MANAGERS, YesNoEnum.YES},
                {"сервисируемый клиент, кампания создана менеджером",
                        Logins.MANAGER_DEFAULT, LOGIN_FOR_USD, TEST_IDM_GROUP_MANAGERS, YesNoEnum.YES},
                {"субклиент, на которого имеет права представитель агентства по работе с клиентами",
                        Logins.AGENCY_TMONEY, Logins.TMONEY_CLIENT0,
                        appendToList(TEST_IDM_GROUP_MANAGERS, Uids.AGENCY_TMONEY_UID, Uids.AGENCY_TMONEY_REP_UID),
                        YesNoEnum.YES},
                {"субклиент, на которого не имеет прав представитель агентства по работе с клиентами",
                        Logins.AGENCY_TMONEY, Logins.TMONEY_CLIENT5,
                        appendToList(TEST_IDM_GROUP_MANAGERS, Uids.AGENCY_TMONEY_UID), YesNoEnum.YES},
                {"субклиент, у агентства которого есть представитель",
                        Logins.AGENCY_CAMPAIGNS, Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        appendToList(TEST_IDM_GROUP_MANAGERS, Uids.AGENCY_CAMPAIGNS_UID, Uids.AGENCY_CAMPAIGNS_REP_UID),
                        YesNoEnum.YES},
                {"at-direct-api-test, кампанию создает менеджер",
                        Logins.MANAGER_DEFAULT, Logins.CLIENT_FREE_YE_DEFAULT,
                        appendToList(TEST_IDM_GROUP_MANAGERS, Uids.AGENCY_DEFAULT_UID,
                                Uids.AGENCY_DEFAULT_UNLIMIT_REP_UID, Uids.AGENCY_DEFAULT_ANOTHER_REP_UID),
                        YesNoEnum.YES},
                //после DIRECT-50575 менеджер создает агентскую кампанию
                {"at-direct-api-test, кампанию создает агентство",
                        Logins.AGENCY_YE_DEFAULT, Logins.CLIENT_FREE_YE_DEFAULT,
                        appendToList(TEST_IDM_GROUP_MANAGERS, Uids.AGENCY_DEFAULT_UID,
                                Uids.AGENCY_DEFAULT_UNLIMIT_REP_UID, Uids.AGENCY_DEFAULT_ANOTHER_REP_UID),
                        YesNoEnum.YES},
        };
        return Arrays.asList(data);
    }

    @SafeVarargs
    private static <T> List<T> appendToList(Collection<T> list, T... elems) {
        // метод простой, поэтому проще написать, чем искать в библиотеках
        List<T> result = new ArrayList<>(list.size() + elems.length);
        result.addAll(list);
        result.addAll(Arrays.asList(elems));
        return result;
    }

    @Test
    public void getUidsByCid() {
        //DIRECT-48377
        //DIRECT-48390
        //DIRECT-48392
        Long cid = api.as(creator).userSteps.campaignSteps()
                .addDefaultTextCampaignWithRequireServicing(settingValue, client);
        api.userSteps.campaignFakeSteps().setRandomOrderID(cid.intValue());
        ExportRightsUidToCidResponse response = api.userSteps.getDarkSideSteps().getExportRightsUidToCidSteps()
                .exportRightsUidToCid(new ExportRightsUidToCidRequest().withCids(cid));
        List<ExportRightsUidToCidResponse.UidCidPair> expectedPairs = expectedUids.stream()
                .map(uid -> new ExportRightsUidToCidResponse.UidCidPair(uid, cid))
                .collect(Collectors.toList());

        List<Matcher<? super ExportRightsUidToCidResponse.UidCidPair>> expectedMatchers =
                expectedPairs.stream().map(BeanDifferMatcher::beanDiffer).collect(Collectors.toList());
        assertThat("вернулись правильные пары uid - cid",
                response.getUidCidPairs(), Matchers.containsInAnyOrder(expectedMatchers));
    }
}
