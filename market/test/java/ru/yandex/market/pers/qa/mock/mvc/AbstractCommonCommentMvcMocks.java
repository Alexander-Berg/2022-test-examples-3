package ru.yandex.market.pers.qa.mock.mvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.utils.ControllerConstants;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.controller.dto.CommentResultDto;
import ru.yandex.market.pers.qa.utils.CommonUtils;
import ru.yandex.market.util.ExecUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.GRADE_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.PARENT_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.SPLIT_LEVEL_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.UID_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.09.2019
 */
public abstract class AbstractCommonCommentMvcMocks extends AbstractMvcMocks {
    public static final long UGLY_LIMIT_TO_DISABLE_TREE_REVERSE = 422442;
    public static final long UGLY_LIMIT_HACK_VALUE = 4;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    private final String entityName;

    public long createEntity(long entityId) {
        return entityId;
    }

    public long createEntity() {
        return createEntity(-1);
    }

    public AbstractCommonCommentMvcMocks(String entityName) {
        this.entityName = entityName;
    }

    public abstract CommentProject getProject();

    protected String getEntityIdKey() {
        return entityName + "Id";
    }

    public BiFunction<Long, Long, CommentResultDto> getCommentResultDtoByParentAndSplitLevel(long entityId,
                                                                                             boolean isYandexUid) {
        return (parentId, splitLevel) -> {
            try {
                if (isYandexUid) {
                    return getResponseDtoByYandexUid(entityId, ControllerTest.YANDEXUID, parentId, splitLevel);
                } else {
                    return getResponseDtoByUid(entityId, ControllerTest.UID, parentId, splitLevel);
                }
            } catch (Exception e) {
                throw ExecUtils.silentError(e);
            }
        };
    }

    public BiFunction<Long, Long, DtoList<CommentResultDto>> getBulkCommentPreview(long[] entityIds,
                                                                                   boolean isYandexUid) {
        return (splitLevel, limit) -> {
            try {
                if (isYandexUid) {
                    return getBulkPreviewResponseDtoByYandexUid(entityIds, ControllerTest.YANDEXUID, splitLevel, limit);
                } else {
                    return getBulkPreviewResponseDtoByUid(entityIds, ControllerTest.UID, splitLevel, limit);
                }
            } catch (Exception e) {
                throw ExecUtils.silentError(e);
            }
        };
    }

    public long getFirstLevelCommentsCount(long entityId) throws Exception {
        return getFirstLevelCommentsCount(entityId, null);
    }

    public long getFirstLevelCommentsCount(long entityId, Long splitLevel) throws Exception {
        final String url = String.format("/comment/" + entityName + "/%s/count", entityId);
        final CountDto countDto = objectMapper.readValue(invokeAndRetrieveResponse(
            get(url)
                .param(SPLIT_LEVEL_KEY, splitLevel != null ? splitLevel.toString() : null),
            status().is2xxSuccessful()),
            new TypeReference<CountDto>() {
            });
        return countDto.getCount();
    }

    public Map<Long, CountDto> getFirstLevelCommentsCountBulk(long[] entityIds, Long splitLevel) {
        return getFirstLevelCommentsCountBulk(entityIds, splitLevel, null);
    }

    public Map<Long, CountDto> getFirstLevelCommentsCountBulk(long[] entityIds, Long splitLevel, Long userId) {
        final String url = "/comment/" + entityName + "/preview/count";
        final Map<Long, CountDto> result = parseValue(invokeAndRetrieveResponse(
            get(url)
                .param(GRADE_ID_KEY, CommonUtils.toStrArray(entityIds))
                .param(SPLIT_LEVEL_KEY, splitLevel != null ? splitLevel.toString() : null)
                .param(UID_KEY, userId != null ? userId.toString() : null),
            status().is2xxSuccessful()),
            new TypeReference<>() {
            });
        return result;
    }

