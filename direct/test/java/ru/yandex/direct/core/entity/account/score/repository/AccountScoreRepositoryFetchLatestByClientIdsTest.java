package ru.yandex.direct.core.entity.account.score.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.account.score.model.AccountScore;
import ru.yandex.direct.core.entity.account.score.model.AccountScoreFactors;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.AccountScoreType;
import ru.yandex.direct.dbschema.ppc.tables.records.AccountScoreRecord;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.tables.AccountScore.ACCOUNT_SCORE;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AccountScoreRepositoryFetchLatestByClientIdsTest {

    @Autowired
    private AccountScoreRepository accountScoreRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    private ClientId clientId;
    private int shard;
    private AccountScoreRecord defaultTableRecord;
    private AccountScore defaultModel;

    @Before
    public void setUp() {
        ClientInfo defaultClient = steps.clientSteps().createDefaultClient();
        clientId = defaultClient.getClientId();
        shard = defaultClient.getShard();

        defaultTableRecord = new AccountScoreRecord()
                .with(ACCOUNT_SCORE.CLIENT_ID, clientId.asLong())
                .with(ACCOUNT_SCORE.DATE, LocalDate.now())
                .with(ACCOUNT_SCORE.FACTORS_JSON, "{}")
                .with(ACCOUNT_SCORE.SCORE, new BigDecimal("0.50"))
                .with(ACCOUNT_SCORE.TYPE, AccountScoreType.client);

        defaultModel = new AccountScore()
                .withClientId(clientId.asLong())
                .withDate(defaultTableRecord.getDate())
                .withFactors(new AccountScoreFactors())
                .withScore(defaultTableRecord.getScore())
                .withType(defaultTableRecord.getType());
    }

    @Test
    public void fetchLatestByClientIds_singleClientSingleRecord() {
        dslContextProvider.ppc(shard)
                .insertInto(ACCOUNT_SCORE)
                .set(defaultTableRecord).execute();

        assertThat(accountScoreRepository.fetchLatestByClientIds(shard, singletonList(clientId)))
                .containsExactly(defaultModel);
    }
}
