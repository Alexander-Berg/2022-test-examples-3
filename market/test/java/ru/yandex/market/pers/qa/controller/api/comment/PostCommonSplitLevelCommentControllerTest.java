package ru.yandex.market.pers.qa.controller.api.comment;

import ru.yandex.market.pers.qa.mock.mvc.AbstractCommonCommentMvcMocks;

/**
 * @author Dionisiy Yuzhakov / bahus@ / 20.03.2020
 */
public class PostCommonSplitLevelCommentControllerTest extends AbstractCommonSplitLevelCommentControllerTest {
    @Override
    protected AbstractCommonCommentMvcMocks getMvc() {
        return postCommentMvcMocks;
    }
}
