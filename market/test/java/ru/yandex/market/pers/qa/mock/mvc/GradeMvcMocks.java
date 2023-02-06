package ru.yandex.market.pers.qa.mock.mvc;

import java.util.Arrays;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.qa.client.dto.CommentTreeDto;
import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.dto.EntityCountDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.GRADE_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.LIMIT_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.SORT_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.UID_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 09.09.2019
 */
@Service
public class GradeMvcMocks extends AbstractMvcMocks {
    public DtoList<CommentTreeDto> getCommentsBulkUid(long uid, long[] gradeIds, String sort) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/comment/grade/UID/" + uid)
                .param(GRADE_ID_KEY, Arrays.stream(gradeIds).mapToObj(String::valueOf).toArray(String[]::new))
                .param(SORT_KEY, sort)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return objectMapper.readValue(response, new TypeReference<DtoList<CommentTreeDto>>() {
        });
    }

    public DtoList<CommentTreeDto> getCommentsBulkYandexUid(String yandexuid, long[] gradeIds) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/comment/grade/YANDEXUID/" + yandexuid)
                .param(GRADE_ID_KEY, Arrays.stream(gradeIds).mapToObj(String::valueOf).toArray(String[]::new))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return objectMapper.readValue(response, new TypeReference<DtoList<CommentTreeDto>>() {
        });
    }

    public DtoList<CommentTreeDto> getCommentsBulkCapiPreview(long[] gradeIds, long limit) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/comment/grade/capi/preview")
                .param(GRADE_ID_KEY, Arrays.stream(gradeIds).mapToObj(String::valueOf).toArray(String[]::new))
                .param(LIMIT_KEY, String.valueOf(limit))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return objectMapper.readValue(response, new TypeReference<DtoList<CommentTreeDto>>() {
        });
    }

    public DtoList<EntityCountDto> getCommentsCountBulkUid(long uid, long[] gradeIds) throws Exception {
        return objectMapper.readValue(getCommentsCountBulkUidDto(uid, gradeIds, status().is2xxSuccessful()),
            new TypeReference<DtoList<EntityCountDto>>() {
        });
    }

    public String getCommentsCountBulkUidDto(long uid, long[] gradeIds, ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            get("/comment/grade/UID/" + uid + "/count")
                .param(GRADE_ID_KEY, Arrays.stream(gradeIds).mapToObj(String::valueOf).toArray(String[]::new))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public DtoList<EntityCountDto> getCommentsCountBulkYandexUid(String yandexuid, long[] gradeIds) throws Exception {
        final String response = invokeAndRetrieveResponse(
            get("/comment/grade/YANDEXUID/" + yandexuid + "/count")
                .param(GRADE_ID_KEY, Arrays.stream(gradeIds).mapToObj(String::valueOf).toArray(String[]::new))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return objectMapper.readValue(response, new TypeReference<DtoList<EntityCountDto>>() {
        });
    }

    public DtoList<CommentTreeDto> getCommentsBulkForPapi(long shopId, long uid, long[] gradeIds) {
        final String response = invokeAndRetrieveResponse(
            get("/partner/api/" + shopId + "/grade/comments")
                .param(UID_KEY, String.valueOf(uid))
                .param(GRADE_ID_KEY, Arrays.stream(gradeIds).mapToObj(String::valueOf).toArray(String[]::new))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return parseValue(response, new TypeReference<DtoList<CommentTreeDto>>() {
        });
    }

    public void deleteComment(long commentId, long userId) {
        invokeAndRetrieveResponse(
            delete("/comment/grade/" + commentId )
                .param(UID_KEY, String.valueOf(userId))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }
}
