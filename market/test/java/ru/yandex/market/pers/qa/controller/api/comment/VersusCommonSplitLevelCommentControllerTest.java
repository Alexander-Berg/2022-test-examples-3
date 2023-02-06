package ru.yandex.market.pers.qa.controller.api.comment;

import ru.yandex.market.pers.qa.mock.mvc.AbstractCommonCommentMvcMocks;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 13.09.2019
 */
public class VersusCommonSplitLevelCommentControllerTest extends AbstractCommonSplitLevelCommentControllerTest {
    @Override
    protected AbstractCommonCommentMvcMocks getMvc() {
        return versusCommentMvc;
    }
}
