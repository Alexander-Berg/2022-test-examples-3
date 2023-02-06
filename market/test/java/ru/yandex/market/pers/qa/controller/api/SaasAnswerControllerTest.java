package ru.yandex.market.pers.qa.controller.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.client.dto.VotesDto;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.api.AnswerController;
import ru.yandex.market.pers.qa.controller.dto.AnswerDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.mock.SaasMocks;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.VoteValueType;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.VoteService;
import ru.yandex.market.pers.qa.service.saas.SaasQueryService;
import ru.yandex.market.util.ExecUtils;
import ru.yandex.market.util.db.ConfigurationService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author vvolokh
 * 02.07.2019
 */
public class SaasAnswerControllerTest extends QAControllerTest {
    private static final long UID_OTHER = -1238761764;

    private static final long firstPage = 1;
    private static final long secondPage = 2;
    private static final long thirdPage = 3;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private VoteService voteService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SaasMocks saasMocks;

    @BeforeEach
    void startUseSaasAsDefaultSearch() {
        configurationService.mergeValue(SaasQueryService.FIELD_ANSWERS_ENABLED, "true");
        configurationService.mergeValue(AnswerController.RANK_FORMULA_ENABLED, "false");
    }

    @Test
    void testLoadOnlyFromSaas() throws Exception {
        final long questionId = createQuestion();

        final long pageSize = 3;

        // create questions
        List<Long> saasAnswers = buildSaasAnswers(questionId, 8);
        List<Long> userAnswers = buildUserAnswers(questionId, UID, 0);

        saasMocks.mockAnswerCall(questionId, pageSize, saasAnswers);

        // check first page
        QAPager<AnswerDto> answers = getAnswers(questionId, firstPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(saasAnswers.subList(0, 3)), toIdArray(answers));

        // check second page
        answers = getAnswers(questionId, secondPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(saasAnswers.subList(3, 6)), toIdArray(answers));

        // check third page
        answers = getAnswers(questionId, thirdPage, pageSize);

        assertEquals(2, answers.getData().size());
        assertArrayEquals(toIdArray(saasAnswers.subList(6, 8)), toIdArray(answers));
    }

    @Test
    void testLoadOnlyByUser() throws Exception {
        final long questionId = createQuestion();

        final long pageSize = 3;

        // create answers
        List<Long> saasAnswers = buildSaasAnswers(questionId, 0);
        List<Long> userAnswers = buildUserAnswers(questionId, UID, 8);

        saasMocks.mockAnswerCall(questionId, pageSize, saasAnswers);

        // check first page
        QAPager<AnswerDto> answers = getAnswers(questionId, firstPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(userAnswers.subList(0, 3)), toIdArray(answers));

        // check second page
        answers = getAnswers(questionId, secondPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(userAnswers.subList(3, 6)), toIdArray(answers));

        // check third page
        answers = getAnswers(questionId, thirdPage, pageSize);

        assertEquals(2, answers.getData().size());
        assertArrayEquals(toIdArray(userAnswers.subList(6, 8)), toIdArray(answers));
    }

    @Test
    void testLoadOnlyByUserWithCacheCheck() throws Exception {
        final long questionId = createQuestion();

        final long pageSize = 3;

        saasMocks.mockAnswerCall(questionId, pageSize, Collections.emptyList());

        // check there are no answers yet
        QAPager<AnswerDto> answers = getAnswers(questionId, firstPage, pageSize);
        assertEquals(0, answers.getData().size());

        // create answers
        List<Long> saasAnswers = buildSaasAnswers(questionId, 0);
        List<Long> userAnswers = buildUserAnswers(questionId, UID, 4);

        PersQaServiceMockFactory.resetMocks();
        saasMocks.mockAnswerCall(questionId, pageSize, saasAnswers);

        // check first page
        answers = getAnswers(questionId, firstPage, pageSize);

        assertEquals(4, answers.getPager().getCount());
        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(userAnswers.subList(0, 3)), toIdArray(answers));

        // delete answer
        deleteAnswer(userAnswers.get(0));

        answers = getAnswers(questionId, firstPage, pageSize);

