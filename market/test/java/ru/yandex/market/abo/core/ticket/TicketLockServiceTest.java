package ru.yandex.market.abo.core.ticket;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.ticket.repository.TicketRepo;
import ru.yandex.market.abo.gen.model.GenId;
import ru.yandex.market.abo.util.ErrorMessageException;

/**
 * @author komarovns
 * @date 22.04.19
 */
class TicketLockServiceTest extends AbstractCoreHierarchyTest {

    @Autowired
    TicketLockService ticketLockService;
    @Autowired
    @Qualifier("jpaPgTransactionManager")
    PlatformTransactionManager transactionManager;
    @Autowired
    TicketRepo ticketRepo;
    @Autowired
    ProblemFailureReasonService failureReasonService;
    /**
     * Запускает два треда, каждый в отдельной транзакции заходит в doInLock(), проверяет что 1 из них отвалился
     */
    @Test
    void testDoInLock() {
        long ticketId = createTicket(-1, GenId.DELIVERY_TODAY);
        entityManager.flush();
        jdbcTemplate.update("COMMIT");
        var transactionTemplate = new TransactionTemplate(
                transactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW)
        );
        var countDownLatch = new CountDownLatch(2);
        var errorCounter = new AtomicInteger();
        Runnable task = () -> {
            try {
                transactionTemplate.executeWithoutResult(__ -> {
                    try {
                        ticketLockService.doInLock(ticketId, () -> {
                            try {
                                countDownLatch.countDown();
                                countDownLatch.await();
                                return null;
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (ErrorMessageException e) {
                        countDownLatch.countDown();
                        errorCounter.incrementAndGet();
                    }
                });
            } catch (Exception ignored) {
                // Спринг знает что упала транзакция, и кидает свой exception
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        var tasks = Stream.of(task, task)
                .map(t -> CompletableFuture.runAsync(t, executor))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(tasks).join();

        Assertions.assertEquals(1, errorCounter.get());
        deleteTicket(ticketId);
    }

    @Test
    void testRecursiveDoInLock() {
        long ticketId = createTicket(-1, GenId.DELIVERY_TODAY);
        ticketLockService.doInLock(ticketId,
                () -> ticketLockService.doInLock(ticketId, () -> null)
        );
    }

    @Test
    @Disabled("uncomment when need to delete tickets by some condition")
    void deleteTicketsCompletely() {
        jdbcTemplate.queryForList("select id from hypothesis where gen_id in (97, 99) " +
                "and date_trunc('day', create_time) = '2020-02-24'", Long.class).forEach(hypId -> {
            System.out.println("deleting all records for hypId " + hypId);
            List<Problem> problems = problemService.loadProblemsByTicketId(hypId);
            System.out.println("found " + problems.size() + " problems for hypId " + hypId);
            problems.forEach(p -> failureReasonService.delete(p.getId()));
            problemService.delete(problems);
            deleteTicket(hypId);
        });
    }

    private void deleteTicket(long ticketId) {
        var ticket = ticketRepo.findByIdOrNull(ticketId);
        if (ticket != null) {
            var hypIds = List.of(ticketId);
            ticketService.deleteTicketsById(hypIds);
            offerDbService.deleteOffers(List.of(ticket.getOfferId()));
            tagService.deleteTicketTagsById(List.of(ticket.getModificationTagId()));
        }
        hypothesisService.deleteHypothesisesById(List.of(ticketId));
        entityManager.flush();
        jdbcTemplate.update("COMMIT");
    }
}
