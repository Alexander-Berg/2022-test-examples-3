package ru.yandex.market.pers.qa.controller.api.comment;

import ru.yandex.market.pers.qa.mock.mvc.AbstractCommonCommentMvcMocks;

public class VideoCommonSplitLevelCommentControllerTest extends AbstractCommonSplitLevelCommentControllerTest {
    @Override
    protected AbstractCommonCommentMvcMocks getMvc() {
        return videoCommentMvcMocks;
    }
}
