package ru.yandex.autotests.directintapi.tests.smoke;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.qatools.hazelcast.SemaphoreRule;
import ru.yandex.terra.junit.rules.BottleMessageRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by pashkus 03.10.2016
 * https://st.yandex-team.ru/TESTIRT-10356
 */

public class TCDBIncrementBaseTest {

    protected static String client = Logins.LOGIN_MAIN;
    protected static final int SHARD = 1;

    protected String dbName;
    protected String host;
    protected long offset;

    @Rule
    public BottleMessageRule bottleMessageRule = new BottleMessageRule();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(client);

    @Rule
    public Trashman trashman = new Trashman(api);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    protected final static int INCREMENT = 5;
    protected Long firstCid;
    protected Long secondCid;

    public TCDBIncrementBaseTest(String dbName, String host, long offset) {
        this.dbName = dbName;
        this.host = host;
        this.offset = offset;
    }

    @Before
    public void createCampaigns() {
        api.url(host);
        List<Long> ids = api.userSteps.campaignSteps().addDefaultTextCampaigns(2);
        firstCid = ids.get(0);
        secondCid = ids.get(1);
    }

    @Test
    public void checkDbIncrement() {
        assertThat("в базе " + dbName + " верный инкремент", (firstCid - secondCid) % INCREMENT, equalTo(0L));
    }

    @Test
    public void checkDbOffset() {
        assertThat("в базе " + dbName + " верное смещение", firstCid % INCREMENT, equalTo(offset));
    }
}
