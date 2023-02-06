package ru.yandex.market.pers.qa.controller.api.system;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.controller.dto.takeout.TakeoutAnswerDto;
import ru.yandex.market.pers.qa.controller.dto.takeout.TakeoutCommentDto;
import ru.yandex.market.pers.qa.controller.dto.takeout.TakeoutComplaintDto;
import ru.yandex.market.pers.qa.controller.dto.takeout.TakeoutDataWrapper;
import ru.yandex.market.pers.qa.controller.dto.takeout.TakeoutQuestionDto;
import ru.yandex.market.pers.qa.controller.dto.takeout.TakeoutResponseDto;
import ru.yandex.market.pers.qa.controller.dto.takeout.TakeoutVoteDto;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.AnswerFilter;
import ru.yandex.market.pers.qa.model.Comment;
import ru.yandex.market.pers.qa.model.CommentFilter;
import ru.yandex.market.pers.qa.model.Complaint;
import ru.yandex.market.pers.qa.model.DateFilter;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.QuestionFilter;
import ru.yandex.market.pers.qa.model.Vote;
import ru.yandex.market.pers.qa.model.VoteValueType;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.service.ComplaintService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.VoteService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author vvolokh
 * 31.01.2019
 */
public class TakeoutControllerTest extends ControllerTest {

    @Autowired
    private QuestionService questionService;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private VoteService voteService;
    @Autowired
    private ComplaintService complaintService;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private Map<Integer, String> complaintReasons;

    private long anotherUid = UID + 1;
    private long yetAnotherUid = anotherUid + 1;

    @BeforeEach
    public void setup() {
        complaintReasons = complaintService.getComplaintReasons();
    }

    @Test
    public void testGetTakeout() throws Exception {
        long questionId = createQuestion(MODEL_ID, UID, "text");
        long answerId = createAnswer(questionId, UID);
        long commentId = commentService.createComment(CommentProject.QA, UID, "test", answerId);
        long articleCommentId = commentService.createComment(CommentProject.ARTICLE, UID, "test", answerId);
        long gradeCommentId = commentService.createComment(CommentProject.GRADE, UID, "test", answerId);
        createVotes(UID, questionId, answerId, commentId);
        createComplaints(UID, questionId, answerId, commentId);

        long anotherUserQuestionId = createQuestion(MODEL_ID, anotherUid, "text");
        long anotherUserAnswerId = createAnswer(questionId, anotherUid);
        long anotherUserCommentId = commentService.createComment(CommentProject.QA, anotherUid, "test", answerId);
        createVotes(UID + 1, anotherUserQuestionId, anotherUserAnswerId, anotherUserCommentId);
        createComplaints(UID + 1, anotherUserQuestionId, anotherUserAnswerId, anotherUserCommentId);

        TakeoutResponseDto responseDto = getTakeoutResponseDto(UID);

        List<Question> questions = questionService.getQuestions(new QuestionFilter().id(questionId));
        List<Answer> answers = answerService.getAnswers(new AnswerFilter().id(answerId));
        List<Comment> comments = commentService.getComments(new CommentFilter().idList(Arrays.asList(commentId, articleCommentId, gradeCommentId)));
        List<Vote> votes = voteService.getAllUserVotes(UID);
        List<Complaint> complaints = complaintService.getComplaintsByUserId(UID);
        verifyQuestionList(questions, responseDto.getQuestions());
        verifyAnswerList(answers, responseDto.getAnswers());
        verifyCommentList(comments, responseDto.getComments());
        verifyVotesList(votes, responseDto.getVotes());
        verifyComplaintsList(complaints, responseDto.getComplaints());
    }

    @Test public void testGetAnswersStatusAndDelete() throws Exception {
        long question = createQuestion("question");
        answerService.createAnswer(this.anotherUid, "text", question);
        deleteData(yetAnotherUid);

        getNotEmptyStatus(UID);

        getNotEmptyStatus(anotherUid);
        deleteData(anotherUid);
        getEmptyStatus(anotherUid);

        getNotEmptyStatus(UID);
    }

    @Test public void testGetQuestionsStatusAndDelete() throws Exception {
        createQuestion("question");
        deleteData(yetAnotherUid);

        getNotEmptyStatus(UID);
        deleteData(UID);
        getEmptyStatus(UID);
    }
    @Test public void testGetVotesStatusAndDelete() throws Exception {
        long question = createQuestion("question");
        voteService.createQuestionLike(question, anotherUid);
        deleteData(yetAnotherUid);

        getNotEmptyStatus(UID);
        questionService.deleteQuestion(question, UID);
        getEmptyStatus(UID);

        getNotEmptyStatus(anotherUid);
        deleteData(anotherUid);
        getEmptyStatus(anotherUid);
    }
    @Test public void testGetComplaintsStatusAndDelete() throws Exception {

        long question = createQuestion("question");
        complaintService.createComplaint(UserType.UID, String.valueOf(anotherUid), QaEntityType.QUESTION, question, 0,
                "text");
        deleteData(yetAnotherUid);

        getNotEmptyStatus(UID);

        getNotEmptyStatus(anotherUid);
        deleteData(anotherUid);
        getEmptyStatus(anotherUid);

        getNotEmptyStatus(UID);
    }

