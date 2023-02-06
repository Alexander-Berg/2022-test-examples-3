package ru.yandex.autotests.directintapi.tests.fakeintapi;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.model.ShardNumbers;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Features;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * User: xy6er
 * https://jira.yandex-team.ru/browse/TESTIRT-1406
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.FAKE_METHODS)
public class ReshardTest {
    private DarkSideSteps darkSideSteps;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MNGR).wsdl(APIPort_PortType.class);

    @Before
    public void unitSteps() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
    }


    @Test
    public void reshardUserTest() {
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.userSteps.clientSteps());
        CreateNewSubclientResponse clientInfo = clientStepsHelper
                .createServicedClient("intapi-servClient20-", Logins.LOGIN_MNGR);

        int newShard =
                darkSideSteps.getClientFakeSteps().getUserShard(clientInfo.getLogin()) == ShardNumbers.DEFAULT_SHARD.getShardNumber()
                        ? ShardNumbers.EXTRA_SHARD.getShardNumber() : ShardNumbers.DEFAULT_SHARD.getShardNumber();
        darkSideSteps.getClientFakeSteps().reshardUser(clientInfo.getLogin(), newShard);
        assertThat("Шард не изменился",
                darkSideSteps.getClientFakeSteps().getUserShard(clientInfo.getLogin()), equalTo(newShard));
    }
}
