package ru.yandex.direct.core.entity.welcomeemail.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.welcomeemail.model.NewUsersData;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.utils.FunctionalUtils;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewUsersDataRepositoryTest {
    private final Long uid1 = 1L;
    private final Long uid2 = 2L;
    private final Duration maxAge = Duration.ofHours(24);

    private LocalDateTime now = LocalDateTime.now();

    @Autowired
    private NewUsersDataRepository repoUnderTest;

    @Before
    public void before() {
        NewUsersData datum1 = new NewUsersData()
                .withUid(uid1)
                .withLastChange(now.minusHours(1).minus(maxAge))
                .withEmail("uid1@yandex.ru");
        NewUsersData datum2 = new NewUsersData()
                .withUid(uid2)
                .withLastChange(now.minusHours(1))
                .withEmail("uid2@yandex.ru");
        repoUnderTest.addNewUserData(Arrays.asList(datum1, datum2));
    }

    @After
    public void after() {
        repoUnderTest.deleteEntriesForUids(Arrays.asList(uid1, uid2), now);
    }

    @Test
    public void getEntriesOlderThan() {
        List<Long> uids = FunctionalUtils.mapList(repoUnderTest.getEntriesOlderThan(now), NewUsersData::getUid);
        assertThat(uids, containsInAnyOrder(1L, 2L));
    }

    @Test
    public void deleteEntriesForUids() {
        int deletedCount = repoUnderTest.deleteEntriesForUids(Arrays.asList(uid2, uid1), now.minus(maxAge));
        assertThat(deletedCount, is(1));

        List<Long> uids = FunctionalUtils.mapList(repoUnderTest.getEntriesOlderThan(now), NewUsersData::getUid);
        assertThat(uids, containsInAnyOrder(uid2));
    }
}
