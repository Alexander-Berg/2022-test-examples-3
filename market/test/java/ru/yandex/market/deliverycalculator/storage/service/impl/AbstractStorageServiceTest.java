package ru.yandex.market.deliverycalculator.storage.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deliverycalculator.storage.util.StorageUtils;

class AbstractStorageServiceTest {

    private TestStorageService testStorageService;
    private EntityManager entityManager;

    @BeforeEach
    void init() {
        entityManager = Mockito.mock(EntityManager.class);
        testStorageService = new TestStorageService(null);
    }

    @Test
    void doInEntityManagerPartitioning() {
        BiFunction<EntityManager, List<Integer>, List<Object>> function =
                new BiFunction<>() {
                    @Override
                    public List<Object> apply(EntityManager entityManager, List<Integer> integers) {
                        return new ArrayList<>();
                    }
                };
        BiFunction<EntityManager, List<Integer>, List<Object>> functionSpy = Mockito.spy(function);
        testStorageService.doInEntityManagerPartitioning(range(0, 40000), functionSpy);

        Mockito.verify(functionSpy).apply(entityManager, range(0, StorageUtils.BATCH_SIZE_PARAM));
        Mockito.verify(functionSpy).apply(entityManager, range(StorageUtils.BATCH_SIZE_PARAM, 20000));
    }


    @Test
    void doInEntityManagerPartitioningCount() {
        BiFunction<EntityManager, List<Integer>, Integer> function =
                new BiFunction<>() {
                    @Override
                    public Integer apply(EntityManager entityManager, List<Integer> integers) {
                        return 10;
                    }
                };
        BiFunction<EntityManager, List<Integer>, Integer> functionSpy = Mockito.spy(function);
        int res = testStorageService.doInEntityManagerPartitioningCount(range(0, 20000), functionSpy);
        Assert.assertEquals(20, res);

        Mockito.verify(functionSpy).apply(entityManager, range(0, StorageUtils.BATCH_SIZE_PARAM));
        Mockito.verify(functionSpy).apply(entityManager, range(StorageUtils.BATCH_SIZE_PARAM, 20000));
    }

    private class TestStorageService extends AbstractStorageService {
        TestStorageService(TransactionTemplate transactionTemplate) {
            super(transactionTemplate);
        }

        @Override
        <T> T doInEntityManager(Function<? super EntityManager, T> action) {
            return action.apply(entityManager);
        }
    }

    private static List<Integer> range(int from, int to) {
        return IntStream.range(from, to).boxed().collect(Collectors.toList());
    }
}
