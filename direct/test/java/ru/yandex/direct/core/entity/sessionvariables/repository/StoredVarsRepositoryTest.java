package ru.yandex.direct.core.entity.sessionvariables.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestStoredVarsRepository;
import ru.yandex.direct.core.testing.steps.StoredVarsSteps;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class StoredVarsRepositoryTest {
    private LocalDateTime localDateTime = LocalDateTime.now();
    private long storedVarsId;

    private static final int MAX_DAYS = 7;
    private static final long min = 1;
    private static final long max = Integer.MAX_VALUE;

    private final long cidToDelete = RandomUtils.nextLong(min, max);
    private final long cidToStay = RandomUtils.nextLong(min, max);

    @Autowired
    private StoredVarsSteps storedVarsSteps;

    @Autowired
    private TestStoredVarsRepository repoUnderTest;

    @Before
    public void before() {
        storedVarsSteps
                .createStoredVar(cidToDelete, localDateTime.minusDays(MAX_DAYS));
        storedVarsId = storedVarsSteps
                .createStoredVar(cidToStay, localDateTime.plusDays(MAX_DAYS));
    }

    @After
    public void after() {
        storedVarsSteps.clearAllStoredVars();
    }

    @Test
    public void deleteStoredVarsOlderThan_DeleteOutdatedFine() {
        int deletedNum = repoUnderTest.deleteStoredVarsOlderThan(localDateTime);
        assertThat("удалена только одна запись", deletedNum, equalTo(1));
    }

    @Test
    public void deleteStoredVarsOlderThan_StayRecentFine() {
        repoUnderTest.deleteStoredVarsOlderThan(localDateTime);
        List<Long> selectedCgiParams = storedVarsSteps.getAllStoredVars();
        assertThat("запись в будущем осталась", selectedCgiParams,
                contains(equalTo(storedVarsId)));
    }
}
