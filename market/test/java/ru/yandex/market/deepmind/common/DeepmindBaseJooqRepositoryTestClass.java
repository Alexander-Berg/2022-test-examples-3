package ru.yandex.market.deepmind.common;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.jooq.exception.NoDataFoundException;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mbo.jooq.JooqPojo;
import ru.yandex.market.mbo.jooq.repo.JooqRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author yuramalinov
 * @created 15.07.2019
 */
public abstract class DeepmindBaseJooqRepositoryTestClass<Entity extends JooqPojo, Id> extends DeepmindBaseDbTestClass {
    protected static final long SEED = 56087497;
    protected static final int BATCH_SIZE = 17;
    protected final Class<Entity> cls;
    protected final Function<Entity, Id> idGetter;

    protected EnhancedRandom random;
    protected String[] generatedFields = {};

    protected DeepmindBaseJooqRepositoryTestClass(Class<Entity> cls, Function<Entity, Id> idGetter) {
        this.cls = cls;
        this.idGetter = idGetter;
    }

    protected abstract JooqRepository<Entity, ?, Id, ?, ?> repository();

    @Before
    public void setUpRandom() {
        random = TestUtils.createMskuRandom(SEED);
    }

    @Test
    public void saveSingle() {
        Entity created = random();
        Entity saved = repository().save(created);

        assertThat(saved).isNotNull();
        assertThat(idGetter.apply(saved)).isNotNull();
        assertThat(saved).isEqualToIgnoringGivenFields(created, generatedFields);
    }

    @Test
    public void saveBatch() {
        List<Entity> values = Stream.generate(this::random).limit(BATCH_SIZE).collect(Collectors.toList());
        List<Entity> saved = repository().save(values);

        assertThat(saved)
            .usingElementComparatorIgnoringFields(generatedFields)
            .containsExactlyInAnyOrderElementsOf(values);
    }

    @Test
    public void delete() {
        Entity saved = repository().save(random());
        Id id = idGetter.apply(saved);
        repository().delete(Collections.singleton(id));

        List<Entity> found = repository().findAll();
        assertThat(found).isEmpty();

        assertThatThrownBy(() -> repository().getById(id))
            .isInstanceOf(NoDataFoundException.class);

        assertThat(repository().getByIds(Collections.singleton(id))).isEmpty();
    }

    @Test
    public void deleteByEntry() {
        Entity saved = repository().save(random());
        repository().deleteByEntities(Collections.singleton(saved));

        Id id = idGetter.apply(saved);
        List<Entity> found = repository().findAll();
        assertThat(found).isEmpty();

        assertThatThrownBy(() -> repository().getById(id))
            .isInstanceOf(NoDataFoundException.class);

        assertThat(repository().getByIds(Collections.singleton(id))).isEmpty();
    }

    protected Entity random() {
        return random.nextObject(cls, generatedFields);
    }
}
