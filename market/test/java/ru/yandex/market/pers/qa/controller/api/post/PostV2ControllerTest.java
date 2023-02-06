package ru.yandex.market.pers.qa.controller.api.post;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.MediaContentDto;
import ru.yandex.market.pers.qa.controller.dto.PhotoDto;
import ru.yandex.market.pers.qa.controller.dto.VideoDto;
import ru.yandex.market.pers.qa.controller.dto.post.PostV2Dto;
import ru.yandex.market.pers.qa.controller.dto.post.PostV2Request;
import ru.yandex.market.pers.qa.controller.dto.post.PostV2ResultDto;
import ru.yandex.market.pers.qa.mock.mvc.PostV2MvcMocks;
import ru.yandex.market.pers.qa.model.DbUser;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.model.post.PostV2;
import ru.yandex.market.pers.qa.service.PhotoService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.mock.mvc.PostV2MvcMocks.DEFAULT_AUTHOR_ID;
import static ru.yandex.market.pers.qa.mock.mvc.PostV2MvcMocks.DEFAULT_P_UID;

/**
 * @author grigor-vlad
 * 23.06.2022
 */
public class PostV2ControllerTest extends QAControllerTest {
    private static final String DEFAULT_AUTHOR_TYPE = "BUSINESS";
    public static final long AUTHOR_ID = 123L;
    public static final long P_UID = 98765L;
    public static final String POST_V2_TEXT = "Неплохой текст для комментария";

    @Autowired
    public PostV2MvcMocks postV2MvcMocks;
    @Autowired
    public PhotoService photoService;

    @Test
    public void testMinSizeOfContentArray() throws Exception {
        PostV2Request request = new PostV2Request();
        request.setText(POST_V2_TEXT);
        postV2MvcMocks.createBrandPostV2(request, DEFAULT_AUTHOR_TYPE, AUTHOR_ID, P_UID, status().is4xxClientError());
    }

    @Test
    public void testPostCreationByBrand() throws Exception {
        int photoCount = 3;
        int videoCount = 1;
        int linkCount = 2;
        PostV2Request request = postV2MvcMocks.buildDefaultRequest(POST_V2_TEXT, photoCount, videoCount, linkCount);

        //create post by brand
        long postId = postV2MvcMocks.createBrandPostV2(request, AUTHOR_ID, P_UID);

        //check post existence in post.post
        List<PostV2> postResult = qaJdbcTemplate.query("select * from post.post where id = ?",
            PostV2::valueOf, postId);

        assertEquals(1, postResult.size());
        PostV2 post = postResult.get(0);
        assertEquals(postId, post.getId());
        assertEquals(State.NEW, post.getState());
        assertEquals(ModState.AWAITS_CONTENT_MODERATION, post.getModState());
        assertEquals(new DbUser(UserType.BRAND, String.valueOf(AUTHOR_ID)), post.getAuthor());
        assertEquals(P_UID, post.getpUid());

        //check post photo existence
        assertEquals(photoCount, qaJdbcTemplate.queryForObject(
            "select count(*) from qa.photo where entity_type = ? and entity_id = ?",
            Long.class, QaEntityType.POST_V2.getValue(), String.valueOf(postId))
        );

        //check post video existence
        assertEquals(videoCount, qaJdbcTemplate.queryForObject(
            "select count(*) from post.video where entity_type = ? and entity_id = ?",
            Long.class, QaEntityType.POST_V2.getValue(), String.valueOf(postId))
        );

        //check post link existence
        assertEquals(linkCount, qaJdbcTemplate.queryForObject(
            "select count(*) from post.link where entity_type = ? and entity_id = ?",
            Long.class, QaEntityType.POST_V2.getValue(), String.valueOf(postId))
        );
    }

