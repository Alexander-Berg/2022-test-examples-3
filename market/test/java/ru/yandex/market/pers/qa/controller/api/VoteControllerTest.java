package ru.yandex.market.pers.qa.controller.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.qa.client.dto.EntityVotesDto;
import ru.yandex.market.pers.qa.client.dto.VotesDto;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.AnswerDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.controller.dto.post.PostV2Dto;
import ru.yandex.market.pers.qa.mock.mvc.PostV2MvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.VoteMvcMocks;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityFeature;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.VoteStat;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.VoteService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.service.VoteService.DISLIKE_QUESTION_ERROR_MESSAGE;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 09.10.2018
 */
public class VoteControllerTest extends QAControllerTest {

    @Autowired
    private VoteService voteService;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private VoteMvcMocks voteMvc;
    @Autowired
    public PostV2MvcMocks postV2MvcMocks;

    @Test
    void testDirectCallDefaultsWorksWithQuestions() throws Exception {
        final long modelId = 3451;

        final long questionId = createQuestion(modelId);
        final long questionIdFictional = -1000;

        final List<Long> questionIds = Arrays.asList(questionId, questionIdFictional);

        // check works with default values
        Map<Long, VoteStat> votes = voteService.getVotesByIdLongList(QaEntityType.QUESTION, questionIds);
        assertEquals(0, votes.get(questionId).getLikesCount());
        assertEquals(0, votes.get(questionIdFictional).getLikesCount());

        // check works after likes
        voteMvc.likeQuestion(UID, questionId);

        votes = voteService.getVotesByIdLongList(QaEntityType.QUESTION, questionIds);
        assertEquals(1, votes.get(questionId).getLikesCount());
        assertEquals(0, votes.get(questionIdFictional).getLikesCount());

        // and works after likes are removed
        voteMvc.deleteQuestionVote(UID, questionId);

        votes = voteService.getVotesByIdLongList(QaEntityType.QUESTION, questionIds);
        assertEquals(0, votes.get(questionId).getLikesCount());
        assertEquals(0, votes.get(questionIdFictional).getLikesCount());
    }

    @Test
    void testVotesBasicWorkWithQuestions() throws Exception {
        final long modelId = 3451;

        final long questionIdNoVotes = createQuestion(modelId);
        final long questionIdWithVotes = createQuestion(modelId);

        // check votes applied
        voteMvc.likeQuestion(UID, questionIdWithVotes);

        Map<Long, QuestionDto> questions = map(getModelQuestions(modelId));
        assertEquals(0, questions.get(questionIdNoVotes).getVotesDto().getLikeCount());
        assertEquals(1, questions.get(questionIdWithVotes).getVotesDto().getLikeCount());

        // check votes can be changed
        voteMvc.likeQuestion(UID, questionIdNoVotes);
        voteMvc.deleteQuestionVote(UID, questionIdWithVotes);

        questions = map(getModelQuestions(modelId));
        assertEquals(1, questions.get(questionIdNoVotes).getVotesDto().getLikeCount());
        assertEquals(0, questions.get(questionIdWithVotes).getVotesDto().getLikeCount());
    }

    @Test
    void testVotesWorksOnDeletedQuestions() throws Exception {
        final long modelId = 3451;

        final long questionIdRemoved = createQuestion(modelId);
        final long questionIdBanned = createQuestion(modelId);

        questionService.deleteQuestion(questionIdRemoved, UID);
        questionService.forceUpdateModState(questionIdRemoved, ModState.TOLOKA_REJECTED);

        // should work ok
        voteMvc.likeQuestion(UID, questionIdRemoved);
        voteMvc.likeQuestion(UID, questionIdBanned);

        final List<Long> questionIds = Arrays.asList(questionIdRemoved, questionIdBanned);
        Map<Long, VoteStat> votes = voteService.getVotesByIdLongList(QaEntityType.QUESTION, questionIds);

        assertEquals(1, votes.get(questionIdRemoved).getLikesCount());
        assertEquals(1, votes.get(questionIdBanned).getLikesCount());
    }

