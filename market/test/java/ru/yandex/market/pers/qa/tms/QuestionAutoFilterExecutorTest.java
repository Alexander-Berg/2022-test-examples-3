package ru.yandex.market.pers.qa.tms;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.cleanweb.CleanWebClient;
import ru.yandex.market.cleanweb.CleanWebContent;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.QuestionType;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.mock.AutoFilterServiceTestUtils;
import ru.yandex.market.pers.qa.mock.EntityForFilter;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.QuestionFilter;
import ru.yandex.market.pers.qa.model.UserBanInfo;
import ru.yandex.market.pers.qa.service.ModerationLogService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.UserBanService;
import ru.yandex.market.pers.qa.tms.filter.QuestionAutoFilterExecutor;
import ru.yandex.market.pers.qa.utils.CommonUtils;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;

/**
 * @author korolyov
 * 20.06.18
 */
public class QuestionAutoFilterExecutorTest extends PersQaTmsTest {

    private static final int N = 10;
    private static final long MODERATOR_ID = 100500;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionAutoFilterExecutor questionAutoFilterExecutor;

    @Autowired
    private UserBanService userBanService;

    @Autowired
    private ModerationLogService moderationLogService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CleanWebClient cleanWebClient;

    private void prepareData() {
        List<String> texts = AutoFilterServiceTestUtils.generateTextsForFilteringMod3(N);
        for (int i = 1; i <= N; i++) {
            questionService.createModelQuestion(i, texts.get(i - 1), N - i);
        }
    }

    private List<EntityForFilter> getQuestionWithModStates() {
        return jdbcTemplate.query(
            "SELECT id, user_id, text, mod_state FROM qa.question",
            (rs, rowNum) -> EntityForFilter.valueOf(rs));
    }

    private void checkResults() {
        List<EntityForFilter> questionsForFilter = getQuestionWithModStates();
        Assertions.assertEquals(N, questionsForFilter.size());
        questionsForFilter.forEach(questionForFilter -> {
            ModState modState = AutoFilterServiceTestUtils.modStateByUserIdMod3(questionForFilter);
            Assertions.assertEquals(modState, questionForFilter.modState, questionForFilter.text);
        });
    }

    @Test
    void testCleanWeb() {
        prepareData();

        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        questionAutoFilterExecutor.filter();

        checkResults();
    }

    @Test
    void testRecheckAfterUnknownStatus() {
        questionService.createModelQuestion(1, "text", 1);

        // test falls to recheck
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_UNKNOWN);
        questionAutoFilterExecutor.filter();
        List<EntityForFilter> questionsForFilter = getQuestionWithModStates();
        Assertions.assertEquals(1, questionsForFilter.size());
        Assertions.assertEquals(questionsForFilter.get(0).modState, ModState.AUTO_FILTER_UNKNOWN);
        Assertions.assertEquals(1, getRechecks().size());

