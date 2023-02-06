package ru.yandex.direct.core.entity.warnplace.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestWarnplaceRepository;
import ru.yandex.direct.core.testing.steps.WarnplaceSteps;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class WarnplaceRepositoryTest {

    private static final int SHARD = 1;
    private static LocalDateTime localDateTime = LocalDateTime.now().withNano(0);
    private Long idBefore;
    private Long idNow;
    private Long idAfter;

    @Autowired
    private WarnplaceSteps warnplaceSteps;

    @Autowired
    private TestWarnplaceRepository warnplaceRepository;

    @Before
    public void before() {
        idBefore = warnplaceSteps.createDefaultWarnplaceWithAddTime(localDateTime.minusMinutes(1), SHARD);
        idNow = warnplaceSteps.createDefaultWarnplaceWithAddTime(localDateTime, SHARD);
        idAfter = warnplaceSteps.createDefaultWarnplaceWithAddTime(localDateTime.plusMinutes(1), SHARD);
        warnplaceRepository.deleteRecordsOlderThan(SHARD, localDateTime);
    }

    @Test
    public void checkNotDeletedRecords() {
        Collection<Long> notDeletedIds = warnplaceRepository.getExistentIds(SHARD, Arrays.asList(idNow, idAfter));
        assertThat("остались две правильные записи", notDeletedIds, containsInAnyOrder(idNow, idAfter));
    }

    @Test
    public void checkDeletedRecord() {
        Collection<Long> deletedId = warnplaceRepository.getExistentIds(SHARD, Arrays.asList(idBefore));
        assertThat("запись, которая должна была быть удалена, действительно удалилась",
                deletedId, emptyCollectionOf(Long.class));
    }
}
