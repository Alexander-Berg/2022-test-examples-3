package ru.yandex.market.pers.qa.mock.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.util.ExecUtils;

/**
 * @author Dionisiy Yuzhakov / bahus@ / 20.03.2020
 */
@Service
public class PostCommentMvcMocks extends AbstractCommonCommentMvcMocks {
    @Autowired
    public PostMvcMocks postMvcMocks;

    @Autowired
    public QuestionService questionService;

    public PostCommentMvcMocks() {
        super("post");
    }

    @Override
    public CommentProject getProject() {
        return CommentProject.POST;
    }

    @Override
    public long createEntity(long entityId) {
        if (questionService.isQuestionExists(entityId)) {
            return entityId;
        }

        long postId;
        try {
            postId = postMvcMocks.createInterestPost();
        } catch (Exception e) {
            throw ExecUtils.silentError(e);
        }

        // special case when need to create new entity
        if (entityId < 0) {
            return postId;
        }

        jdbcTemplate.update(
            "update qa.question\n" +
                "set id = ? \n" +
                "where id = ?",
            entityId,
            postId
        );

        return entityId;
    }

}
