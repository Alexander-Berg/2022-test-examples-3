package ru.yandex.market.pers.qa.controller.api.comment;

import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.mock.mvc.AbstractCommonCommentMvcMocks;

/**
 * @author Dionisiy Yuzhakov / bahus@ / 20.03.2020
 */
public class PostCommonCrudCommentControllerTest extends AbstractCommonCrudCommentControllerTest {
    @Override
    protected AbstractCommonCommentMvcMocks getMvc() {
        return postCommentMvcMocks;
    }

    protected CommentProject getProject() {
        return CommentProject.POST;
    }

}