    @Test
    void testDirectCallDefaultsWorksWithPosts() throws Exception {

        final long postId = postMvcMocks.createInterestPost();
        final long postIdFictional = -1000;

        final List<Long> postIds = Arrays.asList(postId, postIdFictional);

        // check works with default values
        Map<Long, VoteStat> votes = voteService.getVotesByIdLongList(QaEntityType.QUESTION, postIds);

        assertEquals(0, votes.get(postId).getLikesCount());
        assertEquals(0, votes.get(postId).getDislikesCount());
        assertEquals(0, votes.get(postIdFictional).getLikesCount());
        assertEquals(0, votes.get(postIdFictional).getDislikesCount());

        // check works after likes
        voteMvc.votePost(UID, postId, VotesDto.VOTE_LIKE);

        votes = voteService.getVotesByIdLongList(QaEntityType.QUESTION, postIds);
        assertEquals(1, votes.get(postId).getLikesCount());
        assertEquals(0, votes.get(postId).getDislikesCount());
        assertEquals(0, votes.get(postIdFictional).getLikesCount());
        assertEquals(0, votes.get(postIdFictional).getDislikesCount());

        // check works after dislikes
        voteMvc.votePost(UID, postId, VotesDto.VOTE_DISLIKE);

        votes = voteService.getVotesByIdLongList(QaEntityType.QUESTION, postIds);
        assertEquals(0, votes.get(postId).getLikesCount());
        assertEquals(1, votes.get(postId).getDislikesCount());
        assertEquals(0, votes.get(postIdFictional).getLikesCount());
        assertEquals(0, votes.get(postIdFictional).getDislikesCount());

        // and works after votes are removed
        voteMvc.deletePostVote(UID, postId);

        votes = voteService.getVotesByIdLongList(QaEntityType.QUESTION, postIds);
        assertEquals(0, votes.get(postId).getLikesCount());
        assertEquals(0, votes.get(postId).getDislikesCount());
        assertEquals(0, votes.get(postIdFictional).getLikesCount());
        assertEquals(0, votes.get(postIdFictional).getDislikesCount());
    }

    @Test
    void testVotesBasicWorkWihPosts() throws Exception {

        final long postIdNoVotes = postMvcMocks.createInterestPost();
        final long postIdWithLike = postMvcMocks.createInterestPost();
        final long postIdWithDislike = postMvcMocks.createInterestPost();

        // check votes applied
        voteMvc.votePost(UID, postIdWithLike, VotesDto.VOTE_LIKE);
        voteMvc.votePost(UID, postIdWithDislike, VotesDto.VOTE_DISLIKE);

        Map<Long, QuestionDto> posts = postMvcMocks.getPostsMap(UID, List.of(postIdNoVotes, postIdWithLike, postIdWithDislike));
        assertEquals(0, posts.get(postIdNoVotes).getVotesDto().getLikeCount());
        assertEquals(0, posts.get(postIdNoVotes).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, posts.get(postIdNoVotes).getVotesDto().getUserVote());

        assertEquals(1, posts.get(postIdWithLike).getVotesDto().getLikeCount());
        assertEquals(0, posts.get(postIdWithLike).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_LIKE, posts.get(postIdWithLike).getVotesDto().getUserVote());

        assertEquals(0, posts.get(postIdWithDislike).getVotesDto().getLikeCount());
        assertEquals(1, posts.get(postIdWithDislike).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_DISLIKE, posts.get(postIdWithDislike).getVotesDto().getUserVote());

        // check votes can be changed
        voteMvc.votePost(UID, postIdNoVotes, VotesDto.VOTE_DISLIKE);
        voteMvc.deletePostVote(UID, postIdWithLike);
        voteMvc.votePost(UID, postIdWithDislike, VotesDto.VOTE_LIKE);

        posts = postMvcMocks.getPostsMap(UID, List.of(postIdNoVotes, postIdWithLike, postIdWithDislike));
        assertEquals(0, posts.get(postIdNoVotes).getVotesDto().getLikeCount());
        assertEquals(1, posts.get(postIdNoVotes).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_DISLIKE, posts.get(postIdNoVotes).getVotesDto().getUserVote());

        assertEquals(0, posts.get(postIdWithLike).getVotesDto().getLikeCount());
        assertEquals(0, posts.get(postIdWithLike).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, posts.get(postIdWithLike).getVotesDto().getUserVote());

        assertEquals(1, posts.get(postIdWithDislike).getVotesDto().getLikeCount());
        assertEquals(0, posts.get(postIdWithDislike).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_LIKE, posts.get(postIdWithDislike).getVotesDto().getUserVote());
    }

