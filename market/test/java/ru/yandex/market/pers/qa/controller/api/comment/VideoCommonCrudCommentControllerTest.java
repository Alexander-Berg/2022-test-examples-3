package ru.yandex.market.pers.qa.controller.api.comment;

import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.mock.mvc.AbstractCommonCommentMvcMocks;

public class VideoCommonCrudCommentControllerTest extends AbstractCommonCrudCommentControllerTest {
    @Override
    protected AbstractCommonCommentMvcMocks getMvc() {
        return videoCommentMvcMocks;
    }

    protected CommentProject getProject() {
        return CommentProject.VIDEO;
    }

}