        // test removes from recheck when result is ok
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_PASSED);
        questionAutoFilterExecutor.filter();
        questionsForFilter = getQuestionWithModStates();
        Assertions.assertEquals(1, questionsForFilter.size());
        Assertions.assertEquals(questionsForFilter.get(0).modState, ModState.AUTO_FILTER_PASSED);
        Assertions.assertTrue(getRechecks().isEmpty(), "must be empty");
    }

    @Test
    void testCleanWebRequestParameters() {
        Question question = questionService.createModelQuestion(1, "text", 1);
        jdbcTemplate.update(
            "insert into qa.security_data (entity_type, entity_id, ip, user_agent, headers) " +
                "values (?, ?, ?, ?, ?)",
            QaEntityType.QUESTION.getValue(), question.getId(), "ip", "user_agent", SECURITY_DATA_HEADERS
        );
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        questionAutoFilterExecutor.filter();

        ArgumentCaptor<CleanWebContent> cleanWebClientArgumentCaptor = ArgumentCaptor.forClass(CleanWebContent.class);
        verify(cleanWebClient).sendContent(cleanWebClientArgumentCaptor.capture(), anyBoolean());
        CleanWebContent value = cleanWebClientArgumentCaptor.getValue();
        assertEquals(
            Map.of(
                "uid", "1",
                "is_shop", false,
                "is_brand", false,
                "ip", "ip",
                "user_agent", "user_agent",
                CommonUtils.HEADER_ICOOKIE, "icookie"
            ),
            value.getParameters()
        );
    }

    @NotNull
    private List<Long> getRechecks() {
        return jdbcTemplate.queryForList("SELECT id FROM qa.auto_recheck", Long.class);
    }

    @ParameterizedTest
    @MethodSource("allQuestionTypes")
    void testBannedAnswers(QuestionType type) {
        long userId = -123;
        Question question = createQuestion(type,  userId, "test");

        // they are published
        assertModState(question.getId(), ModState.NEW);

        userBanService.ban(UserBanInfo.forever(UserType.UID, String.valueOf(userId), "Some reason", MODERATOR_ID));

        // still published
        assertModState(question.getId(), ModState.NEW);

        // run auto-filter
        questionAutoFilterExecutor.filter();

        // now banned
        assertModState(question.getId(), ModState.AUTO_FILTER_REJECTED);
    }


    @ParameterizedTest
    @MethodSource("allQuestionTypes")
    void testBannedAnswersBanTimeExpired(QuestionType type) {
        long userId = -123;
        Question question = createQuestion(type,  userId, "test");

        userBanService
            .ban(UserBanInfo.limited(
                UserType.UID,
                String.valueOf(userId),
                "Some reason",
                MODERATOR_ID, Instant.now().minus(2, ChronoUnit.MINUTES)
            ));

        // run auto-filter
        questionAutoFilterExecutor.filter();

        // now banned
        assertModState(question.getId(), ModState.AUTO_FILTER_PASSED);
    }

    @ParameterizedTest
    @MethodSource("allQuestionTypes")
    void testBannedAnswersBanTimeGood(QuestionType type) {
        long userId = -123;
        Question question = createQuestion(type,  userId, "test");

        userBanService
            .ban(UserBanInfo.limited(
                UserType.UID,
                String.valueOf(userId),
                "Some reason",
                MODERATOR_ID, Instant.now().plus(2, ChronoUnit.MINUTES)
            ));

        // run auto-filter
        questionAutoFilterExecutor.filter();

        // now banned
        assertModState(question.getId(), ModState.AUTO_FILTER_REJECTED);
    }

    @ParameterizedTest
    @MethodSource("allQuestionTypes")
    void testFilteredByLocalFilter(QuestionType type) {
        Question questionGood = createQuestion(type, "Хочу пойти заготовить лес, подскажите, какая производительность?");
        Question questionBad = createQuestion(type, "Простой вопрос с неожиданным и непредсказуемым подвохом");
        Question questionEvil = createQuestion(type, "Каждая ли дурочка сможет справиться с этой бензопилой?");
        Question questionEvilDead = createQuestion(type, "Как спрятать тело, если за тобой гонятся капибары?");

        // дурочк* filtered by default
        // filters 'жид' inclusions
        jdbcTemplate.update("insert into qa.auto_filter(id, reg_expr) values (-1, 'жид') on conflict DO NOTHING");
        // filters texts with words started with 'прят*'
        jdbcTemplate.update("insert into qa.auto_filter(id, reg_expr) values (-2, '\"(^|.*?[^а-яА-Я])прят.*\"') on conflict DO NOTHING");

        // run auto-filter
        questionAutoFilterExecutor.filter();

        assertModState(questionGood.getId(), ModState.AUTO_FILTER_PASSED);
        assertModState(questionBad.getId(), ModState.AUTO_FILTER_REJECTED);
        assertModState(questionEvil.getId(), ModState.AUTO_FILTER_REJECTED);
        assertModState(questionEvilDead.getId(), ModState.AUTO_FILTER_PASSED);
    }

    @ParameterizedTest
    @MethodSource("allQuestionTypes")
    void testAllBadLocalFilter(QuestionType type) {
        String[] badQuestionTexts = {
            "далбаёбы", "долбоящеры", "далбаящик", "долбачтототамвдалеке",
            "какой *здец", "какой 3.1415здец",
            "дибилизм",
            "дурочка", "дурачок", "дурачились",
        };

        List<Question> questions = Arrays.stream(badQuestionTexts)
            .map((String text) -> createQuestion(type, text))
            .collect(Collectors.toList());

        // run auto-filter
        questionAutoFilterExecutor.filter();

        // check all failed
        questions.forEach(x -> assertModState(x.getId(), ModState.AUTO_FILTER_REJECTED));
    }

    @ParameterizedTest
    @MethodSource("allQuestionTypes")
    void testAllGoodLocalFilter(QuestionType type) {
        // add here false positive fires that should never happen again
        String[] badQuestionTexts = {
            "ох и вкусно же я поел",
        };

        List<Question> questions = Arrays.stream(badQuestionTexts)
            .map((String text) -> createQuestion(type, text))
            .collect(Collectors.toList());

        // run auto-filter
        questionAutoFilterExecutor.filter();

        // check all failed
        questions.forEach(x -> assertModState(x.getId(), ModState.AUTO_FILTER_PASSED));
    }

    private void assertModState(long questionId, ModState modState) {
        QuestionFilter questionFilter = new QuestionFilter().id(questionId).allowsNonPublic();

        List<Question> questions = questionService.getQuestions(questionFilter);
        assertEquals(1, questions.size());
        Question question = questions.get(0);

        assertEquals(modState, question.getModState(), "Check state of question: " + question.getText());
        if (modState != ModState.NEW) {
            assertEquals(1, moderationLogService.getModerationLogRecordsCount(QaEntityType.QUESTION, questionId, modState));
        }
    }


    private Question createQuestion(QuestionType type, String text) {
        long userId = -123;
        return createQuestion(type, userId, text);
    }

    private Question createQuestion(QuestionType type, long userId, String text) {
        Question question = Question.buildModelQuestion(userId, text, -1);
        question.setQuestionType(type);
        return questionService.createQuestion(question, null);
    }

    private static Stream<Arguments> allQuestionTypes() {
        return Arrays.stream(QuestionType.values())
                .map(Arguments::of);
    }

}
