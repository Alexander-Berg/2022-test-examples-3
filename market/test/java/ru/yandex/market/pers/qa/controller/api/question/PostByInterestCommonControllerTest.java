package ru.yandex.market.pers.qa.controller.api.question;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.qa.client.avatarnica.AvararnicaInfoResponse;
import ru.yandex.market.pers.qa.client.avatarnica.AvatarnicaClient;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.dto.InterestIdDto;
import ru.yandex.market.pers.qa.controller.dto.PhotoDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.controller.request.AbstractQuestionRequest;
import ru.yandex.market.pers.qa.mock.mvc.PostMvcMocks;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Question;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.controller.request.AbstractQuestionRequest.TITLE_FIELD_MAX_LENGTH;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.03.2020
 */
public class PostByInterestCommonControllerTest extends AbstractQuestionCommonControllerTest {

    public static final String POST_TEXT = UUID.randomUUID().toString();
    public static final long ENTITY_ID = 3923370598L;
    public static final String POST_TITLE = "post title";

    @Autowired
    private PostMvcMocks postMvc;
    @Autowired
    private AvatarnicaClient avatarnicaClient;

    @Override
    public String doCreateQuestion(long entityId,
                                   long userId,
                                   String text,
                                   ResultMatcher resultMatcher) throws Exception {
        return postMvc.createInterestPost(entityId, userId, text, resultMatcher);
    }

    @Override
    public QuestionDto doGetQuestion(long questionId, long userId, ResultMatcher resultMatcher) throws Exception {
        return postMvc.getPost(questionId, userId, resultMatcher);
    }

    @Override
    public QuestionDto doGetQuestionYandexUid(long questionId,
                                              String yandexUid,
                                              ResultMatcher resultMatcher) throws Exception {
        return postMvc.getPostYandexUid(questionId, yandexUid, resultMatcher);
    }

    @Override
    public String doDeleteQuestion(long questionId,
                                   long userId,
                                   ResultMatcher resultMatcher) throws Exception {
        return postMvc.deletePost(questionId, userId, resultMatcher);
    }

    @Override
    public List<QuestionDto> doGetQuestionsBulk(List<Long> questionIds,
                                                UserType type,
                                                ResultMatcher resultMatcher) throws Exception {
        return postMvc.getPostsBulk(questionIds, type, resultMatcher);
    }

    @Override
    public void checkEntityId(QuestionDto question, long entityId) {
        assertEquals(QuestionDto.POST, question.getEntity());
        assertNull(question.getProductIdDto());
        assertNull(question.getCategoryIdDto());
        assertNotNull(question.getInterestIdDto());
        assertEquals(entityId, question.getInterestIdDto().getId());
        assertEquals(InterestIdDto.INTEREST, question.getInterestIdDto().getEntity());
    }

    @Override
    public boolean supportsAnswers() {
        return false;
    }

    @Test
    public void testPostWithPhotoCreation() throws Exception {
        final List<PhotoDto> photos = new ArrayList<>();
        photos.add(new PhotoDto("ns1", "group1", "one"));
        photos.add(new PhotoDto("ns2", "group2", "two"));
        photos.add(new PhotoDto("ns3", "group3", "three"));

        final String response = postMvc
                .createInterestPost(ENTITY_ID, UID, POST_TEXT, POST_TITLE, photos,
                    Collections.emptyList(), status().is2xxSuccessful());
        final List<PhotoDto> dbPhotos = objectMapper.readValue(response, QuestionDto.class).getPhotos();

        assertTrue((dbPhotos != null) && !dbPhotos.isEmpty());
        assertAll(
                () -> assertPhoto(photos.get(0), "ns1", "group1", "one"),
                () -> assertPhoto(photos.get(1), "ns2", "group2", "two"),
                () -> assertPhoto(photos.get(2), "ns3", "group3", "three")
        );
    }

    private void assertPhoto(PhotoDto photo, String ns, String group, String name) {
        assertAll(
                () -> assertEquals(ns, photo.getNamespace()),
                () -> assertEquals(group, photo.getGroupId()),
                () -> assertEquals(name, photo.getImageName())
        );
    }

    @Test
    public void testPostWithPhotoCreationAndModeration() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long entityId = 3923370598L;
        final List<PhotoDto> photos = new ArrayList<>();
        photos.add(new PhotoDto("ns1", "group1", "one"));

