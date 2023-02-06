package ru.yandex.market.pers.qa.controller.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.grade.statica.client.PersStaticClient;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.client.dto.VotesDto;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.CategoryIdDto;
import ru.yandex.market.pers.qa.controller.dto.ProductIdDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.mock.SaasMocks;
import ru.yandex.market.pers.qa.mock.mvc.PostMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.ProfileMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.QuestionMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.VoteMvcMocks;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.util.ExecUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.controller.service.QuestionHelper.DAYS_TO_SHOW_QUESTIONS_FOR_AGITATION;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 20.11.2019
 */
public class ProfileControllerTest extends QAControllerTest {

    private static final long ANOTHER_UID = 1250;

    @Autowired
    private QuestionMvcMocks questionMvc;
    @Autowired
    private VoteMvcMocks voteMvc;
    @Autowired
    private ProfileMvcMocks profileMvc;
    @Autowired
    private PostMvcMocks postMvcMocks;

    @Autowired
    private PersStaticClient persStaticClient;
    @Autowired
    private SaasMocks saasMocks;


    @Test
    public void testGetAuthorQuestions() throws Exception {
        List<Long> questions = Arrays.asList(
                createModelQuestion(MODEL_ID, UID),
                createCategoryQuestion(CATEGORY_HID, UID),
                postMvcMocks.createInterestPost(INTEREST_ID, UID)
        );

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorQuestions(UID);

        // post should be excluded
        assertEquals(questions.size() - 1, qaPager.getData().size());

        List<Long> modelQuestions = qaPager.getData().stream()
                .filter(x -> Objects.nonNull(x.getProductIdDto())
                        && x.getProductIdDto().getEntity().equals(ProductIdDto.PRODUCT))
                .map(QuestionDto::getId)
                .collect(Collectors.toList());
        assertEquals(1, modelQuestions.size());
        assertEquals(questions.get(0), modelQuestions.get(0));

        List<Long> categoryQuestions = qaPager.getData().stream()
                .filter(x -> Objects.nonNull(x.getCategoryIdDto())
                        && x.getCategoryIdDto().getEntity().equals(CategoryIdDto.CATEGORY))
                .map(QuestionDto::getId)
                .collect(Collectors.toList());
        assertEquals(1, categoryQuestions.size());
        assertEquals(questions.get(1), categoryQuestions.get(0));
    }

    @Test
    public void testGetAuthorQuestionsWithDifferentUsers() throws Exception {
        createModelQuestion(MODEL_ID, UID);
        createModelQuestion(MODEL_ID, ANOTHER_UID);
        createCategoryQuestion(CATEGORY_HID, UID);
        createCategoryQuestion(CATEGORY_HID, ANOTHER_UID);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorQuestions(UID);
        assertEquals(2, qaPager.getData().size());
        assertTrue(qaPager.getData().stream().allMatch(q -> q.getUserDto().getUserId() == UID));
    }

    @Test
    public void testGetAuthorQuestionsWithIgnoringBannedOrRemovedQuestions() throws Exception {
        createQuestionsReturnAll(() -> createModelQuestion(MODEL_ID, UID));
        createQuestionsReturnAll(() -> createCategoryQuestion(CATEGORY_HID, UID));

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorQuestions(UID, 1, 40);
        assertEquals(2 * ModState.PUBLISHED.size(), qaPager.getData().size());
    }

    @Test
    public void testGetAuthorQuestionsWithCleanCacheAfterCreateQuestion() throws Exception {
        QAPager<QuestionDto> before = profileMvc.getAuthorQuestions(UID);
        createCategoryQuestion(CATEGORY_HID, UID);
        createModelQuestion(MODEL_ID, UID);
        QAPager<QuestionDto> after = profileMvc.getAuthorQuestions(UID);

        assertEquals(before.getData().size() + 2, after.getData().size());
    }

    @Test
    public void testGetAuthorQuestionsWithCleanCacheAfterRemoveQuestion() throws Exception {
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, UID);
        long modelQuestionId = createModelQuestion(MODEL_ID, UID);

        QAPager<QuestionDto> before = profileMvc.getAuthorQuestions(UID);
        deleteQuestion(categoryQuestionId);
        deleteQuestion(modelQuestionId);
        QAPager<QuestionDto> after = profileMvc.getAuthorQuestions(UID);

