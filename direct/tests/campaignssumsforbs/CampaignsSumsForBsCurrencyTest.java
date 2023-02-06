package ru.yandex.autotests.directintapi.tests.campaignssumsforbs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsCurrency;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
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
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 03.11.15.
 * https://st.yandex-team.ru/TESTIRT-7635
 */
@Aqua.Test(title = "CampaignsSumsForBS - проверка поля CurrencyISOCode")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.CAMPAIGNS_SUMS_FOR_BS)
@Issue("https://st.yandex-team.ru/DIRECT-47474")
@RunWith(Parameterized.class)
public class CampaignsSumsForBsCurrencyTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private final static String LOGIN = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static Long cid;
    private Float sum;

    @Parameterized.Parameter(0)
    public String currencyInDB;

    @Parameterized.Parameter(1)
    public Integer currencyISOCode;

    @Parameterized.Parameters(name = "currencyInDB = {0}, currencyISOCode = {1}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {Currency.YND_FIXED.value(), Currency.YND_FIXED.getIsoCode()},
                {Currency.RUB.value(), Currency.RUB.getIsoCode()},
                {Currency.USD.value(), Currency.USD.getIsoCode()},
                {Currency.EUR.value(), Currency.EUR.getIsoCode()},
                {Currency.CHF.value(), Currency.CHF.getIsoCode()},
                {Currency.KZT.value(), Currency.KZT.getIsoCode()},
                {Currency.TRY.value(), Currency.TRY.getIsoCode()},
                {Currency.UAH.value(), Currency.UAH.getIsoCode()},
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void createCampaign() {
        cid = api.as(Logins.LOGIN_MAIN).userSteps.campaignSteps().addDefaultTextCampaign();
    }

    @Step("Подготовка данных для теста")
    @Before
    public void prepareCampaign() {
        sum = RandomUtils.getRandomFloat(0f, 1000000f);
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN);
        CampaignsRecord campRecord = api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignById(cid);
        campRecord.setSum(BigDecimal.valueOf(sum));
        campRecord.setCurrency(CampaignsCurrency.valueOf(currencyInDB));
        api.userSteps.getDirectJooqDbSteps().campaignsSteps().updateCampaigns(campRecord);
    }

    @Test
    public void campaignsSumsForBsTest() {
        List<CampaignsSumsForBsResponse> response = darkSideSteps.getCampaignsSumsForBsSteps().get(cid);
        assertThat("ручка CampaignsSumsForBS вернула правильный ответ", response,
                beanDiffer(Arrays.asList(
                        new CampaignsSumsForBsResponse(cid, sum, Status.PENDING, CampaignsType.TEXT.value(), currencyISOCode)
                )));
    }
}
