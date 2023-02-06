package ru.yandex.market.pers.notify.export;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author vtarasoff
 * @since 03.08.2021
 */
public class ChangedSubscriptionDaoTest extends MockedDbTest {
    @Autowired
    private ChangedSubscriptionDao changedSubscriptionDao;

	@Test
	public void shouldCorrectSaveChanges() {
	    long id1 = 123;
        long id2 = 456;

        changedSubscriptionDao.saveAll(List.of(id1, id2));

        var savedIds = changedSubscriptionDao.findAll();

        assertThat(savedIds.size(), is(2));
        assertThat(savedIds, containsInAnyOrder(equalTo(id1), equalTo(id2)));
	}

    @Test
    public void shouldSaveDuplicateChanges() {
        long id = 123;

        changedSubscriptionDao.saveAll(List.of(id, id));

        var savedIds = changedSubscriptionDao.findAll();

        assertThat(savedIds.size(), is(2));
        assertThat(savedIds, equalTo(List.of(id, id)));
    }

    @Test
    public void shouldCorrectDeleteChanges() {
        long id1 = 123;
        long id2 = 456;
        long id3 = 789;

        changedSubscriptionDao.saveAll(List.of(id1, id2, id3, id1));
        changedSubscriptionDao.deleteAll(List.of(id1, id3));

        var savedIds = changedSubscriptionDao.findAll();

        assertThat(savedIds.size(), is(1));
        assertThat(savedIds.get(0), equalTo(id2));
    }
}
