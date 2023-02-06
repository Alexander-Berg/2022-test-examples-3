package ru.yandex.market.abo.core.indexer.caps;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.abo.util.entity.DeletableEntityService;
import ru.yandex.market.abo.util.entity.DeletableEntityServiceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author artemmz
 * @date 27.10.17.
 */
public class CapsExceptionServiceTest extends DeletableEntityServiceTest<CapsException, String> {
    private static final Long USER_ID = RND.nextLong();

    @Autowired
    private CapsExceptionService capsExceptionService;

    @Test
    public void findAlike() {
        CapsException savedNotLike = capsExceptionService.addIfNotExistsOrDeleted(
                new CapsException("2b or not", 2L, "b - that is the question", false), USER_ID);
        CapsException savedAlike = capsExceptionService.addIfNotExistsOrDeleted(
                new CapsException("special", 4L, "u", false), USER_ID);

        Page<CapsException> foundPage = capsExceptionService.findAll(
                new CapsException("pec", null, null, false), PageRequest.of(0, 100));
        assertEquals(1, foundPage.getNumberOfElements());

        CapsException foundAlike = foundPage.getContent().get(0);
        assertEquals(savedAlike, foundAlike);
        assertNotEquals(savedNotLike, foundAlike);
    }

    @Override
    protected DeletableEntityService<CapsException, String> service() {
        return capsExceptionService;
    }

    @Override
    protected String extractId(CapsException entity) {
        return entity.getException();
    }

    @Override
    protected CapsException newEntity() {
        return new CapsException("2b or not", 2L, "b - that is the question", false);
    }

    @Override
    protected CapsException example() {
        return new CapsException();
    }
}