    @Test
    public void testPostCreationByBusiness() throws Exception {
        int photoCount = 3;
        int videoCount = 2;
        int linkCount = 1;
        PostV2Request request = postV2MvcMocks.buildDefaultRequest(POST_V2_TEXT, photoCount, videoCount, linkCount);

        //create post by business
        long postId = postV2MvcMocks.createBusinessPostV2(request, AUTHOR_ID, P_UID);

        //check post existence
        assertEquals(1, qaJdbcTemplate.queryForObject(
            "select count(*) from post.post where id = ?",
            Long.class, postId)
        );

        //check post photo existence
        assertEquals(photoCount, qaJdbcTemplate.queryForObject(
            "select count(*) from qa.photo where entity_type = ? and entity_id = ?",
            Long.class, QaEntityType.POST_V2.getValue(), String.valueOf(postId))
        );

        //check post video existence
        assertEquals(videoCount, qaJdbcTemplate.queryForObject(
            "select count(*) from post.video where entity_type = ? and entity_id = ?",
            Long.class, QaEntityType.POST_V2.getValue(), String.valueOf(postId))
        );

        //check post link existence
        assertEquals(linkCount, qaJdbcTemplate.queryForObject(
            "select count(*) from post.link where entity_type = ? and entity_id = ?",
            Long.class, QaEntityType.POST_V2.getValue(), String.valueOf(postId))
        );
    }

    @Test
    public void testGetPostById() throws Exception {
        int photoCount = 1;
        int videoCount = 3;
        int linkCount = 4;

        long postId =  postV2MvcMocks.createBrandPostV2(photoCount, videoCount, linkCount);

        //post doesn't exist, because of bad mod_state
        postV2MvcMocks.getPostById(postId, P_UID, status().is4xxClientError());

        forceUpdatePostState(postId, ModState.NEW);
        PostV2Dto post = postV2MvcMocks.getPostById(postId, P_UID);

        assertNotNull(post);
        assertEquals(new AuthorIdDto(UserType.BRAND, String.valueOf(AUTHOR_ID)), post.getAuthor());
        assertEquals(photoCount  + videoCount, post.getContent().size());
        assertEquals(linkCount, post.getLinks().size());
    }

    @Test
    public void testGetAllPostsByAuthor() throws Exception {
        postV2MvcMocks.createBrandPostV2();
        postV2MvcMocks.createBrandPostV2();
        postV2MvcMocks.createBrandPostV2();

        PostV2ResultDto brandPostsV2 = postV2MvcMocks.getPagedBrandPostsV2(AUTHOR_ID, P_UID, null, null);
        assertEquals(3, brandPostsV2.getData().size());
    }

    @Test
    public void testPagedPostsByAuthor() throws Exception {
        long postId1 = postV2MvcMocks.createBrandPostV2();
        long postId2 = postV2MvcMocks.createBrandPostV2();
        long postId3 = postV2MvcMocks.createBrandPostV2();

        PostV2ResultDto pagedBrandPosts = postV2MvcMocks.getPagedBrandPostsV2(AUTHOR_ID, P_UID, 2L, null);
        assertEquals(2, pagedBrandPosts.getData().size());
        Set<Long> postIdsSet = pagedBrandPosts.getData().stream()
            .map(PostV2Dto::getId)
            .collect(Collectors.toSet());
        assertEquals(Set.of(postId2, postId3), postIdsSet);
        assertNull(pagedBrandPosts.getBorderId());

        PostV2ResultDto pagedBrandPosts2 = postV2MvcMocks.getPagedBrandPostsV2(AUTHOR_ID, P_UID, 2L, postId2);
        assertEquals(1, pagedBrandPosts2.getData().size());
        assertEquals(postId1, pagedBrandPosts2.getData().get(0).getId());
        assertEquals(postId2, pagedBrandPosts2.getBorderId());

        PostV2ResultDto pagedBrandPosts3 = postV2MvcMocks.getPagedBrandPostsV2(AUTHOR_ID, P_UID, 2L, postId1);
        assertTrue(pagedBrandPosts3.getData().isEmpty());
        assertEquals(postId1, pagedBrandPosts3.getBorderId());
    }

