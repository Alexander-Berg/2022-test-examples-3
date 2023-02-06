package ru.yandex.market.pers.qa.controller.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.AnswerDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.mock.SaasMocks;
import ru.yandex.market.pers.qa.service.saas.SaasQueryService;
import ru.yandex.market.util.ExecUtils;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.createMockedHttpResponse;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 29.12.2018
 */
public class SaasQuestionControllerTest extends QAControllerTest {
    private static final long UID_OTHER = -1238761764;

    private static final long firstPage = 1;
    private static final long secondPage = 2;
    private static final long thirdPage = 3;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SaasMocks saasMocks;

    @BeforeEach
    void startUseSaasAsDefaultSearch() {
        configurationService.mergeValue(SaasQueryService.FIELD_QUESTIONS_ENABLED, "true");
    }

    @Test
    void testLoadOnlyFromSaasLite() throws Exception {
        final long modelId = 1714248882;

        final long pageSize = 3;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 8);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 0);

        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);

        // check first page
        QAPager<QuestionDto> questions = getModelQuestionsLite(modelId, firstPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(0, 3)), toIdArray(questions));

        // check second page
        questions = getModelQuestionsLite(modelId, secondPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(3, 6)), toIdArray(questions));

        // check third page
        questions = getModelQuestionsLite(modelId, thirdPage, pageSize);

        assertEquals(2, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(6, 8)), toIdArray(questions));
    }

    @Test
    void testLoadOnlyFromSaasYandexuidLite() throws Exception {
        final long modelId = 1714248882;

        final long pageSize = 3;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 8);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 0);

        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);

        // check first page
        QAPager<QuestionDto> questions = getModelQuestionsYandexUidLite(modelId, firstPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(0, 3)), toIdArray(questions));

        // check second page
        questions = getModelQuestionsYandexUidLite(modelId, secondPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(3, 6)), toIdArray(questions));

        // check third page
        questions = getModelQuestionsYandexUidLite(modelId, thirdPage, pageSize);

        assertEquals(2, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(6, 8)), toIdArray(questions));
    }

    @Test
    void testLoadOnlyFromSaas() throws Exception {
        final long modelId = 1714248882;

        final long pageSize = 3;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 8);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 0);

        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);

        // check first page
        QAPager<QuestionDto> questions = getModelQuestions(modelId, firstPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(0, 3)), toIdArray(questions));

        // check second page
        questions = getModelQuestions(modelId, secondPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(3, 6)), toIdArray(questions));

        // check third page
        questions = getModelQuestions(modelId, thirdPage, pageSize);

        assertEquals(2, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(6, 8)), toIdArray(questions));
    }

    @Test
    void testLoadOnlyByUser() throws Exception {
        final long modelId = 1714248882;

        final long pageSize = 3;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 0);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 8);

        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);

        // check first page
        QAPager<QuestionDto> questions = getModelQuestions(modelId, firstPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(userQuestions.subList(0, 3)), toIdArray(questions));

        // check second page
        questions = getModelQuestions(modelId, secondPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(userQuestions.subList(3, 6)), toIdArray(questions));

        // check third page
        questions = getModelQuestions(modelId, thirdPage, pageSize);

        assertEquals(2, questions.getData().size());
        assertArrayEquals(toIdArray(userQuestions.subList(6, 8)), toIdArray(questions));
    }

    @Test
    void testLoadOnlyByUserWithCacheCheck() throws Exception {
        final long modelId = 1714248882;

        final long pageSize = 3;

        saasMocks.mockModelCall(modelId, pageSize, Collections.emptyList());

        // check there are no questions yet
        QAPager<QuestionDto> questions = getModelQuestions(modelId, firstPage, pageSize);
        assertEquals(0, questions.getData().size());

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 0);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 4);

        PersQaServiceMockFactory.resetMocks();
        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);

        // check first page
        questions = getModelQuestions(modelId, firstPage, pageSize);

        assertEquals(4, questions.getPager().getCount());
        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(userQuestions.subList(0, 3)), toIdArray(questions));

        // delete question
        deleteQuestion(userQuestions.get(0));

        questions = getModelQuestions(modelId, firstPage, pageSize);

        assertEquals(3, questions.getPager().getCount());
        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(userQuestions.subList(1, 4)), toIdArray(questions));
    }

    @Test
    void testQueryWithUserAndSaasQuestions() throws Exception {
        final long modelId = 19295879561L;

        final long pageSize = 3;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 5);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 2);

        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);

        // check first page
        QAPager<QuestionDto> questions = getModelQuestions(modelId, firstPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(new long[]{userQuestions.get(0), userQuestions.get(1), saasQuestions.get(0)},
            toIdArray(questions));

        // check second page
        questions = getModelQuestions(modelId, secondPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(1, 4)), toIdArray(questions));

        // check third page
        questions = getModelQuestions(modelId, thirdPage, pageSize);

        assertEquals(1, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(4, 5)), toIdArray(questions));
    }

    @Test
    void testQueryWithUserAndSaasQuestionsLastDeleted() throws Exception {
        final long modelId = 19295879561L;

        final long pageSize = 3;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 2);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 1);

        // delete user answer, rerunt from saas all answers
        deleteQuestion(userQuestions.get(0));
        List<Long> allQuestions = Stream.concat(saasQuestions.stream(), userQuestions.stream()).collect(Collectors.toList());
        saasMocks.mockModelCall(modelId, pageSize, allQuestions);

        // check page - only saas data returned
        QAPager<QuestionDto> questions = getModelQuestions(modelId, firstPage, pageSize);

        assertEquals(2, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions), toIdArray(questions));
    }

    @Test
    void testWithTopAnswers() throws Exception {
        final long modelId = 19295879561L;

        final long pageSize = 3;
        final int answersLimit = 2;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 5);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 2);
        List<Long> allQuestions = join(userQuestions, saasQuestions);

        // answers for topAnswers result.
        // result would be ordered by creation time desc
        List<List<Long>> answers = Arrays.asList(
            // for user questions
            Collections.emptyList(),
            Arrays.asList(
                createAnswer(userQuestions.get(1))
            ),
            // for saas questions
            Arrays.asList(
                createAnswer(saasQuestions.get(0)),
                createAnswer(saasQuestions.get(0))
            ),
            Collections.emptyList(),
            Arrays.asList(
                createAnswer(saasQuestions.get(2)),
                createAnswer(saasQuestions.get(2)),
                createAnswer(saasQuestions.get(2))
            ),
            Collections.emptyList(),
            Collections.emptyList()
        );

        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);
        saasMocks.mockBestAnswersSaasCall(pageSize, allQuestions, answersLimit);

        // check first page
        QAPager<QuestionDto> questions = getModelQuestionsWithAnswers(modelId, firstPage, pageSize, answersLimit);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(allQuestions.subList(0, 3)), toIdArray(questions));
        checkAnswers(questions, answers.subList(0, 3), answersLimit);

        // check second page
        questions = getModelQuestionsWithAnswers(modelId, secondPage, pageSize, answersLimit);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(allQuestions.subList(3, 6)), toIdArray(questions));
        checkAnswers(questions, answers.subList(3, 6), answersLimit);

        // check third page
        questions = getModelQuestionsWithAnswers(modelId, thirdPage, pageSize, answersLimit);

        assertEquals(1, questions.getData().size());
        assertArrayEquals(toIdArray(allQuestions.subList(6, 7)), toIdArray(questions));
        checkAnswers(questions, answers.subList(6, 7), answersLimit);
    }

    private void checkAnswers(QAPager<QuestionDto> questions, List<List<Long>> answers, int answersLimit) {
        for (int idx = 0; idx < questions.getData().size(); idx++) {
            QuestionDto questionDto = questions.getData().get(idx);
            long[] actualAnswerIds = questionDto.getAnswers().stream().mapToLong(AnswerDto::getAnswerId).toArray();

            List<Long> expected = new ArrayList<>(answers.get(idx));
            if (expected.size() > answersLimit) {
                expected = expected.subList(expected.size() - answersLimit, expected.size());
            }
            Collections.reverse(expected);

            assertEquals(answers.get(idx).size(), questionDto.getAnswersCount());
            assertEquals(expected.size(), questionDto.getAnswers().size());

            assertArrayEquals(toIdArray(expected), actualAnswerIds);

            questionDto.getAnswers().forEach(ans->{
                assertNotNull(ans.getVotesDto());
                assertEquals(0, ans.getVotesDto().getLikeCount());
                assertEquals(0, ans.getVotesDto().getDislikeCount());
                assertEquals(0, ans.getVotesDto().getUserVote());
            });
        }
    }

    @Test
    void testQueryWithUserAndSaasNoInDbQuestions() throws Exception {
        final long modelId = 19295879561L;

        final long pageSize = 3;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 0);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 2);

        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);

        // check first page
        QAPager<QuestionDto> questions = getModelQuestions(modelId, firstPage, pageSize);

        // check that there would be only data that exists in DB
        assertEquals(2, questions.getData().size());
        assertArrayEquals(toIdArray(userQuestions.subList(0, 2)), toIdArray(questions));
    }

    @Test
    void testQueryWithFirstPageByUserRough() throws Exception {
        final long modelId = 19295879561L;

        final long pageSize = 3;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 5);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 4);

        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);

        // check first page (all from db)
        QAPager<QuestionDto> questions = getModelQuestions(modelId, firstPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(userQuestions.subList(0, 3)), toIdArray(questions));

        // check second page (started using saas)
        questions = getModelQuestions(modelId, secondPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(new long[]{userQuestions.get(3), saasQuestions.get(0), saasQuestions.get(1)},
            toIdArray(questions));

        // check third page
        questions = getModelQuestions(modelId, thirdPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(2, 5)), toIdArray(questions));
    }

    @Test
    void testQueryWithFirstPageByUserClear() throws Exception {
        final long modelId = 19295879561L;

        final long pageSize = 3;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 5);
        List<Long> userQuestions = buildUserQuestions(modelId, UID, 3);

        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);

        // check first page (all from db)
        QAPager<QuestionDto> questions = getModelQuestions(modelId, firstPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(userQuestions), toIdArray(questions));

        // check second page (started using saas)
        questions = getModelQuestions(modelId, secondPage, pageSize);

        assertEquals(3, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(0, 3)), toIdArray(questions));

        // check third page
        questions = getModelQuestions(modelId, thirdPage, pageSize);

        assertEquals(2, questions.getData().size());
        assertArrayEquals(toIdArray(saasQuestions.subList(3, 5)), toIdArray(questions));
    }

    private long[] toIdArray(QAPager<QuestionDto> questions) {
        return questions.getData().stream()
            .mapToLong(QuestionDto::getId)
            .toArray();
    }

    private long[] toIdArray(List<Long> questionIds) {
        return questionIds.stream()
            .mapToLong(x -> x)
            .toArray();
    }

    @Test
    void testSaasContentIsOk() throws Exception {
        final long modelId = 1714248882;

        final long pageSize = 3;

        // create questions
        List<Long> saasQuestions = buildSaasQuestions(modelId, 8);

        saasMocks.mockModelCall(modelId, pageSize, saasQuestions);

        // check first page
        Map<Long, QuestionDto> questions = map(getModelQuestions(modelId, firstPage, pageSize));

        assertEquals(pageSize, questions.size());

        QuestionDto question1 = questions.get(saasQuestions.get(0));
        assertEquals("Question-saas-0", question1.getText());
        assertEquals(modelId, question1.getProductIdDto().getId());

        QuestionDto question2 = questions.get(saasQuestions.get(1));
        assertEquals("Question-saas-1", question2.getText());
        assertEquals(modelId, question2.getProductIdDto().getId());

        QuestionDto question3 = questions.get(saasQuestions.get(2));
        assertEquals("Question-saas-2", question3.getText());
        assertEquals(modelId, question3.getProductIdDto().getId());
    }

    @Test
    public void testGetCountPublicQuestionsYandexUid() throws Exception {
        List<Long> saasQuestions = createModelQuestionsReturnPublic(UID);
        saasMocks.mockModelCountCall(MODEL_ID, saasQuestions);

        CountDto countDto = getModelQuestionsCountYandexUid(MODEL_ID, YANDEXUID);
        assertEquals(saasQuestions.size(), countDto.getCount());
    }

    @Test
    public void testGetCountPublicQuestionsWithSaasRetry() throws Exception {
        List<Long> saasQuestions = createModelQuestionsReturnPublic(UID);

        try {
            MutableInt call = new MutableInt(0);
            Answer<Object> objectAnswer = invocation -> {
                if (call.getAndAdd(1) == 0) {
                    return createMockedHttpResponse(HttpStatus.SC_BAD_GATEWAY,
                            saasMocks.mockQuestionListSaasResponse(saasQuestions.size() + 1, saasQuestions));
                }
                return createMockedHttpResponse(HttpStatus.SC_OK,
                        saasMocks.mockQuestionListSaasResponse(saasQuestions.size(), saasQuestions));
            };
            when(saasMocks.getSaasHttpClientMock().execute(any())).thenAnswer(objectAnswer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CountDto countDto = getModelQuestionsCountYandexUid(MODEL_ID, YANDEXUID);
        assertEquals(saasQuestions.size(), countDto.getCount());
        verify(saasMocks.getSaasHttpClientMock(), times(2)).execute(any());
    }

    @Test
    public void testInvalidateCountCache() throws Exception {
        List<Long> saasQuestions = Arrays.asList(
            createQuestion(MODEL_ID),
            createQuestion(MODEL_ID)
        );

        saasMocks.mockModelCountCall(MODEL_ID, saasQuestions);

        createQuestion(MODEL_ID);
        CountDto countDto = getModelQuestionsCountUid(MODEL_ID, UID);

        long newQuestionId = createQuestion(MODEL_ID);
        CountDto countDtoAfterAdd = getModelQuestionsCountUid(MODEL_ID, UID);

        deleteQuestion(newQuestionId);
        CountDto countDtoAfterDelete = getModelQuestionsCountUid(MODEL_ID, UID);

        assertEquals(countDto.getCount() + 1, countDtoAfterAdd.getCount());
        assertEquals(countDtoAfterAdd.getCount() - 1, countDtoAfterDelete.getCount());
    }

    @Test
    public void testGetCountPublicQuestionsUid() throws Exception {
        int pageSize = 3;

        List<Long> saasQuestions = buildSaasQuestions(MODEL_ID, 8);
        List<Long> userQuestions = buildUserQuestions(MODEL_ID, UID, 3);
        List<Long> userAnotherQuestions = buildUserQuestions(MODEL_ID, UID + 1, 2);

        saasMocks.mockModelCountCallExcludeUid(UID, MODEL_ID, join(saasQuestions, userAnotherQuestions));
        saasMocks.mockModelCountCallExcludeUid(UID + 1, MODEL_ID, join(saasQuestions, userQuestions));
        saasMocks.mockModelCountCallExcludeUid(UID_OTHER, MODEL_ID, userQuestions);

        assertEquals(13, getModelQuestionsCountUid(MODEL_ID, UID).getCount());
        assertEquals(13, getModelQuestionsCountUid(MODEL_ID, UID + 1).getCount());
        assertEquals(11, getModelQuestionsCountUid(MODEL_ID, UID_OTHER).getCount());
    }

    private List<Long> buildSaasQuestions(long modelId, int count) {
        return IntStream.range(0, count)
            .mapToObj(x -> {
                try {
                    return createQuestion(modelId, UID_OTHER, "Question-saas-" + x);
                } catch (Exception e) {
                    throw ExecUtils.silentError(e);
                }
            })
            .collect(Collectors.toList());
    }

    private List<Long> buildUserQuestions(long modelId, long userId, int count) {
        return IntStream.range(0, count)
            .mapToObj(x -> {
                try {
                    return createQuestion(modelId, userId, "Question-" + x);
                } catch (Exception e) {
                    throw ExecUtils.silentError(e);
                }
            })
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());
    }
}
