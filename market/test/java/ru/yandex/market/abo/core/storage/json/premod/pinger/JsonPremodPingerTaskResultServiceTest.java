package ru.yandex.market.abo.core.storage.json.premod.pinger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.pinger.premod.model.PremodPingerTaskResult;
import ru.yandex.market.abo.core.pinger.util.PremodPingerProblem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author valeriashanti
 * @date 13.11.2020
 */
class JsonPremodPingerTaskResultServiceTest extends EmptyTest {

    private static final int RESULTS_BY_PROBLEM_LIMIT = 10;

    private static final long TICKET_ID = 123;
    private static final long TICKET_ID_2 = 321;
    private static final long TICKET_ID_3 = 231;
    private static final long TICKET_ID_4 = 111;

    private static final LocalDateTime MAX_FINISH_TIME = LocalDateTime.parse("2020-12-04T02:09:00");


    @Autowired
    private JsonPremodPingerTaskResultService taskResultService;
    @Autowired
    private JsonPremodPingerTaskResultRepo taskResultRepo;

    @Test
    public void serializationTest() {
        var problem = PremodPingerProblem.DESKTOP_PRICE;
        var results = createPingerResultsList(problem, 1);
        saveResults(TICKET_ID, Map.of(problem, results));

        var resultFromDB = taskResultService.loadResults(TICKET_ID, problem.getJsonEntityType());
        assertEquals(JsonPremodPingerTaskResultService.sortAndLimit(results), resultFromDB);
    }

    @Test
    public void sortAndLimitResults() {
        Map<PremodPingerProblem, List<PremodPingerTaskResult>> resultMap = new HashMap<>();
        var result = createPingerResultsList(
                PremodPingerProblem.MOBILE_PRICE, (int) TICKET_ID_4, MAX_FINISH_TIME.minusYears(10)
        );
        result.add(createResult(PremodPingerProblem.MOBILE_PRICE, (int) TICKET_ID_4, MAX_FINISH_TIME));
        IntStream.range(0, RESULTS_BY_PROBLEM_LIMIT)
                .forEach(index -> {
                    result.add(createResult(PremodPingerProblem.MOBILE_PRICE, (int) TICKET_ID_4,
                            MAX_FINISH_TIME.minusYears(1)));
                });
        resultMap.put(PremodPingerProblem.MOBILE_PRICE, result);

        saveResults(TICKET_ID_4, resultMap);
        var resultsFromDb = taskResultService.loadResults(TICKET_ID_4);

        assertFalse(resultsFromDb.isEmpty());
        assertNotEquals(resultsFromDb, resultMap);

        var mobileResults = resultsFromDb.get(PremodPingerProblem.MOBILE_PRICE);
        assertEquals(MAX_FINISH_TIME, mobileResults.get(0).getFinishedTime());
        assertTrue(mobileResults.size() <= RESULTS_BY_PROBLEM_LIMIT);
    }

    @Test
    public void mergeValuesOnSameTicketId() {
        var firstResults = createPingerResultsMap(1);
        var secondResults = createPingerResultsMap(343);

        saveResults(TICKET_ID_2, firstResults);
        var firstResultsFromDB = taskResultService.loadResults(
                TICKET_ID_2, PremodPingerProblem.DESKTOP_AVAILABILITY.getJsonEntityType());
        saveResults(TICKET_ID_2, secondResults);
        var secondResultsFromDB = taskResultService.loadResults(TICKET_ID_2);
        assertNotEquals(firstResultsFromDB, secondResultsFromDB);
    }

    @Test
    void saveEmptyMapTest() {
        Map<PremodPingerProblem, List<PremodPingerTaskResult>> resultsMap = new HashMap<>();
        saveResults(TICKET_ID_3, resultsMap);
        assertTrue(taskResultService.loadResults(TICKET_ID_3).isEmpty());
        assertEquals(0, taskResultRepo.count());
    }

    private void saveResults(long ticketId, Map<PremodPingerProblem, List<PremodPingerTaskResult>> results) {
        taskResultService.saveIfNeeded(ticketId, results);
        flushAndClear();
    }

    private static Map<PremodPingerProblem, List<PremodPingerTaskResult>> createPingerResultsMap(int ticketId) {
        Map<PremodPingerProblem, List<PremodPingerTaskResult>> resultMap = new HashMap<>();
        var resultList = createPingerResultsList(PremodPingerProblem.MOBILE_PRICE, ticketId);
        resultMap.put(PremodPingerProblem.MOBILE_PRICE, resultList);
        resultMap.put(PremodPingerProblem.DESKTOP_AVAILABILITY, resultList);
        resultMap.put(PremodPingerProblem.DESKTOP_PRICE, resultList);
        return resultMap;
    }

    private static List<PremodPingerTaskResult> createPingerResultsList(PremodPingerProblem problem, int ticketId) {
        return createPingerResultsList(problem, ticketId, null);
    }

    private static List<PremodPingerTaskResult> createPingerResultsList(
            PremodPingerProblem problem, int ticketId, LocalDateTime finishTime
    ) {
        List<PremodPingerTaskResult> resultList = new ArrayList<>();
        resultList.add(createResult(problem, ticketId, finishTime));
        return resultList;
    }

    private static PremodPingerTaskResult createResult(
            PremodPingerProblem problem, int ticketId, LocalDateTime finishTime
    ) {
        return PremodPingerTaskResult.builder()
                .ticketId(ticketId)
                .premodPingerProblem(problem)
                .finishedTime(finishTime == null ? MAX_FINISH_TIME : finishTime)
                .build();
    }
}
