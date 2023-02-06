package ru.yandex.direct.jobs.autooverdraft;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.autooverdraftmail.repository.ClientOverdraftLimitChangesRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_OVERDRAFT_LIMIT_CHANGES;

@JobsTest
@ExtendWith(SpringExtension.class)
class DeleteFromMailTableByClientIdsTest {
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private ClientOverdraftLimitChangesRepository repository;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private Steps steps;

    DirectConfig directConfig = mock(DirectConfig.class);

    private OverdraftLimitChangesMailSenderService service;

    private ClientId clientId1;
    private ClientId clientId2;
    private ClientId clientId3;
    private ClientId clientId4;
    private ClientId clientId5;
    private ClientId clientId6;
    private ClientId clientId7;

    @BeforeEach
    @SuppressWarnings("ConstantConditions")
    void setup() {
        when(directConfig.getBranch(anyString())).thenReturn(directConfig);
        when(directConfig.getString(anyString())).thenReturn("");
        service = new OverdraftLimitChangesMailSenderService(
                directConfig,
                null,
                shardHelper,
                repository,
                null
        );
        clientId1 = steps.clientSteps().createDefaultClient().getClientId();
        clientId2 = steps.clientSteps().createDefaultClientAnotherShard().getClientId();
        clientId3 = steps.clientSteps().createDefaultClient().getClientId();
        clientId4 = steps.clientSteps().createDefaultClientAnotherShard().getClientId();
        clientId5 = steps.clientSteps().createDefaultClient().getClientId();
        clientId6 = steps.clientSteps().createDefaultClientAnotherShard().getClientId();
        clientId7 = steps.clientSteps().createDefaultClient().getClientId();
        dslContextProvider.ppc(1)
                .insertInto(CLIENT_OVERDRAFT_LIMIT_CHANGES)
                .columns(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID,
                        CLIENT_OVERDRAFT_LIMIT_CHANGES.IS_NOTIFICATION_SENT)
                .values(clientId1.asLong(), 0L)
                .values(clientId3.asLong(), 0L)
                .values(clientId5.asLong(), 0L)
                .execute();
        dslContextProvider.ppc(2)
                .insertInto(CLIENT_OVERDRAFT_LIMIT_CHANGES)
                .columns(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID,
                    CLIENT_OVERDRAFT_LIMIT_CHANGES.IS_NOTIFICATION_SENT)
                .values(clientId2.asLong(), 0L)
                .values(clientId4.asLong(), 0L)
                .values(clientId6.asLong(), 0L)
                .execute();
    }

    @Test
    void deleteByClientIds() {
        var count = service
                .deleteFromMailTableByClientIds(List.of(clientId2, clientId3, clientId4, clientId5, clientId7));
        var longIds1 = dslContextProvider.ppc(1)
                .select(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID)
                .from(CLIENT_OVERDRAFT_LIMIT_CHANGES)
                .where(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID
                        .in(List.of(clientId1.asLong(), clientId3.asLong(), clientId5.asLong())))
                .fetchSet(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID);
        var longIds2 = dslContextProvider.ppc(2)
                .select(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID)
                .from(CLIENT_OVERDRAFT_LIMIT_CHANGES)
                .where(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID
                        .in(List.of(clientId2.asLong(), clientId4.asLong(), clientId6.asLong())))
                .fetchSet(CLIENT_OVERDRAFT_LIMIT_CHANGES.CLIENT_ID);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(count).isEqualTo(4);
            softly.assertThat(longIds1.size()).isEqualTo(1);
            softly.assertThat(longIds1.contains(clientId1.asLong())).isTrue();
            softly.assertThat(longIds2.size()).isEqualTo(1);
            softly.assertThat(longIds2.contains(clientId6.asLong())).isTrue();
        });
    }
}