    @Test
    public void testVotesBasicWorksWithPostV2() throws Exception {
        final long postIdNoVotes = postV2MvcMocks.createBrandPostV2();
        final long postIdWithLike = postV2MvcMocks.createBrandPostV2();
        //force approve post mod state
        qaJdbcTemplate.update("update post.post set mod_state = ? where id in (?, ?)",
            ModState.CONFIRMED.getValue(), postIdNoVotes, postIdWithLike);

        //like post
        voteMvc.likePostV2(UID, postIdWithLike);

        PostV2Dto postWithoutVotes = postV2MvcMocks.getPostById(postIdNoVotes, UID);
        assertEquals(0, postWithoutVotes.getVotes().getLikeCount());
        assertEquals(0, postWithoutVotes.getVotes().getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, postWithoutVotes.getVotes().getUserVote());

        PostV2Dto postWithLike = postV2MvcMocks.getPostById(postIdWithLike, UID);
        assertEquals(1, postWithLike.getVotes().getLikeCount());
        assertEquals(0, postWithLike.getVotes().getDislikeCount());
        assertEquals(VotesDto.VOTE_LIKE, postWithLike.getVotes().getUserVote());

        //remove post like
        voteMvc.deletePostV2Like(UID, postIdWithLike);

        postWithoutVotes = postV2MvcMocks.getPostById(postIdNoVotes, UID);
        assertEquals(0, postWithoutVotes.getVotes().getLikeCount());
        assertEquals(0, postWithoutVotes.getVotes().getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, postWithoutVotes.getVotes().getUserVote());

        postWithLike = postV2MvcMocks.getPostById(postIdWithLike, UID);
        assertEquals(0, postWithLike.getVotes().getLikeCount());
        assertEquals(0, postWithLike.getVotes().getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, postWithLike.getVotes().getUserVote());
    }

    @Test
    void testVotesWorksOnDeletedPosts() throws Exception {

        final long postIdRemoved = postMvcMocks.createInterestPost();
        final long postIdBanned = postMvcMocks.createInterestPost();

        questionService.deleteQuestion(postIdRemoved, UID);
        questionService.forceUpdateModState(postIdRemoved, ModState.TOLOKA_REJECTED);

        // should work ok
        voteMvc.votePost(UID, postIdRemoved, VotesDto.VOTE_LIKE);
        voteMvc.votePost(UID, postIdBanned, VotesDto.VOTE_DISLIKE);

        final List<Long> postIds = Arrays.asList(postIdRemoved, postIdBanned);
        Map<Long, VoteStat> votes = voteService.getVotesByIdLongList(QaEntityType.QUESTION, postIds);

        assertEquals(1, votes.get(postIdRemoved).getLikesCount());
        assertEquals(0, votes.get(postIdRemoved).getDislikesCount());

        assertEquals(0, votes.get(postIdBanned).getLikesCount());
        assertEquals(1, votes.get(postIdBanned).getDislikesCount());
    }

    @Test
    void testDislikeQuestionTroughPostEndpointFails() throws Exception {
        final long questionId = createQuestion();

        voteMvc.votePost(UID, questionId, VotesDto.VOTE_DISLIKE, status().is4xxClientError());

        ResultMatcher expect = content().json("{\"result\":2,\"code\":\"VALIDATION_ERROR\",\"http_status\":400," +
                "\"error\":\"" + DISLIKE_QUESTION_ERROR_MESSAGE + "\"}");
        voteMvc.votePost(UID, questionId, VotesDto.VOTE_DISLIKE, expect);
    }

