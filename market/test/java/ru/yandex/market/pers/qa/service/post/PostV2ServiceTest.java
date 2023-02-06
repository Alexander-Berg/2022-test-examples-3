package ru.yandex.market.pers.qa.service.post;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.exception.DuplicateEntityException;
import ru.yandex.market.pers.qa.exception.EntityNotFoundException;
import ru.yandex.market.pers.qa.exception.QaResult;
import ru.yandex.market.pers.qa.model.DbUser;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.model.post.PostV2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author grigor-vlad
 * 27.06.2022
 */
public class PostV2ServiceTest extends PersQATest {
    private static final String POST_TEXT = "Хороший текст для поста";
    private static final DbUser BRAND_USER = new DbUser(UserType.BRAND, String.valueOf(1L));
    private static final Long P_UID = 98765L;

    @Autowired
    private PostV2Service postService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate qaJdbcTemplate;

    @Test
    public void testSavePostAndLock() {
        //create post by brand and default person Uid
        PostV2 post = buildGeneralPost();
        long postId = postService.createPostV2GetId(post, new SecurityData());

        //check post creation
        Long countPosts = qaJdbcTemplate.queryForObject(
            "select count(*) from post.post where id = ?", Long.class, postId);
        assertEquals(1L, countPosts);

        //create post by brand and another personUid
        PostV2 post2 = buildGeneralPost();
        post2.setpUid(123456789L);
        try {
            postService.createPostV2GetId(post2, new SecurityData());
            Assertions.fail();
        } catch (DuplicateEntityException ex) {
            assertEquals(QaResult.DUPLICATE_POST_FOUND, ex.getResult());
        }
    }

    @Test
    public void testPostDeletion() {
        PostV2 post = postService.createPostV2(buildGeneralPost(), new SecurityData());
        postService.deletePostV2(post.getId(), P_UID);

        List<State> states = qaJdbcTemplate.query("select state from post.post where id = ?",
            (rs, rowNum) -> State.valueOf(rs.getInt("state")),
            post.getId()
        );
        assertEquals(1, states.size());
        assertEquals(State.DELETED, states.get(0));

        //delete this post again
        try {
            postService.deletePostV2(post.getId(), P_UID);
            fail();
        } catch (EntityNotFoundException ex) {
            assertEquals(QaResult.POST_NOT_FOUND, ex.getResult());
            assertEquals("Can't remove already removed post", ex.getMessage());
        }

        //delete not existed post
        long notExistedPostId = post.getId() + 1;
        try {
            postService.deletePostV2(notExistedPostId, P_UID);
            fail();
        } catch (EntityNotFoundException ex) {
            assertEquals(QaResult.POST_NOT_FOUND, ex.getResult());
            assertEquals(String.format("Post with id=%d doesn't exist", notExistedPostId), ex.getMessage());
        }

    }

    private PostV2 buildGeneralPost() {
        PostV2 post = new PostV2();
        post.setAuthor(BRAND_USER);
        post.setpUid(P_UID);
        post.setText(POST_TEXT);
        return post;
    }

}
