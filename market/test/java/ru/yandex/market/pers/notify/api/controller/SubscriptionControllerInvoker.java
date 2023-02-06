package ru.yandex.market.pers.notify.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatusDto;
import ru.yandex.market.pers.notify.model.subscription.ReturnMode;
import ru.yandex.market.pers.notify.model.web.PersNotifyTag;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SubscriptionControllerInvoker {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;

    private ObjectMapper objectMapper = new ObjectMapper();

    public String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    public List<EmailSubscription> getSubscriptions(Uid uid, NotificationType[] types) throws Exception {
        MvcResult result = mockMvc.perform(get("/subscription/")
                .contentType(MediaType.APPLICATION_JSON)
                .param(PersNotifyTag.UID, String.valueOf(uid.getValue()))
                .param(PersNotifyTag.SUBSCRIPTION_TYPE, Stream.of(types).map(NotificationType::toString).collect(Collectors.joining(","))))
                .andDo(print())
                .andReturn();
        return getSubscriptions(result);
    }

    public EmailSubscription getSubscription(long subscriptionId) throws Exception {
        MvcResult result = mockMvc.perform(get("/subscription/")
            .contentType(MediaType.APPLICATION_JSON)
            .param(PersNotifyTag.ID, String.valueOf(subscriptionId)))
            .andDo(print())
            .andReturn();
        return getSubscriptions(result).get(0);
    }

    public List<EmailSubscription> getSubscriptions(Identity... identities) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/subscription/")
                .contentType(MediaType.APPLICATION_JSON);
        for (Identity identity : identities) {
            String tag = getIdentityTag(identity);
            requestBuilder.param(tag, String.valueOf(identity.getValue()));
        }
        MvcResult result = mockMvc.perform(requestBuilder)
                .andDo(print())
                .andReturn();

        return getSubscriptions(result);
    }

    public List<EmailSubscription> getSubscriptions(Uid uid, Integer page, Integer pageSize) {
        MvcResult result;
        try {
            result = mockMvc.perform(get("/subscription/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param(PersNotifyTag.UID, String.valueOf(uid.getValue()))
                    .param(PersNotifyTag.PAGE, String.valueOf(page))
                    .param(PersNotifyTag.PAGE_SIZE, String.valueOf(pageSize)))
                    .andDo(print())
                    .andReturn();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return getSubscriptions(result);
    }

    public List<EmailSubscription> getSubscriptions(Uid uid) {
        return getSubscriptions(uid, (NotificationType) null);
    }

    public List<EmailSubscription> getSubscriptions(Uid uid, NotificationType type) {
        return getSubscriptions(uid, type, (Long) null);
    }

    public List<EmailSubscription> getSubscriptions(Uid uid, NotificationType type,
                                                     EmailSubscriptionStatus... statuses) {
        return getSubscriptions(uid, type, null, statuses);
    }

    public List<EmailSubscription> getSubscriptions(Uid uid, NotificationType type, Long modelId,
                                                     EmailSubscriptionStatus... statuses) {
        return getSubscriptions(uid, type, modelId, null, null,
                Arrays.stream(statuses).map(Object::toString).toArray(String[]::new));
    }

    public List<EmailSubscription> getSubscriptionsByQuestionId(Uid uid, NotificationType type, Long questionId,
                                                    EmailSubscriptionStatus... statuses) {
        return getSubscriptions(uid, type, null, questionId, null,
            Arrays.stream(statuses).map(Object::toString).toArray(String[]::new));
    }

    public void getSubscriptionsWithoutQuestionId(NotificationType type, ReturnMode returnMode,
                                                  Consumer<MvcResult> resultConsumer) {
        MockHttpServletRequestBuilder builder = get("/subscription/")
                .contentType(MediaType.APPLICATION_JSON)
                .param(PersNotifyTag.SUBSCRIPTION_TYPE, String.valueOf(type))
                .param(PersNotifyTag.RETURN_MODE, String.valueOf(returnMode));

        MvcResult result;
        try {
            result = mockMvc.perform(builder)
                    .andDo(print())
                    .andReturn();
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
        resultConsumer.accept(result);
    }

    public void getSubscriptionsByQuestionId(NotificationType type, Long questionId,
                                             ReturnMode returnMode, Consumer<MvcResult> resultConsumer) {
        MockHttpServletRequestBuilder builder = get("/subscription/")
                .contentType(MediaType.APPLICATION_JSON)
                .param(PersNotifyTag.SUBSCRIPTION_TYPE, String.valueOf(type))
                .param(PersNotifyTag.QUESTION_ID, String.valueOf(questionId))
                .param(PersNotifyTag.RETURN_MODE, String.valueOf(returnMode));

        MvcResult result;
        try {
            result = mockMvc.perform(builder)
                    .andDo(print())
                    .andReturn();
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
        resultConsumer.accept(result);
    }

    public List<EmailSubscription> getSubscriptionsByStreamId(Uid uid, NotificationType type, Long streamId,
                                                              EmailSubscriptionStatus... statuses) {
        return getSubscriptions(uid, type, null, null, null, streamId,
            Arrays.stream(statuses).map(Object::toString).toArray(String[]::new), null);
    }

    public List<EmailSubscription> getSubscriptionsByAnswerId(Uid uid, NotificationType type, Long answerId,
        EmailSubscriptionStatus... statuses) {
        return getSubscriptions(uid, type, null, null, answerId,
            Arrays.stream(statuses).map(Object::toString).toArray(String[]::new));
    }

    public List<EmailSubscription> getSubscriptions(Uid uid, NotificationType type,
                                                     String[] statuses) {
        return getSubscriptions(uid, type, null, null, null, statuses);
    }

    public List<EmailSubscription> getSubscriptions(Uid uid, NotificationType type, Long modelId, Long questionId,
                                                    Long answerId, String[] statuses) {
        return getSubscriptions(uid, type, modelId, questionId, answerId, null, statuses, null);
    }

    public List<EmailSubscription> getSubscriptions(Uid uid, NotificationType type, Long modelId, Long questionId,
                                                    Long answerId, Long streamId, String[] statuses,
                                                    ReturnMode returnMode) {
        MockHttpServletRequestBuilder builder = get("/subscription/")
                .contentType(MediaType.APPLICATION_JSON)
                .param(PersNotifyTag.UID, String.valueOf(uid.getValue()));
        if (type != null) {
            builder.param(PersNotifyTag.SUBSCRIPTION_TYPE, String.valueOf(type));
        }
        if (modelId != null) {
            builder.param(PersNotifyTag.MODEL_ID, String.valueOf(modelId));
        }
        if (questionId != null) {
            builder.param(PersNotifyTag.QUESTION_ID, String.valueOf(questionId));
        }
        if (answerId != null) {
            builder.param(PersNotifyTag.ANSWER_ID, String.valueOf(answerId));
        }

        if (streamId != null) {
            builder.param(PersNotifyTag.LIVE_STREAM_ID, String.valueOf(streamId));
        }

        if (statuses.length > 0){
            builder.param(PersNotifyTag.SUBSCRIPTION_STATUS, statuses);
        }
        if (returnMode != null) {
            builder.param(PersNotifyTag.RETURN_MODE, String.valueOf(returnMode));
        }

        MvcResult result;
        try {
            result = mockMvc.perform(builder)
                    .andDo(print())
                    .andReturn();
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
        return getSubscriptions(result);
    }

    public List<EmailSubscription> getSubscriptions(MvcResult result) {
        try {
            String content = result.getResponse().getContentAsString();
            EmailSubscription[] resultSubscriptions = objectMapper.readValue(content, EmailSubscription[].class);
            return asList(resultSubscriptions);
        } catch (IOException e) {
            throw new RuntimeException("", e);
        }
    }

    public List<EmailSubscription> createSubscriptions(Identity<?> identity, String email,
                                                       List<EmailSubscription> subscriptions) throws Exception {
        return createSubscriptions(identity, email, subscriptions, false);
    }

    public List<EmailSubscription> createSubscriptions(Identity<?> identity, String email,
                                                       List<EmailSubscription> subscriptions,
                                                       boolean forceConfirm) throws Exception {
        return createSubscriptions(identity, email, subscriptions, forceConfirm, HttpStatus.CREATED.value());
    }

    public List<EmailSubscription> createSubscriptions(Identity<?> identity, String email,
                                                       List<EmailSubscription> subscriptions, boolean forceConfirm,
                                                       int expectedStatus) throws Exception {
        String tag = getIdentityTag(identity);

        MvcResult mvcResult = mockMvc.perform(post("/subscription/")
            .contentType(MediaType.APPLICATION_JSON)
            .param(tag, String.valueOf(identity.getValue()))
            .param(PersNotifyTag.EMAIL, email)
            .param(PersNotifyTag.USER_AGENT, "someAgent")
            .param(PersNotifyTag.USER_IP, "someIp")
            .param(PersNotifyTag.FORCE_CONFIRM_SUBSCRIPTION, String.valueOf(forceConfirm))
            .content(toJson(subscriptions)))
            .andDo(print())
            .andExpect(status().is(expectedStatus)).andReturn();
        subscriptionAndIdentityService.reloadNotificationPool();
        String contentAsString = mvcResult.getResponse().getContentAsString();
        if (expectedStatus == HttpStatus.CREATED.value()) {
            return objectMapper.readValue(contentAsString, new TypeReference<List<EmailSubscription>>() { });
        } else {
            return null;
        }
    }

    private String getIdentityTag(Identity<?> identity) {
        if (identity instanceof Uid) {
            return PersNotifyTag.UID;
        } else if (identity instanceof Uuid) {
            return PersNotifyTag.UUID;
        } else {
            return PersNotifyTag.YANDEX_UID;
        }
    }

    public void unsubscribe(String email, NotificationType notificationType, ResultMatcher matcher,
                            Identity<?> identity) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = delete("/subscription/type")
            .contentType(MediaType.APPLICATION_JSON)
            .param(PersNotifyTag.EMAIL, email)
            .param(PersNotifyTag.SUBSCRIPTION_TYPE, notificationType.name());
        if (identity != null) {
            requestBuilder = requestBuilder.param(getIdentityTag(identity), String.valueOf(identity.getValue()));
        }
        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(matcher);

        subscriptionAndIdentityService.reloadNotificationPool();
    }

    public void unsubscribe(String email, NotificationType notificationType) throws Exception {
        unsubscribe(email, notificationType, status().isOk(), null);
    }


    public void unsubscribe(String email, NotificationType notificationType, Identity<?> identity) throws Exception {
        unsubscribe(email, notificationType, status().isOk(), identity);
    }

    public void unsubscribeForbidden(String email, NotificationType notificationType,
                                     Identity<?> identity) throws Exception {
        unsubscribe(email, notificationType, status().isForbidden(), identity);
    }

    public List<EmailSubscriptionStatusDto> isSubscribed(String email, NotificationType notificationType) throws Exception {
        return isSubscribed(email, Collections.singletonList(notificationType));    }

    public List<EmailSubscriptionStatusDto> isSubscribed(String email, List<NotificationType> types) throws Exception {
        MvcResult result = mockMvc.perform(get("/subscription/type")
                .contentType(MediaType.APPLICATION_JSON)
                .param(PersNotifyTag.EMAIL, email)
                .param(PersNotifyTag.SUBSCRIPTION_TYPE, types.stream().map(Enum::name).toArray(String[]::new)))
                .andDo(print())
                .andExpect(status().is(200))
                .andReturn();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<EmailSubscriptionStatusDto>>() {});
    }

    public void updateSubscription(Identity identity, EmailSubscription subscription) throws Exception {
        String tag = getIdentityTag(identity);

        mockMvc.perform(put("/subscription/" + subscription.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .param(tag, String.valueOf(identity.getValue()))
                .param(PersNotifyTag.USER_AGENT, "someAgent")
                .param(PersNotifyTag.USER_IP, "someIp")
                .content(toJson(subscription)))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
    }

    public void deleteSubscription(Identity identity, long subscriptionId,
                                   ResultMatcher resultMatcher) throws Exception {
        String tag = getIdentityTag(identity);

        mockMvc.perform(delete("/subscription/" + subscriptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .param(tag, String.valueOf(identity.getValue()))
                .param(PersNotifyTag.USER_AGENT, "someAgent")
                .param(PersNotifyTag.USER_IP, "someIp"))
                .andDo(print())
                .andExpect(resultMatcher);
    }

    public void deleteSubscription(Identity identity, long subscriptionId) throws Exception {
        deleteSubscription(identity, subscriptionId, status().is2xxSuccessful());
    }

    public void deleteSubscriptionForbidden(Identity identity, long subscriptionId) throws Exception {
        deleteSubscription(identity, subscriptionId, status().isForbidden());
    }

    long createProductSubscription(NotificationType type, Identity identity, String email, String modelId,
                                   String regionId, String price) throws Exception {
        return createSubscriptions(identity, email,
                    Collections.singletonList(EmailSubscription.builder()
                            .setSubscriptionType(type)
                            .addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, modelId)
                            .addParameter(EmailSubscriptionParam.PARAM_REGION_ID, regionId)
                            .addParameter(EmailSubscriptionParam.PARAM_PRICE, price).build())).get(0).getId();
    }
}
