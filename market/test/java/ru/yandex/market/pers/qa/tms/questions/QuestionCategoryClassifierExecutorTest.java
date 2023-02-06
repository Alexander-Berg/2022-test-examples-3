package ru.yandex.market.pers.qa.tms.questions;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.QuestionCategory;
import ru.yandex.market.pers.qa.model.QuestionCategoryRegexpType;
import ru.yandex.market.pers.qa.service.QuestionService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

/**
 * @author vvolokh
 * 21.12.2018
 */
public class QuestionCategoryClassifierExecutorTest extends PersQaTmsTest {
    @Autowired
    private QuestionService questionService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private QuestionCategoryClassifierExecutor questionCategoryClassifierExecutor;

    @Test
    @Transactional
    @Rollback
    public void testCategories() {
        //given
        jdbcTemplate.update("DELETE FROM qa.CATEGORY_QUESTION_REGEXP");
        jdbcTemplate.update("insert into qa.CATEGORY_QUESTION_REGEXP(id, regexp, category_id) values(?, ?, ?)", 1,
            ".*?[^а-яА-Я]тестэкран.*", QuestionCategory.FUNCTION.getId());
        jdbcTemplate.update("insert into qa.CATEGORY_QUESTION_REGEXP(id, regexp, category_id) values(?, ?, ?)", 2,
            ".*?[^а-яА-Я]тестоворозов.*", QuestionCategory.COLOR.getId());
        Question question1 = createConfirmedQuestion("что с тестэкраном?");
        Question question2 = createConfirmedQuestion("что с тестоворозовым цветом?");
        Question question3 = createConfirmedQuestion("что с редиской?");
        Question question4 = createConfirmedQuestion("что с тестоворозовым цветом тестэкрана?");
        //when
        questionCategoryClassifierExecutor.process();

        //then
        assertCategories(Collections.singletonList(QuestionCategory.FUNCTION), question1);
        assertCategories(Collections.singletonList(QuestionCategory.COLOR), question2);
        assertCategories(Collections.emptyList(), question3);
        assertCategories(Arrays.asList(QuestionCategory.COLOR, QuestionCategory.FUNCTION), question4);
    }

    @Test
    public void testWithRealRegexps() throws Exception {
        Map<Question, List<QuestionCategory>> expected = getQuestionsFromJson();

        questionCategoryClassifierExecutor.process();

        expected.forEach(((question, questionCategories) -> assertCategories(questionCategories, question)));
    }

    @NotNull
    private Map<Question, List<QuestionCategory>> getQuestionsFromJson() throws IOException, JSONException {
        Map<Question, List<QuestionCategory>> expected = new HashMap<>();
        InputStream inputStream = this.getClass().getResourceAsStream("/data/question_category_mapping.json");
        String jsonContents = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        JSONArray jsonArray = new JSONArray(jsonContents);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            String questionText = object.optString("text");
            JSONArray categoriesJson = object.optJSONArray("categories");
            List<QuestionCategory> categories = new ArrayList<>();
            for (int j = 0; j < categoriesJson.length(); j++) {
                categories.add(QuestionCategory.valueOf(categoriesJson.optString(j)));
            }
            expected.put(createConfirmedQuestion(questionText), categories);
        }
        return expected;
    }

    private Question createConfirmedQuestion(String text) {
        Question question = questionService.createModelQuestion(1, text, 1);
        questionService.forceUpdateModState(question.getId(), ModState.CONFIRMED);
        return question;
    }

    private void assertCategories(List<QuestionCategory> categories, Question question) {
        List<Integer> list =
            jdbcTemplate.queryForList("SELECT DISTINCT category_id \n" +
                                      "FROM qa.question_category qc \n" +
                                      "WHERE qc.id=? \n" +
                                      "  AND qc.regexp_type = " + QuestionCategoryRegexpType.POSITIVE.getValue() + "\n" +
                                      "  AND qc.category_id NOT IN (\n" +
                                      "     SELECT DISTINCT category_id \n" +
                                      "     FROM qa.question_category \n" +
                                      "     WHERE id=? \n" +
                                      "         AND regexp_type = + " + QuestionCategoryRegexpType.NEGATIVE.getValue() + ")",
                                      Integer.class,
                                      question.getId(), question.getId());
        if (list.contains(QuestionCategory.USER_EXPERIENCE_STOPWORDS.getId())) {
            list = Arrays.asList(QuestionCategory.USER_EXPERIENCE_STOPWORDS.getId());
        }
        list.sort(Integer::compareTo);

        List<Integer> expected =
            categories.stream().map(QuestionCategory::getId).sorted(Integer::compareTo).collect(Collectors.toList());

        assertIterableEquals(expected, list, String.format(
            "Categories of question '%s' didn't match the list %s actual list was %s",
            question.getText(), expected.toString(), list.toString()));
    }

    private void assertCategories(List<QuestionCategory> categories, List<Question> questions) {
        questions.forEach(question -> assertCategories(categories, question));
    }
}
