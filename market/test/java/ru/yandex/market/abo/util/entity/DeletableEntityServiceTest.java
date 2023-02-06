package ru.yandex.market.abo.util.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author artemmz
 * @date 15.03.18.
 */
public abstract class DeletableEntityServiceTest<T extends DeletableEntity, ID extends Serializable> extends EmptyTest {

    protected abstract DeletableEntityService<T, ID> service();

    protected abstract ID extractId(T entity);

    protected abstract T newEntity();

    protected abstract T example();

    @Test
    public void findAll() {
        T saved = service().addIfNotExistsOrDeleted(newEntity(), RND.nextLong());
        Page<T> found = service().findAll(example(), PageRequest.of(0, 100));

        assertEquals(1, found.getNumberOfElements());
        assertEquals(saved, found.getContent().get(0));
    }

    @Test
    public void alreadyExists() {
        T newEntity = newEntity();
        service().addIfNotExistsOrDeleted(newEntity, RND.nextLong());
        assertThrows(IllegalArgumentException.class, () ->
                service().addIfNotExistsOrDeleted(newEntity, RND.nextLong()));
    }

    @Test
    public void findAllNotDeleted() {
        T newEntity = newEntity();
        Optional<T> alikeInDb = service().findAlikeNotDeleted(newEntity);
        assertFalse(alikeInDb.isPresent());

        T saved = service().addIfNotExistsOrDeleted(newEntity, RND.nextLong());
        List<T> notDeleted = service().findAllNotDeleted();

        assertEquals(1, notDeleted.size());
        assertEquals(extractId(saved), extractId(notDeleted.iterator().next()));

        service().markDeleted(extractId(saved), RND.nextLong());
        assertEquals(0, service().findAllNotDeleted().size());
    }
}
