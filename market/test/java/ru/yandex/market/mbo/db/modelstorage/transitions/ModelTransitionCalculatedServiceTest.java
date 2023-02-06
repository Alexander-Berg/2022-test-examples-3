package ru.yandex.market.mbo.db.modelstorage.transitions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionReason;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransition;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransitionCalculated;
import ru.yandex.market.mbo.db.KeyValueMapService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelTransitionCalculatedRepositoryStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelTransitionRepositoryStub;
import ru.yandex.market.mbo.db.repo.ModelTransitionsCalculatedRepository;

import static org.mockito.ArgumentMatchers.anyString;

@RunWith(Parameterized.class)
public class ModelTransitionCalculatedServiceTest {

    private static final long MODEL_1 = 1L;
    private static final long MODEL_2 = 2L;
    private static final long MODEL_3 = 3L;
    private static final long MODEL_4 = 4L;
    private static final long MODEL_5 = 5L;
    private static final long MODEL_6 = 6L;
    private static final long MODEL_7 = 7L;
    private static final long MODEL_8 = 8L;

    private static final int BATCH_SIZE_1 = 1;
    private static final int BATCH_SIZE_2 = 2;
    private static final int BATCH_SIZE_1000 = 1000;

    private final int batchSize;

    private ModelTransitionCalculatedRepositoryStub modelTransitionCalculatedRepositoryStub;
    private ModelTransitionRepositoryStub modelTransitionRepositoryStub;
    private ModelTransitionCalculatedService modelTransitionCalculatedService;

    public ModelTransitionCalculatedServiceTest(int batchSize) {
        this.batchSize = batchSize;
    }

    @Parameterized.Parameters
    public static List<Object[]> initParams() {
        return Arrays.asList(new Object[][]{
            {BATCH_SIZE_1}, {BATCH_SIZE_2}, {BATCH_SIZE_1000}
        });
    }

    @Before
    public void setUp() {
        modelTransitionCalculatedRepositoryStub = new ModelTransitionCalculatedRepositoryStub();
        modelTransitionRepositoryStub = new ModelTransitionRepositoryStub();

        KeyValueMapService keyValueMapService = Mockito.mock(KeyValueMapService.class);
        Mockito.when(keyValueMapService.getInteger(anyString()))
            .thenReturn(batchSize);

        modelTransitionCalculatedService = new ModelTransitionCalculatedService(
            modelTransitionRepositoryStub,
            modelTransitionCalculatedRepositoryStub,
            keyValueMapService
        );
    }

