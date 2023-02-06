package ru.yandex.market.mboc.tms.executors.transitions;

import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.market.mbo.common.logbroker.LogbrokerContext;
import ru.yandex.market.mbo.common.logbroker.LogbrokerProducerService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MappingSkuType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MappingTransition;
import ru.yandex.market.mboc.common.offers.repository.MappingTransitionsRepository;
import ru.yandex.market.mboc.common.test.RandomTestUtils;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.export.MbocExport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MappingTransitionsExportExecutorTest extends BaseDbTestClass {

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    @Autowired
    MappingTransitionsRepository mappingTransitionsRepository;

    @Mock
    LogbrokerProducerService<MbocExport.MappingTransition> logbrokerProducerService;

    @Mock
    LogbrokerClientFactory logbrokerClientFactory;
    @Mock
    AsyncProducerConfig asyncProducerConfig;

    MappingTransitionsExportExecutor executor;

    @Before
    public void setUp() {
        executor = Mockito.spy(new MappingTransitionsExportExecutor(
            mappingTransitionsRepository,
            logbrokerClientFactory, asyncProducerConfig));

        Mockito.doReturn(logbrokerProducerService).when(executor).createLogbrokerProducerService();

    }

    @Test
    public void testTransitionsExported() {
        MappingTransition transition1 = createAndSaveTransition();
        MappingTransition transition2 = createAndSaveTransition();

        when(logbrokerProducerService.uploadEvents(any()))
            .thenAnswer(i -> {
                LogbrokerContext<MbocExport.MappingTransition> context = i.getArgument(0);
                context.getOnSuccessBatchCallback().accept(context.getEvents());
                return context.getEvents();
            });

        executor.execute();

        MappingTransition transition1Updated = mappingTransitionsRepository.getById(transition1.getId());
        MappingTransition transition2Updated = mappingTransitionsRepository.getById(transition2.getId());
        assertThat(transition1Updated.getExportedDate()).isNotNull();
        assertThat(transition2Updated.getExportedDate()).isNotNull();
    }

    @Test
    public void testFastSKUToPSKUTransitionsExported() {
        MappingTransition transition1 = createAndSaveTransition(tr -> tr
            .setOldMskuId(100500L)
            .setNewMskuId(100500L)
            .setOldMskuType(MappingSkuType.FAST_SKU)
            .setNewMskuType(MappingSkuType.PARTNER20)
        );

        when(logbrokerProducerService.uploadEvents(any()))
            .thenAnswer(i -> {
                LogbrokerContext<MbocExport.MappingTransition> context = i.getArgument(0);
                context.getOnSuccessBatchCallback().accept(context.getEvents());
                return context.getEvents();
            });

        executor.execute();

        MappingTransition transition1Updated = mappingTransitionsRepository.getById(transition1.getId());
        assertThat(transition1Updated.getExportedDate()).isNotNull();
    }

    @Test
    public void testTransitionsWithNullsExported() {
        MappingTransition transition1 = createAndSaveTransition(tr -> tr
            .setOldMskuId(null)
            .setOldMskuType(null));
        MappingTransition transition2 = createAndSaveTransition(tr -> tr
            .setNewMskuId(null)
            .setNewMskuType(null));

        when(logbrokerProducerService.uploadEvents(any()))
            .thenAnswer(i -> {
                LogbrokerContext<MbocExport.MappingTransition> context = i.getArgument(0);
                context.getOnSuccessBatchCallback().accept(context.getEvents());
                return context.getEvents();
            });

        executor.execute();

        MappingTransition transition1Updated = mappingTransitionsRepository.getById(transition1.getId());
        MappingTransition transition2Updated = mappingTransitionsRepository.getById(transition2.getId());
        assertThat(transition1Updated.getExportedDate()).isNotNull();
        assertThat(transition2Updated.getExportedDate()).isNotNull();
    }

    @Test
    public void testTransitionsExportFailed() {
        MappingTransition transition1 = createAndSaveTransition();
        MappingTransition transition2 = createAndSaveTransition();

        when(logbrokerProducerService.uploadEvents(any()))
            .thenAnswer(i -> {
                LogbrokerContext<MbocExport.MappingTransition> context = i.getArgument(0);
                context.getOnFailureBatchCallback().accept(context.getEvents());
                return context.getEvents();
            });

        assertThatThrownBy(() -> executor.execute())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("There are 2 failed logbroker events out of 2 total.");

        MappingTransition transition1Updated = mappingTransitionsRepository.getById(transition1.getId());
        MappingTransition transition2Updated = mappingTransitionsRepository.getById(transition2.getId());
        assertThat(transition1Updated.getExportedDate()).isNull();
        assertThat(transition2Updated.getExportedDate()).isNull();
    }

    @Test
    public void testTransitionsExportPartiallyFailed() {
        MappingTransition transition1 = createAndSaveTransition();
        MappingTransition transition2 = createAndSaveTransition();

        when(logbrokerProducerService.uploadEvents(any()))
            .thenAnswer(i -> {
                LogbrokerContext<MbocExport.MappingTransition> context = i.getArgument(0);
                context.getOnSuccessBatchCallback().accept(ImmutableList.of(context.getEvents().get(0)));
                context.getOnFailureBatchCallback().accept(ImmutableList.of(context.getEvents().get(1)));
                return context.getEvents();
            });

        assertThatThrownBy(() -> executor.execute())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("There are 1 failed logbroker events out of 2 total.");

        MappingTransition transition1Updated = mappingTransitionsRepository.getById(transition1.getId());
        MappingTransition transition2Updated = mappingTransitionsRepository.getById(transition2.getId());
        assertThat(transition1Updated.getExportedDate()).isNotNull();
        assertThat(transition2Updated.getExportedDate()).isNull();
    }

    private MappingTransition createAndSaveTransition() {
        return createAndSaveTransition(Function.identity());
    }

    private MappingTransition createAndSaveTransition(Function<MappingTransition, MappingTransition> modifier) {
        MappingTransition transition =
            RandomTestUtils.randomObject(MappingTransition.class, "id", "actionId", "exportedDate");
        transition = modifier.apply(transition);
        return mappingTransitionsRepository.save(transition);
    }
}
