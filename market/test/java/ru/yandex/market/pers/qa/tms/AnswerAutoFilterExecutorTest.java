package ru.yandex.market.pers.qa.tms;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.cleanweb.CleanWebClient;
import ru.yandex.market.cleanweb.CleanWebContent;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.mock.AutoFilterServiceTestUtils;
import ru.yandex.market.pers.qa.mock.EntityForFilter;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.AnswerFilter;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.UserBanInfo;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.ModerationLogService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.UserBanService;
import ru.yandex.market.pers.qa.tms.filter.AnswerAutoFilterExecutor;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;

/**
 * @author korolyov
 * 20.06.18
 */
public class AnswerAutoFilterExecutorTest extends PersQaTmsTest {
    private static final int N = 10;
    private static final int N_VENDOR = 10;
    private static final int N_SHOP = 50;
    private static final long MODERATOR_ID = 100500;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerAutoFilterExecutor answerAutoFilterExecutor;

    @Autowired
    private UserBanService userBanService;

    @Autowired
    private ModerationLogService moderationLogService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CleanWebClient cleanWebClient;

    @Test
    void testFilter() {
        prepareData();
        Map<Long, ModState> vendorAnswersIds = createVendorAnswers(N_VENDOR);
        Map<Long, ModState> shopAnswersIds = createShopAnswers(N_SHOP);

        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        answerAutoFilterExecutor.filter();

        List<EntityForFilter> answers = getAnswersWithModStates();
        assertEquals(N + N_VENDOR + N_SHOP, answers.size());

        answers.forEach(answer -> {
            long id = answer.id;

            final ModState expectedModState;
            if (vendorAnswersIds.containsKey(id)) {
                ModState modState = vendorAnswersIds.get(id);
                // auto-filter should change NEW -> CONFIRMED, everything else should not be changed
                expectedModState = modState == ModState.NEW ? ModState.CONFIRMED : modState;
            } else if (shopAnswersIds.containsKey(id)) {
                ModState modState = shopAnswersIds.get(id);
                // auto-filter should change NEW and AUTO_FILTER_PASSED -> CONFIRMED
                // should not change everything else
                if (modState == ModState.NEW) {
                    ModState filterDecision = getExpectedModState(answer);
                    expectedModState = filterDecision == ModState.AUTO_FILTER_PASSED
                        ? ModState.CONFIRMED
                        : filterDecision;
                } else {
                    expectedModState = modState == ModState.AUTO_FILTER_PASSED
                        ? ModState.CONFIRMED
                        : modState;
                }
            } else {
                expectedModState = getExpectedModState(answer);
            }

            assertEquals(expectedModState, answer.modState);
        });
    }

    @Test
    void testCleanWebFilter() {
        prepareData();
        Map<Long, ModState> vendorAnswersIds = createVendorAnswers(N_VENDOR);
        Map<Long, ModState> shopAnswersIds = createShopAnswers(N_SHOP);

        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        answerAutoFilterExecutor.filter();

        List<EntityForFilter> answers = getAnswersWithModStates();
        assertEquals(N + N_VENDOR + N_SHOP, answers.size());

        answers.forEach(answer -> {
            long id = answer.id;

            final ModState expectedModState;
            if (vendorAnswersIds.containsKey(id)) {
                ModState modState = vendorAnswersIds.get(id);
                // auto-filter should change NEW -> CONFIRMED, everything else should not be changed
                expectedModState = modState == ModState.NEW ? ModState.CONFIRMED : modState;
            } else if (shopAnswersIds.containsKey(id)) {
                ModState modState = shopAnswersIds.get(id);
                // auto-filter should change NEW and AUTO_FILTER_PASSED -> CONFIRMED
                // should not change everything else
                if (modState == ModState.NEW) {
                    ModState filterDecision = getExpectedModState(answer);
                    expectedModState = filterDecision == ModState.AUTO_FILTER_PASSED
                        ? ModState.CONFIRMED
                        : filterDecision;
                } else {
                    expectedModState = modState == ModState.AUTO_FILTER_PASSED
                        ? ModState.CONFIRMED
                        : modState;
                }
            } else {
                expectedModState = getExpectedModState(answer);
            }

            assertEquals(expectedModState, answer.modState);
        });
    }

    @Test
    void testRecheck() {
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_UNKNOWN);

        final Question question = questionService.createModelQuestion(1, "Test question?", 1);
        answerService.createAnswer(1, "text", question.getId());

        answerAutoFilterExecutor.filter();

