package ru.yandex.market.pers.qa.controller.api.comment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.model.CommentState;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author vvolokh
 * 11.12.2018
 */
public class QaLegacyCommentControllerTest extends ControllerTest {
    private static final int USER_ID = 1;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private AnswerService answerService;

    @Test
    public void testGetQaComment() throws Exception {
        final Question question = questionService.createModelQuestion(1, "Question", 1);
        final Answer answer = answerService.createAnswer(1, "Answer", question.getId());
        long commentId = commentService.createAnswerComment(USER_ID, "text", answer.getId());

        String response = invokeAndRetrieveResponse(get("/comment/qa/" + commentId), status().is2xxSuccessful());
        CommentDto commentDto = objectMapper.readValue(response, CommentDto.class);

        assertEquals(commentId, commentDto.getId());
        assertEquals(answer.getId(), commentDto.getEntityId());
        assertEquals(String.valueOf(USER_ID), commentDto.getAuthor().getId());
        assertEquals("user", commentDto.getAuthor().getEntity());
        assertEquals("text", commentDto.getText());
        assertEquals(CommentState.NEW.getName(), commentDto.getState());
    }

    @Test
    public void testGetNotExistingQaComment() throws Exception {
        invokeAndRetrieveResponse(get("/comment/qa/" + Integer.MIN_VALUE), status().is4xxClientError());
    }

}
