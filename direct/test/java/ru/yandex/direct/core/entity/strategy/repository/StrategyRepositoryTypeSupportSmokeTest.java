package ru.yandex.direct.core.entity.strategy.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.strategy.model.BaseStrategy;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.InsertHelperAggregator;
import ru.yandex.direct.jooqmapperhelper.UpdateHelperAggregator;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Набор примитивных смоук тестов для проверки базовой функциональности репозиторного уровня на этапе разработки.
 * */
public abstract class StrategyRepositoryTypeSupportSmokeTest<T extends BaseStrategy> {

    protected Long nextStrategyId() {
        return shardHelper.generateStrategyIds(clientInfo.getClientId().asLong(), 1).get(0);
    }

    abstract protected T generateModel();

    abstract protected ModelChanges<T> generateModelChanges(T model);

    @Autowired
    StrategyRepositoryTypeSupportFacade strategyRepositoryTypeSupportFacade;

    @Autowired
    DslContextProvider ppcDslContextProvider;

    @Autowired
    StrategyTypedRepository strategyTypedRepository;

    @Autowired
    Steps steps;

    @Autowired
    ShardHelper shardHelper;

    ClientInfo clientInfo;

    @Before
    public void init() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void addAndGet_success() {
        var model = generateModel();
        var insertHelperAggregator = new InsertHelperAggregator(ppcDslContextProvider.ppc(clientInfo.getShard()));
        strategyRepositoryTypeSupportFacade.pushToInsert(insertHelperAggregator, List.of(model));
        insertHelperAggregator.executeIfRecordsAdded();

        var actualModel =
                strategyTypedRepository.getIdToModelTyped(clientInfo.getShard(), List.of(model.getId())).get(model.getId());

        assertThat(actualModel).isEqualTo(model);
    }

    @Test
    public void updateAndGet_success() {
        var model = generateModel();
        var insertHelperAggregator = new InsertHelperAggregator(ppcDslContextProvider.ppc(clientInfo.getShard()));
        strategyRepositoryTypeSupportFacade.pushToInsert(insertHelperAggregator, List.of(model));
        insertHelperAggregator.executeIfRecordsAdded();

        UpdateHelperAggregator updateHelper =
                new UpdateHelperAggregator(ppcDslContextProvider.ppc(clientInfo.getShard()));
        var changes = generateModelChanges(model);
        var appliedChanges = changes.applyTo(model);

        strategyRepositoryTypeSupportFacade.processUpdate(updateHelper, List.of(appliedChanges));
        updateHelper.execute();

        var actualModel =
                strategyTypedRepository.getIdToModelTyped(clientInfo.getShard(), List.of(model.getId())).get(model.getId());

        assertThat(actualModel).isEqualTo(appliedChanges.getModel());
    }

}