    @Test
    void testDirectCallDefaultsWorksWithAnswers() throws Exception {
        final long questionId = createQuestion();

        final long answerId = createAnswer(questionId);
        final long answerIdFictional = -1000;

        final List<Long> answerIds = Arrays.asList(answerId, answerIdFictional);

        // check works with default values
        Map<Long, VoteStat> votes = voteService.getVotesByIdLongList(QaEntityType.ANSWER, answerIds);

        assertEquals(0, votes.get(answerId).getLikesCount());
        assertEquals(0, votes.get(answerId).getDislikesCount());
        assertEquals(0, votes.get(answerIdFictional).getLikesCount());
        assertEquals(0, votes.get(answerIdFictional).getDislikesCount());

        // check works after likes
        voteMvc.voteAnswer(UID, answerId, VotesDto.VOTE_LIKE);

        votes = voteService.getVotesByIdLongList(QaEntityType.ANSWER, answerIds);
        assertEquals(1, votes.get(answerId).getLikesCount());
        assertEquals(0, votes.get(answerId).getDislikesCount());
        assertEquals(0, votes.get(answerIdFictional).getLikesCount());
        assertEquals(0, votes.get(answerIdFictional).getDislikesCount());

        // check works after dislikes
        voteMvc.voteAnswer(UID, answerId, VotesDto.VOTE_DISLIKE);

        votes = voteService.getVotesByIdLongList(QaEntityType.ANSWER, answerIds);
        assertEquals(0, votes.get(answerId).getLikesCount());
        assertEquals(1, votes.get(answerId).getDislikesCount());
        assertEquals(0, votes.get(answerIdFictional).getLikesCount());
        assertEquals(0, votes.get(answerIdFictional).getDislikesCount());

        // and works after votes are removed
        voteMvc.deleteAnswerVote(UID, answerId);

        votes = voteService.getVotesByIdLongList(QaEntityType.ANSWER, answerIds);
        assertEquals(0, votes.get(answerId).getLikesCount());
        assertEquals(0, votes.get(answerId).getDislikesCount());
        assertEquals(0, votes.get(answerIdFictional).getLikesCount());
        assertEquals(0, votes.get(answerIdFictional).getDislikesCount());
    }

    @Test
    void testVotesBasicWorkWithAnswers() throws Exception {
        final long questionId = createQuestion();

        final long answerIdNoVotes = createAnswer(questionId);
        final long answerIdWithLike = createAnswer(questionId);
        final long answerIdWithDislike = createAnswer(questionId);

        // check votes applied
        voteMvc.voteAnswer(UID, answerIdWithLike, VotesDto.VOTE_LIKE);
        voteMvc.voteAnswer(UID, answerIdWithDislike, VotesDto.VOTE_DISLIKE);

        Map<Long, AnswerDto> answers = getAnswersMap(questionId);
        assertEquals(0, answers.get(answerIdNoVotes).getVotesDto().getLikeCount());
        assertEquals(0, answers.get(answerIdNoVotes).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, answers.get(answerIdNoVotes).getVotesDto().getUserVote());

        assertEquals(1, answers.get(answerIdWithLike).getVotesDto().getLikeCount());
        assertEquals(0, answers.get(answerIdWithLike).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_LIKE, answers.get(answerIdWithLike).getVotesDto().getUserVote());

        assertEquals(0, answers.get(answerIdWithDislike).getVotesDto().getLikeCount());
        assertEquals(1, answers.get(answerIdWithDislike).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_DISLIKE, answers.get(answerIdWithDislike).getVotesDto().getUserVote());

        // check votes can be changed
        voteMvc.voteAnswer(UID, answerIdNoVotes, VotesDto.VOTE_DISLIKE);
        voteMvc.deleteAnswerVote(UID, answerIdWithLike);
        voteMvc.voteAnswer(UID, answerIdWithDislike, VotesDto.VOTE_LIKE);

        answers = getAnswersMap(questionId);
        assertEquals(0, answers.get(answerIdNoVotes).getVotesDto().getLikeCount());
        assertEquals(1, answers.get(answerIdNoVotes).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_DISLIKE, answers.get(answerIdNoVotes).getVotesDto().getUserVote());

        assertEquals(0, answers.get(answerIdWithLike).getVotesDto().getLikeCount());
        assertEquals(0, answers.get(answerIdWithLike).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, answers.get(answerIdWithLike).getVotesDto().getUserVote());

        assertEquals(1, answers.get(answerIdWithDislike).getVotesDto().getLikeCount());
        assertEquals(0, answers.get(answerIdWithDislike).getVotesDto().getDislikeCount());
        assertEquals(VotesDto.VOTE_LIKE, answers.get(answerIdWithDislike).getVotesDto().getUserVote());
    }

