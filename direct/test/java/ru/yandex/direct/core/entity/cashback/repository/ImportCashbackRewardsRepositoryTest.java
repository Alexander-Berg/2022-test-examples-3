package ru.yandex.direct.core.entity.cashback.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.cashback.model.CashbackRewardDetails;
import ru.yandex.direct.core.entity.cashback.model.CashbackRewardDetailsRow;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_CASHBACK_DETAILS;

@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class ImportCashbackRewardsRepositoryTest {
    private static final LocalDate DATE = LocalDate.of(2020, 8, 1);

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ImportCashbackRewardsRepository repository;

    @Autowired
    private UserSteps userSteps;

    private UserInfo userInfo;

    @Before
    public void init() {
        userInfo = userSteps.createDefaultUser();
    }

    @Test
    public void testSaveRewardDetails_noData() {
        repository.saveRewardDetails(Map.of(), DATE);
        assertThat(countDbRows()).isZero();
    }

    @Test
    public void testSaveRewardDetails_noReward() {
        var reward = new CashbackRewardDetails()
                .withDetails(List.of(
                        new CashbackRewardDetailsRow()
                                .withProgramId(1L)
                                .withReward(BigDecimal.ZERO)
                                .withRewardWithoutNds(BigDecimal.ZERO)
                ));
        repository.saveRewardDetails(Map.of(userInfo.getClientId(), reward), DATE);
        assertThat(countDbRows()).isZero();
    }

    @Test
    public void testSaveRewardDetails_success() {
        var reward = new CashbackRewardDetails()
                .withDetails(List.of(
                        new CashbackRewardDetailsRow()
                                .withProgramId(1L)
                                .withReward(new BigDecimal("41.5619939"))
                                .withRewardWithoutNds(new BigDecimal("34.63504636"))
                ));
        repository.saveRewardDetails(Map.of(userInfo.getClientId(), reward), DATE);
        assertThat(countDbRows()).isEqualTo(1);
    }

    private int countDbRows() {
        return dslContextProvider.ppc(userInfo.getShard())
                .selectCount()
                .from(CLIENTS_CASHBACK_DETAILS)
                .where(CLIENTS_CASHBACK_DETAILS.CLIENT_ID.eq(userInfo.getClientId().asLong()))
                .fetchOne().value1();
    }
}
