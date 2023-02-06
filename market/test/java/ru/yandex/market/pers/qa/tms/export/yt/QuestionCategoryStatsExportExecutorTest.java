package ru.yandex.market.pers.qa.tms.export.yt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.StringUtils;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.QuestionCategory;
import ru.yandex.market.pers.qa.model.QuestionCategoryRegexpType;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.tms.export.yt.model.CategorizedQuestion;
import ru.yandex.market.pers.qa.tms.export.yt.model.QuestionCategoryStat;
import ru.yandex.market.pers.qa.tms.export.yt.model.UncategorizedQuestion;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientProvider;
import ru.yandex.market.pers.yt.YtClusterType;

import static org.mockito.ArgumentMatchers.eq;

class QuestionCategoryStatsExportExecutorTest extends PersQaTmsTest {

    @Autowired
    QuestionCategoryStatsExportExecutor executor;

    @Autowired
    YtClientProvider ytClientProvider;

    @Autowired
    QuestionService questionService;

    @Autowired
    AnswerService answerService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<List<?>> entitiesToUploadCaptor;

    @Captor
    private ArgumentCaptor<YPath> createTableCaptor;

    @Captor
    private ArgumentCaptor<YPath> createLinkCaptor;

    @Captor
    private ArgumentCaptor<YPath> currentLinkCaptor;

    @Captor
    private ArgumentCaptor<YPath> appendCaptor;

    private YtClient ytClient;

    @BeforeEach
    void init() {
        ytClient = ytClientProvider.getClient(YtClusterType.HAHN);
    }

    @Test
    void export() {
        List<Long> questionsWithoutCategories = IntStream.range(0, 10)
            .mapToObj(i -> {
                return createQuestion(100 + i, i).getId();
            })
            .collect(Collectors.toList());

        List<Long> questionsWithCategories = IntStream.range(0, 12)
            .mapToObj(idx -> {
                Question question = createQuestion(1000 + idx, idx);

                // add category
                jdbcTemplate.update("insert into qa.question_category(id, category_id, regexp, regexp_type) values (?, ?, 'test', 0)",
                    question.getId(),
                    idx);

                return question.getId();
            })
            .collect(Collectors.toList());

        executor.exportCategoryStats();

        Mockito.verify(ytClient).createTable(createTableCaptor.capture(), eq(QuestionCategoryStat.tableSchema()));
        Mockito.verify(ytClient).append(appendCaptor.capture(), entitiesToUploadCaptor.capture());
        Mockito.verify(ytClient).updateLinks(createLinkCaptor.capture(), currentLinkCaptor.capture());

        Assertions.assertEquals(QuestionCategory.values().length + 2, entitiesToUploadCaptor.getValue().size());
        Assertions.assertEquals(createTableCaptor.getValue(), appendCaptor.getValue());
        Assertions.assertEquals(createLinkCaptor.getValue(), appendCaptor.getValue());
    }

    @Test
    void exportQuestionsWithoutCategories() {
        List<Long> questionsWithoutCategories = IntStream.range(0, 10)
            .mapToObj(i -> {
                return createQuestion(100 + i, i).getId();
            })
            .collect(Collectors.toList());

        List<Long> questionsWithCategories = IntStream.range(0, 12)
            .mapToObj(i -> {
                Question question = createQuestion(1000 + i, i);

                // add category
                jdbcTemplate.update("insert into qa.question_category(id, category_id, regexp, regexp_type) values (?, 1, 'test', 0)",
                    question.getId());

                return question.getId();
            })
            .collect(Collectors.toList());

        executor.dumpQuestionsWithoutCategories();

        Mockito.verify(ytClient).createTable(createTableCaptor.capture(), eq(UncategorizedQuestion.tableSchema()));
        Mockito.verify(ytClient).append(appendCaptor.capture(), entitiesToUploadCaptor.capture());
        Mockito.verify(ytClient).updateLinks(createLinkCaptor.capture(), currentLinkCaptor.capture());

        Assertions.assertEquals(questionsWithoutCategories.size(), entitiesToUploadCaptor.getValue().size());
        Assertions.assertEquals(createTableCaptor.getValue(), appendCaptor.getValue());
        Assertions.assertEquals(createLinkCaptor.getValue(), appendCaptor.getValue());
    }

