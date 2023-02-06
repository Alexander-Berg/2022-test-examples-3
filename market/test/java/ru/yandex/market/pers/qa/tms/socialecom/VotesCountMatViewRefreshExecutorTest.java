package ru.yandex.market.pers.qa.tms.socialecom;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.model.DbUser;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.model.post.PostV2;
import ru.yandex.market.pers.qa.model.socialecom.VoteStatistic;
import ru.yandex.market.pers.qa.service.post.PostV2Service;
import ru.yandex.market.pers.qa.utils.socialecom.VoteStatisticTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VotesCountMatViewRefreshExecutorTest extends PersQaTmsTest {

    private static final String POST_TEXT = "ТЕКСТ ТЕКСТ ТЕКСТ?? ИЛИ ТЕКСТ!";
    private static final DbUser BRAND_USER = new DbUser(UserType.BRAND, String.valueOf(1L));
    private static final DbUser BUSINESS_USER = new DbUser(UserType.BUSINESS, String.valueOf(1L));
    private static final Long P_UID = 10000L;

    @Autowired
    private VoteStatisticTestHelper helper;

    @Autowired
    private PostV2Service postV2Service;

    @Autowired
    private VotesCountMatViewRefreshExecutor executor;

    @BeforeEach
    private void cleanUp() {
        helper.refreshVotesMatView();
    }

    @Test
    public void testRefreshView() {
        assertTrue(selectFromView().isEmpty());
        PostV2 post = helper.buildGeneralPost(P_UID, BRAND_USER);
        long postId = postV2Service.createPostV2GetId(post, new SecurityData());
        helper.createPostVote(postId, 123L);
        helper.createPostVote(postId, 124L);
        helper.createPostVote(postId, 125L);

        executor.refreshVotesCountView();

        assertEquals(1, selectFromView().size());
        assertEquals(3, selectFromView().get(0).getVotesCount());
    }

    @Test
    public void testRefreshViewWithMultiplePostsAndSameId() {

        assertTrue(selectFromView().isEmpty());
        PostV2 post = helper.buildGeneralPost(P_UID, BRAND_USER);
        PostV2 secondPost = helper.buildGeneralPost(P_UID + 1, BUSINESS_USER);

        long postId = postV2Service.createPostV2GetId(post, new SecurityData());
        // не понятно, насколько реальная ситуация, но второй пост создаем с тем же айдишником,
        // но другим типом пользователя
        long secondPostId = postV2Service.createPostV2GetId(secondPost, new SecurityData());

        long postNoVotes = postV2Service.createPostV2GetId(
            helper.buildGeneralPost(P_UID + 2, new DbUser(UserType.BRAND, "19999")), new SecurityData());

        helper.createPostVote(postId, 123L);
        helper.createPostVote(postId, 124L);
        helper.createPostVote(postId, 125L);

        //лайки с разным типами, чтобы проверить группировку
        helper.createPostVote(secondPostId, 123L);
        helper.createPostVote(secondPostId, 124L);
        helper.createPostVote(secondPostId, 125L);
        helper.createPostVote(secondPostId, UserType.BRAND, 123L);
        helper.createPostVote(secondPostId, UserType.BUSINESS, 123L);

        executor.refreshVotesCountView();

        List<VoteStatistic> result = selectFromView();

        assertEquals(3, result.size());
        assertVoteStats(result, 5L);
        assertVoteStats(result, 3L);
        assertVoteStats(result, 0L);
    }

    private void assertVoteStats(List<VoteStatistic> res, long count) {
        assertTrue(res.stream().anyMatch(e -> e.getVotesCount() == count));
    }

    private List<VoteStatistic> selectFromView() {
        return helper.getJdbcTemplate()
            .query("select * from qa.mv_se_vote_stat", VoteStatistic.ROW_MAPPER);
    }
}
