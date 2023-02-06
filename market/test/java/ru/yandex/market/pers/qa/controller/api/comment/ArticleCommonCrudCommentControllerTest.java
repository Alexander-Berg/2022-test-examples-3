package ru.yandex.market.pers.qa.controller.api.comment;

import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.mock.mvc.AbstractCommonCommentMvcMocks;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 13.09.2019
 */
public class ArticleCommonCrudCommentControllerTest extends AbstractCommonCrudCommentControllerTest {
    @Override
    protected AbstractCommonCommentMvcMocks getMvc() {
        return articleCommentMvc;
    }

    protected CommentProject getProject() {
        return CommentProject.ARTICLE;
    }
}
