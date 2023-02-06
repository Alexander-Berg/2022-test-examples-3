package ru.yandex.market.pers.qa.mock.mvc;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.controller.dto.PhotoDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.POST_ID_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.03.2020
 */
@Service
public class PostMvcMocks extends AbstractMvcMocks {
    protected static final String POST_REQUEST_BODY = "{\n" +
        "  \"text\": \"%s\",\n" +
        "  \"title\": \"%s\",\n" +
        " \"photos\": %s\n," +
        " \"productIds\": %s\n" +
        "}";

    protected static final String COMMENT_BODY = "{\n" +
        "  \"text\": \"%s\"\n" +
        "}";
    public static final String PHOTO_JSON_TEMPLATE =
        "{ \"entity\": \"photo\", \"groupId\": \"%s\", \"imageName\": \"%s\", \"namespace\": \"%s\" }";
    public static final String DEF_POST_TITLE = "def title";

    public QuestionDto getPost(long questionId, long userId) throws Exception {
        return getPost(questionId, userId, status().is2xxSuccessful());
    }

    public QuestionDto getPost(long postId, long userId, ResultMatcher resultMatcher) throws Exception {
        return FormatUtils.fromJson(invokeAndRetrieveResponse(
            get("/post/" + postId + "/UID/" + userId),
            resultMatcher
        ), QuestionDto.class);
    }


    public QuestionDto getPostYandexUid(long questionId, String yandexUid) throws Exception {
        return getPostYandexUid(questionId, yandexUid, status().is2xxSuccessful());
    }

    public QuestionDto getPostYandexUid(long postId,
                                        String yandexUid,
                                        ResultMatcher resultMatcher) throws Exception {
        return FormatUtils.fromJson(invokeAndRetrieveResponse(
            get("/post/" + postId + "/YANDEXUID/" + yandexUid),
            resultMatcher
        ), QuestionDto.class);
    }

    public void deletePost(long id) throws Exception {
        deletePost(id, ControllerTest.UID, status().is2xxSuccessful());
    }

    public String deletePost(long id, long uid, ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            delete("/post/" + id).param("userId", String.valueOf(uid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public List<QuestionDto> getPostsBulk(List<Long> questionIds,
                                          UserType type,
                                          ResultMatcher resultMatcher) throws Exception {
        String user = type == UserType.UID ? "UID/" + ControllerTest.UID : "YANDEXUID/" + ControllerTest.YANDEXUID;
        DtoList<QuestionDto> result = objectMapper.readValue(invokeAndRetrieveResponse(
            get("/post/" + user + "/bulk")
                .param(POST_ID_KEY, questionIds.stream().map(Object::toString).toArray(String[]::new)),
            resultMatcher
        ), new TypeReference<DtoList<QuestionDto>>() {
        });
        return result.getData();
    }

    public List<QuestionDto> getPostsBulkFromDb(List<Long> postIds,
                                                long userId,
                                                ResultMatcher resultMatcher) throws Exception {
        List<QuestionDto> list = new ArrayList<>();
        for (Long id : postIds) {
            list.add(
                objectMapper.readValue(invokeAndRetrieveResponse(
                    get("/post/" + id + "/UID/" + userId),
                    resultMatcher),
                    QuestionDto.class));
        }
        return list;
    }

    public long createInterestPost() throws Exception {
        return createInterestPost(UUID.randomUUID().toString());
    }

    public long createInterestPost(long interestId) throws Exception {
        return createInterestPost(interestId, ControllerTest.UID);
    }

    public long createInterestPost(long interestId, long userId) throws Exception {
        return createInterestPost(interestId, userId, UUID.randomUUID().toString());
    }

    public long createInterestPost(String text) throws Exception {
        return createInterestPost(ControllerTest.INTEREST_ID, ControllerTest.UID, text);
    }

    public long createInterestPost(long interestId, long userId, String text) throws Exception {
        String response = createInterestPost(interestId, userId, text, status().is2xxSuccessful());
        return objectMapper.readValue(response, QuestionDto.class).getId();
    }

    public String createInterestPost(long interestId, long userId, String text, ResultMatcher resultMatcher)
        throws Exception {
        return createInterestPost(interestId, userId, text, DEF_POST_TITLE, Collections.emptyList(), Collections.emptyList(), resultMatcher);
    }

    public String createInterestPost(long interestId,
                                     long userId,
                                     String text,
                                     List<Long> productIds,
                                     ResultMatcher resultMatcher) throws Exception {
        return createInterestPost(interestId, userId, text, DEF_POST_TITLE, Collections.emptyList(), productIds, resultMatcher);
    }

    public String createInterestPost(long interestId,
                                     long userId,
                                     String text,
                                     String title,
                                     List<PhotoDto> photos,
                                     List<Long> productIds,
                                     ResultMatcher resultMatcher) throws Exception {
        String photoStr = photos
            .stream()
            .map(p -> String.format(PHOTO_JSON_TEMPLATE, p.getGroupId(), p.getImageName(), p.getNamespace()))
            .collect(Collectors.joining(",", "[", "]"));
        String productIdsStr = productIds.stream().map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
        return invokeAndRetrieveResponse(
            post("/post/interest/" + interestId + "/UID/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(POST_REQUEST_BODY, text, title, photoStr, productIdsStr))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public Map<Long, QuestionDto> getPostsMap(long userId, List<Long> postIds) throws Exception {
        return getPosts(userId, postIds).stream()
            .collect(Collectors.toMap(
                QuestionDto::getId,
                x -> x
            ));
    }

    public List<QuestionDto> getPosts(long userId, List<Long> postIds) throws Exception {
        return getPostsBulkFromDb(postIds, userId, status().is2xxSuccessful());
    }

}