    @Test
    void exportQuestionsWithCategories() {
        List<Long> questionsWithoutCategories = IntStream.range(0, 10)
            .mapToObj(i -> createQuestion(100 + i, i).getId())
            .collect(Collectors.toList());

        List<Long> questionsWithCategories = createQuestionsWithCategoryResult(1, Arrays.asList(QuestionCategory.FUNCTION, QuestionCategory.COLOR), 12);

        List<Long> questionsWithCategoriesAndStopWords =
            createQuestionsWithCategoryResult(2, Collections.singletonList(QuestionCategory.FUNCTION), Collections.singletonList(QuestionCategory.FUNCTION), 5);

        List<Long> questionsWithTooLongText = createQuestionsWithCategoryResult(3, StringUtils.multiply("t", 301, ""), Collections.singletonList(QuestionCategory.FUNCTION), 5);

        List<Long> questionsWithCategoryAndCommonStopWords = createQuestionsWithCategoryResult(
            4, StringUtils.multiply("t", 150, ""), Arrays.asList(QuestionCategory.FUNCTION, QuestionCategory.USER_EXPERIENCE_STOPWORDS), 5);

        executor.dumpQuestionsWithCategories();

        Mockito.verify(ytClient).createTable(createTableCaptor.capture(), eq(CategorizedQuestion.tableSchema()));
        Mockito.verify(ytClient).append(appendCaptor.capture(), entitiesToUploadCaptor.capture());
        Mockito.verify(ytClient).updateLinks(createLinkCaptor.capture(), currentLinkCaptor.capture());

        Assertions.assertEquals(questionsWithCategoryAndCommonStopWords.size() //only user experience category should be uploaded
                                    + questionsWithCategories.size() * 2, //has 2 categories for each question
                                entitiesToUploadCaptor.getValue().size());
        Assertions.assertEquals(createTableCaptor.getValue(), appendCaptor.getValue());
        Assertions.assertEquals(createLinkCaptor.getValue(), appendCaptor.getValue());
    }

    @NotNull
    private List<Long> createQuestionsWithCategoryResult(long userId, List<QuestionCategory> categoriesToCreate, int questionCount) {
        return createQuestionsWithCategoryResult(userId, categoriesToCreate, Collections.emptyList(), questionCount);
    }

    @NotNull
    private List<Long> createQuestionsWithCategoryResult(long userId, String questionText, List<QuestionCategory> categoriesToCreate, int questionCount) {
        return createQuestionsWithCategoryResult(userId, questionText, categoriesToCreate, Collections.emptyList(), questionCount);
    }

    @NotNull
    private List<Long> createQuestionsWithCategoryResult(long userId, List<QuestionCategory> categoriesToCreate, List<QuestionCategory> stopwordsToCreate, int questionCount) {
        return createQuestionsWithCategoryResult(userId, "test text", categoriesToCreate, stopwordsToCreate, questionCount);
    }

    @NotNull
    private List<Long> createQuestionsWithCategoryResult(long userId, String questionText, List<QuestionCategory> categoriesToCreate, List<QuestionCategory> stopwordsToCreate, int questionCount) {
        return IntStream.range(0, questionCount)
                .mapToObj(i -> {
                    Question question = createQuestion(4000 + i, userId, questionText);

                    categoriesToCreate.forEach(questionCategory -> {
                        insertCategoryResult(question.getId(), questionCategory, QuestionCategoryRegexpType.POSITIVE);
                    });
                    stopwordsToCreate.forEach(questionCategory -> {
                        insertCategoryResult(question.getId(), questionCategory, QuestionCategoryRegexpType.NEGATIVE);
                    });

                    return question.getId();
                })
                .collect(Collectors.toList());
    }

    private void insertCategoryResult(long questionId, QuestionCategory category, QuestionCategoryRegexpType type) {
        jdbcTemplate.update("insert into qa.question_category(id, category_id, regexp, regexp_type) values " +
                                "(?, ?, ?, ?)", questionId, category.getId(), UUID.randomUUID().toString(), type.getValue());
    }

    @NotNull
    private Question createQuestion(long modelId, long userId) {
        return createQuestion(modelId, userId, "text test " + userId);
    }

    @NotNull
    private Question createQuestion(long modelId, long userId, String questionText) {
        Question question = questionService.createModelQuestion(userId, questionText, modelId);

        // create at least 2 answers
        answerService.createAnswer(Answer.buildBasicAnswer(userId, "test text" + userId, question.getId()), null);
        answerService.createAnswer(Answer.buildBasicAnswer(userId, "test text 2 " + userId, question.getId()), null);
        return question;
    }

}
