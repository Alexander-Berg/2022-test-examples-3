package ru.yandex.market.mbo.tms.model.transitions;

import com.google.common.collect.ImmutableList;
import org.jooq.exception.NoDataFoundException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransition;
import ru.yandex.market.mbo.common.logbroker.LogbrokerContext;
import ru.yandex.market.mbo.common.logbroker.LogbrokerProducerService;
import ru.yandex.market.mbo.common.utils.PGaaSInitializer;
import ru.yandex.market.mbo.db.repo.ModelTransitionsRepository;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.tms.configs.TmsDbTestConfiguration;
import ru.yandex.market.mbo.utils.BaseDbTest;
import ru.yandex.market.mbo.utils.RandomTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(
    initializers = PGaaSInitializer.class,
    classes = TmsDbTestConfiguration.class
)
public class ModelTransitionsExportExecutorTest extends BaseDbTest {

    @Autowired
    ModelTransitionsRepository modelTransitionsRepository;

    LogbrokerProducerService logbrokerProducerService;

    ModelTransitionsExportExecutor skuExecutor;
    ModelTransitionsExportExecutor modelExecutor;

    @Before
    public void setUp() {
        logbrokerProducerService = Mockito.mock(LogbrokerProducerService.class);
        skuExecutor = new ModelTransitionsExportExecutor(
            modelTransitionsRepository,
            null,
            null,
            ImmutableList.of(EntityType.SKU)) {

            @Override
            protected LogbrokerProducerService createLogbrokerProducerService() {
                return logbrokerProducerService;
            }
        };

        modelExecutor = new ModelTransitionsExportExecutor(
            modelTransitionsRepository,
            null,
            null,
            ImmutableList.of(EntityType.MODEL, EntityType.CLUSTER)) {

            @Override
            protected LogbrokerProducerService createLogbrokerProducerService() {
                return logbrokerProducerService;
            }
        };
    }

    @Test
    public void testSkuTransitionsExported() {
        ModelTransition transition1 = createAndSaveTransition(EntityType.SKU);
        ModelTransition transition2 = createAndSaveTransition(EntityType.SKU);
        ModelTransition transition3 = createAndSaveTransition(EntityType.MODEL);
        ModelTransition transition4 = createAndSaveTransition(EntityType.CLUSTER);

        when(logbrokerProducerService.uploadEvents(any()))
            .thenAnswer(i -> {
                LogbrokerContext<ModelStorage.ModelTransition> context = i.getArgument(0);
                context.getOnSuccessBatchCallback().accept(context.getEvents());
                return context.getEvents();
            });

        skuExecutor.doRealJob(null);

        ModelTransition transition1Updated = modelTransitionsRepository.getById(transition1.getId());
        ModelTransition transition2Updated = modelTransitionsRepository.getById(transition2.getId());
        ModelTransition transition3Updated = modelTransitionsRepository.getById(transition3.getId());
        ModelTransition transition4Updated = modelTransitionsRepository.getById(transition4.getId());

        assertThat(transition1Updated.getExportedDate()).isNotNull();
        assertThat(transition2Updated.getExportedDate()).isNotNull();
        assertThat(transition3Updated.getExportedDate()).isNull();
        assertThat(transition4Updated.getExportedDate()).isNull();
    }

    @Test
    public void testModelTransitionsExported() {
        ModelTransition transition1 = createAndSaveTransition(EntityType.SKU);
        ModelTransition transition2 = createAndSaveTransition(EntityType.SKU);
        ModelTransition transition3 = createAndSaveTransition(EntityType.MODEL);
        ModelTransition transition4 = createAndSaveTransition(EntityType.CLUSTER);

        when(logbrokerProducerService.uploadEvents(any()))
            .thenAnswer(i -> {
                LogbrokerContext<ModelStorage.ModelTransition> context = i.getArgument(0);
                context.getOnSuccessBatchCallback().accept(context.getEvents());
                return context.getEvents();
            });

        modelExecutor.doRealJob(null);

        ModelTransition transition1Updated = modelTransitionsRepository.getById(transition1.getId());
        ModelTransition transition2Updated = modelTransitionsRepository.getById(transition2.getId());
        ModelTransition transition3Updated = modelTransitionsRepository.getById(transition3.getId());

        assertThat(transition1Updated.getExportedDate()).isNull();
        assertThat(transition2Updated.getExportedDate()).isNull();
        assertThat(transition3Updated.getExportedDate()).isNotNull();
        assertThatThrownBy(() -> {
           modelTransitionsRepository.getById(transition4.getId());
        }).isInstanceOf(NoDataFoundException.class);
    }

    @Test
    public void testTransitionsExportFailed() {
        ModelTransition transition1 = createAndSaveTransition(EntityType.SKU);
        ModelTransition transition2 = createAndSaveTransition(EntityType.SKU);
        ModelTransition transition3 = createAndSaveTransition(EntityType.MODEL);

        when(logbrokerProducerService.uploadEvents(any()))
            .thenAnswer(i -> {
                LogbrokerContext<ModelStorage.ModelTransition> context = i.getArgument(0);
                context.getOnFailureBatchCallback().accept(context.getEvents());
                return context.getEvents();
            });

        assertThatThrownBy(() -> skuExecutor.doRealJob(null))
             .isInstanceOf(RuntimeException.class)
             .hasMessage("There are 2 failed logbroker events out of 2 total.");

        ModelTransition transition1Updated = modelTransitionsRepository.getById(transition1.getId());
        ModelTransition transition2Updated = modelTransitionsRepository.getById(transition2.getId());
        ModelTransition transition3Updated = modelTransitionsRepository.getById(transition3.getId());
        assertThat(transition1Updated.getExportedDate()).isNull();
        assertThat(transition2Updated.getExportedDate()).isNull();
        assertThat(transition3Updated.getExportedDate()).isNull();
    }

    @Test
    public void testTransitionsExportPartiallyFailed() {
        ModelTransition transition1 = createAndSaveTransition(EntityType.SKU);
        ModelTransition transition2 = createAndSaveTransition(EntityType.SKU);
        ModelTransition transition3 = createAndSaveTransition(EntityType.MODEL);

        when(logbrokerProducerService.uploadEvents(any()))
            .thenAnswer(i -> {
                LogbrokerContext<ModelStorage.ModelTransition> context = i.getArgument(0);
                context.getOnSuccessBatchCallback().accept(ImmutableList.of(context.getEvents().get(0)));
                context.getOnFailureBatchCallback().accept(ImmutableList.of(context.getEvents().get(1)));
                return context.getEvents();
            });

        assertThatThrownBy(() -> skuExecutor.doRealJob(null))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("There are 1 failed logbroker events out of 2 total.");

        ModelTransition transition1Updated = modelTransitionsRepository.getById(transition1.getId());
        ModelTransition transition2Updated = modelTransitionsRepository.getById(transition2.getId());
        ModelTransition transition3Updated = modelTransitionsRepository.getById(transition3.getId());
        assertThat(transition1Updated.getExportedDate()).isNotNull();
        assertThat(transition2Updated.getExportedDate()).isNull();
        assertThat(transition3Updated.getExportedDate()).isNull();
    }

    private ModelTransition createAndSaveTransition(EntityType entityType) {
        ModelTransition transition =
            RandomTestUtils.randomObject(ModelTransition.class, "id", "actionId", "exportedDate")
                .setEntityType(entityType);
        return  modelTransitionsRepository.save(transition);
    }
}
