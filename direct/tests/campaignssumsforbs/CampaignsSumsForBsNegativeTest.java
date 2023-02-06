package ru.yandex.autotests.directintapi.tests.campaignssumsforbs;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.CampaignsSumsForBsResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by omaz on 25.07.14.
 * https://jira.yandex-team.ru/browse/TESTIRT-2378
 */
@Aqua.Test(title = "CampaignsSumsForBS - негативные тесты")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.CAMPAIGNS_SUMS_FOR_BS)
public class CampaignsSumsForBsNegativeTest {
    private static DarkSideSteps darkSideSteps;
    private static Long cid;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @BeforeClass
    public static void initTestData() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        api.userSteps.campaignFakeSteps().makeCampaignReadyForDelete(cid);
        api.userSteps.campaignSteps().campaignsDelete(cid);
    }

    @Test
    public void campaignsSumsForBsDeletedCampaignTest() {
        List<CampaignsSumsForBsResponse> response = darkSideSteps.getCampaignsSumsForBsSteps().get(cid);
        assertThat("Ответ должен быть пустым", response, empty());
    }

    @Test
    public void campaignsSumsForBsNoCampaignTest() {
        List<CampaignsSumsForBsResponse> response = darkSideSteps.getCampaignsSumsForBsSteps().get();
        assertThat("Ответ должен быть пустым", response, empty());
    }

}
