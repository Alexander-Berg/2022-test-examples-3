package ru.yandex.direct.core.entity.strategy.service.update;

import java.math.BigDecimal;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa;
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringRunner.class)
public class StrategyUpdateOperationTest extends StrategyUpdateOperationTestBase {
    private UserInfo user;

    @Autowired
    public DslContextProvider dslContextProvider;

    @Before
    public void before() {
        user = steps.userSteps().createDefaultUser();
        walletService.createWalletForNewClient(user.getClientId(), user.getUid());
    }

    @Test
    public void updateSimpleStrategy() {
        AutobudgetAvgCpa autobudgetAvgCpa = autobudgetAvgCpa();

        StrategyAddOperation operation = createAddOperation(List.of(autobudgetAvgCpa), new StrategyOperationOptions());
        var result = operation.prepareAndApply();
        Long id = result.get(0).getResult();

        var modelChanges = new ModelChanges<>(id, AutobudgetAvgCpa.class)
                .process(BigDecimal.valueOf(222.22), AutobudgetAvgCpa.BID)
                .process(BigDecimal.valueOf(12134), AutobudgetAvgCpa.SUM);

        StrategyUpdateOperation<AutobudgetAvgCpa> updateOperation = createUpdateOperation(
                List.of(modelChanges),
                AutobudgetAvgCpa.class,
                new StrategyOperationOptions()
        );
        updateOperation.prepareAndApply();

        AutobudgetAvgCpa actualStrategy = strategyTypedRepository.getIdToModelSafely(user.getShard(), List.of(id),
                AutobudgetAvgCpa.class).get(id);

        var expectedStrategy = modelChanges.applyTo(autobudgetAvgCpa).getModel()
                .withId(id)
                .withLastChange(null)
                .withLastBidderRestartTime(null);

        assertThat(actualStrategy).is(matchedBy(beanDiffer(expectedStrategy).useCompareStrategy(onlyExpectedFields())));
    }

    @Override
    public int getShard() {
        return user.getShard();
    }

    @NotNull
    @Override
    public ClientId getClientId() {
        return user.getClientId();
    }

    @Override
    public long getOperatorUid() {
        return user.getUid();
    }
}