    @Test public void testGetCommentsStatusAndDelete() throws Exception {

        long question = createQuestion("question");
        long answer = createAnswer(question, UID);

        commentService.createComment(CommentProject.QA, anotherUid, "test", answer);
        deleteData(yetAnotherUid);

        getNotEmptyStatus(UID);

        getNotEmptyStatus(anotherUid);
        deleteData(anotherUid);
        getEmptyStatus(anotherUid);

        getNotEmptyStatus(UID);
    }
    @Test public void testGetEmptyStatusAndDelete() throws Exception {
        getEmptyStatus(UID);
        deleteData(UID);
        getEmptyStatus(UID);
    }

    @Test
    public void testGetCommentsWithoutVendor() throws Exception {
        long vendorId = 999L;
        long question = createQuestion("question");
        long vendorAnswer = createVendorAnswer(vendorId, UID, question, "test");

        // создаем ответ пользхователя и выставляем дату, чтобы комментарий удалился
        long answer = createAnswer(question, UID);
        setAnswerTime(Date.from(Instant.now().minus(2, ChronoUnit.DAYS)), answer);

        long commentId = commentService.createComment(CommentProject.QA, UID, "test", vendorAnswer);
        long commentId2 = commentService.createComment(CommentProject.QA, UID, "test_tesst", answer);
        long vendorComment = commentService.createVendorComment(CommentProject.QA, UID, "text", answer, vendorId);

        // коммент через кабинет вендора не должен попасть в выдачу
        TakeoutResponseDto responseDto = getTakeoutResponseDto(UID);
        assertEquals(2, responseDto.getComments().size());

        answerService.deleteAnswer(answer, UID);
        answerService.deleteHardAnswersByFilter(getAnswerDeleteHardFilter(UID, Date.from(Instant.now().minus(1, ChronoUnit.DAYS))));

        //просто удаляем данные и ожидаем, что ничего не упадет
        deleteData(UID);
    }

    private void setAnswerTime(Date date, long answerId) {
        jdbcTemplate.update("update qa.answer set cr_time = ? where id = ?", date, answerId);
    }

    private AnswerFilter getAnswerDeleteHardFilter(Long userId, Date dateTo) {
        return new AnswerFilter()
            .authorUid(userId)
            .dateFilter(new DateFilter(null, dateTo))
            .fromReplica()
            .allowsNonPublic();
    }

    private void getEmptyStatus(long uid) throws Exception {
        invokeAndRetrieveResponse(
                get("/takeout/status?uid=" + uid),
                matchAll(status().isOk(),
                        content().json("{\"types\":[]}")));
    }

    private void deleteData(long uid) throws Exception {
        invokeAndRetrieveResponse(
                post("/takeout/delete?types=qa&uid=" + uid),
                matchAll(status().isOk(),
                        content().string("")));
    }

    private void getNotEmptyStatus(long uid) throws Exception {
        invokeAndRetrieveResponse(
                get("/takeout/status?uid=" + uid),
                matchAll(status().isOk(),
                content().json("{\"types\":[\"qa\"]}")));
    }

    private TakeoutResponseDto getTakeoutResponseDto(long uid) throws Exception {
        String response = invokeAndRetrieveResponse(get("/takeout?color=white&uid=" + uid), status().is2xxSuccessful());
        TakeoutDataWrapper takeoutData = objectMapper.readValue(response, TakeoutDataWrapper.class);
        return takeoutData.getTakeoutData();
    }

    private void createVotes(long uid, long questionId, long answerId, long commentId) {
        voteService.createCommentVote(commentId, uid, VoteValueType.LIKE, QaEntityType.COMMENT);
        voteService.createAnswerVote(answerId, uid, VoteValueType.LIKE);
        voteService.createQuestionLike(questionId, uid);
    }

    private void createComplaints(long uid, long questionId, long answerId, long commentId) {
        complaintService.createComplaint(UserType.UID, String.valueOf(uid), QaEntityType.COMMENT, commentId, 0, "text");
        complaintService.createComplaint(UserType.UID, String.valueOf(uid), QaEntityType.ANSWER, answerId, 0, "text");
        complaintService.createComplaint(UserType.UID, String.valueOf(uid), QaEntityType.QUESTION, questionId, 0,
                "text");
    }

