package ru.yandex.market.mboc.common;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.jooq.JooqPojo;
import ru.yandex.market.mbo.jooq.repo.JooqWithDeletedFieldRepository;
import ru.yandex.market.mboc.common.msku.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseJooqWithDeletedFieldRepositoryTestClass<Entity extends JooqPojo, Id>
    extends BaseJooqRepositoryTestClass<Entity, Id> {

    protected BaseJooqWithDeletedFieldRepositoryTestClass(Class<Entity> cls, Function<Entity, Id> idGetter) {
        super(cls, idGetter);
    }

    protected abstract JooqWithDeletedFieldRepository<Entity, ?, Id, ?, ?> repository();

    @Before
    public void setUpRandom() {
        random = TestUtils.createMskuRandom(SEED);
    }

    @Test
    public void delete() {
        Entity saved = repository().save(random());
        Id id = idGetter.apply(saved);
        repository().delete(Collections.singleton(id));

        ReflectionTestUtils.setField(saved, "deleted", true);
        List<Entity> found = repository().findAll();
        assertThat(found)
            .usingElementComparatorIgnoringFields(generatedFields)
            .containsExactlyInAnyOrder(saved);

        Entity byId = repository().getById(id);
        assertThat(byId).isEqualToIgnoringGivenFields(saved, generatedFields);
    }

    @Test
    public void deleteByEntry() {
        Entity saved = repository().save(random());
        repository().deleteByEntities(Collections.singleton(saved));

        Id id = idGetter.apply(saved);
        ReflectionTestUtils.setField(saved, "deleted", true);
        List<Entity> found = repository().findAll();
        assertThat(found)
            .usingElementComparatorIgnoringFields(generatedFields)
            .containsExactlyInAnyOrder(saved);

        Entity byId = repository().getById(id);
        assertThat(byId).isEqualToIgnoringGivenFields(saved, generatedFields);
    }

    protected Entity random() {
        return random.nextObject(cls, generatedFields);
    }
}
