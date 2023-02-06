package ru.yandex.market.mboc.common.offers.repository;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MappingTransition;
import ru.yandex.market.mboc.common.test.RandomTestUtils;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

public class MappingTransitionsRepositoryTest extends BaseDbTestClass {

    @Autowired
    private MappingTransitionsRepository mappingTransitionsRepository;

    @Test
    public void testCreateAndFindTransitions() {
        MappingTransition transition1 = createTransition();
        MappingTransition transition2 = createTransition();
        MappingTransition transition3 = createTransition();

        List<MappingTransition> transitions = Arrays.asList(transition1, transition2, transition3);
        List<MappingTransition> createdTransitions = mappingTransitionsRepository.save(transitions);

        assertThat(createdTransitions).allSatisfy(modelTransition -> {
            assertThat(modelTransition.getId()).isNotNull();
            assertThat(modelTransition.getActionId()).isNotNull();
            assertThat(modelTransition.getExportedDate()).isNull();
        });

        assertThat(createdTransitions)
            .usingElementComparatorIgnoringFields("id", "actionId")
            .containsExactlyInAnyOrderElementsOf(transitions);

        List<MappingTransition> foundTransitions = mappingTransitionsRepository
            .find(new MappingTransitionsRepository.Filter().setNotExportedYet(true));

        assertThat(foundTransitions).containsExactlyInAnyOrderElementsOf(createdTransitions);

    }

    private MappingTransition createTransition() {
        return RandomTestUtils.randomObject(MappingTransition.class, "id", "actionId", "exportedDate");
    }
}