        assertEquals(3, answers.getPager().getCount());
        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(userAnswers.subList(1, 4)), toIdArray(answers));
    }

    @Test
    void testQueryWithUserAndSaasQuestions() throws Exception {
        final long questionId = createQuestion();

        final long pageSize = 3;

        // create questions
        List<Long> saasAnswers = buildSaasAnswers(questionId, 5);
        List<Long> userAnswers = buildUserAnswers(questionId, UID, 2);

        saasMocks.mockAnswerCall(questionId, pageSize, saasAnswers);

        // check first page
        QAPager<AnswerDto> answers = getAnswers(questionId, firstPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(new long[]{userAnswers.get(0), userAnswers.get(1), saasAnswers.get(0)},
            toIdArray(answers));

        // check second page
        answers = getAnswers(questionId, secondPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(saasAnswers.subList(1, 4)), toIdArray(answers));

        // check third page
        answers = getAnswers(questionId, thirdPage, pageSize);

        assertEquals(1, answers.getData().size());
        assertArrayEquals(toIdArray(saasAnswers.subList(4, 5)), toIdArray(answers));
    }

    @Test
    void testQueryWithUserAndSaasAnswersLastDeleted() throws Exception {
        final long questionId = createQuestion();

        final long pageSize = 3;

        // create answers
        List<Long> saasAnswers = buildSaasAnswers(questionId, 2);
        List<Long> userAnswers = buildUserAnswers(questionId, UID, 1);

        // delete user answer, rerunt from saas all answers
        deleteAnswer(userAnswers.get(0));
        List<Long> allAnswers = Stream.concat(saasAnswers.stream(), userAnswers.stream()).collect(Collectors.toList());
        saasMocks.mockAnswerCall(questionId, pageSize, allAnswers);


        // check page - only saas data returned
        QAPager<AnswerDto> answers = getAnswers(questionId, firstPage, pageSize);

        assertEquals(2, answers.getData().size());
        assertArrayEquals(toIdArray(saasAnswers), toIdArray(answers));
    }

    @Test
    void testQueryWithFirstPageByUserRough() throws Exception {
        final long questionId = createQuestion();

        final long pageSize = 3;

        // create questions
        List<Long> saasAnswers = buildSaasAnswers(questionId, 5);
        List<Long> userAnswers = buildUserAnswers(questionId, UID, 4);

        saasMocks.mockAnswerCall(questionId, pageSize, saasAnswers);

        // check first page (all from db)
        QAPager<AnswerDto> answers = getAnswers(questionId, firstPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(userAnswers.subList(0, 3)), toIdArray(answers));

        // check second page (started using saas)
        answers = getAnswers(questionId, secondPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(new long[]{userAnswers.get(3), saasAnswers.get(0), saasAnswers.get(1)},
            toIdArray(answers));

        // check third page
        answers = getAnswers(questionId, thirdPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(saasAnswers.subList(2, 5)), toIdArray(answers));
    }

    @Test
    void testQueryWithFirstPageByUserClear() throws Exception {
        final long questionId = createQuestion();

        final long pageSize = 3;

        // create questions
        List<Long> saasAnswers = buildSaasAnswers(questionId, 5);
        List<Long> userAnswers = buildUserAnswers(questionId, UID, 3);

        saasMocks.mockAnswerCall(questionId, pageSize, saasAnswers);

        // check first page (all from db)
        QAPager<AnswerDto> answers = getAnswers(questionId, firstPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(userAnswers), toIdArray(answers));

        // check second page (started using saas)
        answers = getAnswers(questionId, secondPage, pageSize);

        assertEquals(3, answers.getData().size());
        assertArrayEquals(toIdArray(saasAnswers.subList(0, 3)), toIdArray(answers));

        // check third page
        answers = getAnswers(questionId, thirdPage, pageSize);

        assertEquals(2, answers.getData().size());
        assertArrayEquals(toIdArray(saasAnswers.subList(3, 5)), toIdArray(answers));
    }

    @Test
    void testSaasContentIsOk() throws Exception {
        final long questionId = createQuestion();

        final long pageSize = 3;

        // create questions
        List<Long> saasAnswers = buildSaasAnswers(questionId, 1);
        long vendorAnswerId = createVendorAnswer(1234L, questionId, "vendor text");
        long shopAnswerId = createPartnerShopAnswer(12345L, questionId, "shop text");

        saasMocks.mockAnswerCall(questionId, pageSize, Arrays.asList(saasAnswers.get(0), vendorAnswerId, shopAnswerId));

        // check first page
        Map<Long, AnswerDto> answers = getAnswers(questionId, firstPage, pageSize).getData().stream()
            .collect(Collectors.toMap(
                AnswerDto::getAnswerId,
                x -> x
            ));

        assertEquals(pageSize, answers.size());

        AnswerDto answer1 = answers.get(saasAnswers.get(0));
        assertEquals("Answer-saas-0", answer1.getText());
        assertEquals(questionId, answer1.getQuestionId().getId());
        assertEquals(String.valueOf(UID_OTHER), answer1.getAuthorDto().getId());
        assertEquals(UID_OTHER, answer1.getUserDto().getUserId());

        AnswerDto answer2 = answers.get(vendorAnswerId);
        assertEquals("vendor text", answer2.getText());
        assertEquals(questionId, answer2.getQuestionId().getId());
        assertEquals(String.valueOf(1234L), answer2.getAuthorDto().getId());
        assertEquals(Long.valueOf(1234L), answer2.getBrandId());
        assertEquals(UID, answer2.getUserDto().getUserId());

        AnswerDto answer3 = answers.get(shopAnswerId);
        assertEquals("shop text", answer3.getText());
        assertEquals(questionId, answer3.getQuestionId().getId());
        assertEquals(Long.valueOf(12345L), answer3.getShopId());
        assertEquals(UID, answer3.getUserDto().getUserId());
    }

    @Test
    public void testGetCountPublicAnswersYandexUid() throws Exception {
        final long questionId = createQuestion();
        List<Long> saasAnswers = createAnswersAndReturnPublic(questionId);
        saasMocks.mockAnswerCountCall(questionId, saasAnswers);

        QuestionDto questionDto = questionMvc.getQuestionYandexUid(questionId, YANDEXUID);
        assertEquals(saasAnswers.size(), questionDto.getAnswersCount());
    }

    @Test
    public void testInvalidateCountCache() throws Exception {
        final long questionId = createQuestion();

        List<Long> saasAnswers = Arrays.asList(
            createAnswer(questionId),
            createAnswer(questionId)
        );

        saasMocks.mockAnswerCountCall(questionId, saasAnswers);

        QuestionDto questionDto = questionMvc.getQuestion(questionId, UID);

        long newAnswerId = createAnswer(questionId);
        QuestionDto questionDtoAfterAdd = questionMvc.getQuestion(questionId, UID);

        deleteAnswer(newAnswerId);
        QuestionDto questionDtoAfterDelete = questionMvc.getQuestion(questionId, UID);

        assertEquals(questionDto.getAnswersCount() + 1, questionDtoAfterAdd.getAnswersCount());
        assertEquals(questionDtoAfterAdd.getAnswersCount() - 1, questionDtoAfterDelete.getAnswersCount());
    }

    @Test
    public void testGetCountPublicAnswersUid() throws Exception {
        final long questionId = createQuestion();
        questionService.forceUpdateModState(questionId, ModState.AUTO_FILTER_PASSED);

        List<Long> saasAnswers = buildSaasAnswers(questionId, 8);
        List<Long> userAnswers = buildUserAnswers(questionId, UID, 3);
        List<Long> user2Answers = buildUserAnswers(questionId, UID + 1, 2);
        List<Long> user3Answers = buildUserAnswers(questionId, UID + 2, 1);

        saasMocks.mockAnswerCallExcludeUid(questionId, UID, DEF_PAGE_SIZE, join(saasAnswers, user2Answers, user3Answers));
        saasMocks.mockAnswerCallExcludeUid(questionId, UID + 1, DEF_PAGE_SIZE, join(saasAnswers, userAnswers, user3Answers));
        //simulate incomplete response from SaaS
        saasMocks.mockAnswerCallExcludeUid(questionId, UID + 2, DEF_PAGE_SIZE, saasAnswers);

        assertEquals(14, getAnswersUid(questionId, UID).getPager().getCount());
        assertEquals(14, getAnswersUid(questionId, UID + 1).getPager().getCount());
        //saasAnswers + user3Answers
        assertEquals(9, getAnswersUid(questionId, UID + 2).getPager().getCount());
    }

    @Test
    public void testVotesAreFromCache() throws Exception {
        final long questionId = createQuestion();
        questionService.forceUpdateModState(questionId, ModState.AUTO_FILTER_PASSED);

        long answerId = createAnswer(questionId, UID);

        saasMocks.mockAnswerCall(questionId, DEF_PAGE_SIZE, Collections.singletonList(answerId));

        voteService.createAnswerVote(answerId, UID, VoteValueType.LIKE);
        voteService.createAnswerVote(answerId, UID + 1, VoteValueType.DISLIKE);
        voteService.createAnswerVote(answerId, UID + 2, VoteValueType.DISLIKE);

        invalidateCache();
        VotesDto votesDto = getAnswersUid(questionId, UID + 1).getData().get(0).getVotesDto();

        assertEquals(1, votesDto.getLikeCount());
        assertEquals(2, votesDto.getDislikeCount());
        assertEquals(VoteValueType.DISLIKE.getValue(), votesDto.getUserVote());
    }

    private List<Long> buildSaasAnswers(long questionId, int count) {
        return IntStream.range(0, count)
            .mapToObj(x -> {
                try {
                    return createAnswer(questionId, UID_OTHER, "Answer-saas-" + x);
                } catch (Exception e) {
                    throw ExecUtils.silentError(e);
                }
            })
            .collect(Collectors.toList());
    }

    private List<Long> buildUserAnswers(long questionId, long userId, int count) {
        return IntStream.range(0, count)
            .mapToObj(x -> {
                try {
                    return createAnswer(questionId, userId, "Answer-" + x);
                } catch (Exception e) {
                    throw ExecUtils.silentError(e);
                }
            })
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.toList());
    }

    private long[] toIdArray(QAPager<AnswerDto> answers) {
        return answers.getData().stream()
            .mapToLong(AnswerDto::getAnswerId)
            .toArray();
    }

    private long[] toIdArray(List<Long> questionIds) {
        return questionIds.stream()
            .mapToLong(x -> x)
            .toArray();
    }
}
