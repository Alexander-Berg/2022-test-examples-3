package ru.yandex.direct.core.entity.client.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.client.model.ClientDomainStripped;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestClientDomainStrippedRepository;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

@CoreTest
@RunWith(Parameterized.class)
public class ClientDomainsStrippedRepositoryTest {

    private static final LocalDateTime DATE = LocalDateTime.now().truncatedTo(SECONDS);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private ClientDomainsStrippedRepository clientDomainsStrippedRepository;

    @Autowired
    private TestClientDomainStrippedRepository testClientDomainStrippedRepository;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private AgencyClientRelationRepository agencyClientRelationRepository;

    @Autowired
    private ShardHelper shardHelper;

    //Ожидаемый результат
    @Parameterized.Parameter()
    public boolean expectedResult;

    //Поле домена не null
    @Parameterized.Parameter(1)
    public boolean isDomainFilled;

    //Клиент привязан к агенству
    @Parameterized.Parameter(2)
    public boolean isClientBinded;

    //Клиент не имеет статуса archived
    @Parameterized.Parameter(3)
    public boolean isClientActual;

    //Номер записи больше последней обработанной записи
    @Parameterized.Parameter(4)
    public boolean isRecordBigEnough;

    //Поле Logtime больше даты и времени последней обработанной записи
    @Parameterized.Parameter(5)
    public boolean isLogtimeBigEnough;

    @Parameterized.Parameters(name = "{0}: {1} {2} {3} {4} {5}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true, true, true, true, true, true},
                {false, false, true, true, true, true},
                {false, true, false, true, true, true},
                {false, true, true, false, true, true},
                {false, true, true, true, false, true},
                {false, true, true, true, true, false},
        });
    }

    @Test
    public void getRecent() {
        ClientId clientId = clientSteps.createClient(new ClientInfo()).getClientId();
        long recordId = shardHelper.generateClientDomainsRecordIds(1).get(0);
        long domainId = isDomainFilled ? 1L : 0;
        ClientId agencyId = ClientId.fromLong(1L);
        int shard = shardHelper.getShardByClientId(clientId);

        ClientDomainStripped elem = new ClientDomainStripped()
                .withClientId(clientId.asLong()).withDomainId(domainId).withRecordId(recordId).withLogtime(DATE);
        testClientDomainStrippedRepository.add(shard, singletonList(elem));

        agencyClientRelationRepository.bindClients(shard, agencyId, singletonList(clientId));

        if (!isClientBinded) {
            agencyClientRelationRepository.unbindClient(shard, clientId, agencyId);
        }

        if (!isClientActual) {
            agencyClientRelationRepository.archiveClients(shard, agencyId, singletonList(clientId));
        }

        long boundaryRecord = isRecordBigEnough ? recordId - 1 : recordId;
        LocalDateTime boundaryTime = isLogtimeBigEnough ? DATE.minusSeconds(1L) : DATE;
        Set<ClientDomainStripped> set = clientDomainsStrippedRepository.getRecent(shard, boundaryRecord, boundaryTime);

        assertEquals(set.contains(elem), expectedResult);
    }
}
