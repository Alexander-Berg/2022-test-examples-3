package ru.yandex.market.pers.qa.mock.mvc;

import java.util.stream.LongStream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.dto.EntityVotesDto;
import ru.yandex.market.pers.qa.model.QaEntityType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 05.12.2019
 */
@Service
public class VoteMvcMocks extends AbstractMvcMocks {

    public void voteAnswer(long userId, long answerId, int vote, ResultMatcher resultMatcher) throws Exception {
        invokeAndRetrieveResponse(
                post("/vote/UID/" + userId + "/answer/" + answerId)
                        .param("vote", String.valueOf(vote))
                        .contentType(MediaType.APPLICATION_JSON),
                resultMatcher
        );
    }

    public void voteAnswer(long userId, long answerId, int vote) throws Exception {
        voteAnswer(userId, answerId, vote, status().is2xxSuccessful());
    }

    public void deleteAnswerVote(long userId, long answerId) throws Exception {
        invokeAndRetrieveResponse(
            delete("/vote/UID/" + userId + "/answer/" + answerId)
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void likeQuestion(long userId, long questionId) throws Exception {
        invokeAndRetrieveResponse(
                post("/vote/UID/" + userId + "/question/" + questionId)
                        .contentType(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
        );
    }

    public void deleteQuestionVote(long userId, long questionId) throws Exception {
        invokeAndRetrieveResponse(
            delete("/vote/UID/" + userId + "/question/" + questionId)
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void votePost(long userId, long postId, int vote) throws Exception {
        invokeAndRetrieveResponse(
                post("/vote/UID/" + userId + "/post/" + postId)
                        .param("vote", String.valueOf(vote))
                        .contentType(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
        );
    }

    public void votePost(long userId, long postId, int vote, ResultMatcher resultMatcher) throws Exception {
        invokeAndRetrieveResponse(
                post("/vote/UID/" + userId + "/post/" + postId)
                        .param("vote", String.valueOf(vote))
                        .contentType(MediaType.APPLICATION_JSON),
                resultMatcher
        );
    }

    public void deletePostVote(long userId, long postId) throws Exception {
        invokeAndRetrieveResponse(
            delete("/vote/UID/" + userId + "/post/" + postId)
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void likePostV2(long userId, long postId) throws Exception {
        invokeAndRetrieveResponse(
            post("/vote/UID/" + userId + "/post/v2/" + postId)
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void deletePostV2Like(long userId, long postId) throws Exception {
        invokeAndRetrieveResponse(
            delete("/vote/UID/" + userId + "/post/v2/" + postId)
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void voteVideo(long userId, long videoId, int vote) throws Exception {
        invokeAndRetrieveResponse(
            post("/vote/UID/" + userId + "/video/" + videoId)
                .param("vote", String.valueOf(vote))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void voteVideo(long userId, long videoId, int vote, ResultMatcher resultMatcher) throws Exception {
        invokeAndRetrieveResponse(
            post("/vote/UID/" + userId + "/video/" + videoId)
                .param("vote", String.valueOf(vote))
                .contentType(MediaType.APPLICATION_JSON),
            resultMatcher
        );
    }

    public void deleteVideoVote(long userId, long videoId) throws Exception {
        invokeAndRetrieveResponse(
            delete("/vote/UID/" + userId + "/video/" + videoId)
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public DtoList<EntityVotesDto> getVideoVotesBulkUid(long[] videoIds, Long userId) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/vote/UID/" + userId + "/video/bulk")
                .param("videoId", LongStream.of(videoIds).mapToObj(String::valueOf).toArray(String[]::new))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        ), new TypeReference<DtoList<EntityVotesDto>>() {
        });
    }

    public DtoList<EntityVotesDto> getVideoVotesBulkYandexUid(long[] videoIds, String yandexUid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/vote/YANDEXUID/" + yandexUid + "/video/bulk")
                .param("videoId", LongStream.of(videoIds).mapToObj(String::valueOf).toArray(String[]::new))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        ), new TypeReference<DtoList<EntityVotesDto>>() {
        });
    }

    public void voteComment(long userId, long commentId, int vote,  QaEntityType commentType) throws Exception {
        voteComment(userId, commentId, vote, status().is2xxSuccessful(), commentType);
    }

    public void voteComment(long userId, long commentId, int vote, ResultMatcher resultMatcher, QaEntityType commentType) throws Exception {
        invokeAndRetrieveResponse(
            post("/vote/UID/" + userId + "/comment/" + commentId)
                .param("vote", String.valueOf(vote))
                .param("commentType", String.valueOf(commentType.getValue()))
                .contentType(MediaType.APPLICATION_JSON),
            resultMatcher
        );
    }

    public void deleteCommentVote(long userId, long commentId, QaEntityType commentType) throws Exception {
        invokeAndRetrieveResponse(
            delete("/vote/UID/" + userId + "/comment/" + commentId)
                .param("commentType", String.valueOf(commentType.getValue()))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void voteArticle(long articleId, long userId, int vote, ResultMatcher resultMatcher) throws Exception {
        invokeAndRetrieveResponse(
            post("/vote/UID/" + userId + "/article/" + articleId)
                .param("vote", String.valueOf(vote))
                .contentType(MediaType.APPLICATION_JSON),
            resultMatcher
        );
    }

    public void deleteArticleVote(long articleId, long userId) throws Exception {
        invokeAndRetrieveResponse(
            delete("/vote/UID/" + userId + "/article/" + articleId)
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public DtoList<EntityVotesDto> getArticleVotesBulkUid(long[] articleIds, Long userId) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/vote/UID/" + userId + "/article/bulk")
                .param("articleId", LongStream.of(articleIds).mapToObj(String::valueOf).toArray(String[]::new))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        ), new TypeReference<DtoList<EntityVotesDto>>() {
        });
    }

    public DtoList<EntityVotesDto> getArticleVotesBulkYandexUid(long[] articleIds, String yandexUid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/vote/YANDEXUID/" + yandexUid + "/article/bulk")
                .param("articleId", LongStream.of(articleIds).mapToObj(String::valueOf).toArray(String[]::new))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        ), new TypeReference<DtoList<EntityVotesDto>>() {
        });
    }


}