    @Test
    void testVotesWorksOnDeletedAnswers() throws Exception {
        final long questionId = createQuestion();

        final long answerIdRemoved = createAnswer(questionId);
        final long answerIdBanned = createAnswer(questionId);

        answerService.deleteAnswer(answerIdRemoved, UID);
        answerService.forceUpdateModState(answerIdRemoved, ModState.TOLOKA_REJECTED);

        // should work ok
        voteMvc.voteAnswer(UID, answerIdRemoved, VotesDto.VOTE_LIKE);
        voteMvc.voteAnswer(UID, answerIdBanned, VotesDto.VOTE_DISLIKE);

        final List<Long> answerIds = Arrays.asList(answerIdRemoved, answerIdBanned);
        Map<Long, VoteStat> votes = voteService.getVotesByIdLongList(QaEntityType.ANSWER, answerIds);

        assertEquals(1, votes.get(answerIdRemoved).getLikesCount());
        assertEquals(0, votes.get(answerIdRemoved).getDislikesCount());

        assertEquals(0, votes.get(answerIdBanned).getLikesCount());
        assertEquals(1, votes.get(answerIdBanned).getDislikesCount());
    }

    @Test
    void testDirectCallDefaultsWorksWithVideo() throws Exception {

        final long videoId = 1;
        final long videoIdFictional = 2;

        final List<Long> videoIds = Arrays.asList(videoId, videoIdFictional);

        // check works with default values
        Map<Long, VoteStat> votes = voteService.getVotesByIdLongList(QaEntityType.VIDEO, videoIds);

        assertEquals(0, votes.get(videoId).getLikesCount());
        assertEquals(0, votes.get(videoId).getDislikesCount());
        assertEquals(0, votes.get(videoIdFictional).getLikesCount());
        assertEquals(0, votes.get(videoIdFictional).getDislikesCount());

        // check works after likes
        voteMvc.voteVideo(UID, videoId, VotesDto.VOTE_LIKE);

        votes = voteService.getVotesByIdLongList(QaEntityType.VIDEO, videoIds);
        assertEquals(1, votes.get(videoId).getLikesCount());
        assertEquals(0, votes.get(videoId).getDislikesCount());
        assertEquals(0, votes.get(videoIdFictional).getLikesCount());
        assertEquals(0, votes.get(videoIdFictional).getDislikesCount());

        // check works after dislikes
        voteMvc.voteVideo(UID, videoId, VotesDto.VOTE_DISLIKE);

        votes = voteService.getVotesByIdLongList(QaEntityType.VIDEO, videoIds);
        assertEquals(0, votes.get(videoId).getLikesCount());
        assertEquals(1, votes.get(videoId).getDislikesCount());
        assertEquals(0, votes.get(videoIdFictional).getLikesCount());
        assertEquals(0, votes.get(videoIdFictional).getDislikesCount());

        // and works after votes are removed
        voteMvc.deleteVideoVote(UID, videoId);

        votes = voteService.getVotesByIdLongList(QaEntityType.VIDEO, videoIds);
        assertEquals(0, votes.get(videoId).getLikesCount());
        assertEquals(0, votes.get(videoId).getDislikesCount());
        assertEquals(0, votes.get(videoIdFictional).getLikesCount());
        assertEquals(0, votes.get(videoIdFictional).getDislikesCount());
    }

