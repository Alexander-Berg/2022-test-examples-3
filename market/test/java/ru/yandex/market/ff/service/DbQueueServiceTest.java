package ru.yandex.market.ff.service;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.model.dbqueue.DbQueueState;
import ru.yandex.market.ff.model.enums.DbQueueType;
import ru.yandex.market.ff.repository.DbQueueRepository;
import ru.yandex.market.ff.service.implementation.DbQueueServiceImpl;

/**
 * Unit-тесты для {@link DbQueueService}.
 */
public class DbQueueServiceTest {

    private DbQueueService dbQueueService;
    private DbQueueRepository dbQueueRepository;
    private SoftAssertions assertions;

    @BeforeEach
    public void init() {
        dbQueueRepository = Mockito.mock(DbQueueRepository.class);
        dbQueueService = new DbQueueServiceImpl(dbQueueRepository);
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @Test
    public void getStateByQueueWorksCorrect() {
        Map<DbQueueType, DbQueueState> expectedResult = new HashMap<>();
        Mockito.when(dbQueueRepository.getStateByQueue())
            .thenReturn(expectedResult);
        Map<DbQueueType, DbQueueState> result = dbQueueService.getStateByQueue();
        assertions.assertThat(result).isEqualTo(expectedResult);
        Mockito.verify(dbQueueRepository).getStateByQueue();
    }
}
