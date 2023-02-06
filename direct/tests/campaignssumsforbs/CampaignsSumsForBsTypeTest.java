package ru.yandex.autotests.directintapi.tests.campaignssumsforbs;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.CampaignsSumsForBsResponse;
import ru.yandex.autotests.directapi.darkside.model.CampaignsType;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.campaigns.Status;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 03.11.15.
 * https://st.yandex-team.ru/TESTIRT-7635
 */
@Aqua.Test(title = "CampaignsSumsForBS - проверка поля type")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.CAMPAIGNS_SUMS_FOR_BS)
@Issue("https://st.yandex-team.ru/DIRECT-47474")
@RunWith(Parameterized.class)
public class CampaignsSumsForBsTypeTest {

    static DarkSideSteps darkSideSteps = new DarkSideSteps();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static Long cid;
    private Float sum;

    @Parameterized.Parameter(0)
    public CampaignsType type;

    @Parameterized.Parameters(name = "type = {0}")
    public static Collection<Object[]> data() {
        return Stream.of(CampaignsType.values()).map(x -> new Object[]{x}).collect(Collectors.toList());
    }

    @BeforeClass
    public static void createCampaign() {
        cid = api.as(Logins.LOGIN_MAIN).userSteps.campaignSteps().addDefaultTextCampaign();
    }

    @Before
    public void prepareCampaign() {
        cid = api.as(Logins.LOGIN_MAIN).userSteps.campaignSteps().addDefaultTextCampaign();
        sum = RandomUtils.getRandomFloat(0f, 1000000f);
        darkSideSteps.getCampaignFakeSteps().setCampaignSum(cid, sum);
        darkSideSteps.getCampaignFakeSteps().setType(cid, type);
    }

    @Test
    public void campaignsSumsForBsTest() {
        List<CampaignsSumsForBsResponse> response = darkSideSteps.getCampaignsSumsForBsSteps().get(cid);
        assertThat("ручка CampaignsSumsForBS вернула правильный ответ", response,
                beanDiffer(Arrays.asList(
                        new CampaignsSumsForBsResponse(cid, sum, Status.PENDING, type.value(), Currency.RUB.getIsoCode())
                )));
    }
}
