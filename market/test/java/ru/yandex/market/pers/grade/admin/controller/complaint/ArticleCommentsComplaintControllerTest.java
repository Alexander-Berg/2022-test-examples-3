package ru.yandex.market.pers.grade.admin.controller.complaint;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pers.grade.admin.article.api.dto.Count;
import ru.yandex.market.pers.qa.client.QaClient;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.model.ComplaintType;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ArticleCommentsComplaintControllerTest extends BaseComplaintsControllerTest {

    @Autowired
    private QaClient qaClient;

    @Test
    public void testGetComplaintsCount() throws Exception {
        int complaintCount = 10;
        when(qaClient.getArticleComplaintsCount()).thenReturn(new CountDto(complaintCount));
        String response = mvc.perform(get("/api/article/comment/complaints/count")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andDo(print())
            .andReturn().getResponse().getContentAsString();
        Count count = objectMapper.readValue(response, Count.class);
        Assert.assertEquals(complaintCount, count.getCount());
    }

    @Test
    public void testGetComplaints() throws Exception {
        mockQaClient();
        String response = mvc.perform(get("/api/article/comment/complaints")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andDo(print())
            .andReturn().getResponse().getContentAsString();

        JSONObject json = new JSONObject(response);
        Assert.assertEquals(2, json.getJSONArray("data").length());
    }

    @Test
    public void testModerate() throws Exception {
        mockQaClient();

        long moderatorId = FAKE_MODERATOR_ID;
        mvc.perform(post("/api/article/comment/complaints")
            .content(getContent("data/qa/article/moderate.json"))
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(status().is2xxSuccessful())
            .andDo(print())
            .andReturn().getResponse().getContentAsString();

        List<CommentComplaintModerationLogObject> list = getComplaints();

        final Optional<CommentComplaintModerationLogObject> first = list.stream()
            .filter(it -> it.complaintId == 1).findFirst();
        checkComplaint(moderatorId, first, 1, ComplaintType.COMMENT_ARTICLE);

        final Optional<CommentComplaintModerationLogObject> second = list.stream()
            .filter(it -> it.complaintId == 2).findFirst();
        checkComplaint(moderatorId, second, 2, ComplaintType.COMMENT_ARTICLE);
    }

}
