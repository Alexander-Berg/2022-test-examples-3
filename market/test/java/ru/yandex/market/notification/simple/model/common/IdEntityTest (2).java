package ru.yandex.market.notification.simple.model.common;

import java.util.Random;

import org.junit.Test;

import ru.yandex.market.notification.simple.model.common.IdEntity;
import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link IdEntity}.
 *
 * @author Vladislav Bauer
 */
public class IdEntityTest extends AbstractModelTest {

    private static final Random RANDOM = new Random();


    @Test
    public void testConstruction() {
        final long id = generateId();

        final IdEntity<Long> idEntity = new IdEntity<>(id);
        final TestIdEntity testIdEntity = new TestIdEntity(id);

        assertThat(idEntity.getId(), equalTo(id));
        assertThat(testIdEntity.getId(), equalTo(id));
    }

    @Test
    public void testBasicMethods() {
        final long id = generateId();
        final TestIdEntity entity = new TestIdEntity(id);
        final TestIdEntity sameEntity = new TestIdEntity(id);
        final TestIdEntity otherEntity = new TestIdEntity(id + 1);

        checkBasicMethods(entity, sameEntity, otherEntity);
    }


    private long generateId() {
        return RANDOM.nextLong();
    }


    private static class TestIdEntity extends IdEntity<Long> {

        TestIdEntity(final Long id) {
            super(id);
        }

    }

}
