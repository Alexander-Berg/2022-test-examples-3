package ru.yandex.market.pers.qa.mock.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.util.ExecUtils;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 13.09.2019
 */
@Service
public class AnswerCommentMvcMocks extends AbstractCommonCommentMvcMocks {
    @Autowired
    public QuestionMvcMocks questionMvc;
    @Autowired
    public AnswerMvcMocks answerMvc;

    @Autowired
    public AnswerService answerService;

    public AnswerCommentMvcMocks() {
        super("answer");
    }

    @Override
    public CommentProject getProject() {
        return CommentProject.QA;
    }

    @Override
    public long createEntity(long entityId) {
        if (answerService.isAnswerExists(entityId)) {
            return entityId;
        }

        long answerId;
        try {
            long questionId = questionMvc.createModelQuestion();
            answerId = answerMvc.createAnswer(questionId);
        } catch (Exception e) {
            throw ExecUtils.silentError(e);
        }

        // special case when need to create new entity
        if (entityId < 0) {
            return answerId;
        }

        jdbcTemplate.update(
            "update qa.answer\n" +
                "set id = ? \n" +
                "where id = ?",
            entityId,
            answerId
        );

        return entityId;
    }

}
