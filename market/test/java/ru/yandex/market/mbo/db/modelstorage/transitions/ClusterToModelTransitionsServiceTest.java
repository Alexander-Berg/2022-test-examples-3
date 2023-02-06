package ru.yandex.market.mbo.db.modelstorage.transitions;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionReason;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransition;
import ru.yandex.market.mbo.db.modelstorage.health.SaveStats;
import ru.yandex.market.mbo.db.repo.ModelTransitionsRepository;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.utils.BaseDbTest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class ClusterToModelTransitionsServiceTest extends BaseDbTest {

    @Autowired
    ModelTransitionsRepository modelTransitionsRepository;

    ClusterToModelTransitionsService clusterToModelTransitionsService;

    LocalDateTime time = LocalDateTime.now().withNano(0);

    @Before
    public void setUp() {
        clusterToModelTransitionsService = new ClusterToModelTransitionsService(modelTransitionsRepository);
    }

    @Test
    public void testErrorTransition() {
        ModelStorage.ModelTransitions transitions = createTransitions(null, null);

        clusterToModelTransitionsService.save(Collections.singletonList(transitions), new SaveStats());

        List<ModelTransition> savedTransitions =
            modelTransitionsRepository.find(ModelTransitionsRepository.Filter.all());

        assertThat(savedTransitions)
            .usingElementComparatorIgnoringFields("id", "actionId")
            .containsExactly(
                createTransition(null, ModelTransitionType.ERROR, false)
            );
    }

    @Test
    public void testDuplicateTransition() {
        ModelStorage.ModelTransitions transitions = createTransitions(2L, null);

        clusterToModelTransitionsService.save(Collections.singletonList(transitions), new SaveStats());

        List<ModelTransition> savedTransitions =
            modelTransitionsRepository.find(ModelTransitionsRepository.Filter.all());

        assertThat(savedTransitions)
            .usingElementComparatorIgnoringFields("id", "actionId")
            .containsExactly(
                createTransition(2L, ModelTransitionType.DUPLICATE, true)
            );
    }

    @Test
    public void testSplitWithPrimaryTransition() {
        ModelStorage.ModelTransitions transitions = createTransitions(2L, ImmutableList.of(3L, 4L));

        clusterToModelTransitionsService.save(Collections.singletonList(transitions), new SaveStats());

        List<ModelTransition> savedTransitions =
            modelTransitionsRepository.find(ModelTransitionsRepository.Filter.all());

        assertThat(savedTransitions)
            .usingElementComparatorIgnoringFields("id", "actionId")
            .containsExactly(
                createTransition(2L, ModelTransitionType.SPLIT, true),
                createTransition(3L, ModelTransitionType.SPLIT, false),
                createTransition(4L, ModelTransitionType.SPLIT, false)
            );
    }

    @Test
    public void testSplitWithoutPrimaryTransition() {
        ModelStorage.ModelTransitions transitions = createTransitions(null, ImmutableList.of(3L, 4L));

        clusterToModelTransitionsService.save(Collections.singletonList(transitions), new SaveStats());

        List<ModelTransition> savedTransitions =
            modelTransitionsRepository.find(ModelTransitionsRepository.Filter.all());

        assertThat(savedTransitions)
            .usingElementComparatorIgnoringFields("id", "actionId")
            .containsExactly(
                createTransition(3L, ModelTransitionType.SPLIT, false),
                createTransition(4L, ModelTransitionType.SPLIT, false)
            );
    }

    public ModelStorage.ModelTransitions createTransitions(Long strong, List<Long> weak) {
        ModelStorage.ModelTransitions.Builder transitions = ModelStorage.ModelTransitions.newBuilder();
        transitions.setFromId(1L);
        transitions.setModelDeleteTimestamp(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        if (strong != null) {
            transitions.setStrong(ModelStorage.ModelTransitions.Transition.newBuilder().setToId(strong).build());
        }
        if (weak != null) {
            weak.forEach(w -> {
                transitions.addWeak(ModelStorage.ModelTransitions.Transition.newBuilder().setToId(w).build());
            });
        }
        return transitions.build();
    }

    public ModelTransition createTransition(Long toId, ModelTransitionType type, boolean primary) {
        return new ModelTransition()
            .setReason(ModelTransitionReason.CLUSTERIZATION)
            .setOldEntityId(1L)
            .setOldEntityDeleted(true)
            .setEntityType(EntityType.CLUSTER)
            .setType(type)
            .setDate(time)
            .setNewEntityId(toId)
            .setPrimaryTransition(primary);
    }
}
