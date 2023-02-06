package ru.yandex.autotests.directintapi.tests.requestblocknumberdetailed;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.RequestBlockNumberDetailedResponse;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * Created by ginger on 22.06.15.
 * https://st.yandex-team.ru/TESTIRT-5809
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.REQUEST_BLOCK_NUMBER_DETAILS)
@Issue("https://st.yandex-team.ru/DIRECT-39360")
@Description("Общие проверки метода: некорректные входные данные + подсчёт общего кол-ва объектов в ответе.")
public class RequestBlockNumberDetailedCommonTest {
    protected static LogSteps log = LogSteps.getLogger(RequestBlockNumberDetailedCommonTest.class);

    private static final String login = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(login);
    private static RequestBlockNumberDetailedResponse response;
    private static Long campaignId;

    @Rule
    public Trashman trasher = new Trashman(api);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private RequestBlockNumberDetailedResponse expectedResponse;

    @BeforeClass
    public static void prepareTestData() {
        log.info("Создадим кампанию.");
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        log.info("Создадим группу с 3мя фразами и 2мя объявлениями.");
        Long adGroupID = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        api.userSteps.keywordsSteps().addDefaultKeywords(adGroupID, 3);
        api.userSteps.adsSteps().addDefaultTextAds(adGroupID, 2);

    }

    @Test
    public void checkNumbersDetails() {
        log.info("Проверим кол-во объявлений, фраз и групп в ответе метода.");
        response =
                api.userSteps.getDarkSideSteps().getRequestBlockNumberDetailedSteps().getRequestBlockNumberDetailed(
                        campaignId
                );
        expectedResponse = new RequestBlockNumberDetailedResponse();
        expectedResponse.setGroupNumber("1");
        expectedResponse.setBlockNumber("2");
        expectedResponse.setRequestNumber("3");
        assertThat("значения полей в ответе совпали с ожидаемыми",
                response, beanEquivalent(expectedResponse));
    }

    @Test
    public void tryGetResponseByInvalidCampaignCode() {
        Long invalidCampaignId = campaignId + 1000;
        String errorText = "campaign " + invalidCampaignId + " does not exists";
        api.userSteps.getDarkSideSteps().getRequestBlockNumberDetailedSteps().getRequestBlockNumberDetailedWithError(
                invalidCampaignId,
                errorText
        );
    }

}