    @Test
    public void testGetPostsCount() throws Exception {
        int publishedPostsCount = 5;
        for (int i = 0; i < publishedPostsCount; i++) {
            long postId = postV2MvcMocks.createBrandPostV2();
            forceUpdatePostState(postId, ModState.AUTO_FILTER_PASSED);
        }
        //create some posts unmoderated
        int unpublishedPostsCount = 3;
        for (int i = 0; i < unpublishedPostsCount; i++) {
            postV2MvcMocks.createBrandPostV2();
        }

        //check all posts count
        @SuppressWarnings("ConstantConditions")
        long allBrandPostsCount = qaJdbcTemplate.queryForObject(
            "select count(*) from post.post where user_type = ? and user_id = ?",
            Long.class,
            UserType.BRAND.getValue(), String.valueOf(DEFAULT_AUTHOR_ID)
        );
        assertEquals(publishedPostsCount + unpublishedPostsCount, allBrandPostsCount);

        //check posts count from controller
        CountDto brandPostsCount = postV2MvcMocks.getBrandPostsCount(DEFAULT_AUTHOR_ID, DEFAULT_P_UID);
        assertEquals(publishedPostsCount, brandPostsCount.getCount());
    }

    @Test
    public void testPostContentOrdering() throws Exception {
        PostV2Request request = new PostV2Request();
        request.setText(POST_V2_TEXT);

        List<MediaContentDto> contentDtos = List.of(
            new VideoDto("video0"),
            new PhotoDto("ns", "gr", "image1"),
            new PhotoDto("ns", "gr", "image2"),
            new VideoDto("video3"),
            new PhotoDto("ns", "gr", "image4")
        );
        request.setContent(contentDtos);

        long postId = postV2MvcMocks.createBrandPostV2(request, AUTHOR_ID, P_UID);
        forceUpdatePostState(postId, ModState.NEW);
        PostV2Dto post = postV2MvcMocks.getPostById(postId, P_UID);

        assertEquals(contentDtos.size(), post.getContent().size());
        for (int i = 0; i < contentDtos.size(); i++) {
            assertEquals(contentDtos.get(i), post.getContent().get(i));
        }
    }

    @Test
    public void testBrandPostDeletion() throws Exception {
        long brandPostId = postV2MvcMocks.createBrandPostV2();

        //check post in appropriate state
        assertEquals(State.NEW, qaJdbcTemplate.queryForObject(
            "select state from post.post where id = ?",
            (rs, rowNum) -> State.valueOf(rs.getInt("state")),
            brandPostId)
        );

        //check state after deletion
        postV2MvcMocks.deletePostFromBrand(brandPostId, P_UID);
        assertEquals(State.DELETED, qaJdbcTemplate.queryForObject(
            "select state from post.post where id = ?",
            (rs, rowNum) -> State.valueOf(rs.getInt("state")),
            brandPostId)
        );
    }

    @Test
    public void testBusinessPostDeletion() throws Exception {
        long brandPostId = postV2MvcMocks.createBrandPostV2();

        //check post in appropriate state
        assertEquals(State.NEW, qaJdbcTemplate.queryForObject(
            "select state from post.post where id = ?",
            (rs, rowNum) -> State.valueOf(rs.getInt("state")),
            brandPostId)
        );

        //check state after deletion
        postV2MvcMocks.deletePostFromBusiness(brandPostId, P_UID);
        assertEquals(State.DELETED, qaJdbcTemplate.queryForObject(
            "select state from post.post where id = ?",
            (rs, rowNum) -> State.valueOf(rs.getInt("state")),
            brandPostId)
        );
    }


    private void forceUpdatePostState(long postId, ModState modState) {
        qaJdbcTemplate.update(
            "update post.post set mod_state = ? where id = ?",
            modState.getValue(),
            postId
        );
    }

}
