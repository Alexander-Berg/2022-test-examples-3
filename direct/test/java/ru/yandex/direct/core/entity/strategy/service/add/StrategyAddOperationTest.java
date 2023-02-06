package ru.yandex.direct.core.entity.strategy.service.add;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.testing.matchers.validation.Matchers;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa;

@CoreTest
@RunWith(SpringRunner.class)
//данный тест проверяет что начальный вариант StrategyAddOperation умеет сохранять стратегию
//не стоит базироваться на этот тест, вместо этого есть хороший пример BannerAddOperationTestBase
public class StrategyAddOperationTest {
    @Autowired
    public WalletService walletService;

    @Autowired
    public StrategyOperationFactory strategyOperationFactory;

    @Autowired
    public StrategyTypedRepository strategyTypedRepository;

    @Autowired
    public Steps steps;
    private UserInfo user;

    @Before
    public void before() {
        user = steps.userSteps().createDefaultUser();
        walletService.createWalletForNewClient(user.getClientId(), user.getUid());
    }

    @Test
    public void addSimpleStrategy() {
        AutobudgetAvgCpa autobudgetAvgCpa = autobudgetAvgCpa();

        StrategyAddOperation operation =
                strategyOperationFactory.createStrategyAddOperation(
                        user.getShard(),
                        user.getUid(),
                        user.getClientId(),
                        user.getUid(),
                        List.of(autobudgetAvgCpa),
                        new StrategyOperationOptions()
                );
        var result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), Matchers.hasNoDefectsDefinitions());

        Long id = result.get(0).getResult();
        AutobudgetAvgCpa actualStrategy = strategyTypedRepository.getIdToModelSafely(user.getShard(), List.of(id),
                AutobudgetAvgCpa.class).get(id);

        var expectedStrategy = autobudgetAvgCpa
                .withId(id)
                .withLastChange(null)
                .withLastBidderRestartTime(actualStrategy.getLastBidderRestartTime());

        assertThat(actualStrategy, beanDiffer(expectedStrategy).useCompareStrategy(onlyExpectedFields()));
    }
}
