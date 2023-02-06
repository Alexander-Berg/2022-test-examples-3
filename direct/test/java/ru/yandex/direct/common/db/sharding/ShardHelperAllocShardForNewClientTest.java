package ru.yandex.direct.common.db.sharding;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardKey;
import ru.yandex.direct.dbutil.sharding.ShardSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@CoreTest
public class ShardHelperAllocShardForNewClientTest {
    @Autowired
    private Steps steps;
    @Autowired
    private ShardSupport shardSupport;
    @Autowired
    private ShardHelper shardHelper;

    private ClientInfo newClientInfo;

    @Before
    public void setUp() {
        newClientInfo = steps.clientSteps().createDefaultClient();
        shardSupport.deleteValues(ShardKey.UID, Collections.singletonList(newClientInfo.getUid()));
        shardSupport.deleteValues(ShardKey.LOGIN,
                Collections.singletonList(newClientInfo.getChiefUserInfo().getLogin()));
    }

    @Test
    public void testSuccess() {
        int shard = shardHelper.allocShardForNewClient(
                newClientInfo.getUid(), newClientInfo.getLogin(), newClientInfo.getClientId().asLong());

        assertEquals(
                shardHelper.getShardByClientId(newClientInfo.getClientId()), shard);
        assertEquals(
                shardHelper.getClientIdByUid(newClientInfo.getUid()), (Long) newClientInfo.getClientId().asLong());
        assertEquals(
                shardHelper.getUidByLogin(newClientInfo.getLogin()), Long.valueOf(newClientInfo.getUid()));
    }

    /**
     * Проверяем, что для удалённого клиента, от которого в метабазе осталась запись о шарде,
     * создание происходит в том же шарде
     */
    @Test
    public void reuseExistingShard_forDeletedClient() {
        Long clientClientId = steps.clientSteps().generateNewClientId().asLong();
        Long uid = steps.userSteps().generateNewUserUid();
        String login = "test-login-reuseExistingShard";
        int expectedShard = 2; // шард, отличный от дефолтного
        // создаём запись о клиенте в метабазе, создавая состояние после удаления
        shardSupport.saveValue(ShardKey.CLIENT_ID, clientClientId, ShardKey.SHARD, expectedShard);

        int shard = shardHelper.allocShardForNewClient(uid, login, clientClientId);

        assertThat(shard)
                .as("shard")
                .isEqualTo(expectedShard);
        assertThat(shardHelper.getShardByClientId(ClientId.fromLong(clientClientId)))
                .as("shard by clientId")
                .isEqualTo(expectedShard);
        assertThat(shardHelper.getClientIdByUid(uid))
                .as("clientId by uid")
                .isEqualTo(clientClientId);
        assertThat(shardHelper.getUidByLogin(login))
                .as("uid by login")
                .isEqualTo(uid);
    }
}
