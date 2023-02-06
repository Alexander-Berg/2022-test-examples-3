package ru.yandex.market.pers.area.controller;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import ru.yandex.market.pers.area.config.PersAreaConfig;
import ru.yandex.market.pers.area.controller.error.ApiErrors;
import ru.yandex.market.pers.area.model.PersAreaUserId;
import ru.yandex.market.pers.area.model.Template;
import ru.yandex.market.pers.area.model.UserNotification;
import ru.yandex.market.pers.area.model.UserNotificationLinkV1;
import ru.yandex.market.pers.area.model.request.UserNotificationCreatePlainTextRequest;
import ru.yandex.market.pers.area.model.request.UserNotificationCreateTemplateRequest;
import ru.yandex.market.pers.area.service.TemplateService;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 10.11.17
 */
public class UserNotificationControllerTest extends MarketUtilsMockedDbTest {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PersAreaConfig.API_DATE_TIME_FORMAT);

    @Autowired
    private TemplateService templateService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testUserNotificationCreatePlainTextRequestSerialization() throws Exception {
        UserNotificationCreatePlainTextRequest request = createPlainTextRequest();
        assertEquals(request, fromJson(toJson(request), UserNotificationCreatePlainTextRequest.class));
    }

    @Test
    public void testUserNotificationSerialization() throws Exception {
        UserNotification notification = createNotification();
        assertEquals(notification, fromJson(toJson(notification), UserNotification.class));
    }

    @Test
    public void createPlainTextNotification() throws Exception {
        UserNotificationCreatePlainTextRequest request = createPlainTextRequest();
        mockMvc.perform(post("/area/notification/plain")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.CREATED.value()))
            .andExpect(notification(request));
    }

    @Test
    public void createPlainTextNotificationInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/area/notification/plain")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andDo(print())
            .andExpect(status().is(HttpStatus.UNPROCESSABLE_ENTITY.value()))
            .andExpect(apiErrors());
    }

    @Test
    public void createTemplateNotificationInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/area/notification/template")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andDo(print())
            .andExpect(status().is(HttpStatus.UNPROCESSABLE_ENTITY.value()))
            .andExpect(apiErrors());
    }

    @Test
    public void createPlainTextNotificationNotSerializableRequestBody() throws Exception {
        mockMvc.perform(post("/area/notification/plain")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":{\"type\":\"bad_type\"}}"))
            .andDo(print())
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
            .andExpect(apiErrors());
    }

    @Test
    public void createTemplateNotification() throws Exception {
        String templateName = "my_template";
        templateService.addTemplate(new Template(templateName, "${message}", Template.Language.FTL));
        templateService.invalidateCache();

        String expectedText = "Hey, you!";
        JSONObject templateModel = new JSONObject();
        templateModel.put("message", expectedText);
        UserNotificationCreateTemplateRequest request = createTemplateRequest(
            new UserNotificationCreateTemplateRequest.TemplateInfo(templateName, templateModel)
        );
        mockMvc.perform(post("/area/notification/template")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.CREATED.value()))
            .andExpect(notification(UserNotificationCreatePlainTextRequest.build(request, expectedText)));
    }

    @Test
    public void createTemplateNotificationNotExistingTemplate() throws Exception {
        JSONObject templateModel = new JSONObject();
        templateModel.put("message", "u no c me");
        UserNotificationCreateTemplateRequest request = createTemplateRequest(
            new UserNotificationCreateTemplateRequest.TemplateInfo("not_existing", templateModel)
        );
        mockMvc.perform(post("/area/notification/template")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
            .andExpect(apiErrors());
    }

    @Test
    public void createTemplateNotificationWrongTemplateModel() throws Exception {
        String templateName = "my_template";
        templateService.addTemplate(new Template(templateName, "${message}", Template.Language.FTL));
        templateService.invalidateCache();

        UserNotificationCreateTemplateRequest request = createTemplateRequest(
            new UserNotificationCreateTemplateRequest.TemplateInfo(templateName, null)
        );
        mockMvc.perform(post("/area/notification/template")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()))
            .andExpect(apiErrors());
    }

    private UserNotification createNotification() {
        JSONObject linkParams = new JSONObject();
        linkParams.put("lpk1", "lpv1");
        linkParams.put("lpk2", 1);
        linkParams.put("lpk3", true);
        JSONArray array = new JSONArray(Arrays.asList(1, 2, 3));
        JSONObject payload = new JSONObject();
        payload.put("pk1", array);
        return new UserNotification(1L,
            new PersAreaUserId(PersAreaUserId.Type.UID, "21312"),
            "my_type",
            UserNotification.Status.READ,
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "body",
            new UserNotificationLinkV1(UserNotificationLinkV1.Target.LIST, true, linkParams),
            payload
        );
    }

    private UserNotificationCreateTemplateRequest createTemplateRequest(
        UserNotificationCreateTemplateRequest.TemplateInfo templateInfo) {
        JSONObject linkParams = new JSONObject();
        linkParams.put("lpk1", "lpv1");
        linkParams.put("lpk2", 1);
        linkParams.put("lpk3", true);
        JSONArray array = new JSONArray(Arrays.asList(1, 2, 3));
        JSONObject payload = new JSONObject();
        payload.put("pk1", array);
        return new UserNotificationCreateTemplateRequest(
            "my_type",
            new PersAreaUserId(PersAreaUserId.Type.UID, "23423"),
            new UserNotificationLinkV1(UserNotificationLinkV1.Target.LIST, false, linkParams),
            payload,
            templateInfo
        );
    }

    private UserNotificationCreatePlainTextRequest createPlainTextRequest() {
        JSONObject linkParams = new JSONObject();
        linkParams.put("lpk1", "lpv1");
        linkParams.put("lpk2", 1);
        linkParams.put("lpk3", true);
        JSONArray array = new JSONArray(Arrays.asList(1, 2, 3));
        JSONObject payload = new JSONObject();
        payload.put("pk1", array);
        return new UserNotificationCreatePlainTextRequest(
            "my_type",
            new PersAreaUserId(PersAreaUserId.Type.YANDEXUID, "874223"),
            new UserNotificationLinkV1(UserNotificationLinkV1.Target.COLLECTION, false, linkParams),
            payload,
            "my_plain_text"
        );
    }

    private ResultMatcher apiErrors() {
        return result -> {
            ApiErrors errors = fromJson(result.getResponse().getContentAsString(), ApiErrors.class);
            assertNotNull(errors);
            assertNotNull(errors.getFieldErrors());
            assertNotNull(errors.getGlobalErrors());
        };
    }

    private UserNotificationResultMatcher notification(UserNotificationCreatePlainTextRequest request) {
        return new UserNotificationResultMatcher(request);
    }

    private static Matcher<?> timeMatcher() {
        return new ArgumentMatcher<Object>() {
            @Override
            public boolean matches(Object argument) {
                String dateStr = (String) argument;
                if (dateStr == null || dateStr.isEmpty()) {
                    return false;
                }
                try {
                    DATE_TIME_FORMATTER.parse(dateStr);
                    return true;
                } catch (DateTimeParseException e) {
                    return false;
                }
            }
        };
    }

    private static void jsonObjectMatches(String json, String jsonPathStr, JSONObject expected) {
        DocumentContext docCtx = JsonPath.parse(json);
        JsonPath jsonPath = JsonPath.compile(jsonPathStr);
        JSONObject params = new JSONObject((Map)docCtx.read(jsonPath));
        assertTrue(params.similar(expected));
    }

    @SuppressWarnings("WeakerAccess")
    private static class UserNotificationResultMatcher implements ResultMatcher {
        private UserNotificationCreatePlainTextRequest request;
        private UserNotification.Status status = UserNotification.Status.NEW;

        public UserNotificationResultMatcher(UserNotificationCreatePlainTextRequest request) {
            this.request = request;
        }

        @Override
        public void match(MvcResult result) throws Exception {
            jsonPath("$.id").isNumber().match(result);
            jsonPath("$.body").value(request.getPlainText()).match(result);
            jsonPath("$.creationTs").value(timeMatcher()).match(result);
            jsonPath("$.modificationTs").value(timeMatcher()).match(result);
            jsonPath("$.status").value(status.getId()).match(result);
            jsonPath("$.type").value(request.getType()).match(result);
            jsonPath("$.userId.type").value(request.getUserId().getType().getId()).match(result);
            jsonPath("$.userId.value").value(request.getUserId().getValue()).match(result);
            if (request.getLinkV1() != null) {
                jsonPath("$.linkV1.target").value(request.getLinkV1().getTarget().getId()).match(result);
                jsonPath("$.linkV1.visual").value(request.getLinkV1().getVisual()).match(result);
                if (request.getLinkV1().getParams() != null) {
                    jsonObjectMatches(result.getResponse().getContentAsString(),
                        "$.linkV1.params",
                        request.getLinkV1().getParams());
                } else {
                    jsonPath("$.linkV1.params").doesNotExist().match(result);
                }
            } else {
                jsonPath("$.linkV1").doesNotExist().match(result);
            }
            if (request.getPayload() != null) {
                jsonObjectMatches(result.getResponse().getContentAsString(),
                    "$.payload",
                    request.getPayload());
            } else {
                jsonPath("$.payload").doesNotExist().match(result);
            }
        }
    }
}