    @Test
    void testVotesWithVideoByUid() throws Exception {

        final long videoIdNoVotes = 1;
        final long videoIdWithLike = 2;
        final long videoIdWithDislike = 3;
        long[] videoIds = new long[]{videoIdNoVotes, videoIdWithLike, videoIdWithDislike};

        // check votes applied
        voteMvc.voteVideo(UID, videoIdWithLike, VotesDto.VOTE_LIKE);
        voteMvc.voteVideo(UID, videoIdWithDislike, VotesDto.VOTE_DISLIKE);

        Map<Long, VotesDto> videos = voteMvc.getVideoVotesBulkUid(videoIds, UID)
            .getData().stream().collect(Collectors.toMap(it -> Long.parseLong(it.getId()), EntityVotesDto::getVotes));

        assertEquals(0, videos.get(videoIdNoVotes).getLikeCount());
        assertEquals(0, videos.get(videoIdNoVotes).getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, videos.get(videoIdNoVotes).getUserVote());

        assertEquals(1, videos.get(videoIdWithLike).getLikeCount());
        assertEquals(0, videos.get(videoIdWithLike).getDislikeCount());
        assertEquals(VotesDto.VOTE_LIKE, videos.get(videoIdWithLike).getUserVote());

        assertEquals(0, videos.get(videoIdWithDislike).getLikeCount());
        assertEquals(1, videos.get(videoIdWithDislike).getDislikeCount());
        assertEquals(VotesDto.VOTE_DISLIKE, videos.get(videoIdWithDislike).getUserVote());

        // check votes can be changed
        voteMvc.voteVideo(UID, videoIdNoVotes, VotesDto.VOTE_DISLIKE);
        voteMvc.deleteVideoVote(UID, videoIdWithLike);
        voteMvc.voteVideo(UID, videoIdWithDislike, VotesDto.VOTE_LIKE);

        videos = voteMvc.getVideoVotesBulkUid(videoIds, UID)
            .getData().stream().collect(Collectors.toMap(it -> Long.parseLong(it.getId()), EntityVotesDto::getVotes));
        assertEquals(0, videos.get(videoIdNoVotes).getLikeCount());
        assertEquals(1, videos.get(videoIdNoVotes).getDislikeCount());
        assertEquals(VotesDto.VOTE_DISLIKE, videos.get(videoIdNoVotes).getUserVote());

        assertEquals(0, videos.get(videoIdWithLike).getLikeCount());
        assertEquals(0, videos.get(videoIdWithLike).getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, videos.get(videoIdWithLike).getUserVote());

        assertEquals(1, videos.get(videoIdWithDislike).getLikeCount());
        assertEquals(0, videos.get(videoIdWithDislike).getDislikeCount());
        assertEquals(VotesDto.VOTE_LIKE, videos.get(videoIdWithDislike).getUserVote());
    }

    @Test
    void testVotesWithVideoByYandexUid() throws Exception {

        final long videoIdNoVotes = 1;
        final long videoIdWithLike = 2;
        final long videoIdWithDislike = 3;
        long[] videoIds = new long[]{videoIdNoVotes, videoIdWithLike, videoIdWithDislike};

        // check votes applied
        voteMvc.voteVideo(UID, videoIdWithLike, VotesDto.VOTE_LIKE);
        voteMvc.voteVideo(UID, videoIdWithDislike, VotesDto.VOTE_DISLIKE);

        Map<Long, VotesDto> videos = voteMvc.getVideoVotesBulkYandexUid(videoIds, YANDEXUID)
            .getData().stream().collect(Collectors.toMap(it -> Long.parseLong(it.getId()), EntityVotesDto::getVotes));

        assertEquals(0, videos.get(videoIdNoVotes).getLikeCount());
        assertEquals(0, videos.get(videoIdNoVotes).getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, videos.get(videoIdNoVotes).getUserVote());

        assertEquals(1, videos.get(videoIdWithLike).getLikeCount());
        assertEquals(0, videos.get(videoIdWithLike).getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, videos.get(videoIdWithLike).getUserVote());

        assertEquals(0, videos.get(videoIdWithDislike).getLikeCount());
        assertEquals(1, videos.get(videoIdWithDislike).getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, videos.get(videoIdWithDislike).getUserVote());
    }

    @Test
    void testVotesBad() throws Exception {
        final long questionId = createQuestion();
        final long answerId = createAnswer(questionId);

        // try vote with invalid value
        voteMvc.voteAnswer(UID, answerId, 42, status().is4xxClientError());
    }

