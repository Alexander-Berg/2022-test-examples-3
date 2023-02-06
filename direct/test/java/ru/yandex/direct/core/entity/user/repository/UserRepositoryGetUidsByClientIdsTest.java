package ru.yandex.direct.core.entity.user.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserRepositoryGetUidsByClientIdsTest {

    private static final int SHARD = 1;

    @Autowired
    private Steps steps;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void oneClientIdWithOneUid() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        int shard = userInfo.getShard();
        Long uid = userInfo.getUid();
        ClientId clientId = userInfo.getClientInfo().getClientId();
        Map<ClientId, List<Long>> map = userRepository.getUidsByClientIds(shard, Collections.singletonList(clientId));
        Map<ClientId, List<Long>> expectedMap = new HashMap<>();
        expectedMap.put(clientId, Collections.singletonList(uid));
        assertThat(map, beanDiffer(expectedMap));
    }

    @Test
    public void oneClientIdWithoutUids() {
        ClientId clientId = steps.clientSteps().generateNewClientId();
        Map<ClientId, List<Long>> map = userRepository.getUidsByClientIds(SHARD, Collections.singletonList(clientId));
        assertThat(map, beanDiffer(Collections.emptyMap()));
    }

    @Test
    public void emptyClientIds() {
        Map<ClientId, List<Long>> map = userRepository.getUidsByClientIds(SHARD, Collections.emptyList());
        assertThat(map, beanDiffer(Collections.emptyMap()));
    }

    @Test
    public void oneClientIdWithTwoUids() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        int shard = userInfo.getShard();
        Long uid1 = userInfo.getUid();
        ClientId clientId = userInfo.getClientInfo().getClientId();
        Long uid2 = steps.userSteps()
                .createRepresentative(userInfo.getClientInfo())
                .getUid();
        Map<ClientId, List<Long>> map = userRepository.getUidsByClientIds(shard, Collections.singletonList(clientId));
        List<Long> uids = map.get(clientId);
        if (uids != null) {
            uids.sort(Long::compareTo);
        }
        Map<ClientId, List<Long>> expectedMap = new HashMap<>();
        expectedMap.put(clientId, Arrays.asList(uid1, uid2));
        assertThat(map, beanDiffer(expectedMap));
    }

    @Test
    public void cientIdWithOneUidAndClientIdWithoutUids() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        int shard = userInfo.getShard();
        Long uid = userInfo.getUid();
        ClientId clientId = userInfo.getClientInfo().getClientId();
        ClientId clientIdWithoutUids = steps.clientSteps().generateNewClientId();
        Map<ClientId, List<Long>> map =
                userRepository.getUidsByClientIds(shard, Arrays.asList(clientId, clientIdWithoutUids));
        Map<ClientId, List<Long>> expectedMap = new HashMap<>();
        expectedMap.put(clientId, Collections.singletonList(uid));
        assertThat(map, beanDiffer(expectedMap));
    }

    @Test
    public void twoClientIsdWithTwoUids() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        int shard = userInfo.getShard();
        Long clientId1uid1 = userInfo.getUid();
        ClientId clientId1 = userInfo.getClientInfo().getClientId();
        Long clientId1uid2 = steps.userSteps()
                .createRepresentative(userInfo.getClientInfo())
                .getUid();

        userInfo = steps.clientSteps()
                .createClient(new ClientInfo().withShard(shard).withClient(defaultClient())).getChiefUserInfo();
        Long clientId2uid1 = userInfo.getUid();
        ClientId clientId2 = userInfo.getClientInfo().getClientId();
        Long clientId2uid2 = steps.userSteps()
                .createRepresentative(userInfo.getClientInfo())
                .getUid();

        Map<ClientId, List<Long>> map = userRepository.getUidsByClientIds(shard, Arrays.asList(clientId1, clientId2));
        Stream.of(map.get(clientId1), map.get(clientId2))
                .filter(Objects::nonNull)
                .forEach(Collections::sort);

        Map<ClientId, List<Long>> expectedMap = new HashMap<>();
        expectedMap.put(clientId2, Arrays.asList(clientId2uid1, clientId2uid2));
        expectedMap.put(clientId1, Arrays.asList(clientId1uid1, clientId1uid2));

        assertThat(map, beanDiffer(expectedMap));
    }
}
