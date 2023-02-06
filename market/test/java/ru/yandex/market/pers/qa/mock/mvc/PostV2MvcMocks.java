package ru.yandex.market.pers.qa.mock.mvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.controller.dto.MediaContentDto;
import ru.yandex.market.pers.qa.controller.dto.PhotoDto;
import ru.yandex.market.pers.qa.controller.dto.VideoDto;
import ru.yandex.market.pers.qa.controller.dto.post.PostV2Dto;
import ru.yandex.market.pers.qa.controller.dto.post.PostV2LinkDto;
import ru.yandex.market.pers.qa.controller.dto.post.PostV2Request;
import ru.yandex.market.pers.qa.controller.dto.post.PostV2ResultDto;
import ru.yandex.market.pers.qa.model.post.LinkType;
import ru.yandex.market.pers.service.common.video.VideoHostingCallback;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.BORDER_ID_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.LIMIT_KEY;
import static ru.yandex.market.pers.qa.client.utils.ControllerConstants.UID_KEY;

/**
 * @author grigor-vlad
 * 23.06.2022
 */
@Service
public class PostV2MvcMocks extends AbstractMvcMocks {
    public static final long DEFAULT_AUTHOR_ID = 123L;
    public static final long DEFAULT_P_UID = 98765L;
    public static final String BRAND = "BRAND";
    public static final String BUSINESS = "BUSINESS";

    public long createBrandPostV2() throws Exception {
        return createBrandPostV2(UUID.randomUUID().toString(), 1, 0, 0);
    }

    public long createBrandPostV2(int photoCount, int videoCount, int linkCount) throws Exception {
        return createBrandPostV2(UUID.randomUUID().toString(), photoCount, videoCount, linkCount);
    }

    public long createBrandPostV2(String postText, int photoCount, int videoCount, int linkCount) throws Exception {
        return createBrandPostV2(postText, photoCount, videoCount, linkCount, status().is2xxSuccessful());
    }

    public long createBrandPostV2(String postText, int photoCount, int videoCount, int linkCount,
                                  ResultMatcher resultMatcher) throws Exception {
        PostV2Request request = buildDefaultRequest(postText, photoCount, videoCount, linkCount);
        String response = createBrandPostV2(request, BRAND, DEFAULT_AUTHOR_ID, DEFAULT_P_UID, resultMatcher);
        return objectMapper.readValue(response, PostV2Dto.class).getId();
    }

    public long createBrandPostV2(PostV2Request request, long brandId, long pUid) throws Exception {
        String response = createBrandPostV2(request, BRAND, brandId, pUid, status().is2xxSuccessful());
        return objectMapper.readValue(response, PostV2Dto.class).getId();
    }

    public long createBusinessPostV2(PostV2Request request, long businessId, long pUid) throws Exception {
        String response = createBrandPostV2(request, BUSINESS, businessId, pUid, status().is2xxSuccessful());
        return objectMapper.readValue(response, PostV2Dto.class).getId();
    }

    public String createBrandPostV2(PostV2Request request, String authorType, long authorId, long pUid,
                                    ResultMatcher expected) throws Exception {
        return invokeAndRetrieveResponse(
            post("/api/post/v2/" + authorType + "/" + authorId)
                .param(UID_KEY, String.valueOf(pUid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON),
            expected);
    }

    public void deletePostFromBrand(long postId, long userId) throws Exception {
        deletePost(BRAND, postId, userId, status().is2xxSuccessful());
    }

    public void deletePostFromBusiness(long postId, long userId) throws Exception {
        deletePost(BUSINESS, postId, userId, status().is2xxSuccessful());
    }

    public void deletePost(String authorType, long postId, long userId, ResultMatcher expected) throws Exception {
        invokeAndRetrieveResponse(
            delete("/api/post/v2/" + authorType + "/" + postId)
                .param(UID_KEY, String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            expected
        );
    }

    public PostV2Dto getPostById(long postId, long userId) throws Exception {
        String response = getPostById(postId, userId, status().is2xxSuccessful());
        return objectMapper.readValue(response, PostV2Dto.class);
    }

    public String getPostById(long postId, long userId, ResultMatcher expected) throws Exception {
        return invokeAndRetrieveResponse(
            get("/api/post/v2/UID/" + userId + "/" + postId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            expected
        );
    }

    public PostV2ResultDto getPagedBrandPostsV2(long brandId, long pUid, Long limit, Long borderId) throws Exception {
        String response = getPagedPostsV2(BRAND, brandId, pUid, limit, borderId);
        return objectMapper.readValue(response, PostV2ResultDto.class);
    }

    public PostV2ResultDto getPagedBusinessPostsV2(long brandId, long pUid, Long limit, Long borderId) throws Exception {
        String response = getPagedPostsV2(BUSINESS, brandId, pUid, limit, borderId);
        return objectMapper.readValue(response, PostV2ResultDto.class);
    }

    public String getPagedPostsV2(String authorType, long authorId, long pUid, Long limit, Long borderId) throws Exception {
        MockHttpServletRequestBuilder getRq = get("/api/post/v2/" + authorType + "/" + authorId)
            .param(UID_KEY, String.valueOf(pUid))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);
        if (limit != null) {
            getRq.param(LIMIT_KEY, String.valueOf(limit));
        }
        if (borderId != null) {
            getRq.param(BORDER_ID_KEY, String.valueOf(borderId));
        }

        return invokeAndRetrieveResponse(getRq, status().is2xxSuccessful());
    }

    public CountDto getBrandPostsCount(long brandId, long pUid) throws Exception {
        return objectMapper.readValue(getPostsCount(BRAND, brandId, pUid), CountDto.class);
    }

    public CountDto getBusinessPostsCount(long businessId, long pUid) throws Exception {
        return objectMapper.readValue(getPostsCount(BUSINESS, businessId, pUid), CountDto.class);
    }

    public String getPostsCount(String authorType, long authorId, long pUid) throws Exception {
        return invokeAndRetrieveResponse(
            get("/api/post/v2/" + authorType + "/" + authorId + "/count")
                .param(UID_KEY, String.valueOf(pUid))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void videoHostingCallback(VideoHostingCallback callback) throws Exception {
        videoHostingCallback(callback, status().is2xxSuccessful());
    }

    public void videoHostingCallback(VideoHostingCallback callback, ResultMatcher expected) throws Exception {
        invokeAndRetrieveResponse(
            post("/video/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(callback))
                .accept(MediaType.APPLICATION_JSON),
            expected
        );
    }

    public PostV2Request buildDefaultRequest(String postText, int photoCount, int videoCount, int linkCount) {
        PostV2Request request = new PostV2Request();
        request.setText(postText);

        //set media content
        List<MediaContentDto> content = new ArrayList<>();
        for (int i = 0; i < photoCount; i++) {
            content.add(new PhotoDto("ns", "gr", "imageName" + i));
        }
        for (int i = 0; i < videoCount; i++) {
            content.add(new VideoDto("video" + i));
        }
        request.setContent(content);

        //set links
        if (linkCount != 0) {
            List<PostV2LinkDto> links = new ArrayList<>();
            for (int i = 0; i < linkCount; i++) {
                links.add(new PostV2LinkDto(LinkType.SKU, "link" + i));
            }
            request.setLinks(links);
        }
        return request;
    }

}
