package ru.yandex.market.pers.author.mock.mvc.socialecom;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;
import ru.yandex.market.pers.author.socialecom.dto.SubscriptionCountDto;
import ru.yandex.market.pers.author.socialecom.dto.SubscriptionDto;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.author.socialecom.controller.ApiConstants.SUBSCRIPTION_ENTITY_ID_KEY;
import static ru.yandex.market.pers.author.socialecom.controller.ApiConstants.SUBSCRIPTION_ENTITY_TYPE_KEY;

@Service
public class SubscriptionMvcMocks extends AbstractMvcMocks {

    public DtoPager<SubscriptionDto> getUserSubscriptions(String uid, String userType) {
        return parseValue(
            invokeAndRetrieveResponse(get("/socialecom/subscription/" + userType + "/" + uid)
                .accept(MediaType.APPLICATION_JSON), status().is2xxSuccessful()
            ), new TypeReference<>() {
            });
    }

    public DtoPager<SubscriptionDto> getSubscribers(String uid, String userType) {
        return parseValue(
            invokeAndRetrieveResponse(get("/socialecom/subscribers/" + userType + "/" + uid)
                .accept(MediaType.APPLICATION_JSON), status().is2xxSuccessful()
            ), new TypeReference<>() {
            });
    }

    public SubscriptionCountDto getSubscribersCountUID(String uid, String type) {
        return parseValue(
            invokeAndRetrieveResponse(get("/socialecom/subscribers/" + type + "/" + uid + "/count")
                .accept(MediaType.APPLICATION_JSON), status().is2xxSuccessful()
            ), new TypeReference<>() {
            });
    }

    public void getUserSubscriptionsAndExpect4xx(String uid) {
            invokeAndRetrieveResponse(get("/socialecom/subscription/VENDOR/" + uid)
                .accept(MediaType.APPLICATION_JSON), status().is4xxClientError()
            );
    }

    public SubscriptionCountDto getUserSubscriptionsCountUID(String uid) {
        return parseValue(
            invokeAndRetrieveResponse(get("/socialecom/subscription/UID/" + uid + "/count")
                .accept(MediaType.APPLICATION_JSON), status().is2xxSuccessful()
            ), new TypeReference<>() {
            });
    }

    public void subscribeUsingUID(String uid, String subId, String subType) {
        invokeAndRetrieveResponse(post("/socialecom/subscription/UID/" + uid)
            .param(SUBSCRIPTION_ENTITY_TYPE_KEY, subType)
            .param(SUBSCRIPTION_ENTITY_ID_KEY, subId), status().is2xxSuccessful()
        );
    }

    public void deleteSubscription(String uid, String subId, String subType) {
        invokeAndRetrieveResponse(delete("/socialecom/subscription/UID/" + uid)
            .param(SUBSCRIPTION_ENTITY_TYPE_KEY, subType)
            .param(SUBSCRIPTION_ENTITY_ID_KEY, subId), status().is2xxSuccessful()
        );
    }

    public SubscriptionDto hasSubscriptionUsingUID(String uid, String subId, String subType) {
        return parseValue(invokeAndRetrieveResponse(get("/socialecom/subscription/UID/" + uid + "/find")
            .param(SUBSCRIPTION_ENTITY_TYPE_KEY, subType)
            .param(SUBSCRIPTION_ENTITY_ID_KEY, subId)
            .accept(MediaType.APPLICATION_JSON), status().is2xxSuccessful()), new TypeReference<>() {});
    }

    public SubscriptionDto hasSubscriptionAndExpectNotFound(String uid, String subId, String subType) {
        invokeAndRetrieveResponse(get("/socialecom/subscription/UID/" + uid + "/find")
            .param(SUBSCRIPTION_ENTITY_TYPE_KEY, subType)
            .param(SUBSCRIPTION_ENTITY_ID_KEY, subId)
            .accept(MediaType.APPLICATION_JSON), status().is4xxClientError());

        return null;
    }
}
