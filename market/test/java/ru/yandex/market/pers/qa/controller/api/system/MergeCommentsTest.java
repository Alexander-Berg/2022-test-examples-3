package ru.yandex.market.pers.qa.controller.api.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.controller.service.CommentHelper;
import ru.yandex.market.pers.qa.model.Comment;
import ru.yandex.market.pers.qa.model.CommentFilter;
import ru.yandex.market.pers.qa.service.CommentService;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.pers.qa.client.model.CommentProject.GRADE;

/**
 * @author : poluektov
 * date: 2019-10-09.
 */
public class MergeCommentsTest extends ControllerTest {
    private final long USER_1 = 148L;
    private final long USER_2 = 322L;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentHelper commentHelper;


    @BeforeEach
    public void createSomeComments() {
        commentService.createComment(GRADE, USER_1, "User comment", 1);
        commentService.createComment(GRADE, USER_2, "toxic comment", 2);
        commentService.createComment(GRADE, USER_1, "such comment", 3);
        commentService.createComment(GRADE, USER_2, "yet another comment", 4);
    }

    @Test
    public void shouldMergeCommentsBetweenUsers() {
        commentHelper.moveComments(USER_2, USER_1);
        List<Comment> firstUserComments = commentService.getComments(new CommentFilter().userId(USER_1));
        List<Comment> secondUserComments = commentService.getComments(new CommentFilter().userId(USER_2));
        assertThat(firstUserComments, hasSize(4));
        assertThat(secondUserComments, hasSize(0));
    }
}
