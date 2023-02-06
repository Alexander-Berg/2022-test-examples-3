package ru.yandex.market.mboc.common.dict.backgroundaction;

import java.util.function.Supplier;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinition;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

/**
 * @author yuramalinov
 * @created 14.05.18
 */
public class BackgroundActionRepositoryTest extends BaseDbTestClass {
    private static final long SEED = 42;

    @Autowired
    private BackgroundActionRepository repository;

    private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED)
        .overrideDefaultInitialization(true)
        .randomize(new FieldDefinition<>("result", Object.class, BackgroundAction.class),
            (Supplier<Integer>) this::randomInt)
        .build();

    @Test
    public void testInsert() {
        BackgroundAction action = repository.insert(new BackgroundAction().setUserLogin("Someone"));
        assertThat(action.getId()).isPositive();
    }

    @Test
    public void testRandomInsert() {
        BackgroundAction action = random.nextObject(BackgroundAction.class);
        repository.insert(action);

        BackgroundAction inserted = repository.findById(action.getId());
        assertThat(inserted).isEqualToComparingFieldByField(action);
    }

    @Test
    public void testRandomInsertOrUpdate() {
        BackgroundAction action = random.nextObject(BackgroundAction.class);
        repository.insertOrUpdate(action);

        BackgroundAction inserted = repository.findById(action.getId());
        assertThat(inserted).isEqualToComparingFieldByField(action);
    }

    @Test
    public void testRandomUpdate() {
        BackgroundAction action = random.nextObject(BackgroundAction.class);
        repository.insert(action);

        BackgroundAction updated = random.nextObject(BackgroundAction.class);
        updated.setId(action.getId());

        assertFalse("Random should produce different field values",
            EqualsBuilder.reflectionEquals(action, updated));

        repository.update(updated);

        BackgroundAction selected = repository.findById(action.getId());
        assertThat(selected).isEqualToComparingFieldByField(updated);
    }

    private Integer randomInt() {
        return random.nextInt();
    }
}