    @Test
    public void testArticleVotes() throws Exception {
        long articleIdNoVotes = 1;
        long articleIdWithLike = 2;
        long articleIdWithDislike = 3;

        long[] allArticles = {
            articleIdNoVotes,
            articleIdWithLike,
            articleIdWithDislike
        };

        voteMvc.voteArticle(articleIdWithLike, UID, VotesDto.VOTE_LIKE, status().is2xxSuccessful());
        voteMvc.voteArticle(articleIdWithDislike, UID, VotesDto.VOTE_DISLIKE, status().is2xxSuccessful());

        Map<Long, VotesDto> votes = voteMvc.getArticleVotesBulkUid(allArticles, UID).getData().stream()
            .collect(Collectors.toMap(x -> Long.parseLong(x.getId()), EntityVotesDto::getVotes));

        assertEquals(0, votes.get(articleIdNoVotes).getLikeCount());
        assertEquals(0, votes.get(articleIdNoVotes).getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, votes.get(articleIdNoVotes).getUserVote());

        assertEquals(1, votes.get(articleIdWithLike).getLikeCount());
        assertEquals(0, votes.get(articleIdWithLike).getDislikeCount());
        assertEquals(VotesDto.VOTE_LIKE, votes.get(articleIdWithLike).getUserVote());

        assertEquals(0, votes.get(articleIdWithDislike).getLikeCount());
        assertEquals(1, votes.get(articleIdWithDislike).getDislikeCount());
        assertEquals(VotesDto.VOTE_DISLIKE, votes.get(articleIdWithDislike).getUserVote());

        // check votes can be changed
        voteMvc.voteArticle(articleIdNoVotes, UID, VotesDto.VOTE_DISLIKE, status().is2xxSuccessful());
        voteMvc.deleteArticleVote(articleIdWithLike, UID);
        voteMvc.voteArticle(articleIdWithDislike, UID, VotesDto.VOTE_LIKE, status().is2xxSuccessful());

        votes = voteMvc.getArticleVotesBulkUid(allArticles, UID).getData().stream()
            .collect(Collectors.toMap(x -> Long.parseLong(x.getId()), EntityVotesDto::getVotes));

        assertEquals(0, votes.get(articleIdNoVotes).getLikeCount());
        assertEquals(1, votes.get(articleIdNoVotes).getDislikeCount());
        assertEquals(VotesDto.VOTE_DISLIKE, votes.get(articleIdNoVotes).getUserVote());

        assertEquals(0, votes.get(articleIdWithLike).getLikeCount());
        assertEquals(0, votes.get(articleIdWithLike).getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, votes.get(articleIdWithLike).getUserVote());

        assertEquals(1, votes.get(articleIdWithDislike).getLikeCount());
        assertEquals(0, votes.get(articleIdWithDislike).getDislikeCount());
        assertEquals(VotesDto.VOTE_LIKE, votes.get(articleIdWithDislike).getUserVote());
    }

    @Test
    public void testArticleVotesYandexUid() throws Exception {
        long articleIdNoVotes = 1;
        long articleIdWithLike = 2;
        long articleIdWithDislike = 3;

        long[] allArticles = {
            articleIdNoVotes,
            articleIdWithLike,
            articleIdWithDislike
        };

        voteMvc.voteArticle(articleIdWithLike, UID, VotesDto.VOTE_LIKE, status().is2xxSuccessful());
        voteMvc.voteArticle(articleIdWithDislike, UID, VotesDto.VOTE_DISLIKE, status().is2xxSuccessful());

        Map<Long, VotesDto> votes = voteMvc.getArticleVotesBulkYandexUid(allArticles, YANDEXUID).getData().stream()
            .collect(Collectors.toMap(x -> Long.parseLong(x.getId()), EntityVotesDto::getVotes));

        assertEquals(0, votes.get(articleIdNoVotes).getLikeCount());
        assertEquals(0, votes.get(articleIdNoVotes).getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, votes.get(articleIdNoVotes).getUserVote());

        assertEquals(1, votes.get(articleIdWithLike).getLikeCount());
        assertEquals(0, votes.get(articleIdWithLike).getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, votes.get(articleIdWithLike).getUserVote());

        assertEquals(0, votes.get(articleIdWithDislike).getLikeCount());
        assertEquals(1, votes.get(articleIdWithDislike).getDislikeCount());
        assertEquals(VotesDto.VOTE_NONE, votes.get(articleIdWithDislike).getUserVote());
    }