    private <T, R> void verifyEntityList(
            List<T> expectedEntities,
            List<R> testedEntities,
            Comparator<T> expectedSortComparator,
            Comparator<R> testedSortComparator,
            BiConsumer<T, R> verificator) {
        assertEquals(expectedEntities.size(), testedEntities.size());

        expectedEntities.sort(expectedSortComparator);
        testedEntities.sort(testedSortComparator);

        for (int i = 0; i < expectedEntities.size(); i++) {
            verificator.accept(expectedEntities.get(i), testedEntities.get(i));
        }
    }

    private void verifyQuestionList(List<Question> questions, List<TakeoutQuestionDto> questionDtoList) {
        verifyEntityList(questions,
            questionDtoList,
            Comparator.comparing(Question::getId),
            Comparator.comparingLong(TakeoutQuestionDto::getId),
            this::verifyQuestion);
    }

    private void verifyQuestion(Question question, TakeoutQuestionDto takeoutQuestionDto) {
        assertEquals((long) question.getId(), takeoutQuestionDto.getId());
        assertEquals(question.getEntityId(), takeoutQuestionDto.getEntityId());
        assertEquals(convertToIsoLocalDateTime(question.getTimestamp()), takeoutQuestionDto.getCreated());
        assertEquals(question.getQuestionType().name(), takeoutQuestionDto.getQuestionType());
        assertEquals(question.getText(), takeoutQuestionDto.getText());
    }

    private void verifyAnswerList(List<Answer> answers, List<TakeoutAnswerDto> answerDtoList) {
        verifyEntityList(answers,
            answerDtoList,
            Comparator.comparing(Answer::getId),
            Comparator.comparingLong(TakeoutAnswerDto::getId),
            this::verifyAnswer);
    }

    private void verifyAnswer(Answer answer, TakeoutAnswerDto takeoutAnswerDto) {
        assertEquals(answer.getId(), takeoutAnswerDto.getId());
        assertEquals(answer.getQuestionId(), takeoutAnswerDto.getQuestionId());
        assertEquals(convertToIsoLocalDateTime(answer.getTimestamp()), takeoutAnswerDto.getCreated());
        assertEquals(answer.getText(), takeoutAnswerDto.getText());
    }

    private void verifyCommentList(List<Comment> comments, List<TakeoutCommentDto> commentDtoList) {
        verifyEntityList(comments,
            commentDtoList,
            Comparator.comparing(Comment::getId),
            Comparator.comparingLong(TakeoutCommentDto::getId),
            this::verifyComment);
    }

    private void verifyComment(Comment comment, TakeoutCommentDto takeoutCommentDto) {
        assertEquals(comment.getId().longValue(), takeoutCommentDto.getId());
        assertEquals(CommentProject.getByProjectId(comment.getProjectId()).getName(),
            takeoutCommentDto.getCommentProject());
        assertEquals(convertToIsoLocalDateTime(comment.getUpdateTime()), takeoutCommentDto.getCreated());
        assertEquals(comment.getText(), takeoutCommentDto.getText());
    }

    private void verifyVotesList(List<Vote> votes, List<TakeoutVoteDto> voteDtoList) {
        verifyEntityList(votes,
            voteDtoList,
            Comparator.comparing(Vote::getId),
            Comparator.comparingLong(TakeoutVoteDto::getId),
            this::verifyVote);
    }

    private void verifyVote(Vote vote, TakeoutVoteDto takeoutVoteDto) {
        assertEquals(vote.getId(), takeoutVoteDto.getId());
        assertEquals(vote.getEntityId(), takeoutVoteDto.getEntityId());
        assertEquals(vote.getEntityType().getSimpleName(), takeoutVoteDto.getEntityType());
        assertEquals(vote.getVoteValue().getValue(), takeoutVoteDto.getVote());
    }

    private void verifyComplaintsList(List<Complaint> complaints, List<TakeoutComplaintDto> complaintDtoList) {
        verifyEntityList(complaints,
            complaintDtoList,
            Comparator.comparing(Complaint::getId),
            Comparator.comparingLong(TakeoutComplaintDto::getId),
            this::verifyComplaint);
    }

    private void verifyComplaint(Complaint complaint, TakeoutComplaintDto takeoutComplaintDto) {
        assertEquals(complaint.getId(), takeoutComplaintDto.getId());
        assertEquals(complaint.getEntityId(), takeoutComplaintDto.getEntityId());
        assertEquals(complaint.getEntityType().getSimpleName(), takeoutComplaintDto.getEntityType());
        assertEquals(complaintReasons.get(complaint.getReasonId()), takeoutComplaintDto.getReason());
        assertEquals(complaint.getUserComplaintText(), takeoutComplaintDto.getText());
    }


}
