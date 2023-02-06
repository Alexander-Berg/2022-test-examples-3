package ru.yandex.market.replenishment.autoorder.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.exception.NotFoundException;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.GeneratedData;
import ru.yandex.market.replenishment.autoorder.repository.postgres.GeneratedDataRepository;
import ru.yandex.market.replenishment.autoorder.service.datagenerator.DataGenerator;
import ru.yandex.market.replenishment.autoorder.service.datagenerator.GenerationDataService;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class GenerationDataServiceTest extends FunctionalTest {
    @Autowired
    private GeneratedDataRepository generatedDataRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private GenerationDataService generationDataService;

    @Before
    public void setup() {
        setTestTime(LocalDateTime.of(2020, 9, 6, 12, 0));

        generationDataService = new GenerationDataService(
            generatedDataRepository,
            900,
            transactionTemplate,
            timeService,
            Executors.newFixedThreadPool(1)
        );
    }

    @After
    public void tearDown() throws InterruptedException {
        generationDataService.stop();
    }

    @Test
    @DbUnitDataSet(before = "GenerationDataServiceTest.before.csv")
    public void generationDataSimpleTest() throws InterruptedException {
        String name = "test_data_name";
        byte[] data = {1, 2, 3};

        Lock locker = new ReentrantLock();
        locker.lock();

        long id = generationDataService.start(new DataGenerator() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public byte[] generate() {
                locker.lock();
                log.info("Generation data...");
                locker.unlock();
                return data;
            }
        });

        GeneratedData generatedDataBefore = generationDataService.extractData(id);
        assertEquals(generatedDataBefore.getName(), name);
        assertNull(generatedDataBefore.getData());

        locker.unlock();

        generationDataService.stop();
        GeneratedData generatedDataAfter = generationDataService.extractData(id);
        assertEquals(generatedDataAfter.getName(), name);
        assertArrayEquals(generatedDataAfter.getData(), data);
    }

    @Test
    @DbUnitDataSet(before = "GenerationDataServiceTest.before.csv")
    public void generationDataWithException() throws InterruptedException {
        String name = "test_data_name";

        long id = generationDataService.start(new DataGenerator() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public byte[] generate() throws IOException {
                throw new IOException("test IO exception");
            }
        });

        generationDataService.stop();

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            generationDataService.extractData(id));
        assertEquals(exception.getMessage(), "Невозможно найти рабочую книгу для запроса №" + id);
    }

    @Test
    @DbUnitDataSet(before = "GenerationDataServiceTest.before.csv")
    public void extractDataWithIncorrectId() {
        long id = 100500;
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> generationDataService.extractData(id));
        assertEquals(exception.getMessage(), "Невозможно найти рабочую книгу для запроса №" + id);
    }

    @Test
    @DbUnitDataSet(before = "GenerationDataServiceTest.before.csv")
    public void deleteOldGeneratedDataTest() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                generationDataService.deleteOldData();
            }
        });
        List<Long> actual = generatedDataRepository.findAll().stream()
            .map(GeneratedData::getId).sorted().collect(Collectors.toList());
        List<Long> expected = Arrays.asList(103L, 104L, 105L);
        assertEquals(actual, expected);
    }
}
