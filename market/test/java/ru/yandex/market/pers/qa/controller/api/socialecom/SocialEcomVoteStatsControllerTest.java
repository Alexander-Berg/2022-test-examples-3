package ru.yandex.market.pers.qa.controller.api.socialecom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pers.qa.client.dto.socialecom.VoteStatsDto;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.model.DbUser;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.model.post.PostV2;
import ru.yandex.market.pers.qa.service.post.PostV2Service;
import ru.yandex.market.pers.qa.utils.socialecom.VoteStatisticTestHelper;
import ru.yandex.market.util.FormatUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SocialEcomVoteStatsControllerTest extends QAControllerTest {

    private static final String POST_TEXT = "ТЕКСТ ТЕКСТ ТЕКСТ?? ИЛИ ТЕКСТ!";
    private static final DbUser BRAND_USER = new DbUser(UserType.BRAND, String.valueOf(1L));
    private static final DbUser BUSINESS_USER = new DbUser(UserType.BUSINESS, String.valueOf(1L));
    private static final Long P_UID = 10000L;

    @Autowired
    private VoteStatisticTestHelper helper;

    @Autowired
    private PostV2Service postV2Service;

    @Test
    public void testGetVoteStats() throws Exception {

        PostV2 post = helper.buildGeneralPost(P_UID, BRAND_USER);
        PostV2 secondPost = helper.buildGeneralPost(P_UID + 1, BRAND_USER);
        secondPost.setText(POST_TEXT + POST_TEXT);
        long postId = postV2Service.createPostV2GetId(post, new SecurityData());
        long secondPostId = postV2Service.createPostV2GetId(secondPost, new SecurityData());

        helper.createPostVote(postId, 123L);
        helper.createPostVote(postId, 124L);
        helper.createPostVote(postId, 125L);

        helper.createPostVote(secondPostId, 123L);
        helper.createPostVote(secondPostId, 124L);
        helper.createPostVote(secondPostId, 125L);

        helper.refreshVotesMatView();

        String response = invokeAndRetrieveResponse(get("/socialecom/votes/BRAND/" + BRAND_USER.getId())
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE), status().is2xxSuccessful());

        VoteStatsDto dto = FormatUtils.fromJson(response, VoteStatsDto.class);
        assertNotNull(dto);
        assertEquals(6, dto.getLikesCount());
    }

    @Test
    public void test4xxResponse() throws Exception {
        //its nothing there - expect 404
        invokeAndRetrieveResponse(get("/socialecom/votes/BRAND/9882")
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE), status().is4xxClientError());

        //illegal type
        invokeAndRetrieveResponse(get("/socialecom/votes/ILLEGALTYPE/1234")
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE), status().is4xxClientError());
    }
}