        checkSingleAnswer(ModState.AUTO_FILTER_UNKNOWN);
        checkAutoRecheckTableSize(1);

        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_PASSED);
        answerAutoFilterExecutor.filter();

        checkSingleAnswer(ModState.AUTO_FILTER_PASSED);
        checkAutoRecheckTableSize(0);
    }

    @Test
    void testBannedAnswers() {
        long userId = -123;
        Question question = questionService.createQuestion(Question.buildModelQuestion(userId, "test", -1), null);
        Answer answer = answerService.createAnswer(userId, "Some text", question.getId());

        // they are published
        assertModState(answer.getId(), ModState.NEW);

        userBanService.ban(UserBanInfo.forever(UserType.UID, String.valueOf(userId), "Some reason", MODERATOR_ID));

        // still published
        assertModState(answer.getId(), ModState.NEW);

        // run auto-filter
        answerAutoFilterExecutor.filter();

        // now banned
        assertModState(answer.getId(), ModState.AUTO_FILTER_REJECTED);
    }

    @Test
    void testBannedAnswersBanTimeExpired() {
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_PASSED);

        long userId = -123;
        Question question = questionService.createQuestion(Question.buildModelQuestion(userId, "test", -1), null);
        Answer answer = answerService.createAnswer(userId, "Some text", question.getId());

        userBanService
            .ban(UserBanInfo.limited(
                UserType.UID,
                String.valueOf(userId),
                "Some reason",
                MODERATOR_ID, Instant.now().minus(2, ChronoUnit.DAYS)
            ));

        // run auto-filter
        answerAutoFilterExecutor.filter();

        // now still not banned
        assertModState(answer.getId(), ModState.AUTO_FILTER_PASSED);
    }

    @Test
    void testBannedAnswersBanTimeGood() {
        long userId = -123;
        Question question = questionService.createQuestion(Question.buildModelQuestion(userId, "test", -1), null);
        Answer answer = answerService.createAnswer(userId, "Some text", question.getId());

        userBanService
            .ban(UserBanInfo.limited(
                UserType.UID,
                String.valueOf(userId),
                "Some reason",
                MODERATOR_ID, Instant.now().plus(2, ChronoUnit.DAYS)
            ));

        // run auto-filter
        answerAutoFilterExecutor.filter();

        // now banned
        assertModState(answer.getId(), ModState.AUTO_FILTER_REJECTED);
    }

    @Test
    void testCleanWebRequest() {
        //mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_UNKNOWN);
        final Question question = questionService.createModelQuestion(1, "Test question?", 1);
        answerService.createAnswer(1, "text", question.getId());

        //start executing
        answerAutoFilterExecutor.filter();

        //check map of parameters in request to cleanWeb
        ArgumentCaptor<CleanWebContent> cleanWebClientArgumentCaptor = ArgumentCaptor.forClass(CleanWebContent.class);
        verify(cleanWebClient).sendContent(cleanWebClientArgumentCaptor.capture(), anyBoolean());
        CleanWebContent value = cleanWebClientArgumentCaptor.getValue();
        assertEquals(Map.of("uid", "1", "is_shop", false, "is_brand", false), value.getParameters());
    }

    private void checkAutoRecheckTableSize(int size) {
        List<Long> rechecks = jdbcTemplate.queryForList("SELECT id FROM qa.auto_recheck", Long.class);
        assertEquals(rechecks.size(), size);
    }

    private void checkSingleAnswer(ModState modState) {
        List<EntityForFilter> answers = getAnswersWithModStates();
        assertEquals(1, answers.size());
        assertEquals(answers.get(0).modState, modState);
    }

    private void prepareData() {
        List<String> texts = AutoFilterServiceTestUtils.generateTextsForFilteringMod3(N);
        for (int userId = 1; userId <= N; userId++) {
            final Question question = questionService.createModelQuestion(userId, someText(), N - userId);
            answerService.createAnswer(userId, texts.get(userId - 1), question.getId());
        }
    }

    private Map<Long, ModState> createVendorAnswers(int count) {
        Map<Long, ModState> vendorAnswerIds = new HashMap<>();
        List<String> texts = AutoFilterServiceTestUtils.generateTextsForFilteringMod3(count);

        for (int userId = 1, brandId = 100500; userId <= count; userId++, brandId++) {
            final Question question = questionService.createModelQuestion(userId, someText(), N - userId);
            final Answer answer = answerService.createVendorAnswer(userId, texts.get(userId - 1), question.getId(),
                brandId);

            final ModState modState = someModState(userId);
            answerService.forceUpdateModState(answer.getId(), modState);
            vendorAnswerIds.putIfAbsent(answer.getId(), modState);
        }
        return vendorAnswerIds;
    }

    private Map<Long, ModState> createShopAnswers(int count) {
        Map<Long, ModState> result = new HashMap<>();
        List<String> texts = AutoFilterServiceTestUtils.generateTextsForFilteringMod3(count);

        for (int userId = 1, shopId = 100500; userId <= count; userId++, shopId++) {
            Question question = questionService.createModelQuestion(userId, someText(), N - userId);
            Answer answer = answerService.createShopAnswer(userId, texts.get(userId - 1), question.getId(), shopId);

            ModState modState = someModState(userId);
            answerService.forceUpdateModState(answer.getId(), modState);
            result.putIfAbsent(answer.getId(), modState);
        }
        return result;
    }

    private ModState someModState(int seed) {
        return ModState.values()[Math.floorMod(seed - 1, ModState.values().length)];
    }

    @NotNull
    private String someText() {
        return UUID.randomUUID().toString();
    }

    private ModState getExpectedModState(EntityForFilter answer) {
        return AutoFilterServiceTestUtils.modStateByUserIdMod3(answer);
    }

    private List<EntityForFilter> getAnswersWithModStates() {
        return jdbcTemplate.query(
            "SELECT id, user_id, text, mod_state FROM qa.answer",
            (rs, rowNum) -> EntityForFilter.valueOf(rs));
    }

    private void assertModState(long answerId, ModState modState) {
        AnswerFilter answerFilter = new AnswerFilter().id(answerId).allowsNonPublic();

        List<Answer> answers = answerService.getAnswers(answerFilter);
        assertEquals(1, answers.size());
        assertEquals(modState, answers.get(0).getModState());
        if (modState != ModState.NEW) {
            assertEquals(1, moderationLogService.getModerationLogRecordsCount(QaEntityType.ANSWER, answerId, modState));
        }
    }

}
