package ru.yandex.direct.core.entity.autooverdraft;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.autooverdraftmail.repository.ClientOverdraftLimitChangesRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.junit.Assert.assertNotNull;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_OVERDRAFT_LIMIT_CHANGES;

// тесты на deleteByClientIds и getAll есть в jobs
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientOverdraftLimitChangesRepositoryTest {
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private ClientOverdraftLimitChangesRepository repository;
    @Autowired
    private Steps steps;

    private ClientId clientId1;
    private ClientId clientId2;
    private ClientId clientId3;

    @Before
    public void addData() {
        clientId1 = steps.clientSteps().createDefaultClient().getClientId();
        clientId2 = steps.clientSteps().createDefaultClientAnotherShard().getClientId();
        clientId3 = steps.clientSteps().createDefaultClient().getClientId();
        dslContextProvider.ppc(1)
                .insertInto(CLIENT_OVERDRAFT_LIMIT_CHANGES)
                .set(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID, clientId1.asLong())
                .set(CLIENT_OVERDRAFT_LIMIT_CHANGES.IS_NOTIFICATION_SENT, 1L)
                .execute();
        dslContextProvider.ppc(2)
                .insertInto(CLIENT_OVERDRAFT_LIMIT_CHANGES)
                .set(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID, clientId2.asLong())
                .set(CLIENT_OVERDRAFT_LIMIT_CHANGES.IS_NOTIFICATION_SENT, 0L)
                .execute();
    }

    @Test
    public void addAsSent_new_added() {
        assertForAdd(clientId3, 1, true);
    }

    @Test
    public void addAsSent_alreadySent_unchanged() {
        assertForAdd(clientId1, 1, true);
    }

    @Test
    public void addAsSent_unsent_updated() {
        assertForAdd(clientId2, 2, true);
    }

    @Test
    public void addAsNotSent_new_added() {
        assertForAdd(clientId3, 1, false);
    }

    @Test
    public void addAsNotSent_alreadySent_updated() {
        assertForAdd(clientId1, 1, false);
    }

    @Test
    public void addAsNotSent_unsent_unchanged() {
        assertForAdd(clientId2, 2, false);
    }

    private void assertForAdd(ClientId clientId, int shard, boolean isSent) {
        long expectedValue = isSent ? 1L : 0L;
        repository.add(shard, clientId, isSent);
        var result = dslContextProvider.ppc(shard)
                .select(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID, CLIENT_OVERDRAFT_LIMIT_CHANGES.IS_NOTIFICATION_SENT)
                .from(CLIENT_OVERDRAFT_LIMIT_CHANGES)
                .where(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID.eq(clientId.asLong()))
                .fetchOne();
        assertNotNull(result);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.value1()).isEqualTo(clientId.asLong());
            softly.assertThat(result.value2()).isEqualTo(expectedValue);
        });
    }
}