    @Test
    public void testLinkedTransitions() {
        // MODEL_1 -> MODEL_2 -> MODEL_3 -> MODEL_4
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_2)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setType(ModelTransitionType.DUPLICATE)
        );
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_2)
                .setNewEntityId(MODEL_3)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setType(ModelTransitionType.DUPLICATE)
        );
        modelTransitionRepositoryStub.save(new ModelTransition()
            .setPrimaryTransition(true)
            .setOldEntityId(MODEL_3)
            .setNewEntityId(MODEL_4)
            .setOldEntityDeleted(true)
            .setDate(LocalDateTime.now())
            .setEntityType(EntityType.MODEL)
            .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
            .setType(ModelTransitionType.DUPLICATE)
        );

        modelTransitionCalculatedService.calculateNewTransitions();

        Map<Long, ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all())
                .stream().collect(Collectors.toMap(ModelTransitionCalculated::getOldEntityId, Function.identity()));

        Assertions.assertThat(calculatedTransitions.values())
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.DUPLICATE)
                    .setReason(ModelTransitionReason.DUPLICATE_REMOVAL),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.DUPLICATE)
                    .setReason(ModelTransitionReason.DUPLICATE_REMOVAL),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_3)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.DUPLICATE)
                    .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
            ));

        Assertions.assertThat(calculatedTransitions.get(MODEL_1).getPrecedingEntityIds())
            .isNull();
        Assertions.assertThat(calculatedTransitions.get(MODEL_2).getPrecedingEntityIds())
            .containsExactlyInAnyOrder(MODEL_1);
        Assertions.assertThat(calculatedTransitions.get(MODEL_3).getPrecedingEntityIds())
            .containsExactlyInAnyOrder(MODEL_1, MODEL_2);
    }

    @Test
    public void testSimpleSplitInsideOneAction() {
        // m1 -> m2
        //    -> m3
        modelTransitionRepositoryStub.save(Arrays.asList(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_2)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT),
            new ModelTransition()
                .setPrimaryTransition(false)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_3)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        ));

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_2)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));
    }

    @Test
    public void testSimpleSplitInsideDifferentActions() {
        // action 1: m1 -> m2
        // action 2:    -> m3
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_2)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        );
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(false)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_3)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        );

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_2)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));
    }

    @Test
    public void testSplitAfterTransitionInsideOneAction() {
        // m1 -> m2 -> m3
        //          -> m4
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_2)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setType(ModelTransitionType.DUPLICATE)
        );
        modelTransitionRepositoryStub.save(Arrays.asList(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_2)
                .setNewEntityId(MODEL_3)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT),
            new ModelTransition()
                .setPrimaryTransition(false)
                .setOldEntityId(MODEL_2)
                .setNewEntityId(MODEL_4)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        ));

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));
    }

    @Test
    public void testSplitAfterTransitionInsideDifferentActions() {
        // m1 -> m2 -> m3
        //          -> m4
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_2)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setType(ModelTransitionType.DUPLICATE)
        );
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_2)
                .setNewEntityId(MODEL_3)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        );
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(false)
                .setOldEntityId(MODEL_2)
                .setNewEntityId(MODEL_4)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        );

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));

        Assertions.assertThat(
            calculatedTransitions.stream()
                .filter(t -> t.getOldEntityId() == MODEL_1)
                .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds()).isNull());
        Assertions.assertThat(
            calculatedTransitions.stream()
                .filter(t -> t.getOldEntityId() == MODEL_2)
                .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds())
                .containsExactlyInAnyOrder(MODEL_1));
    }

    @Test
    public void testTwoSplitsIntoSameModelInsideOneAction() {
        //       m5 -> m6
        //          -> m3
        // m1 -> m2 -> m3
        //          -> m4
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_2)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setType(ModelTransitionType.DUPLICATE)
        );
        modelTransitionRepositoryStub.save(Arrays.asList(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_5)
                .setNewEntityId(MODEL_6)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT),
            new ModelTransition()
                .setPrimaryTransition(false)
                .setOldEntityId(MODEL_5)
                .setNewEntityId(MODEL_3)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        ));
        modelTransitionRepositoryStub.save(Arrays.asList(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_2)
                .setNewEntityId(MODEL_3)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT),
            new ModelTransition()
                .setPrimaryTransition(false)
                .setOldEntityId(MODEL_2)
                .setNewEntityId(MODEL_4)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        ));

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_5)
                    .setNewEntityId(MODEL_6)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_5)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));
    }

    @Test
    public void testTwoSplitsIntoSameModelInsideDifferentActions() {
        //       m5 -> m6
        //          -> m3
        // m1 -> m2 -> m3
        //          -> m4
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_2)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setType(ModelTransitionType.DUPLICATE)
        );
        modelTransitionRepositoryStub.save(Arrays.asList(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_5)
                .setNewEntityId(MODEL_6)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT),
            new ModelTransition()
                .setPrimaryTransition(false)
                .setOldEntityId(MODEL_5)
                .setNewEntityId(MODEL_3)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        ));
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_2)
                .setNewEntityId(MODEL_3)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        );
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(false)
                .setOldEntityId(MODEL_2)
                .setNewEntityId(MODEL_4)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        );

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_5)
                    .setNewEntityId(MODEL_6)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_5)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));
    }

    @Test
    @SuppressWarnings("checkstyle:methodLength")
    public void testMultipleSplits() {
        // original splits transitions:
        // m1 -> m2             -> m7
        //    -> m3 -> m4 -> m6 -> m8
        //          -> m5

        // m1 split to m2, m3
        modelTransitionRepositoryStub.save(Arrays.asList(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_2)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT),
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_1)
                .setNewEntityId(MODEL_3)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        ));

        // m3 split to m4, m5
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_3)
                .setNewEntityId(MODEL_4)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        );
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_3)
                .setNewEntityId(MODEL_5)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        );

        // m4 removed for m6
        modelTransitionRepositoryStub.save(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_4)
                .setNewEntityId(MODEL_6)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setType(ModelTransitionType.DUPLICATE)
        );

        // m6 split to m7, m8
        modelTransitionRepositoryStub.save(Arrays.asList(
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_6)
                .setNewEntityId(MODEL_7)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT),
            new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(MODEL_6)
                .setNewEntityId(MODEL_8)
                .setOldEntityDeleted(true)
                .setDate(LocalDateTime.now())
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setType(ModelTransitionType.SPLIT)
        ));

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                // m1 -> {m2, m7, m8, m5}
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_2)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_7)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_8)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_5)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                // m3 -> {m7, m8, m5}
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_3)
                    .setNewEntityId(MODEL_7)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_3)
                    .setNewEntityId(MODEL_8)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_3)
                    .setNewEntityId(MODEL_5)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                // m4 -> {m7, m8}
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_4)
                    .setNewEntityId(MODEL_7)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_4)
                    .setNewEntityId(MODEL_8)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                // m6 -> {m7, m8}
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_6)
                    .setNewEntityId(MODEL_7)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_6)
                    .setNewEntityId(MODEL_8)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));

        Assertions.assertThat(
            calculatedTransitions.stream()
                .filter(t -> t.getOldEntityId() == MODEL_1)
                .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds()).isNull());
        Assertions.assertThat(
            calculatedTransitions.stream()
                .filter(t -> t.getOldEntityId() == MODEL_3)
                .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds())
                .containsExactlyInAnyOrder(MODEL_1));
        Assertions.assertThat(
            calculatedTransitions.stream()
                .filter(t -> t.getOldEntityId() == MODEL_4)
                .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds())
                .containsExactlyInAnyOrder(MODEL_1, MODEL_3));
        Assertions.assertThat(
            calculatedTransitions.stream()
                .filter(t -> t.getOldEntityId() == MODEL_5)
                .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds())
                .containsExactlyInAnyOrder(MODEL_1));
        Assertions.assertThat(
            calculatedTransitions.stream()
                .filter(t -> t.getOldEntityId() == MODEL_6)
                .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds())
                .containsExactlyInAnyOrder(MODEL_1, MODEL_3, MODEL_4));
    }

    @Test
    public void testLinkedTransitionsAndRevert() {
        // MODEL_1 -> MODEL_2 -> MODEL_3 -> MODEL_4 -> REVERT(MODEL_2)
        createModelTransitionChain(
            Arrays.asList(MODEL_1, MODEL_2, MODEL_3, MODEL_4),
            EntityType.MODEL,
            ModelTransitionType.DUPLICATE,
            ModelTransitionReason.DUPLICATE_REMOVAL,
            true
        ).forEach(modelTransitionRepositoryStub::save);

        createRevertTransitions(Arrays.asList(MODEL_2), EntityType.MODEL)
            .forEach(modelTransitionRepositoryStub::save);

        modelTransitionCalculatedService.calculateNewTransitions();

        Map<Long, ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all())
                .stream().collect(Collectors.toMap(ModelTransitionCalculated::getOldEntityId, Function.identity()));

        Assertions.assertThat(calculatedTransitions.values())
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_2)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.DUPLICATE)
                    .setReason(ModelTransitionReason.DUPLICATE_REMOVAL),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_3)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.DUPLICATE)
                    .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
            ));

        Assertions.assertThat(calculatedTransitions.get(MODEL_1).getPrecedingEntityIds())
            .isNull();
        Assertions.assertThat(calculatedTransitions.get(MODEL_3).getPrecedingEntityIds())
            .isNull();
    }

    @Test
    public void testSplitAfterTransitionInsideOneActionAndRevert() {
        // m1 -> m2 -> m3
        //          -> m4
        //                 revert(m1)
        modelTransitionRepositoryStub.save(
            createModelTransitionChain(
                Arrays.asList(MODEL_1, MODEL_2),
                EntityType.SKU,
                ModelTransitionType.DUPLICATE,
                ModelTransitionReason.DUPLICATE_REMOVAL,
                true
            )
        );
        modelTransitionRepositoryStub.save(
            createSplitTransitions(
                MODEL_2,
                Arrays.asList(MODEL_3, MODEL_4),
                EntityType.SKU,
                true,
                false
            )
        );

        modelTransitionRepositoryStub.save(createRevertTransitions(Arrays.asList(MODEL_1), EntityType.SKU));

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));
    }

    @Test
    public void testSplitAfterTransitionInsideDifferentActionsAndRevert() {
        // m1 -> m2 -> m3
        //          -> m4
        //                 revert m1
        modelTransitionRepositoryStub.save(
            createModelTransitionChain(
                Arrays.asList(MODEL_1, MODEL_2),
                EntityType.SKU,
                ModelTransitionType.DUPLICATE,
                ModelTransitionReason.DUPLICATE_REMOVAL,
                true
            )
        );
        createSplitTransitions(
            MODEL_2,
            Arrays.asList(MODEL_3, MODEL_4),
            EntityType.SKU,
            true,
            false
        ).forEach(modelTransitionRepositoryStub::save);

        modelTransitionRepositoryStub.save(createRevertTransitions(Arrays.asList(MODEL_1), EntityType.SKU));

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_2)
                    .setNewEntityId(MODEL_4)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));

        Assertions.assertThat(
                calculatedTransitions.stream()
                    .filter(t -> t.getOldEntityId() == MODEL_2)
                    .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds()).isNull());
    }

    @Test
    public void testTwoSplitsIntoSameModelInsideOneActionAndRevert() {
        //       m5 -> m6
        //          -> m3
        // m1 -> m2 -> m3
        //          -> m4
        //                 revert(m2)
        modelTransitionRepositoryStub.save(
            createModelTransitionChain(
                Arrays.asList(MODEL_1, MODEL_2),
                EntityType.SKU,
                ModelTransitionType.DUPLICATE,
                ModelTransitionReason.DUPLICATE_REMOVAL,
                true
            )
        );
        modelTransitionRepositoryStub.save(
            createSplitTransitions(
                MODEL_5,
                Arrays.asList(MODEL_6, MODEL_3),
                EntityType.SKU,
                true,
                false
            )
        );
        modelTransitionRepositoryStub.save(
            createSplitTransitions(
                MODEL_2,
                Arrays.asList(MODEL_3, MODEL_4),
                EntityType.SKU,
                true,
                false
            )
        );
        modelTransitionRepositoryStub.save(createRevertTransitions(Arrays.asList(MODEL_2), EntityType.SKU));

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_2)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_5)
                    .setNewEntityId(MODEL_6)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_5)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(false)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));
    }

    @Test
    @SuppressWarnings("checkstyle:methodLength")
    public void testMultipleSplitsAndRevert() {
        // original splits transitions:
        // m1 -> m2                             -> m7
        //    -> m3 -> m4 -> m6, revert(m3), m6 -> m8
        //          -> m5

        // m1 split to m2, m3
        modelTransitionRepositoryStub.save(
            createSplitTransitions(
                MODEL_1,
                Arrays.asList(MODEL_2, MODEL_3),
                EntityType.SKU,
                true,
                true
            )
        );

        // m3 split to m4, m5
        modelTransitionRepositoryStub.save(
            createSplitTransitions(
                MODEL_3,
                Arrays.asList(MODEL_4, MODEL_5),
                EntityType.SKU,
                true,
                true
            )
        );

        // m4 removed for m6
        modelTransitionRepositoryStub.save(
            createModelTransitionChain(
                Arrays.asList(MODEL_4, MODEL_6),
                EntityType.SKU,
                ModelTransitionType.DUPLICATE,
                ModelTransitionReason.DUPLICATE_REMOVAL,
                true
            )
        );

        //revert(m3)
        modelTransitionRepositoryStub.save(createRevertTransitions(Arrays.asList(MODEL_3), EntityType.SKU));

        // m6 split to m7, m8
        modelTransitionRepositoryStub.save(
            createSplitTransitions(
                MODEL_6,
                Arrays.asList(MODEL_7, MODEL_8),
                EntityType.SKU,
                true,
                true
            )
        );

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                // m1 -> {m2, m3}
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_2)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                // m4 -> {m7, m8}
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_4)
                    .setNewEntityId(MODEL_7)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_4)
                    .setNewEntityId(MODEL_8)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                // m6 -> {m7, m8}
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_6)
                    .setNewEntityId(MODEL_7)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_6)
                    .setNewEntityId(MODEL_8)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));

        Assertions.assertThat(
                calculatedTransitions.stream()
                    .filter(t -> t.getOldEntityId() == MODEL_1)
                    .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds()).isNull());
        Assertions.assertThat(
                calculatedTransitions.stream()
                    .filter(t -> t.getOldEntityId() == MODEL_4)
                    .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds()).isNull());
        Assertions.assertThat(
                calculatedTransitions.stream()
                    .filter(t -> t.getOldEntityId() == MODEL_6)
                    .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds())
                .containsExactlyInAnyOrder(MODEL_4));
    }

    @Test
    @SuppressWarnings("checkstyle:methodLength")
    public void testMultipleSplitsAndRevertLast() {
        // original splits transitions:
        // m1 -> m2             -> m7
        //    -> m3 -> m4 -> m6 -> m8, revert(m3)
        //          -> m5

        // m1 split to m2, m3
        modelTransitionRepositoryStub.save(
            createSplitTransitions(
                MODEL_1,
                Arrays.asList(MODEL_2, MODEL_3),
                EntityType.SKU,
                true,
                true
            )
        );

        // m3 split to m4, m5
        modelTransitionRepositoryStub.save(
            createSplitTransitions(
                MODEL_3,
                Arrays.asList(MODEL_4, MODEL_5),
                EntityType.SKU,
                true,
                true
            )
        );

        // m4 removed for m6
        modelTransitionRepositoryStub.save(
            createModelTransitionChain(
                Arrays.asList(MODEL_4, MODEL_6),
                EntityType.SKU,
                ModelTransitionType.DUPLICATE,
                ModelTransitionReason.DUPLICATE_REMOVAL,
                true
            )
        );

        // m6 split to m7, m8
        modelTransitionRepositoryStub.save(
            createSplitTransitions(
                MODEL_6,
                Arrays.asList(MODEL_7, MODEL_8),
                EntityType.SKU,
                true,
                true
            )
        );

        //revert(m3)
        modelTransitionRepositoryStub.save(createRevertTransitions(Arrays.asList(MODEL_3), EntityType.SKU));

        modelTransitionCalculatedService.calculateNewTransitions();

        List<ModelTransitionCalculated> calculatedTransitions =
            modelTransitionCalculatedRepositoryStub.find(ModelTransitionsCalculatedRepository.Filter.all());

        Assertions.assertThat(calculatedTransitions)
            .usingElementComparatorOnFields(
                "oldEntityId", "newEntityId", "oldEntityDeleted", "primaryTransition", "type", "reason")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                // m1 -> {m2, m3}
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_2)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_1)
                    .setNewEntityId(MODEL_3)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                // m4 -> {m7, m8}
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_4)
                    .setNewEntityId(MODEL_7)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_4)
                    .setNewEntityId(MODEL_8)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),

                // m6 -> {m7, m8}
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_6)
                    .setNewEntityId(MODEL_7)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT),
                new ModelTransitionCalculated()
                    .setOldEntityId(MODEL_6)
                    .setNewEntityId(MODEL_8)
                    .setOldEntityDeleted(true)
                    .setPrimaryTransition(true)
                    .setType(ModelTransitionType.SPLIT)
                    .setReason(ModelTransitionReason.SKU_SPLIT)
            ));

        Assertions.assertThat(
                calculatedTransitions.stream()
                    .filter(t -> t.getOldEntityId() == MODEL_1)
                    .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds()).isNull());
        Assertions.assertThat(
                calculatedTransitions.stream()
                    .filter(t -> t.getOldEntityId() == MODEL_4)
                    .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds()).isNull());
        Assertions.assertThat(
                calculatedTransitions.stream()
                    .filter(t -> t.getOldEntityId() == MODEL_6)
                    .collect(Collectors.toList()))
            .allSatisfy(tr -> Assertions.assertThat(tr.getPrecedingEntityIds())
                .containsExactlyInAnyOrder(MODEL_4));
    }

    private List<ModelTransition> createModelTransitionChain(
        List<Long> modelIds,
        EntityType entityType,
        ModelTransitionType type,
        ModelTransitionReason reason,
        boolean deleted
    ) {
        List<ModelTransition> result = new ArrayList<>();

        for (int i = 0; i < modelIds.size() - 1; i++) {
            result.add(new ModelTransition()
                .setPrimaryTransition(true)
                .setOldEntityId(modelIds.get(i))
                .setNewEntityId(modelIds.get(i + 1))
                .setOldEntityDeleted(deleted)
                .setDate(LocalDateTime.now())
                .setEntityType(entityType)
                .setReason(reason)
                .setType(type)
            );
        }

        return result;
    }

    private List<ModelTransition> createSplitTransitions(
        long fromId,
        List<Long> toIds,
        EntityType entityType,
        boolean deleted,
        boolean allPrimary
    ) {
        Optional<Long> firstId = Optional.ofNullable(CollectionUtils.isNotEmpty(toIds) ? toIds.get(0) : null);
        return toIds.stream()
            .map(id -> new ModelTransition()
                .setEntityType(entityType)
                .setReason(entityType == EntityType.SKU ?
                    ModelTransitionReason.SKU_SPLIT :
                    ModelTransitionReason.MODEL_SPLIT)
                .setType(ModelTransitionType.SPLIT)
                .setOldEntityId(fromId)
                .setNewEntityId(id)
                .setOldEntityDeleted(deleted)
                .setPrimaryTransition(allPrimary || firstId.map(f -> f.equals(id)).orElse(false))
            )
            .collect(Collectors.toList());
    }


    private List<ModelTransition> createRevertTransitions(List<Long> modelIds, EntityType entityType) {
        return modelIds.stream()
            .map(id -> new ModelTransition()
                .setEntityType(entityType)
                .setReason(ModelTransitionReason.UNDELETE)
                .setType(ModelTransitionType.REVERT)
                .setOldEntityId(id)
                .setNewEntityId(id)
                .setOldEntityDeleted(false)
                .setPrimaryTransition(true)
            )
            .collect(Collectors.toList());
    }
}