    @ParameterizedTest
    @MethodSource("commentProject")
    public void testVoteOnComment(QaEntityType qaEntityType) throws Exception {
        long rootId = 12345;
        switch (qaEntityType) {
            case COMMENT:
                long questionId = createQuestion();
                rootId = createAnswer(questionId);
                break;
            case COMMENT_POST:
                rootId = postMvcMocks.createInterestPost();
        }

        long commentNoLike = commentService
            .createComment(QaEntityType.getCommentProjectByType(qaEntityType), UID, UUID.randomUUID().toString(), rootId);
        long commentIdLike = commentService
            .createComment(QaEntityType.getCommentProjectByType(qaEntityType), UID, UUID.randomUUID().toString(), rootId);
        long commentIdDislike = commentService
            .createComment(QaEntityType.getCommentProjectByType(qaEntityType), UID, UUID.randomUUID().toString(), rootId);

        List<Long> commentIds = Arrays.asList(commentNoLike, commentIdLike, commentIdDislike);
        Map<Long, VoteStat> votes = voteService.getVotesByIdLongList(qaEntityType, commentIds);
        assertVoteStat(votes.get(commentNoLike), 0, 0);
        assertVoteStat(votes.get(commentIdLike), 0, 0);
        assertVoteStat(votes.get(commentIdDislike), 0, 0);

        // check votes applied
        voteMvc.voteComment(UID, commentIdLike, VotesDto.VOTE_LIKE, qaEntityType);
        voteMvc.voteComment(UID, commentIdDislike, VotesDto.VOTE_DISLIKE, qaEntityType);

        votes = voteService.getVotesByIdLongList(qaEntityType, commentIds);
        assertVoteStat(votes.get(commentNoLike), 0, 0);
        assertVoteStat(votes.get(commentIdLike), 1, 0);
        assertVoteStat(votes.get(commentIdDislike), 0, 1);

        // check votes can be changed
        voteMvc.voteComment(UID, commentNoLike, VotesDto.VOTE_DISLIKE, qaEntityType);
        voteMvc.deleteCommentVote(UID, commentIdLike, qaEntityType);
        voteMvc.voteComment(UID, commentIdDislike, VotesDto.VOTE_LIKE, qaEntityType);

        votes = voteService.getVotesByIdLongList(qaEntityType, commentIds);
        assertVoteStat(votes.get(commentNoLike), 0, 1);
        assertVoteStat(votes.get(commentIdLike), 0, 0);
        assertVoteStat(votes.get(commentIdDislike), 1, 0);
    }

    @ParameterizedTest
    @MethodSource("commentProject")
    public void testVotesOnDeletedComment(QaEntityType qaEntityType) throws Exception {
        long rootId = 12345;
        switch (qaEntityType) {
            case COMMENT:
                long questionId = createQuestion();
                rootId = createAnswer(questionId);
                break;
            case COMMENT_POST:
                rootId = postMvcMocks.createInterestPost();
        }

        long commentDeleted = commentService.createComment(QaEntityType.getCommentProjectByType(qaEntityType),
            1234, UUID.randomUUID().toString(), rootId);

        commentService.banCommentByManager(commentDeleted);
        List<Long> commentIdList = Collections.singletonList(commentDeleted);

        Map<Long, VoteStat> votes = voteService.getVotesByIdLongList(qaEntityType, commentIdList);
        assertVoteStat(votes.get(commentDeleted), 0, 0);

        voteMvc.voteComment(UID, commentDeleted, VotesDto.VOTE_LIKE, qaEntityType);
        votes = voteService.getVotesByIdLongList(qaEntityType, commentIdList);
        assertVoteStat(votes.get(commentDeleted), 1, 0);

        voteMvc.voteComment(UID + 1, commentDeleted, VotesDto.VOTE_DISLIKE, qaEntityType);
        votes = voteService.getVotesByIdLongList(qaEntityType, commentIdList);
        assertVoteStat(votes.get(commentDeleted), 1, 1);
    }

    @Test
    public void testVoteableComments() {
        Set<QaEntityType> types = getVoteableCommentsStream()
            .collect(Collectors.toSet());
        List<QaEntityType> expectedTypes = Arrays.asList(
            QaEntityType.COMMENT,
            QaEntityType.COMMENT_ARTICLE,
            QaEntityType.COMMENT_VERSUS,
            QaEntityType.COMMENT_GRADE,
            QaEntityType.COMMENT_POST,
            QaEntityType.COMMENT_VIDEO
        );
        assertTrue(types.containsAll(expectedTypes));
        assertEquals(expectedTypes.size(), types.size());
    }

    private void assertVoteStat(VoteStat voteStat, int expectedLikes, int expectedDislikes) {
        assertEquals(expectedLikes, voteStat.getLikesCount());
        assertEquals(expectedDislikes, voteStat.getDislikesCount());
    }

    @NotNull
    private static Stream<QaEntityType> getVoteableCommentsStream() {
        return Arrays.stream(QaEntityType.values())
            .filter(QaEntityType::isComment)
            .filter(x -> x.support(QaEntityFeature.VOTING));
    }

    private static Stream<Arguments> commentProject() {
        return getVoteableCommentsStream()
            .map(Arguments::of);
    }

}