        String post = postMvc.createInterestPost(entityId, UID, text, POST_TITLE, photos,
            Collections.emptyList(), status().is2xxSuccessful());
        QuestionDto questionDto = objectMapper.readValue(post, QuestionDto.class);

        Mockito.when(avatarnicaClient.getInfo(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new AvararnicaInfoResponse(true, 0, 0, 0));
        Question question = questionService.getQuestionByIdInternal(questionDto.getId());
        assertEquals(question.getModState(), ModState.AWAITS_CONTENT_MODERATION);

        photoService.autoModerate();
        questionService.autoFilterQuestions();

        question = questionService.getQuestionById(questionDto.getId());
        assertEquals(question.getModState(), ModState.NEW);
    }

    @Test
    public void testPostWithPhotoCreationAndModerationFailed() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long entityId = 3923370598L;
        final List<PhotoDto> photos = new ArrayList<>();
        photos.add(new PhotoDto("ns1", "group1", "one"));

        String post = postMvc.createInterestPost(entityId, UID, text, POST_TITLE, photos,
            Collections.emptyList(), status().is2xxSuccessful());
        QuestionDto questionDto = objectMapper.readValue(post, QuestionDto.class);

        Mockito.when(avatarnicaClient.getInfo(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new AvararnicaInfoResponse(true, 200, 200, 200));
        Question question = questionService.getQuestionByIdInternal(questionDto.getId());
        assertEquals(question.getModState(), ModState.AWAITS_CONTENT_MODERATION);

        photoService.autoModerate();
        questionService.autoFilterQuestions();

        question = questionService.getQuestionByIdInternal(questionDto.getId());
        assertEquals(question.getModState(), ModState.AUTO_FILTER_REJECTED);
    }

    @Test
    public void testPostWithAlmostTooManyPhotos() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long entityId = 3923370598L;
        final List<PhotoDto> photos = new ArrayList<>();
        for (int i = 0; i < AbstractQuestionRequest.PHOTOS_MAX_QUANTITY; i++) {
            photos.add(new PhotoDto("ns", "group", "name" + i));
        }

        String post = postMvc.createInterestPost(entityId, UID, text, POST_TITLE, photos,
            Collections.emptyList(), status().is2xxSuccessful());
        QuestionDto questionDto = objectMapper.readValue(post, QuestionDto.class);

