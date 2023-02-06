package ru.yandex.market.pers.qa.mock.mvc;

import org.springframework.stereotype.Service;

import ru.yandex.market.pers.qa.client.model.CommentProject;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 13.09.2019
 */
@Service
public class GradeCommentMvcMocks extends AbstractCommonCommentMvcMocks {
    public GradeCommentMvcMocks() {
        super("grade");
    }

    @Override
    public CommentProject getProject() {
        return CommentProject.GRADE;
    }
}
