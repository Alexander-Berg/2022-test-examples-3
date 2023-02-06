package ru.yandex.market.pers.qa.controller.api.comment;

import java.util.Comparator;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.controller.service.CommentHelper;
import ru.yandex.market.pers.qa.mock.mvc.AbstractCommonCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.AnswerCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.ArticleCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.GradeCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.GradeFixIdCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.GradeShopCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.PostCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.VersusCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.VideoCommentMvcMocks;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.09.2019
 */
public abstract class AbstractCommonCommentControllerTest extends AbstractCommentControllerTest {
    protected static final Long ROOT_ID = 0L;
    protected static final Comparator<Long> COMMENT_CMP = Comparator.naturalOrder();
    protected static final Comparator<Long> FIRST_LEVEL_COMMENT_CMP = COMMENT_CMP.reversed();
    protected static final long ENTITY_ID = 111111;
    protected static final long ANOTHER_ENTITY_ID = 11112;
    protected static final long FAKE_ENTITY_ID = 111112;

    @Autowired
    protected CommentHelper commentHelper;
    @Autowired
    protected ArticleCommentMvcMocks articleCommentMvc;
    @Autowired
    protected VersusCommentMvcMocks versusCommentMvc;
    @Autowired
    protected AnswerCommentMvcMocks answerCommentMvc;
    @Autowired
    protected GradeCommentMvcMocks gradeCommentMvc;
    @Autowired
    protected GradeFixIdCommentMvcMocks gradeFixCommentMvc;
    @Autowired
    protected GradeShopCommentMvcMocks gradeShopCommentMvc;
    @Autowired
    protected PostCommentMvcMocks postCommentMvcMocks;
    @Autowired
    protected VideoCommentMvcMocks videoCommentMvcMocks;

    protected AbstractCommonCommentMvcMocks mvc;

    @BeforeEach
    public void initialize() {
        mvc = getMvc();
        mvc.createEntity(ENTITY_ID);
        mvc.createEntity(ENTITY_ID + 1);
        mvc.createEntity(ENTITY_ID + 2);
        mvc.createEntity(ENTITY_ID + 3);
        mvc.createEntity(ENTITY_ID + 3);
        mvc.createEntity(ANOTHER_ENTITY_ID);
        mvc.createEntity(FAKE_ENTITY_ID);
    }

    protected abstract AbstractCommonCommentMvcMocks getMvc();

    public void deleteComment(long commentId) throws Exception {
        mvc.deleteComment(commentId);
    }

    public long createComment(long entityId, long userId, Long parentCommentId, String body) throws Exception {
        return mvc.createComment(entityId, userId, parentCommentId, body);
    }

    public CommentDto createCommentDto(long entityId, long userId, Long parentCommentId, String body) throws Exception {
        return mvc.createCommentDto(entityId, userId, parentCommentId, body);
    }

    public CommentDto createComment(long entityId,
                                    long userId,
                                    String body,
                                    Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> reqBuilder
    ) throws Exception {
        return mvc.createComment(entityId, userId, body, reqBuilder);
    }

    public String editComment(long commentId,
                              long userId,
                              String body,
                              ResultMatcher resultMatcher,
                              Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> reqBuilder
    ) throws Exception {
        return mvc.editComment(commentId, userId, body, resultMatcher, reqBuilder);
    }

}
