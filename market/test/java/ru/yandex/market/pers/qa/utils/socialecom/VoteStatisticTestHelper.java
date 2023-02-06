package ru.yandex.market.pers.qa.utils.socialecom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.model.DbUser;
import ru.yandex.market.pers.qa.model.post.PostV2;

@Service
public class VoteStatisticTestHelper {

    public static final String POST_TEXT = "ТЕКСТ ТЕКСТ ТЕКСТ?? ИЛИ ТЕКСТ!";

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public PostV2 buildGeneralPost(Long pUid, DbUser user) {
        PostV2 post = new PostV2();
        post.setAuthor(user);
        post.setpUid(pUid);
        post.setText(POST_TEXT);
        return post;
    }

    public void createPostVote(Long postId, Long userId) {
        createPostVote(postId, UserType.UID, userId);
    }

    public void createPostVote(Long postId, UserType type, Long userId) {
        jdbcTemplate.update("insert into qa.vote " +
            "(user_type, user_id, entity_type, entity_id, cr_time, type) values " +
            "(?, ?, 11, ?, now(), 1)", type.getValue(), userId, postId);
    }

    public JdbcTemplate getJdbcTemplate() {
        return this.jdbcTemplate;
    }

    public void refreshVotesMatView() {
        jdbcTemplate.execute("refresh materialized view qa.mv_se_vote_stat");
    }
}
