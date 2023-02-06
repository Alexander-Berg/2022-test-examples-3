package ru.yandex.autotests.directintapi.tests.smoke;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;
import ru.yandex.terra.junit.rules.BottleMessageRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by pavryabov on 27.04.15.
 * https://st.yandex-team.ru/TESTIRT-4861
 */
@Aqua.Test()
@Ignore() //Because of https://st.yandex-team.ru/TESTIRT-10356
@Features(FeatureNames.INCREMENT_OFFSET_MONITORING)
@Description("Проверка инкремента и оффсета в базах Директа")
@RunWith(Parameterized.class)
public class TCDBIncrementTest {

    private static String client = Logins.LOGIN_MAIN;

    private static final int SHARD = 1;

    @Rule
    public BottleMessageRule bottleMessageRule = new BottleMessageRule();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(client);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private final static int INCREMENT = 5;
    private Long firstCid;
    private Long secondCid;

    @Parameterized.Parameter(0)
    public String dbName;

    @Parameterized.Parameter(1)
    public String host;

    @Parameterized.Parameter(2)
    public long offset;

    @Parameterized.Parameters(name = "db={0}")
    public static Collection data() {
        Object[][] data = new Object[][]{
                {"TC", "http://test-direct.yandex.ru", 3},
                {"TC2", "http://test2-direct.yandex.ru", 4}
        };
        return Arrays.asList(data);
    }

    @Before
    public void createCampaigns() {
        api.url(host);
        firstCid = api.userSteps.getDBSteps().getCampaignsSteps().getMaxCid(null, SHARD);
        secondCid = api.userSteps.getDBSteps().getCampaignsSteps().getMaxCid(Restrictions.lt("cid", firstCid), SHARD);
        String startTime = api.userSteps.getDBSteps().getCampaignsSteps().getStartTimeByCid(secondCid, SHARD);
        DateTime dateTime = DateTime.parse(startTime);
        if (!dateTime.isAfter(DateTime.now().minusDays(3).getMillis())) {
            firstCid = api.userSteps.campaignSteps().addDefaultTextCampaign();
            secondCid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        }
    }

    @Test
    public void checkDbIncrement() {
        assertThat("в базе " + dbName + " верный инкремент", (firstCid - secondCid) % INCREMENT, equalTo(0l));
    }

    @Test
    public void checkDbOffset() {
        assertThat("в базе " + dbName + " верное смещение", firstCid % INCREMENT, equalTo(offset));
    }
}
