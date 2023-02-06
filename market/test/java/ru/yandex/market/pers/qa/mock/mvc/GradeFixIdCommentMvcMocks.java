package ru.yandex.market.pers.qa.mock.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.client.model.CommentProject;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 13.09.2019
 */
@Service
public class GradeFixIdCommentMvcMocks extends AbstractCommonCommentMvcMocks {
    @Autowired
    private GradeClient client;
    private long counter = 12345000L;

    public GradeFixIdCommentMvcMocks() {
        super("grade");
    }

    @Override
    public CommentProject getProject() {
        return CommentProject.GRADE;
    }

    @Override
    public long createEntity(long entityId) {
        if (entityId < 0) {
            entityId = counter++;
        }

        // mock model grade
        PersQaServiceMockFactory.mockModelGradeWithFixId(client, entityId, 1, 1);

        return entityId;
    }

}