    public Map<Long, CountDto> getFirstLevelCommentsBulkCount(long[] entityIds, Long splitLevel) throws Exception {
        final String url = "/comment/" + entityName + "/preview/count";
        MockHttpServletRequestBuilder request = get(url);
        request.param(getEntityIdKey(), CommonUtils.toStrArray(entityIds));
        request.param(SPLIT_LEVEL_KEY, splitLevel != null ? splitLevel.toString() : null);
        return objectMapper.readValue(invokeAndRetrieveResponse(
            request,
            status().is2xxSuccessful()),
            new TypeReference<Map<Long, CountDto>>() {
            });
    }

    public String getFirstLevelCommentsBulkCountEmpty4xx() throws Exception {
        final String url = "/comment/" + entityName + "/preview/count";
        MockHttpServletRequestBuilder request = get(url);
        request.param(getEntityIdKey(), "");
        return invokeAndRetrieveResponse(
            request,
            status().is4xxClientError());
    }

    public void deleteComment(long commentId) throws Exception {
        long userId = jdbcTemplate.queryForObject(
            "select user_id::bigint from com.comment where id = ?",
            Long.class,
            commentId);

        invokeAndRetrieveResponse(delete(String.format("/comment/" + entityName + "/%s?userId=%s", commentId, userId)),
            status().is2xxSuccessful());
    }

    public String deleteComment4xx(long commentId, long userId) throws Exception {
        return invokeAndRetrieveResponse(delete(String
                .format("/comment/" + entityName + "/%s?userId=%s", commentId, userId)),
            status().is4xxClientError());
    }

    public long createComment(long entityId, long userId, Long parentCommentId, String body) throws Exception {
        return createCommentDto(entityId, userId, parentCommentId, body).getId();
    }

    public CommentDto createCommentDto(long entityId, long userId, Long parentCommentId, String body) throws Exception {
        return createComment(entityId,
            userId,
            body,
            parentCommentId == null
                ? x -> x
                : x -> x.param(PARENT_ID_KEY, parentCommentId.toString())
        );
    }

    public CommentDto createComment(long entityId,
                                    long userId,
                                    String body,
                                    Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> reqBuilder
    ) throws Exception {
        String url = String.format("/comment/" + entityName + "/%s/UID/%s", entityId, userId);

        return objectMapper.readValue(invokeAndRetrieveResponse(
            reqBuilder.apply(post(url))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
            status().is2xxSuccessful()),
            new TypeReference<CommentDto>() {
            });
    }

