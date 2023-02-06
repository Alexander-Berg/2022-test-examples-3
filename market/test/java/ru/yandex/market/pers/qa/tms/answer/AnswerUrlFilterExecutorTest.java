package ru.yandex.market.pers.qa.tms.answer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.common.util.db.DbUtil;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author varvara
 * 11.12.2018
 */
public class AnswerUrlFilterExecutorTest extends PersQaTmsTest {

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private AnswerUrlFilterExecutor answerUrlFilterExecutor;

    @Test
    void testAnswerUrlFilter() {
        // fill this map by creating answers
        Map<Long, ModState> expectedResults = new HashMap<>();

        createAndFillAnswersFromTestTexts("/data/text_with_urls.txt", expectedResults);

        final List<ModState> skippedModStates = Arrays.stream(ModState.values())
            .filter(it -> it != ModState.AUTO_FILTER_PASSED).collect(Collectors.toList());

        // every answers in mod_state != AUTO_FILTER_PASSED should be skipped by job
        createAndFillAnswersByModStates(skippedModStates, expectedResults);

        answerUrlFilterExecutor.answerUrlFilter();

        final Map<Long, ModState> actualResult = getModStatesByIds(expectedResults.keySet());
        expectedResults.forEach((id, modState) ->
            assertEquals(modState, actualResult.get(id)));
    }

    private Map<Long, ModState> getModStatesByIds(Set<Long> ids) {
        final Map<Long, ModState> actualResult = new HashMap<>();
        DbUtil.queryInList(ids,
            (sqlBindList, list) ->
                jdbcTemplate.query(
                    "SELECT a.id as id, a.mod_state as mod_state\n" +
                        "FROM qa.answer a\n" +
                        "WHERE id in (" + sqlBindList + ")\n",
                    rs -> {
                        while (rs.next()) {
                            actualResult.put(
                                rs.getLong("id"),
                                ModState.valueOf(rs.getInt("mod_state"))
                            );
                        }
                        return null;
                    },
                    list.toArray()));
        return actualResult;
    }

    private void createAndFillAnswersFromTestTexts(String filename, Map<Long, ModState> results) {
        InputStream inputStream = this.getClass().getResourceAsStream(filename);
        Scanner s = new Scanner(inputStream);

        int n = 0;
        while (s.hasNextLine()) {
            n++;
            int withUrl = s.nextInt();
            String text = s.nextLine().substring(1);

            final Question question = questionService.createModelQuestion(n, UUID.randomUUID().toString(), n + 1);
            final Answer answer = answerService.createAnswer(n + 2, text, question.getId());
            answerService.forceUpdateModState(answer.getId(), ModState.AUTO_FILTER_PASSED);
            results.put(answer.getId(), withUrl == 1 ? ModState.TOLOKA_UNKNOWN : ModState.CONFIRMED);

            // deleted answers should be skipped by job
            final Question deletedQuestion = questionService.createModelQuestion(n, UUID.randomUUID().toString(), n + 1);
            final Answer deletedAnswer = answerService.createAnswer(n + 2, text, deletedQuestion.getId());
            answerService.forceUpdateModState(deletedAnswer.getId(), ModState.AUTO_FILTER_PASSED);
            jdbcTemplate.update("update qa.answer set state = 1 where id = ?", deletedAnswer.getId());
            results.put(deletedAnswer.getId(), ModState.AUTO_FILTER_PASSED);
        }
        s.close();
    }

    private void createAndFillAnswersByModStates(List<ModState> modStates, Map<Long, ModState> results) {
        int n = 0;
        for (ModState modState : modStates) {
            n++;
            final Question question = questionService.createModelQuestion(n, UUID.randomUUID().toString(), n + 1);
            final Answer answer = answerService.createAnswer(n + 2, UUID.randomUUID().toString(), question.getId());
            answerService.forceUpdateModState(answer.getId(), modState);
            results.put(answer.getId(), modState);
        }
    }

}
