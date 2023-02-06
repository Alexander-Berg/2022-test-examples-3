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
import ru.yandex.market.pers.qa.model.Sort;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.GRADE_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.PARENT_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.SORT_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.UID_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.12.2020
 */
@Service
public class PartnerShopMvcMocks extends AbstractMvcMocks {

    public long createAnswerComment(long shopId, long uid, long answerId, String text) throws Exception {
        final String response = createAnswerComment(shopId, uid, answerId, text, status().is2xxSuccessful());
        return objectMapper.readValue(response, CommentDto.class).getId();
    }

    public String createAnswerComment(long shopId,
                                      long uid,
                                      long answerId,
                                      String text,
                                      ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            post("/partner/shop/" + shopId + "/answer/" + answerId + "/comment")
                .param("userId", String.valueOf(uid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(AnswerMvcMocks.ANSWER_BODY, text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public long createGradeComment(long shopId, long uid, Long parentId, long gradeId, String text) throws Exception {
        final String response = createGradeComment(shopId, uid, parentId, gradeId, text, status().is2xxSuccessful());
        return objectMapper.readValue(response, CommentDto.class).getId();
    }

    public String createGradeComment(long shopId,
                                     long uid,
                                     Long parentId,
                                     long gradeId,
                                     String text,
                                     ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            post("/partner/shop/" + shopId + "/grade/" + gradeId + "/comment")
                .param("userId", String.valueOf(uid))
                .param(PARENT_ID_KEY, parentId == null ? null : String.valueOf(parentId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(AnswerMvcMocks.ANSWER_BODY, text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public List<CommentDto> getAnswerComments(long shopId, long uid, long answerId) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/partner/shop/" + shopId + "/answer/" + answerId + "/comments")
                .param("userId", String.valueOf(uid))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        DtoList<CommentDto> result = objectMapper.readValue(response, new TypeReference<DtoList<CommentDto>>() {
        });
        return result.getData();
    }

    public List<CommentTreeDto> getGradeComments(long shopId,
                                                 long uid,
                                                 long[] gradeIds,
                                                 Boolean isAsc) throws Exception {
        String response = getGradeComments(shopId, uid, gradeIds, isAsc, status().is2xxSuccessful());
        DtoList<CommentTreeDto> result = objectMapper.readValue(response, new TypeReference<DtoList<CommentTreeDto>>() {
        });
        return result.getData();
    }

    public String getGradeComments(long shopId,
                                   long uid,
                                   long[] gradeIds,
                                   Boolean isAsc,
                                   ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            get("/partner/shop/" + shopId + "/grade/comments")
                .param(GRADE_ID_KEY, Arrays.stream(gradeIds).mapToObj(String::valueOf).toArray(String[]::new))
                .param(UID_KEY, String.valueOf(uid))
                .param(SORT_KEY, isAsc == null ? null : isAsc ? Sort.ASC : Sort.DESC)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public String deleteComment(long shopId,
                                   long uid,
                                   long commentId,
                                   ResultMatcher resultMatcher) throws Exception {
        final String response = invokeAndRetrieveResponse(

            delete("/partner/shop/" + shopId + "/comment/" + commentId)
                .param("userId", String.valueOf(uid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
        return response;
    }
}
