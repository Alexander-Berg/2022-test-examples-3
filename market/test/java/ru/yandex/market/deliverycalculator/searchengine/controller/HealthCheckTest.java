package ru.yandex.market.deliverycalculator.searchengine.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deliverycalculator.searchengine.FunctionalTest;
import ru.yandex.market.deliverycalculator.searchengine.service.FeedParserWorkflowService;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.RegularCourierGeneration;
import ru.yandex.market.deliverycalculator.storage.util.StorageUtils;
import ru.yandex.misc.thread.ThreadUtils;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HealthCheckTest extends FunctionalTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private FeedParserWorkflowService feedParserWorkflowService;

    private static final String PING_OK = "0;Ok";
    private static final String PING_ERROR_GENERATION = "2;Delivery calculator generation is not up to date";

    /**
     * Тест проверяет, что ручка {@code /ping} не ломается из-за отставания поколений после первого старта КД,
     * но ручка {@code /monitorGenerations} успешно обнаруживает проблему.
     */
    @Test
    void testPingDoesntFailAfterStart() throws Exception {
        assertPingOk();

        // создаем хорошее поколение и ждем его успешной загрузки
        StorageUtils.doInEntityManager(
                transactionTemplate,
                em -> {
                    createTestGeneration(em, 1, Instant.now().minus(30, ChronoUnit.MINUTES), true);
                }
        );

        // загрузка новых поколений отключена, ждем, когда мониторинг заметить проблему
        waitUntil(this::monitoringFailed, 15, ChronoUnit.SECONDS);

        // иммитируем запуск ImportNewGenerationsTask
        feedParserWorkflowService.importGenerations();
        // иммитируем запуск ActiveGenerationUpdaterTask
        feedParserWorkflowService.updateActiveGenerationId();

        // ждем когда все наладится
        waitUntil(this::monitoringOk, 15, ChronoUnit.SECONDS);
        assertPingOk();

        //создаем поколение, которое не сможет загрузиться
        StorageUtils.doInEntityManager(
                transactionTemplate,
                em -> {
                    createTestGeneration(em, 2, Instant.now().minus(25, ChronoUnit.MINUTES), false);
                }
        );

        // ждем, что мониторинг это заметит
        waitUntil(this::monitoringFailed, 15, ChronoUnit.SECONDS);

        // убеждаемся, что пинг продолжает отвечать 0;Ok
        assertPingOk();
    }

    private void assertPingOk() throws Exception {
        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string(PING_OK));
    }

    private boolean monitoringOk() {
        try {
            mockMvc.perform(get("/monitorGenerations"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(PING_OK));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean monitoringFailed() {
        try {
            mockMvc.perform(get("/monitorGenerations"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(PING_ERROR_GENERATION));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static void waitUntil(BooleanSupplier test, long timeout, TemporalUnit temporalUnit) {
        Instant startTime = Instant.now();
        while (startTime.plus(timeout, temporalUnit).isAfter(Instant.now())) {
            if (test.getAsBoolean()) {
                return;
            }

            ThreadUtils.doSleep(100, TimeUnit.MILLISECONDS);
        }

        fail("Timed out.");
    }

    private static void createTestGeneration(
            EntityManager entityManager,
            long generationId,
            Instant generationTime,
            boolean validTariff
    ) {
        Generation generation = new Generation(generationId, generationId);
        RegularCourierGeneration courierGeneration = new RegularCourierGeneration();
        courierGeneration.setGeneration(generation);
        courierGeneration.setDeleted(false);
        courierGeneration.setShopId(123L);
        courierGeneration.setBucketsUrl("http://test.ru");
        courierGeneration.setTariffInfo(validTariff ? "<tariff/>" : "сломано, приходите позже");

        Set<RegularCourierGeneration> courierGenerations = new HashSet<>();
        courierGenerations.add(courierGeneration);
        generation.setRegularCourierGenerations(courierGenerations);

        entityManager.persist(generation);

        Query query = entityManager.createQuery("update Generation set time = :time where id = :id");
        query.setParameter("time", generationTime);
        query.setParameter("id", generationId);
        query.executeUpdate();
    }

}
