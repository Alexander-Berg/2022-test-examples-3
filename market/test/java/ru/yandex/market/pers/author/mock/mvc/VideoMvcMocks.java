package ru.yandex.market.pers.author.mock.mvc;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.author.client.api.PersAuthorApiConstants;
import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;
import ru.yandex.market.pers.author.security.SecurityData;
import ru.yandex.market.pers.author.video.dto.VideoModStatesDto;
import ru.yandex.market.pers.author.video.dto.VideoRequest;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.AUTHOR_ID_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.ID_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.MODEL_ID_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.PAGE_NUM_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.PAGE_SIZE_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.UID_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.VIDEO_ID_KEY;


@Service
public class VideoMvcMocks extends AbstractMvcMocks {

    private static final String VIDEO_SAVE_BODY = "{\n" +
        "    \"videoId\" : \"%s\",\n" +
        "    \"modelId\" : %s,\n" +
        "    \"title\" : \"%s\"\n" +
        "  }";

    public VideoInfoDto saveVideoByUid(String videoId, String title, long modelId, long userId)
        throws Exception {
        String response = saveVideoByUid(videoId, title, modelId, userId, status().is2xxSuccessful());
        return objectMapper.readValue(response, new TypeReference<VideoInfoDto>() {
        });
    }

    public String saveVideoByUid(String videoId, String title, long modelId, long userId, ResultMatcher resultMatcher)
        throws Exception {

        String body = String.format(VIDEO_SAVE_BODY, videoId, modelId, title);

        return saveVideoByUid(body, userId, resultMatcher);
    }

