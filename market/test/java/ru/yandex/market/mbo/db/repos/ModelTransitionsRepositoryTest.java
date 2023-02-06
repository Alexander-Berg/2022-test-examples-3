package ru.yandex.market.mbo.db.repos;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionReason;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransition;
import ru.yandex.market.mbo.db.repo.ModelTransitionsRepository;
import ru.yandex.market.mbo.utils.BaseDbTest;
import ru.yandex.market.mbo.utils.RandomTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class ModelTransitionsRepositoryTest extends BaseDbTest {
    public static final int SEED = 777;

    EnhancedRandom rnd = RandomTestUtils.createNewRandom(SEED);

    @Autowired
    private ModelTransitionsRepository modelTransitionsRepository;

    @Test
    public void testCreateAndFindTransitions() {
        ModelTransition transition1 = createTransition();
        ModelTransition transition2 = createTransition();
        ModelTransition transition3 = createTransition();

        List<ModelTransition> transitions = Arrays.asList(transition1, transition2, transition3);
        List<ModelTransition> createdTransitions = modelTransitionsRepository.save(transitions);

        assertThat(createdTransitions).allSatisfy(modelTransition -> {
            assertThat(modelTransition.getId()).isNotNull();
            assertThat(modelTransition.getActionId()).isNotNull();
            assertThat(modelTransition.getExportedDate()).isNull();
        });

        assertThat(createdTransitions)
            .usingElementComparatorIgnoringFields("id", "actionId")
            .containsExactlyInAnyOrderElementsOf(transitions);

        List<ModelTransition> foundTransitions = modelTransitionsRepository
            .find(new ModelTransitionsRepository.Filter().setNotExportedYet(true));

        assertThat(foundTransitions).containsExactlyInAnyOrderElementsOf(createdTransitions);

    }

    @Test
    public void testFilterByDate() {
        ModelTransition transition1 = createTransition()
            .setDate(LocalDateTime.now().minusDays(1));
        ModelTransition transition2 = createTransition()
            .setDate(LocalDateTime.now().minusDays(3));
        ModelTransition transition3 = createTransition()
            .setDate(LocalDateTime.now().minusDays(5));

        List<ModelTransition> expected = Arrays.asList(transition1, transition2);
        modelTransitionsRepository.save(Arrays.asList(transition1, transition2, transition3));

        List<ModelTransition> foundTransitions = modelTransitionsRepository.find(
            new ModelTransitionsRepository.Filter().setTransitionAfter(LocalDateTime.now().minusDays(5)));

        assertThat(foundTransitions)
            .usingElementComparatorIgnoringFields("id", "actionId")
            .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testFilterByOldIds() {
        ModelTransition transition1 = createTransition();
        ModelTransition transition2 = createTransition();
        ModelTransition transition3 = createTransition();
        ModelTransition transition4 = createTransition();

        List<ModelTransition> saved = modelTransitionsRepository.save(
            Arrays.asList(transition1, transition2, transition3, transition4));

        List<ModelTransition> expected = Arrays.asList(
            saved.get(rnd.nextInt(saved.size())),
            saved.get(rnd.nextInt(saved.size()))
        );

        List<ModelTransition> foundTransitions = modelTransitionsRepository.find(
            new ModelTransitionsRepository.Filter().addAllOldEntityIds(
                expected.stream()
                    .map(ModelTransition::getOldEntityId)
                    .collect(Collectors.toList())
            )
        );

        assertThat(foundTransitions)
            .usingElementComparatorIgnoringFields("id", "actionId")
            .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testRemoveExportedUndeleteTransitions() {
        ModelTransition transition1 = createUndeleteTransition(EntityType.SKU)
            .setExportedDate(LocalDateTime.now());
        ModelTransition transition2 = createUndeleteTransition(EntityType.SKU);
        ModelTransition transition3 = createUndeleteTransition(EntityType.MODEL)
            .setExportedDate(LocalDateTime.now());
        ModelTransition transition4 = createTransition()
            .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
            .setType(ModelTransitionType.DUPLICATE)
            .setExportedDate(LocalDateTime.now());

        List<ModelTransition> expected = Arrays.asList(transition2, transition3, transition4);

        modelTransitionsRepository.save(Arrays.asList(transition1, transition2, transition3, transition4));

        int deleted = modelTransitionsRepository.removeExportedUndeleteTransitions(
            Collections.singletonList(EntityType.SKU));

        List<ModelTransition> foundTransitions = modelTransitionsRepository.find(
            ModelTransitionsRepository.Filter.all());

        assertThat(deleted).isEqualTo(1);
        assertThat(foundTransitions)
            .usingElementComparatorIgnoringFields("id", "actionId")
            .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testRemoveExportedUndeleteTransitionsWithDependent() {
        ModelTransition transition1 = createUndeleteTransition(EntityType.SKU)
            .setExportedDate(LocalDateTime.now());

        ModelTransition transition2 = createUndeleteTransition(EntityType.SKU);

        ModelTransition transition3 = createDependentTransition(transition1, true)
            .setExportedDate(LocalDateTime.now());
        ModelTransition transition4 = createDependentTransition(transition1, true);
        ModelTransition transition5 = createDependentTransition(transition1, false);

        ModelTransition transition6 = createDependentTransition(transition2, true)
            .setExportedDate(LocalDateTime.now());

        List<ModelTransition> expected = Arrays.asList(transition2, transition4, transition5, transition6);

        modelTransitionsRepository.save(Arrays.asList(transition1, transition2, transition3, transition4,
            transition5, transition6));

        int deleted = modelTransitionsRepository.removeExportedUndeleteTransitions(
            Collections.singletonList(EntityType.SKU));

        List<ModelTransition> foundTransitions = modelTransitionsRepository.find(
            ModelTransitionsRepository.Filter.all());

        assertThat(deleted).isEqualTo(2);
        assertThat(foundTransitions)
            .usingElementComparatorIgnoringFields("id", "actionId")
            .containsExactlyInAnyOrderElementsOf(expected);
    }


    private ModelTransition createTransition() {
        return RandomTestUtils.randomObject(ModelTransition.class, "id", "actionId", "exportedDate");
    }

    private ModelTransition createUndeleteTransition(EntityType entityType) {
        return createTransition()
            .setEntityType(entityType)
            .setReason(ModelTransitionReason.UNDELETE)
            .setType(ModelTransitionType.REVERT);
    }

    private ModelTransition createDependentTransition(ModelTransition transition, boolean deleted) {
        return createTransition()
            .setEntityType(transition.getEntityType())
            .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
            .setType(ModelTransitionType.DUPLICATE)
            .setOldEntityDeleted(deleted)
            .setOldEntityId(transition.getNewEntityId());
    }
}
