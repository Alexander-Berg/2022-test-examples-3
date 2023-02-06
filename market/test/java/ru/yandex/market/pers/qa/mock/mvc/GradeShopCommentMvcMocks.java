package ru.yandex.market.pers.qa.mock.mvc;

import org.springframework.stereotype.Service;

import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.utils.ControllerConstants;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 13.09.2019
 */
@Service
public class GradeShopCommentMvcMocks extends AbstractCommonCommentMvcMocks {
    public GradeShopCommentMvcMocks() {
        super("grade/shop");
    }

    @Override
    protected String getEntityIdKey() {
        return ControllerConstants.GRADE_ID_KEY;
    }

    @Override
    public CommentProject getProject() {
        return CommentProject.GRADE;
    }
}