        assertEquals(before.getData().size() - 2, after.getData().size());
    }

    @Test
    public void testGetAuthorQuestionsWithPager() throws Exception {
        createQuestionsReturnAll(() -> createModelQuestion(MODEL_ID, UID));
        createQuestionsReturnAll(() -> createCategoryQuestion(CATEGORY_HID, UID));

        QAPager<QuestionDto> firstPage = profileMvc.getAuthorQuestions(UID, 1, 10);
        QAPager<QuestionDto> secondPage = profileMvc.getAuthorQuestions(UID, 2, 10);

        assertEquals(10, firstPage.getData().size());
        assertEquals(4, secondPage.getData().size());
    }

    @Test
    public void testGetAuthorQuestionsWithVotes() throws Exception {
        long modelQuestionId = createModelQuestion(MODEL_ID, UID);
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, UID);
        voteMvc.likeQuestion(ANOTHER_UID, modelQuestionId);
        voteMvc.likeQuestion(UID, categoryQuestionId);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorQuestions(UID);

        assertEquals(1, qaPager.getData().get(0).getVotesDto().getLikeCount());
        assertEquals(1, qaPager.getData().get(0).getVotesDto().getUserVote());
        assertEquals(1, qaPager.getData().get(1).getVotesDto().getLikeCount());
    }

    @Test
    public void testGetAuthorAnswers() throws Exception {
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, ANOTHER_UID);
        long categoryAnswerId = createAnswer(categoryQuestionId, UID);
        long modelQuestionId = createModelQuestion(MODEL_ID, ANOTHER_UID);
        long modelAnswerId = createAnswer(modelQuestionId, UID);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorAnswers(UID);

        assertEquals(getAnswer(categoryAnswerId, UID).getText(), qaPager.getData().stream()
                .filter(x -> x.getId() == categoryQuestionId).findFirst().get().getAnswers().get(0).getText());
        assertEquals(getAnswer(modelAnswerId, UID).getText(), qaPager.getData().stream()
                .filter(x -> x.getId() == modelQuestionId).findFirst().get().getAnswers().get(0).getText());
    }

    @Test
    public void testGetAuthorAnswersWithoutShopAndBrandAnswers() throws Exception {
        long questionId = createModelQuestion(MODEL_ID, UID + 1);
        createVendorAnswer(123, questionId, "Вендорский ответ");
        createPartnerShopAnswer(1234, questionId, "Ответ магазина");
        long userAnswerId = createAnswer(questionId);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorAnswers(UID);
        Assertions.assertEquals(1, qaPager.getData().size());
        Assertions.assertEquals(questionId, qaPager.getData().get(0).getId());
        Assertions.assertEquals(3, qaPager.getData().get(0).getAnswersCount());
        Assertions.assertEquals(userAnswerId, qaPager.getData().get(0).getAnswers().get(0).getAnswerId());
    }

    @Test
    public void testGetAuthorAnswersWithTwoAnswersToOneQuestion() throws Exception {
        List<String> questionText = Arrays.asList(
                "Где лучше всего покупать такой аппарат?",
                "Неужели скорость работы ключевой параметр?"
        );

        List<String> answerText = Arrays.asList(
                "Мне кажется, что надо ехать на окраину города.",
                "Да, тот самый магазин на перекрестке после светофора.",
                "Да, ведь именно это надо большинству пользователей.",
                "Но есть и исключения, которым важно только время работы!"
        );

        List<Long> questions = Arrays.asList(
                createQuestion(MODEL_ID, ANOTHER_UID, questionText.get(0)),
                createCategoryQuestion(CATEGORY_HID, ANOTHER_UID, questionText.get(1))
        );

        List<Long> answers = Arrays.asList(
                createAnswer(questions.get(0), UID, answerText.get(0)),
                createAnswer(questions.get(0), UID, answerText.get(1)),
                createAnswer(questions.get(1), UID, answerText.get(2)),
                createAnswer(questions.get(1), UID, answerText.get(3))
        );

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorAnswers(UID);

        assertEquals(4, qaPager.getData().size());
        assertQuestion(qaPager.getData().get(0), questions.get(1), questionText.get(1), answers.get(3), answerText.get(3));
        assertQuestion(qaPager.getData().get(1), questions.get(1), questionText.get(1), answers.get(2), answerText.get(2));
        assertQuestion(qaPager.getData().get(2), questions.get(0), questionText.get(0), answers.get(1), answerText.get(1));
        assertQuestion(qaPager.getData().get(3), questions.get(0), questionText.get(0), answers.get(0), answerText.get(0));
    }

    private void assertQuestion(QuestionDto questionDto,
                                long questionId, String questionText,
                                long answerId, String answerText) {
        assertQuestion(questionDto, questionId, answerId);
        assertEquals(questionDto.getText(), questionText);
        assertEquals(questionDto.getAnswers().get(0).getText(), answerText);
    }

    @Test
    public void testGetAuthorAnswersWithCheckIndexMatch() throws Exception {
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, ANOTHER_UID);
        long modelQuestionId = createModelQuestion(MODEL_ID, ANOTHER_UID);
        long categoryAnswerId = createAnswer(categoryQuestionId, UID);
        long modelAnswerId = createAnswer(modelQuestionId, UID);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorAnswers(UID);

        assertQuestion(qaPager.getData().get(0), modelQuestionId, modelAnswerId);
        assertQuestion(qaPager.getData().get(1), categoryQuestionId, categoryAnswerId);
    }

    private void assertQuestion(QuestionDto questionDto, long questionId, long answerId) {
        assertEquals(questionId, questionDto.getId());
        assertEquals(answerId, questionDto.getAnswers().get(0).getAnswerId());
        assertEquals(questionDto.getId(), questionDto.getAnswers().get(0).getQuestionId().getId());
    }

    @Test
    public void testGetAuthorAnswersWithEmptyAnswer() throws Exception {
        // no answers
        QAPager<QuestionDto> qaPager = profileMvc.getAuthorAnswers(UID);

        assertEquals(0, qaPager.getData().size());
    }

    @Test
    public void testGetAuthorAnswersWithDifferentUsers() throws Exception {
        List<Long> questions = Arrays.asList(
                createModelQuestion(MODEL_ID, ANOTHER_UID),
                createCategoryQuestion(CATEGORY_HID, ANOTHER_UID)
        );

        createAnswer(questions.get(0), ANOTHER_UID);
        createAnswer(questions.get(1), ANOTHER_UID);
        createAnswer(questions.get(0), UID);
        createAnswer(questions.get(1), UID);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorAnswers(UID);
        assertEquals(2, qaPager.getData().size());
        assertTrue(qaPager.getData().stream().flatMap(q -> q.getAnswers().stream())
                .allMatch(q -> q.getUserDto().getUserId() == UID));
    }

    @Test
    public void testGetAuthorAnswersWithIgnoringBannedOrRemovedQuestions() throws Exception {
        List<Long> modelQuestions = createQuestionsReturnAll(() -> createModelQuestion(MODEL_ID, ANOTHER_UID));
        List<Long> categoryQuestions = createQuestionsReturnAll(() -> createCategoryQuestion(CATEGORY_HID, ANOTHER_UID));

        for (Long questionId : modelQuestions) {
            createAnswer(questionId, UID);
        }

        for (Long questionId : categoryQuestions) {
            createAnswer(questionId, UID);
        }

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorAnswers(UID, 1, 40);
        assertEquals(2 * ModState.PUBLISHED.size(), qaPager.getData().size());
    }

    @Test
    public void testGetAuthorAnswersWithIgnoringBannedOrRemovedAnswers() throws Exception {
        long modelQuestion = createModelQuestion(MODEL_ID, ANOTHER_UID);
        long categoryQuestion = createCategoryQuestion(CATEGORY_HID, ANOTHER_UID);
        createAnswersReturnAll(() -> createAnswer(modelQuestion, UID));
        createAnswersReturnAll(() -> createAnswer(categoryQuestion, UID));

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorAnswers(UID, 1, 40);
        assertEquals(2 * ModState.PUBLISHED.size(), qaPager.getData().size());
    }

    @Test
    public void testGetAuthorAnswersWithPager() throws Exception {
        List<Long> modelQuestions = createQuestionsReturnAll(() -> createModelQuestion(MODEL_ID, ANOTHER_UID));
        List<Long> categoryQuestions = createQuestionsReturnAll(() -> createCategoryQuestion(CATEGORY_HID, ANOTHER_UID));

        for (Long questionId : modelQuestions) {
            createAnswer(questionId, UID);
        }

        for (Long questionId : categoryQuestions) {
            createAnswer(questionId, UID);
        }

        QAPager<QuestionDto> firstPage = profileMvc.getAuthorAnswers(UID, 1, 10);
        QAPager<QuestionDto> secondPage = profileMvc.getAuthorAnswers(UID, 2, 10);

        assertEquals(10, firstPage.getData().size());
        assertEquals(4, secondPage.getData().size());
        assertEquals(2, firstPage.getPager().getTotalPageCount());
        assertEquals(2, secondPage.getPager().getTotalPageCount());
    }

    @Test
    public void testGetAuthorAnswersWithCleanCacheAfterCreateAnswer() throws Exception {
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, ANOTHER_UID);
        long modelQuestionId = createModelQuestion(MODEL_ID, ANOTHER_UID);

        QAPager<QuestionDto> before = profileMvc.getAuthorAnswers(UID);
        createAnswer(categoryQuestionId, UID);
        createAnswer(modelQuestionId, UID);
        QAPager<QuestionDto> after = profileMvc.getAuthorAnswers(UID);

        assertEquals(before.getData().size() + 2, after.getData().size());
    }

    @Test
    public void testGetAuthorAnswersWithCleanCacheAfterRemoveAnswer() throws Exception {
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, ANOTHER_UID);
        long modelQuestionId = createModelQuestion(MODEL_ID, ANOTHER_UID);
        long categoryAnswerId = createAnswer(categoryQuestionId, UID);
        long modelAnswerId = createAnswer(modelQuestionId, UID);

        QAPager<QuestionDto> before = profileMvc.getAuthorAnswers(UID);
        deleteAnswer(categoryAnswerId, UID, status().is2xxSuccessful());
        deleteAnswer(modelAnswerId, UID, status().is2xxSuccessful());
        QAPager<QuestionDto> after = profileMvc.getAuthorAnswers(UID);

        assertEquals(before.getData().size() - 2, after.getData().size());
    }

    @Test
    public void testGetAuthorAnswersWithVotes() throws Exception {
        long questionId = createModelQuestion(MODEL_ID, ANOTHER_UID);

        List<Long> answerIds = Arrays.asList(
                createAnswer(questionId, UID),
                createAnswer(questionId, UID),
                createAnswer(questionId, UID)
        );

        voteMvc.voteAnswer(UID, answerIds.get(0), VotesDto.VOTE_LIKE);
        voteMvc.voteAnswer(UID + 1, answerIds.get(1), VotesDto.VOTE_DISLIKE);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorAnswers(UID);

        assertEquals(0, qaPager.getData().get(0).getAnswers().get(0).getVotesDto().getDislikeCount());
        assertEquals(0, qaPager.getData().get(0).getAnswers().get(0).getVotesDto().getLikeCount());
        assertEquals(1, qaPager.getData().get(1).getAnswers().get(0).getVotesDto().getDislikeCount());
        assertEquals(0, qaPager.getData().get(1).getAnswers().get(0).getVotesDto().getLikeCount());
        assertEquals(0, qaPager.getData().get(2).getAnswers().get(0).getVotesDto().getDislikeCount());
        assertEquals(1, qaPager.getData().get(2).getAnswers().get(0).getVotesDto().getLikeCount());
        assertEquals(1, qaPager.getData().get(2).getAnswers().get(0).getVotesDto().getUserVote());
    }

    @Test
    public void testGetAuthorAnswerCountByUidWithCleanCacheAfterCreateAnswer() throws Exception {
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, ANOTHER_UID);
        long modelQuestionId = createModelQuestion(MODEL_ID, ANOTHER_UID);

        CountDto before = profileMvc.getAuthorAnswerCount(UID);
        createAnswer(categoryQuestionId, UID);
        createAnswer(modelQuestionId, UID);
        CountDto after = profileMvc.getAuthorAnswerCount(UID);

        assertEquals(before.getCount() + 2, after.getCount());
    }

    @Test
    public void testGetAuthorAnswerCountWithCleanCacheAfterRemoveAnswer() throws Exception {
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, ANOTHER_UID);
        long modelQuestionId = createModelQuestion(MODEL_ID, ANOTHER_UID);
        long categoryAnswerId = createAnswer(categoryQuestionId, UID);
        long modelAnswerId = createAnswer(modelQuestionId, UID);

        CountDto before = profileMvc.getAuthorAnswerCount(UID);
        deleteAnswer(categoryAnswerId, UID, status().is2xxSuccessful());
        deleteAnswer(modelAnswerId, UID, status().is2xxSuccessful());
        CountDto after = profileMvc.getAuthorAnswerCount(UID);

        assertEquals(before.getCount() - 2, after.getCount());
    }

    @Test
    public void testGetAuthorAnswerCountByUidWithAnswersForOwnQuestion() throws Exception {
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, UID);
        long modelQuestionId = createModelQuestion(MODEL_ID, UID);
        createAnswer(categoryQuestionId, UID);
        createAnswer(modelQuestionId, UID);

        CountDto countDto = profileMvc.getAuthorAnswerCount(UID);

        assertEquals(2, countDto.getCount());
    }

    @Test
    public void testGetAuthorAnswerCountByUidWithIgnoringBannedOrRemovedQuestions() throws Exception {
        List<Long> modelQuestions = createQuestionsReturnAll(() -> createModelQuestion(MODEL_ID, ANOTHER_UID));
        List<Long> categoryQuestions = createQuestionsReturnAll(() -> createCategoryQuestion(CATEGORY_HID, ANOTHER_UID));

        for (Long questionId : modelQuestions) {
            createAnswer(questionId, UID);
        }

        for (Long questionId : categoryQuestions) {
            createAnswer(questionId, UID);
        }

        CountDto countDto = profileMvc.getAuthorAnswerCount(UID);
        assertEquals(2 * ModState.PUBLISHED.size(), countDto.getCount());
    }

    @Test
    public void testGetAuthorAnswerCountByUidWithIgnoringBannedOrRemovedAnswers() throws Exception {
        long modelQuestion = createModelQuestion(MODEL_ID, ANOTHER_UID);
        long categoryQuestion = createCategoryQuestion(CATEGORY_HID, ANOTHER_UID);
        createAnswersReturnAll(() -> createAnswer(modelQuestion, UID));
        createAnswersReturnAll(() -> createAnswer(categoryQuestion, UID));

        CountDto countDto = profileMvc.getAuthorAnswerCount(UID);
        assertEquals(2 * ModState.PUBLISHED.size(), countDto.getCount());
    }

    @Test
    public void testGetAuthorAnswerCountByUidWithTwoAnswersToOneQuestion() throws Exception {
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, ANOTHER_UID);
        long modelQuestionId = createModelQuestion(MODEL_ID, ANOTHER_UID);
        createAnswer(categoryQuestionId, UID);
        createAnswer(categoryQuestionId, UID);
        createAnswer(modelQuestionId, UID);
        createAnswer(modelQuestionId, UID);

        CountDto countDto = profileMvc.getAuthorAnswerCount(UID);

        assertEquals(4, countDto.getCount());
    }

    @Test
    public void testGetAuthorAnswerCountByUidWithDifferentUsers() throws Exception {
        long categoryQuestionId = createCategoryQuestion(CATEGORY_HID, UID + 1);
        long modelQuestionId = createModelQuestion(MODEL_ID, UID + 1);
        createAnswer(categoryQuestionId, ANOTHER_UID);
        createAnswer(categoryQuestionId, ANOTHER_UID);
        createAnswer(modelQuestionId, ANOTHER_UID);
        createAnswer(modelQuestionId, ANOTHER_UID);

        createAnswer(categoryQuestionId, UID);
        createAnswer(modelQuestionId, UID);

        CountDto user = profileMvc.getAuthorAnswerCount(UID);
        CountDto anotherUser = profileMvc.getAuthorAnswerCount(ANOTHER_UID);

        assertEquals(2, user.getCount());
        assertEquals(4, anotherUser.getCount());
    }

    @Test
    public void testGetModelQuestionAgitation() throws Exception {
        long currentUser = UID + 31231;

        long[] questionIds = {
            // questions agitated by models from grade
            questionMvc.createModelQuestion(1, UID + 1),
            questionMvc.createModelQuestion(2, currentUser),
            questionMvc.createModelQuestion(2, UID + 1),
            // own question and question from other user with same model
            questionMvc.createModelQuestion(3, currentUser),
            questionMvc.createModelQuestion(3, UID + 2),
            // model for question with own answer and one without answer
            questionMvc.createModelQuestion(4, UID + 1),
            questionMvc.createModelQuestion(4, UID + 2),
            // model to not be agitated
            questionMvc.createModelQuestion(5, UID + 3),
        };

        // answer for one of the questions
        answerMvc.createAnswer(questionIds[5], currentUser);

        Mockito.when(persStaticClient.getUserModels(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        Mockito.when(persStaticClient.getUserModels(eq(currentUser), anyLong())).thenReturn(Arrays.asList(1L, 2L));

        // check full data

        saasMocks.mockModelAgitationCall(currentUser, Arrays.asList(1L, 2L, 3L, 4L), DEF_PAGE_SIZE);
        assertArrayEquals(
            new long[]{
                // desc by id
                // by answer in same model
                questionIds[6],
                // by question in same model
                questionIds[4],
                // by saas response
                questionIds[2],
                questionIds[0],
            },
            profileMvc.getModelQuestionForAgitation(currentUser, DEF_PAGE_SIZE)
                .getData().stream().mapToLong(QuestionDto::getId).toArray()
        );

        // check other user
        saasMocks.mockModelAgitationCall(UID + 1, Arrays.asList(1L, 2L, 4L), DEF_PAGE_SIZE);
        assertArrayEquals(
            new long[]{
                questionIds[6],
                questionIds[1],
            },
            profileMvc.getModelQuestionForAgitation(UID + 1, DEF_PAGE_SIZE)
                .getData().stream().mapToLong(QuestionDto::getId).toArray()
        );
    }

    @Test
    public void testGetModelQuestionAgitationWithOldQuestion() throws Exception {
        long currentUser = UID + 31231;

        long[] questionIds = {
            // questions agitated by models from grade
            questionMvc.createModelQuestion(1, UID + 1),
            questionMvc.createModelQuestion(2, UID + 1),
            questionMvc.createModelQuestion(2, UID + 2),
        };

        // make second question older than threshold
        qaJdbcTemplate.update("UPDATE qa.question SET cr_time = now() - interval '" +
            (DAYS_TO_SHOW_QUESTIONS_FOR_AGITATION + 1) + "' day WHERE id= " + questionIds[1]);
        // make third question just a bit newer than threshold
        qaJdbcTemplate.update("UPDATE qa.question SET cr_time = now() - interval '" +
            (DAYS_TO_SHOW_QUESTIONS_FOR_AGITATION - 1) + "' day WHERE id= " + questionIds[2]);

        Mockito.when(persStaticClient.getUserModels(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        Mockito.when(persStaticClient.getUserModels(eq(currentUser), anyLong())).thenReturn(Arrays.asList(1L, 2L));

        saasMocks.mockModelAgitationCall(currentUser, Arrays.asList(1L, 2L), DEF_PAGE_SIZE);
        assertArrayEquals(
            new long[]{
                questionIds[2],
                questionIds[0],
            },
            profileMvc.getModelQuestionForAgitation(currentUser, DEF_PAGE_SIZE)
                .getData().stream().mapToLong(QuestionDto::getId).toArray()
        );
    }

    @Test
    public void testGetAuthorPublicQuestion() throws Exception {
        List<Long> allQuestions = Arrays.asList(
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID)
        );
        saasMocks.mockAuthorQuestionCall(UID, DEF_PAGE_SIZE, allQuestions);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorPublicQuestionByUser(UID, ANOTHER_UID);

        assertEquals(2, qaPager.getData().size());
        assertEquals(allQuestions.get(0).longValue(), qaPager.getData().get(0).getId());
        assertEquals(allQuestions.get(1).longValue(), qaPager.getData().get(1).getId());
    }

    @Test
    public void testGetAuthorPublicQuestionWithOtherUserLike() throws Exception {
        List<Long> allQuestions = Arrays.asList(
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID)
        );
        saasMocks.mockAuthorQuestionCall(UID, DEF_PAGE_SIZE, allQuestions);
        voteMvc.likeQuestion(ANOTHER_UID, allQuestions.get(2));

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorPublicQuestionByUser(UID, ANOTHER_UID);

        assertEquals(3, qaPager.getData().size());
        assertVotes(qaPager.getData().get(0).getVotesDto(), 0, 0, VotesDto.VOTE_NONE);
        assertVotes(qaPager.getData().get(1).getVotesDto(), 0, 0, VotesDto.VOTE_NONE);
        assertVotes(qaPager.getData().get(2).getVotesDto(), 1, 0, VotesDto.VOTE_LIKE);
    }

    private void assertVotes(VotesDto voteDto, long likeCount, long dislikeCount, long userVote) {
        assertEquals(likeCount, voteDto.getLikeCount());
        assertEquals(dislikeCount, voteDto.getDislikeCount());
        assertEquals(userVote, voteDto.getUserVote());
    }

    @Test
    public void testGetAuthorPublicQuestionByYandexUidWithVotes() throws Exception {
        List<Long> allQuestions = Arrays.asList(
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID)
        );
        saasMocks.mockAuthorQuestionCall(UID, DEF_PAGE_SIZE, allQuestions);
        voteMvc.likeQuestion(ANOTHER_UID, allQuestions.get(0));
        voteMvc.likeQuestion(ANOTHER_UID, allQuestions.get(2));

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorPublicQuestionByYandexUid(UID, YANDEXUID);

        assertEquals(3, qaPager.getData().size());
        assertVotes(qaPager.getData().get(0).getVotesDto(), 1, 0, VotesDto.VOTE_NONE);
        assertVotes(qaPager.getData().get(1).getVotesDto(), 0, 0, VotesDto.VOTE_NONE);
        assertVotes(qaPager.getData().get(2).getVotesDto(), 1, 0, VotesDto.VOTE_NONE);
    }

    @Test
    public void testGetAuthorPublicQuestionWithOtherUserQuestion() throws Exception {
        List<Long> userQuestions = Arrays.asList(
                createModelQuestion(MODEL_ID, UID)
        );
        List<Long> anotherUserQuestions = Arrays.asList(
                createModelQuestion(MODEL_ID, ANOTHER_UID),
                createModelQuestion(MODEL_ID, ANOTHER_UID)
        );
        saasMocks.mockAuthorQuestionCall(UID, DEF_PAGE_SIZE, userQuestions);
        saasMocks.mockAuthorQuestionCall(ANOTHER_UID, DEF_PAGE_SIZE, anotherUserQuestions);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorPublicQuestionByUser(UID, UID + 1);
        assertEquals(1, qaPager.getData().size());
        assertUserQuestion(qaPager.getData().get(0), UID, userQuestions.get(0));

        QAPager<QuestionDto> qaPagerAnother = profileMvc.getAuthorPublicQuestionByUser(ANOTHER_UID, UID + 1);
        assertEquals(2, qaPagerAnother.getData().size());
        assertUserQuestion(qaPagerAnother.getData().get(0), ANOTHER_UID, anotherUserQuestions.get(0));
        assertUserQuestion(qaPagerAnother.getData().get(1), ANOTHER_UID, anotherUserQuestions.get(1));
    }

    private void assertUserQuestion(QuestionDto questionDto, long userId, long questionId) {
        assertEquals(userId, questionDto.getUserDto().getUserId());
        assertEquals(questionId, questionDto.getId());
    }

    @Test
    public void testGetAuthorPublicQuestionWithPager() throws Exception {
        long pageSize = 3;
        List<Long> allQuestions = Arrays.asList(
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID)
        );
        saasMocks.mockAuthorQuestionCall(UID, pageSize, allQuestions);

        QAPager<QuestionDto> firstPage = profileMvc.getAuthorPublicQuestionByUser(UID, UID + 1, 1, pageSize);
        QAPager<QuestionDto> secondPage = profileMvc.getAuthorPublicQuestionByUser(UID, UID + 1, 2, pageSize);

        assertEquals(3, firstPage.getData().size());
        assertEquals(2, secondPage.getData().size());
        assertEquals(2, firstPage.getPager().getTotalPageCount());
        assertEquals(2, secondPage.getPager().getTotalPageCount());
    }

    @Test
    public void testGetAuthorPublicQuestionByYandexUidWithPager() throws Exception {
        long pageSize = 3;
        List<Long> allQuestions = Arrays.asList(
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID)
        );
        saasMocks.mockAuthorQuestionCall(UID, pageSize, allQuestions);

        QAPager<QuestionDto> result = profileMvc.getAuthorPublicQuestionByYandexUid(UID, YANDEXUID, 1, pageSize);
        assertEquals(3, result.getData().size());
        assertEquals(2, result.getPager().getTotalPageCount());
        assertUserQuestion(result.getData().get(0), UID, allQuestions.get(0));
        assertUserQuestion(result.getData().get(1), UID, allQuestions.get(1));
        assertUserQuestion(result.getData().get(2), UID, allQuestions.get(2));

        result = profileMvc.getAuthorPublicQuestionByYandexUid(UID, YANDEXUID, 2, pageSize);
        assertEquals(2, result.getData().size());
        assertEquals(2, result.getPager().getTotalPageCount());
        assertUserQuestion(result.getData().get(0), UID, allQuestions.get(3));
        assertUserQuestion(result.getData().get(1), UID, allQuestions.get(4));
    }

    @Test
    public void testGetAuthorPublicQuestionWithTwoYandexUid() throws Exception {
        List<Long> userQuestions = Arrays.asList(
                createModelQuestion(MODEL_ID, UID),
                createModelQuestion(MODEL_ID, UID)
        );
        saasMocks.mockAuthorQuestionCall(UID, DEF_PAGE_SIZE, userQuestions);

        QAPager<QuestionDto> result = profileMvc.getAuthorPublicQuestionByYandexUid(UID, YANDEXUID);
        assertEquals(2, result.getData().size());
        assertUserQuestion(result.getData().get(0), UID, userQuestions.get(0));
        assertUserQuestion(result.getData().get(1), UID, userQuestions.get(1));

        result = profileMvc.getAuthorPublicQuestionByYandexUid(UID, YANDEXUID + 1);
        assertEquals(2, result.getData().size());
        assertUserQuestion(result.getData().get(0), UID, userQuestions.get(0));
        assertUserQuestion(result.getData().get(1), UID, userQuestions.get(1));
    }

    @Test
    public void testGetAuthorPublicAnswers() throws Exception {
        long questionId = createModelQuestion(MODEL_ID, UID + 1);
        List<Long> userAnswerIds = buildUserAnswers(questionId, UID, 2);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorPublicAnswerByUid(UID, ANOTHER_UID);

        assertEquals(2, qaPager.getData().size());
        assertQuestionDto(qaPager.getData().get(0), questionId, userAnswerIds.get(1), "Answer-1");
        assertQuestionDto(qaPager.getData().get(1), questionId, userAnswerIds.get(0), "Answer-0");
    }

    @Test
    public void testGetAuthorPublicAnswersWithEmptyAnswers() throws Exception {

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorPublicAnswerByUid(UID, ANOTHER_UID);

        assertEquals(0, qaPager.getData().size());
    }

    @Test
    public void testGetAuthorPublicAnswersWithDeletedQuestion() throws Exception {
        long questionId = createModelQuestion(MODEL_ID, UID + 1);
        long deletedQuestionId = createModelQuestion(MODEL_ID, UID);
        deleteQuestion(deletedQuestionId);
        List<Long> userAnswerIds = buildUserAnswers(questionId, UID, 3);
        userAnswerIds.addAll(buildUserAnswers(deletedQuestionId, UID, 1));

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorPublicAnswerByUid(UID, ANOTHER_UID);

        assertEquals(3, qaPager.getData().size());
        assertQuestionDto(qaPager.getData().get(0), questionId, userAnswerIds.get(2), "Answer-2");
        assertQuestionDto(qaPager.getData().get(1), questionId, userAnswerIds.get(1), "Answer-1");
        assertQuestionDto(qaPager.getData().get(2), questionId, userAnswerIds.get(0), "Answer-0");
    }

    @Test
    public void testGetAuthorPublicAnswersWithIgnoringBannedOrRemovedQuestions() throws Exception {
        List<Long> modelQuestions = createQuestionsReturnAll(() -> createModelQuestion(MODEL_ID, ANOTHER_UID));
        List<Long> categoryQuestions = createQuestionsReturnAll(() -> createCategoryQuestion(CATEGORY_HID, ANOTHER_UID));

        for (Long questionId : modelQuestions) {
            createAnswer(questionId, UID);
        }

        for (Long questionId : categoryQuestions) {
            createAnswer(questionId, UID);
        }

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorPublicAnswerByUid(UID, ANOTHER_UID, 1, 40);
        assertEquals(2 * ModState.PUBLISHED.size(), qaPager.getData().size());
    }

    @Test
    public void testGetAuthorPublicAnswersWithoutShopAndBrandAnswers() throws Exception {
        long questionId = createModelQuestion(MODEL_ID, UID + 1);
        createVendorAnswer(123, questionId, "Вендорский ответ");
        createPartnerShopAnswer(1234, questionId, "Ответ магазина");
        long userAnswerId = createAnswer(questionId);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorPublicAnswerByUid(UID, ANOTHER_UID, 1, 40);
        Assertions.assertEquals(1, qaPager.getData().size());
        Assertions.assertEquals(questionId, qaPager.getData().get(0).getId());
        Assertions.assertEquals(3, qaPager.getData().get(0).getAnswersCount());
        Assertions.assertEquals(userAnswerId, qaPager.getData().get(0).getAnswers().get(0).getAnswerId());
    }


    @Test
    public void testGetAuthorPublicAnswersWithPager() throws Exception {
        List<Long> questionIds = Arrays.asList(
                createModelQuestion(MODEL_ID, ANOTHER_UID),
                createCategoryQuestion(CATEGORY_HID, ANOTHER_UID)
        );
        List<Long> userAnswerIds = buildUserAnswers(questionIds.get(0), UID, 2);
        userAnswerIds.addAll(buildUserAnswers(questionIds.get(1), UID, 2));

        QAPager<QuestionDto> firstPage = profileMvc.getAuthorPublicAnswerByUid(UID, ANOTHER_UID, 1, 3);
        QAPager<QuestionDto> secondPage = profileMvc.getAuthorPublicAnswerByUid(UID, ANOTHER_UID, 2, 3);

        assertEquals(3, firstPage.getData().size());
        assertEquals(1, secondPage.getData().size());
        assertEquals(2, firstPage.getPager().getTotalPageCount());
        assertEquals(2, secondPage.getPager().getTotalPageCount());
    }

    private void assertQuestionDto(QuestionDto questionDto, long expectedQuestionId, long expextedAnswerId, String expectedAnswerText) {
        assertEquals(expectedQuestionId, questionDto.getId());
        assertEquals(1, questionDto.getAnswers().size());
        assertEquals(expextedAnswerId, questionDto.getAnswers().get(0).getId());
        assertEquals(expectedAnswerText, questionDto.getAnswers().get(0).getText());
    }

    @Test
    public void testGetAuthorPublicAnswersWithVotes() throws Exception {
        long questionId = createModelQuestion(MODEL_ID, ANOTHER_UID);
        List<Long> userAnswerIds = buildUserAnswers(questionId, UID, 3);
        Collections.reverse(userAnswerIds);

        voteMvc.voteAnswer(ANOTHER_UID, userAnswerIds.get(2), VotesDto.VOTE_LIKE);
        voteMvc.voteAnswer(UID + 1, userAnswerIds.get(1), VotesDto.VOTE_DISLIKE);

        QAPager<QuestionDto> qaPager = profileMvc.getAuthorPublicAnswerByUid(UID, ANOTHER_UID);

        assertVotes(qaPager.getData().get(0).getAnswers().get(0).getVotesDto(), 0 , 0, 0);
        assertVotes(qaPager.getData().get(1).getAnswers().get(0).getVotesDto(), 0 , 1, 0);
        assertVotes(qaPager.getData().get(2).getAnswers().get(0).getVotesDto(), 1 , 0, 1);
    }

    @Test
    public void testGetAuthorPublicAnswersCount() throws Exception {
        List<Long> questionIds = Arrays.asList(
                createModelQuestion(MODEL_ID, ANOTHER_UID),
                createCategoryQuestion(CATEGORY_HID, ANOTHER_UID)
        );

        List<Long> userAnswerIds = buildUserAnswers(questionIds.get(0), UID, 4);
        userAnswerIds.addAll(buildUserAnswers(questionIds.get(1), UID, 3));

        CountDto countDto = profileMvc.getAuthorPublicAnswerCount(UID);

        assertEquals(7, countDto.getCount());
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
}