    public String saveVideoByUid(String body, long userId, ResultMatcher resultMatcher)
        throws Exception {

        return invokeAndRetrieveResponse(
            post("/video/UID/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(HttpHeaders.EMPTY)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public VideoInfoDto saveVideoByUidWithSource(String videoId, long modelId, long userId,
                                                 String source) {
        String requestResult = invokeAndRetrieveResponse(
            post("/video/UID/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(SecurityData.HEADER_X_SOURCE, source)
                .content(FormatUtils.toJson(new VideoRequest(modelId, videoId)))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());

        return FormatUtils.fromJson(requestResult, VideoInfoDto.class);
    }

    public VideoInfoDto saveVideoByUidWithIp(String videoId, long modelId, long userId, String ip, Integer port) {
        MockHttpServletRequestBuilder builder = post("/video/UID/" + userId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(SecurityData.HEADER_X_REAL_IP, ip)
            .content(FormatUtils.toJson(new VideoRequest(modelId, videoId)))
            .accept(MediaType.APPLICATION_JSON);

        if (port != null) {
            builder.header(SecurityData.HEADER_X_REAL_PORT, port);
        }

        String requestResult = invokeAndRetrieveResponse(
            builder,
            status().is2xxSuccessful());

        return FormatUtils.fromJson(requestResult, VideoInfoDto.class);
    }

    public void deleteVideo(String videoId, long userId, ResultMatcher resultMatcher)
        throws Exception {

        invokeAndRetrieveResponse(
            delete("/video/" + videoId)
                .param(UID_KEY, String.valueOf(userId))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public void deleteVideoByVideoId(String videoId, long userId, ResultMatcher resultMatcher)
        throws Exception {

        invokeAndRetrieveResponse(
            delete("/video")
                .param(UID_KEY, String.valueOf(userId))
                .param(VIDEO_ID_KEY, videoId)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public void deleteVideoById(long id, long userId, ResultMatcher resultMatcher)
        throws Exception {

        invokeAndRetrieveResponse(
            delete("/video")
                .param(UID_KEY, String.valueOf(userId))
                .param(ID_KEY, String.valueOf(id))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public DtoList<VideoInfoDto> getVideoInfoDtosByUid(List<Long> videoIds, long userId)
        throws Exception {

        return objectMapper.readValue(getVideoInfoByUid(videoIds, userId), new TypeReference<DtoList<VideoInfoDto>>() {
        });
    }

    public DtoPager<VideoInfoDto> getPagedVideoInfoByParams(long userId, List<Long> videoIds) throws Exception {
        return getPagedVideoInfoByParams(userId, null, videoIds);
    }

    public DtoPager<VideoInfoDto> getPagedVideoInfoByParams(long userId,
                                                            Long authorId,
                                                            List<Long> videoIds)
        throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/video/UID/" + userId + "/byParams/pager")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);

        if (videoIds != null && !videoIds.isEmpty()) {
            requestBuilder = requestBuilder.param(ID_KEY,
                videoIds.stream().map(Object::toString).toArray(String[]::new));
        }

        if (authorId != null) {
            requestBuilder = requestBuilder.param(AUTHOR_ID_KEY, authorId.toString());
        }

        return objectMapper.readValue(
            invokeAndRetrieveResponse(requestBuilder, status().is2xxSuccessful()),
            new TypeReference<DtoPager<VideoInfoDto>>() {
            });
    }

    public List<VideoInfoDto> getVideoInfoForModeration(long userId) throws Exception {
        return getVideoInfoForModeration(userId, null);
    }

    public List<VideoInfoDto> getVideoInfoForModeration(long userId, Long size)
        throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/video/UID/" + userId + "/moderation")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);

        if (size != null) {
            requestBuilder = requestBuilder.param(PAGE_SIZE_KEY, size.toString());
        }

        return objectMapper.readValue(
            invokeAndRetrieveResponse(requestBuilder, status().is2xxSuccessful()),
            new TypeReference<List<VideoInfoDto>>() {
            });
    }

    public long getVideoCountForModeration(long userId) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/video/UID/" + userId + "/moderation/count")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);

        return objectMapper.readValue(
            invokeAndRetrieveResponse(requestBuilder, status().is2xxSuccessful()),
            new TypeReference<Long>() {
            });
    }

    public String getVideoInfoByUid(List<Long> videoIds, long userId) throws Exception {
        return invokeAndRetrieveResponse(
            get("/video/UID/" + userId + "/bulk")
                .param(ID_KEY, videoIds.stream().map(Object::toString).toArray(String[]::new))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public DtoPager<VideoInfoDto> getVideoDtosByModelByUid(long modelId,
                                                           long userId,
                                                           int pageNum,
                                                           int pageSize) throws Exception {
        return objectMapper.readValue(getVideosByModelByUid(modelId, userId, pageNum, pageSize),
            new TypeReference<DtoPager<VideoInfoDto>>() {
            });
    }

    public DtoPager<VideoInfoDto> getVideoDtosByModelByYandexUid(long modelId,
                                                                 String yandexUid,
                                                                 int pageNum,
                                                                 int pageSize) throws Exception {
        return objectMapper.readValue(getVideosByModelByYandexUid(modelId, yandexUid, pageNum, pageSize),
            new TypeReference<DtoPager<VideoInfoDto>>() {
            });
    }

    public String getVideosByModelByUid(long modelId, long userId, int pageNum, int pageSize) throws Exception {
        return invokeAndRetrieveResponse(
            get("/video/model/" + modelId + "/UID/" + userId)
                .param(MODEL_ID_KEY, String.valueOf(modelId))
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public String getVideosByModelByYandexUid(long modelId,
                                              String yandexUid,
                                              int pageNum,
                                              int pageSize) throws Exception {
        return invokeAndRetrieveResponse(
            get("/video/model/" + modelId + "/YANDEXUID/" + yandexUid)
                .param(MODEL_ID_KEY, String.valueOf(modelId))
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public DtoList<VideoInfoDto> getVideoInfoDtosByYandexUid(List<Long> videoIds, String yandexUid)
        throws Exception {

        return objectMapper.readValue(getVideoInfoByYandexUid(videoIds, yandexUid),
            new TypeReference<DtoList<VideoInfoDto>>() {
            });
    }

    public String getVideoInfoByYandexUid(List<Long> videoIds, String yandexUid) throws Exception {
        return invokeAndRetrieveResponse(
            get("/video/YANDEXUID/" + yandexUid + "/bulk")
                .param(ID_KEY, videoIds.stream().map(Object::toString).toArray(String[]::new))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public String getInternalVideoInfo(List<Long> ids) throws Exception {
        return invokeAndRetrieveResponse(
            get("/video/internal/bulk")
                .param(ID_KEY, ids.stream().map(Object::toString).toArray(String[]::new))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public DtoList<Long> getAuthorModelVideoAgitationByUidDto(long userId)
        throws Exception {

        return getAuthorModelVideoAgitationByUidDto(userId, PersAuthorApiConstants.AGITATION_SRC_LIMIT);
    }

    public DtoList<Long> getAuthorModelVideoAgitationByUidDto(long userId, int pageSize)
        throws Exception {

        return objectMapper.readValue(getAuthorModelVideoAgitationByUid(userId, pageSize),
            new TypeReference<DtoList<Long>>() {
            });
    }

    public String getAuthorModelVideoAgitationByUid(long userId, int pageSize) throws Exception {
        return invokeAndRetrieveResponse(
            get("/profile/author/UID/" + userId + "/video/model/agitation")
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public String getAuthorModelVideoAgitationByUid(long userId, ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            get("/profile/author/UID/" + userId + "/video/model/agitation")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        );
    }

    public void callback(String body, ResultMatcher resultMatcher)
        throws Exception {

        invokeAndRetrieveResponse(
            post("/video/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public VideoModStatesDto banVideo(long id, ResultMatcher resultMatcher)
        throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/video/ban/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher),
            new TypeReference<VideoModStatesDto>() {
            });
    }

    public VideoModStatesDto publishVideo(long id, ResultMatcher resultMatcher)
        throws Exception {

        return objectMapper.readValue(invokeAndRetrieveResponse(
            post("/video/publish/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher),
            new TypeReference<VideoModStatesDto>() {
            });
    }

    public DtoPager<VideoInfoDto> getVideoByUser(long userId, long pageNum, long pageSize) throws Exception {
        return objectMapper.readValue(getVideoByUser(userId, pageNum, pageSize, status().is2xxSuccessful()),
            new TypeReference<DtoPager<VideoInfoDto>>() {
            });
    }

    public DtoPager<VideoInfoDto> getPublicVideoByUser(long userId, long pageNum, long pageSize) throws Exception {
        return objectMapper.readValue(getPublicVideoByUser(userId, pageNum, pageSize, status().is2xxSuccessful()),
            new TypeReference<DtoPager<VideoInfoDto>>() {
            });
    }

    public String getVideoByUser(long userId,
                                 long pageNum,
                                 long pageSize,
                                 ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            get("/profile/author/UID/" + userId + "/video")
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize))
                .param(UID_KEY, String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        );
    }

    public String getPublicVideoByUser(long userId,
                                       long pageNum,
                                       long pageSize,
                                       ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            get("/profile/author/UID/" + userId + "/video/public")
                .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                .param(PAGE_SIZE_KEY, String.valueOf(pageSize))
                .param(UID_KEY, String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        );
    }

    public String sendComplaint(long id, String body, ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            post(String.format("/video/complaint/%s/create", id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

}
