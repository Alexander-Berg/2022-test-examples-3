package ru.yandex.market.pers.qa.controller.api.comment;

import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.mock.mvc.AbstractCommonCommentMvcMocks;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 13.09.2019
 */
public class GradeFixIdCommonCrudCommentControllerTest extends AbstractCommonCrudCommentControllerTest {
    @Override
    protected AbstractCommonCommentMvcMocks getMvc() {
        return gradeFixCommentMvc;
    }

    protected CommentProject getProject() {
        return CommentProject.GRADE;
    }

    @Override
    protected boolean canRemapId() {
        return true;
    }

    @Override
    protected long remapId(long id) {
        return PersQaServiceMockFactory.getFixIdById(id);
    }
}