        assertEquals(AbstractQuestionRequest.PHOTOS_MAX_QUANTITY, questionDto.getPhotos().size());
    }

    @Test
    public void testPostWithTooManyPhotos() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long entityId = 3923370598L;
        final List<PhotoDto> photos = new ArrayList<>();
        for (int i = 0; i <= AbstractQuestionRequest.PHOTOS_MAX_QUANTITY; i++) {
            photos.add(new PhotoDto("ns", "group", "name" + i));
        }

        String response = postMvc.createInterestPost(entityId, UID, text, POST_TITLE, photos,
            Collections.emptyList(), status().is4xxClientError());
        JSONObject jsonObject = new JSONObject(response);
        assertEquals("VALIDATION_ERROR", jsonObject.getString("code"));
        assertTrue(jsonObject.getJSONArray("error").getString(0)
            .endsWith("photos size must be between 0 and " + AbstractQuestionRequest.PHOTOS_MAX_QUANTITY));
    }

    @Test
    public void testPostWithProducts() throws Exception {
        List<Long> expectedProductIds = Arrays.asList(2313L, 123L, 45L, 542341L, 10L);

        String response = postMvc.createInterestPost(ENTITY_ID, UID, POST_TEXT, expectedProductIds,
                status().is2xxSuccessful());
        List<Long> productIds = objectMapper.readValue(response, QuestionDto.class).getProductIds();

        assertEquals(expectedProductIds, productIds);
    }

    @Test
    public void testPostWithMax10Products() throws Exception {
        List<Long> expectedProductIds = LongStream.range(1, 11).boxed().collect(Collectors.toList());
        postMvc.createInterestPost(ENTITY_ID, UID, POST_TEXT, expectedProductIds, status().is2xxSuccessful());
        expectedProductIds = LongStream.range(1, 12).boxed().collect(Collectors.toList());
        postMvc.createInterestPost(ENTITY_ID, UID, POST_TEXT, expectedProductIds, status().is4xxClientError());
    }

    @Test
    public void testPostWithTwoPostProducts() throws Exception {
        List<Long> firstPostProductIds = Arrays.asList(2313L, 123L, 45L, 542341L, 10L);
        postMvc.createInterestPost(ENTITY_ID, UID, POST_TEXT, firstPostProductIds, status().is2xxSuccessful());

        List<Long> secondPostProductIds = Arrays.asList(23413L, 1234L, 45L, 542341L, 10L);
        String response = postMvc
                .createInterestPost(ENTITY_ID + 1, UID, POST_TEXT, secondPostProductIds, status().is2xxSuccessful());
        List<Long> productIds = objectMapper.readValue(response, QuestionDto.class).getProductIds();

        assertEquals(secondPostProductIds, productIds);
    }

    @Test
    public void testPostWithTwoPostWithEqualProductIds() throws Exception {
        List<Long> productIds = Arrays.asList(2313L, 123L, 45L, 542341L, 10L);
        postMvc.createInterestPost(ENTITY_ID, UID, POST_TEXT, productIds, status().is2xxSuccessful());
        postMvc.createInterestPost(ENTITY_ID, UID, POST_TEXT, productIds, status().is4xxClientError());
    }

    @Test
    public void testPostWithEmptyProducts() throws Exception {
        List<Long> expectedProductIds = Collections.emptyList();

        String response = postMvc.createInterestPost(ENTITY_ID, UID, POST_TEXT, expectedProductIds, status().is2xxSuccessful());
        List<Long> productIds = objectMapper.readValue(response, QuestionDto.class).getProductIds();

        assertEquals(expectedProductIds, productIds);
    }

    @Test
    public void testPostWithDoubledProducts() throws Exception {
        String response = postMvc.createInterestPost(ENTITY_ID, UID, POST_TEXT,
                List.of(23413L, 1234L, 45L, 1234L, 1234L), status().is2xxSuccessful());
        List<Long> productIds = objectMapper.readValue(response, QuestionDto.class).getProductIds();

        assertEquals(List.of(23413L, 1234L, 45L), productIds);
    }

    @Test
    public void testPostWithoutTitle() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long entityId = 3923370598L;

        String response = postMvc.createInterestPost(entityId, UID, text, "", Collections.emptyList(),
            Collections.emptyList(), status().is4xxClientError());
        JSONObject jsonObject = new JSONObject(response);
        assertEquals("VALIDATION_ERROR", jsonObject.getString("code"));
        assertEquals("Post should contains 'Title' field.",
            jsonObject.getString("error"));

    }

    @Test
    public void testWithTooLongTitle() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long entityId = 3923370598L;

        String response = postMvc.createInterestPost(entityId, UID, text, "a".repeat(TITLE_FIELD_MAX_LENGTH + 1),
            Collections.emptyList(), Collections.emptyList(), status().is4xxClientError());
        JSONObject jsonObject = new JSONObject(response);
        assertEquals("VALIDATION_ERROR", jsonObject.getString("code"));
        assertEquals("title size must be between 0 and " + TITLE_FIELD_MAX_LENGTH,
            jsonObject.getJSONArray("error").getString(0));

        response = postMvc.createInterestPost(entityId, UID, text, "a".repeat(TITLE_FIELD_MAX_LENGTH),
            Collections.emptyList(), Collections.emptyList(), status().is2xxSuccessful());

        String title = objectMapper.readValue(response, QuestionDto.class).getTitle();

        assertEquals(80, title.length());
    }

    @Test
    public void testQuestionLockWithTitle() throws Exception {
        final String text = UUID.randomUUID().toString();
        final String title = UUID.randomUUID().toString();

        String expectedLock = String.format("0_%s_2_%s_%s", UID, INTEREST_ID, (title + " " + text).hashCode());
        assertFalse(hasLock(expectedLock));

        postMvc.createInterestPost(INTEREST_ID, UID, text, title,
            Collections.emptyList(),Collections.emptyList(), status().is2xxSuccessful());
        assertTrue(hasLock(expectedLock));
    }

    @Test
    public void testEmptyPost() throws Exception {
        // tests all questions because of AbstractQuestionRequest
        String POST_REQUEST_BODY = "{\n" +
            "  \"text\": null,\n" +
            "  \"title\": \"title\"\n" +
            "}";
        String response = invokeAndRetrieveResponse(
            post("/post/interest/1/UID/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(POST_REQUEST_BODY)
                .accept(MediaType.APPLICATION_JSON),
            status().is4xxClientError());
        JSONObject jsonObject = new JSONObject(response);
        assertEquals("VALIDATION_ERROR", jsonObject.getString("code"));
        assertEquals("text must not be empty",
            jsonObject.getJSONArray("error").getString(0));
    }
}