    public String createComment4xx(long entityId, long userId, String body) throws Exception {
        return invokeAndRetrieveResponse(
            post("/comment/" + entityName + "/" + entityId + "/UID/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
            status().is4xxClientError());
    }

    public String createComment4xx(long entityId, long userId, long parentId, String body) throws Exception {
        return invokeAndRetrieveResponse(
            post("/comment/" + entityName + "/" + entityId + "/UID/" + userId)
                .param(PARENT_ID_KEY, String.valueOf(parentId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
            status().is4xxClientError());
    }

    public String editComment(long commentId,
                              long userId,
                              String body,
                              ResultMatcher resultMatcher,
                              Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> reqBuilder
    ) throws Exception {
        String url = String.format("/comment/" + entityName + "/%s/UID/%s", commentId, userId);

        return invokeAndRetrieveResponse(
            reqBuilder.apply(patch(url))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
            resultMatcher);
    }

    public List<CommentDto> getCommentsByYandexUid(long entityId, String yandexUid) throws Exception {
        return getResponseDtoByYandexUid(entityId, yandexUid).getData();
    }

    public CommentResultDto getResponseDtoByYandexUid(long entityId, String yandexUid) throws Exception {
        return getResponseDtoByYandexUid(entityId, yandexUid, null);
    }

    public CommentResultDto getResponseDtoByYandexUid(long entityId,
                                                      String yandexUid,
                                                      Long parentId) throws Exception {
        return getResponseDtoByYandexUid(entityId, yandexUid, parentId, null);
    }

    private CommentResultDto getResponseDtoByYandexUid(long entityId,
                                                       String yandexUid,
                                                       Long parentId,
                                                       Long splitLevel) throws Exception {
        return getResponseDto(String.format("/comment/" + entityName + "/%s/YANDEXUID/%s", entityId, yandexUid),
            parentId,
            splitLevel,
            null,
            null);
    }

    public List<CommentDto> getCommentsByUid(long entityId, long userId) throws Exception {
        return getResponseDtoByUid(entityId, userId).getData();
    }

    public CommentResultDto getResponseDtoByUid(long entityId, long userId) throws Exception {
        return getResponseDto(String.format("/comment/" + entityName + "/%s/UID/%s", entityId, userId),
            null,
            null,
            null,
            null);
    }

    public CommentResultDto getResponseDtoByUid(long entityId, long userId, Long parentId) throws Exception {
        return getResponseDto(String.format("/comment/" + entityName + "/%s/UID/%s", entityId, userId),
            parentId,
            null,
            null,
            null);
    }

    private CommentResultDto getResponseDtoByUid(long entityId,
                                                 long userId,
                                                 Long parentId,
                                                 Long splitLevel) throws Exception {
        return getResponseDto(String.format("/comment/" + entityName + "/%s/UID/%s", entityId, userId),
            parentId,
            splitLevel,
            null,
            null);
    }

    private DtoList<CommentResultDto> getBulkPreviewResponseDtoByUid(long[] entityIds,
                                                                     long userId,
                                                                     Long splitLevel,
                                                                     Long limit) throws Exception {
        return getBulkCommentPreview(String.format("/comment/" + entityName + "/preview/UID/%s", userId),
            entityIds, splitLevel, limit);
    }

    private DtoList<CommentResultDto> getBulkPreviewResponseDtoByYandexUid(long[] entityIds,
                                                                           String yandexUid,
                                                                           Long splitLevel,
                                                                           Long limit) throws Exception {
        return getBulkCommentPreview(String.format("/comment/" + entityName + "/preview/YANDEXUID/%s", yandexUid),
            entityIds, splitLevel, limit);
    }


    public CommentResultDto getCommentsWithBorderIdAndLimit(long entityId,
                                                            boolean isYandexUid,
                                                            Long parentId,
                                                            long splitLevel,
                                                            Long borderId,
                                                            Long limit) throws Exception {
        if (isYandexUid) {
            return getResponseDtoByYandexUidAndBorderId(entityId,
                ControllerTest.YANDEXUID,
                parentId,
                splitLevel,
                limit,
                borderId);
        } else {
            return getResponseDtoByUidAndBorderIdId(entityId,
                ControllerTest.UID,
                parentId,
                splitLevel,
                limit,
                borderId);
        }
    }

    public CommentResultDto getResponseDtoByYandexUidAndBorderId(long entityId,
                                                                 String yandexUid,
                                                                 Long parentId,
                                                                 Long splitLevel,
                                                                 Long limit,
                                                                 Long borderId) throws Exception {
        return getResponseDto(String.format("/comment/" + entityName + "/%s/YANDEXUID/%s", entityId, yandexUid),
            parentId,
            splitLevel,
            limit,
            borderId);
    }

    public CommentResultDto getResponseDtoByUidAndBorderIdId(long entityId,
                                                             long userId,
                                                             Long parentId,
                                                             Long splitLevel,
                                                             Long limit,
                                                             Long borderId) throws Exception {
        return getResponseDto(String.format("/comment/" + entityName + "/%s/UID/%s", entityId, userId),
            parentId,
            splitLevel,
            limit,
            borderId);
    }

    private CommentResultDto getResponseDto(String url,
                                            Long parentId,
                                            Long splitLevel,
                                            Long limit,
                                            Long borderId) throws Exception {
        final MockHttpServletRequestBuilder request = get(url);

        if (parentId != null) {
            request.param("parentId", String.valueOf(parentId));
        }

        if (splitLevel != null) {
            request.param("splitLevel", String.valueOf(splitLevel));
        }

        if (limit != null) {
            if (limit == UGLY_LIMIT_TO_DISABLE_TREE_REVERSE) {
                request.param("limit", String.valueOf(UGLY_LIMIT_HACK_VALUE));
            } else {
                request.param("limit", String.valueOf(limit));
            }
        }

        if (borderId != null) {
            request.param("borderId", String.valueOf(borderId));
        }

        // ugly hack to prevent more parameters to this request
        if (limit != null && limit == UGLY_LIMIT_TO_DISABLE_TREE_REVERSE) {
            request.param(ControllerConstants.REVERSE_TREE_KEY, "false");
        }

        return objectMapper.readValue(invokeAndRetrieveResponse(
            request,
            status().is2xxSuccessful()),
            new TypeReference<CommentResultDto>() {
            });
    }

    private DtoList<CommentResultDto> getBulkCommentPreview(String url,
                                                            long[] entityIds,
                                                            Long splitLevel,
                                                            Long limit) throws Exception {
        final MockHttpServletRequestBuilder request = get(url);

        for (long entityId : entityIds) {
            request.param(getEntityIdKey(), String.valueOf(entityId));
        }

        if (splitLevel != null) {
            request.param("splitLevel", String.valueOf(splitLevel));
        }

        if (limit != null) {
            request.param("limit", String.valueOf(limit));
        }

        return objectMapper.readValue(invokeAndRetrieveResponse(
            request,
            status().is2xxSuccessful()),
            new TypeReference<DtoList<CommentResultDto>>() {
            });
    }

    public String getCommentsBulkCountWithoutMapping(long[] entityIds,
                                                     ResultMatcher expected) throws Exception {
        MockHttpServletRequestBuilder builder = get("/comment/" + entityName + "/count");
        if (entityIds != null && entityIds.length > 0) {
            builder.param(getEntityIdKey(), Arrays.stream(entityIds)
                .boxed()
                .map(String::valueOf)
                .toArray(String[]::new));
        } else {
            builder.param(getEntityIdKey(), "");
        }
        return invokeAndRetrieveResponse(
            builder,
            expected);
    }

    public Map<Long, CountDto> getCommentsBulkCount(long[] entityIds, ResultMatcher expected) throws Exception {
        return objectMapper.readValue(getCommentsBulkCountWithoutMapping(entityIds, expected),
            new TypeReference<Map<Long, CountDto>>() {
            });
    }

    public String getCommentsResponse4xx(long entityId, long userId, long borderId, long limit) throws Exception {
        return invokeAndRetrieveResponse(
            get(String.format("/comment/" + entityName + "/%s/UID/%s?borderId=%s&limit=%s",
                entityId, userId, borderId, limit)),
            status().is4xxClientError());
    }

    public String getCommentsResponseNoLimit(long entityId, long userId, long borderId) throws Exception {
        return invokeAndRetrieveResponse(
            get(String.format("/comment/" + entityName + "/%s/UID/%s?borderId=%s",
                entityId, userId, borderId)),
            status().is2xxSuccessful());
    }

    public String getCommentsResponseForFormat(long entityId, long userId) throws Exception {
        return invokeAndRetrieveResponse(
            get(String.format("/comment/" + entityName + "/%s/UID/%s", entityId, userId)),
            status().is2xxSuccessful());
    }

    public List<CommentDto> getAllComments(long entityId) throws Exception {
        return getResponseDtoByUidAndBorderIdId(entityId, ControllerTest.UID, null, 1L, null, null).getData();
    }

    public List<CommentDto> getParentComments(long entityId, long parentId) throws Exception {
        return getResponseDtoByUidAndBorderIdId(entityId, ControllerTest.UID, parentId, null, null, null).getData();
    }

}
