package ru.yandex.market.pers.qa.mock.mvc;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CommentTreeDto;
import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.controller.api.comment.AbstractCommentControllerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.model.CommentProject.GRADE;
import static ru.yandex.market.pers.qa.client.model.CommentProject.QA;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.ANSWER_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.COMMENT_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.GRADE_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.PARENT_ID_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.12.2020
 */
@Service
public class VendorMvcMocks extends AbstractMvcMocks {

    public long createComment(CommentProject project,
                              long brandId,
                              long uid,
                              Long parentId,
                              long entityId,
                              String text) throws Exception {
        final String response = createComment(
            project,
            brandId,
            uid,
            parentId,
            entityId,
            text,
            status().is2xxSuccessful());
        return objectMapper.readValue(response, CommentDto.class).getId();
    }

    public String createComment(CommentProject project,
                                long brandId,
                                long uid,
                                Long parentId,
                                long entityId,
                                String text,
                                ResultMatcher resultMatcher) throws Exception {
        String entity = project == QA ? "answer" : project == GRADE ? "grade" : "unknown";

        return invokeAndRetrieveResponse(
            post("/vendor/" + brandId + "/comment/" + entity + "/" + entityId)
                .param("userId", String.valueOf(uid))
                .param(PARENT_ID_KEY, parentId == null ? null : String.valueOf(parentId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(AbstractCommentControllerTest.getBody(text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public String editComment(Long commentId,
                              long brandId,
                              long uid,
                              String text,
                              ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            patch("/vendor/" + brandId + "/comment/" + commentId)
                .param("userId", String.valueOf(uid))
                .param(COMMENT_ID_KEY, commentId == null ? null : String.valueOf(commentId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(AbstractCommentControllerTest.getBody(text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public List<CommentTreeDto> getComments(CommentProject project,
                                               long brandId,
                                               long uid,
                                               long[] gradeIds) throws Exception {
        String entity = project == QA ? "answer" : project == GRADE ? "grade" : "unknown";
        String entityKey = project == QA ? ANSWER_ID_KEY : project == GRADE ? GRADE_ID_KEY : "unknown";

        final String response = invokeAndRetrieveResponse(
            get("/vendor/" + brandId + "/comment/" + entity)
                .param(entityKey, Arrays.stream(gradeIds).mapToObj(String::valueOf).toArray(String[]::new))
                .param("userId", String.valueOf(uid))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        DtoList<CommentTreeDto> result = objectMapper.readValue(response, new TypeReference<DtoList<CommentTreeDto>>() {
        });
        return result.getData();
    }

    public String deleteComment(long commentId, long brandId,
                                   long uid,
                                   ResultMatcher resultMatcher) throws Exception {
        final String response = invokeAndRetrieveResponse(

            delete("/vendor/" + brandId + "/comment/" + commentId)
                .param("userId", String.valueOf(uid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
        return response;
    }
}
