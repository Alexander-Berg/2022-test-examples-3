package ru.yandex.market.pers.notify.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import ru.yandex.market.pers.notify.api.controller.dto.subscription.Entity;
import ru.yandex.market.pers.notify.api.controller.dto.subscription.GetSubscriptionsResponse;
import ru.yandex.market.pers.notify.api.controller.dto.subscription.SettingType;
import ru.yandex.market.pers.notify.api.controller.dto.subscription.Status;
import ru.yandex.market.pers.notify.api.controller.dto.subscription.TrailType;
import ru.yandex.market.pers.notify.api.controller.dto.subscription.UpdateSubscriptionsRequest;
import ru.yandex.market.pers.notify.assertions.SubscriptionAssertions;
import ru.yandex.market.pers.notify.json.JsonSerializer;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.subscription.Subscription;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionHistoryItem;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus;
import ru.yandex.market.pers.notify.service.time.ClockProvider;
import ru.yandex.market.pers.notify.subscription.SubscriptionService;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author vtarasoff
 * @since 04.10.2021
 */
public class SubscriptionSettingsControllerTest extends MarketUtilsMockedDbTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private ClockProvider clockProvider;

    @Autowired
    private SubscriptionAssertions subscriptionAssertions;

    private final ObjectMapper objectMapper = JsonSerializer.mapper();

    @Test
    void shouldReturnAllUnsubscribedIfNotExists() throws Exception {
        mockMvc.perform(
                        get("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(
                        expectedSubscriptions(Status.DISABLED, Status.DISABLED, Status.DISABLED, Status.DISABLED)
                )));
    }

    @Test
    void shouldReturnOnlyForRequestedUuid() throws Exception {
        subscriptionService.saveOrUpdate(List.of(
                subscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_GENERAL_ADVERTISING, SubscriptionStatus.SUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_PERSONAL_ADVERTISING, SubscriptionStatus.SUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_UGC, SubscriptionStatus.SUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_LIVE_STREAM, SubscriptionStatus.SUBSCRIBED),
                subscription("otherUuid", NotificationType.STORE_PUSH_GENERAL_ADVERTISING, SubscriptionStatus.UNSUBSCRIBED)
        ));

        mockMvc.perform(
                        get("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(
                        expectedSubscriptions(Status.ENABLED, Status.ENABLED, Status.ENABLED, Status.ENABLED)
                )));
    }

    @Test
    void shouldReturnAllCorrect() throws Exception {
        subscriptionService.saveOrUpdate(List.of(
                subscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_GENERAL_ADVERTISING, SubscriptionStatus.UNSUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_PERSONAL_ADVERTISING, SubscriptionStatus.SUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_UGC, SubscriptionStatus.UNSUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_LIVE_STREAM, SubscriptionStatus.SUBSCRIBED)
        ));

        mockMvc.perform(
                        get("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(
                        expectedSubscriptions(Status.DISABLED, Status.ENABLED, Status.DISABLED, Status.ENABLED)
                )));
    }

    @Test
    void shouldReturnExistsAndUnsubscribedIfNotExists() throws Exception {
        subscriptionService.saveOrUpdate(List.of(
                subscription("someUuid", NotificationType.STORE_PUSH_GENERAL_ADVERTISING, SubscriptionStatus.SUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_PERSONAL_ADVERTISING, SubscriptionStatus.SUBSCRIBED)
        ));

        mockMvc.perform(
                        get("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(
                        expectedSubscriptions(Status.ENABLED, Status.ENABLED, Status.DISABLED, Status.DISABLED)
                )));
    }

    @Test
    void shouldUpdateNothingIfEmptyRequest() throws Exception {
        mockMvc.perform(
                        put("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(new UpdateSubscriptionsRequest()))
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(toJson(new Error(
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "subscriptions is empty",
                        HttpStatus.BAD_REQUEST.value()
                ))));

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @ParameterizedTest
    @MethodSource("shouldFailUpdateIfEmptyRequiredFieldsArgumentsProvider")
    void shouldFailUpdateIfEmptyRequiredFields(Integer id,
                                               Status status,
                                               SettingType type,
                                               Long updated) throws Exception {
        mockMvc.perform(
                        put("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(new UpdateSubscriptionsRequest()
                                        .setSubscriptions(List.of(
                                                updatedSubscription(Entity.SUBSCRIPTION, id, status, type, updated)
                                        ))
                                ))
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(toJson(new Error(
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        (id == null ? "id" : status == null ? "status" : type == null ? "type" : "updated") + " is null",
                        HttpStatus.BAD_REQUEST.value()
                ))));

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    private static Stream<Arguments> shouldFailUpdateIfEmptyRequiredFieldsArgumentsProvider() {
        return Stream.of(
                arguments(null, Status.ENABLED, SettingType.PUSH, 0L),
                arguments(NotificationType.STORE_PUSH_GENERAL_ADVERTISING.getId(), null, SettingType.PUSH, 0L),
                arguments(NotificationType.STORE_PUSH_GENERAL_ADVERTISING.getId(), Status.ENABLED, null, 0L),
                arguments(NotificationType.STORE_PUSH_GENERAL_ADVERTISING.getId(), Status.ENABLED, SettingType.PUSH, null)
        );
    }

    @Test
    void shouldFailUpdateIfEntityNotValid() throws Exception {
        mockMvc.perform(
                        put("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(new UpdateSubscriptionsRequest()
                                        .setSubscriptions(List.of(updatedSubscription(
                                                null,
                                                NotificationType.STORE_PUSH_GENERAL_ADVERTISING.getId(),
                                                Status.ENABLED,
                                                SettingType.PUSH,
                                                0L
                                        )))
                                ))
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(toJson(new Error(
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "entity not supported. " + Entity.SUBSCRIPTION + " expected",
                        HttpStatus.BAD_REQUEST.value()
                ))));

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @Test
    void shouldFailUpdateIfIdNotValid() throws Exception {
        mockMvc.perform(
                        put("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(new UpdateSubscriptionsRequest()
                                        .setSubscriptions(List.of(updatedSubscription(
                                                NotificationType.STORE_PUSH_ORDER_STATUS.getId(), Status.ENABLED
                                        )))
                                ))
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(toJson(new Error(
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "id not supported",
                        HttpStatus.BAD_REQUEST.value()
                ))));

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @Test
    void shouldCreateNewIfNotExists() throws Exception {
        mockMvc.perform(
                        put("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(
                                        new UpdateSubscriptionsRequest().setSubscriptions(List.of(
                                                updatedSubscription(
                                                        NotificationType.STORE_PUSH_GENERAL_ADVERTISING.getId(),
                                                        Status.ENABLED
                                                ),
                                                updatedSubscription(
                                                        NotificationType.STORE_PUSH_PERSONAL_ADVERTISING.getId(),
                                                        Status.ENABLED
                                                ),
                                                updatedSubscription(
                                                        NotificationType.STORE_PUSH_UGC.getId(),
                                                        Status.DISABLED
                                                ),
                                                updatedSubscription(
                                                        NotificationType.STORE_PUSH_LIVE_STREAM.getId(),
                                                        Status.DISABLED
                                                )
                                        ))
                                ))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(
                        expectedSubscriptions(Status.ENABLED, Status.ENABLED, Status.DISABLED, Status.DISABLED)
                )));

        subscriptionAssertions.assertSubscriptions(
                Set.of(
                        NotificationType.STORE_PUSH_GENERAL_ADVERTISING,
                        NotificationType.STORE_PUSH_PERSONAL_ADVERTISING
                ),
                "someUuid",
                SubscriptionStatus.SUBSCRIBED,
                List.of(SubscriptionHistoryItem.created(
                        1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId())
                ))
        );

        subscriptionAssertions.assertSubscriptions(
                Set.of(
                        NotificationType.STORE_PUSH_UGC,
                        NotificationType.STORE_PUSH_LIVE_STREAM
                ),
                "someUuid",
                SubscriptionStatus.UNSUBSCRIBED,
                List.of(SubscriptionHistoryItem.created(
                        1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId())
                ))
        );
    }

    @Test
    void shouldCreateOnlyRequested() throws Exception {
        mockMvc.perform(
                        put("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(
                                        new UpdateSubscriptionsRequest().setSubscriptions(List.of(updatedSubscription(
                                                NotificationType.STORE_PUSH_GENERAL_ADVERTISING.getId(), Status.ENABLED
                                        )))
                                ))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(
                        expectedSubscriptions(Status.ENABLED, Status.DISABLED, Status.DISABLED, Status.DISABLED)
                )));

        subscriptionAssertions.assertSubscription(
                "someUuid",
                NotificationType.STORE_PUSH_GENERAL_ADVERTISING,
                SubscriptionStatus.SUBSCRIBED,
                List.of(SubscriptionHistoryItem.created(
                        1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId())
                )));

        assertTrue(
                subscriptionService.get(new Uuid("someUuid"), Set.of(
                        NotificationType.STORE_PUSH_PERSONAL_ADVERTISING,
                        NotificationType.STORE_PUSH_UGC,
                        NotificationType.STORE_PUSH_LIVE_STREAM
                )).isEmpty()
        );
    }

    @Test
    void shouldUpdateIfExists() throws Exception {
        subscriptionService.saveOrUpdate(List.of(
                subscription("someUuid", NotificationType.STORE_PUSH_GENERAL_ADVERTISING, SubscriptionStatus.UNSUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_PERSONAL_ADVERTISING, SubscriptionStatus.SUBSCRIBED),
                subscription("someUuid", NotificationType.STORE_PUSH_UGC, SubscriptionStatus.SUBSCRIBED)
        ));

        mockMvc.perform(
                        put("/subscription-settings")
                                .queryParam("uuid", "someUuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(
                                        new UpdateSubscriptionsRequest().setSubscriptions(List.of(
                                                updatedSubscription(
                                                        NotificationType.STORE_PUSH_GENERAL_ADVERTISING.getId(),
                                                        Status.ENABLED
                                                ),
                                                updatedSubscription(
                                                        NotificationType.STORE_PUSH_PERSONAL_ADVERTISING.getId(),
                                                        Status.DISABLED
                                                )
                                        ))
                                ))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(
                        expectedSubscriptions(Status.ENABLED, Status.DISABLED, Status.ENABLED, Status.DISABLED)
                )));

        subscriptionAssertions.assertSubscription(
                "someUuid",
                NotificationType.STORE_PUSH_GENERAL_ADVERTISING,
                SubscriptionStatus.SUBSCRIBED,
                List.of(
                        SubscriptionHistoryItem.created(
                                1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId())
                        ),
                        SubscriptionHistoryItem.updated(
                                1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId())
                        )
                ));

        subscriptionAssertions.assertSubscription(
                "someUuid",
                NotificationType.STORE_PUSH_PERSONAL_ADVERTISING,
                SubscriptionStatus.UNSUBSCRIBED,
                List.of(
                        SubscriptionHistoryItem.created(
                                1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId())
                        ),
                        SubscriptionHistoryItem.updated(
                                1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId())
                        )
                ));

        subscriptionAssertions.assertSubscription(
                "someUuid",
                NotificationType.STORE_PUSH_UGC,
                SubscriptionStatus.SUBSCRIBED,
                List.of(SubscriptionHistoryItem.created(
                        1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId())
                )));

        assertTrue(
                subscriptionService
                        .get(new Uuid("someUuid"), Set.of(NotificationType.STORE_PUSH_LIVE_STREAM))
                        .isEmpty()
        );
    }

    private GetSubscriptionsResponse expectedSubscriptions(Status general, Status personal, Status ugc, Status live) {
        return new GetSubscriptionsResponse()
                .setSections(List.of(new GetSubscriptionsResponse.Section()
                        .setLead(
                                new GetSubscriptionsResponse.Lead()
                                        .setTitle("Какие уведомления присылать?")
                                        .setStyle(GetSubscriptionsResponse.Style.LARGE)
                        )
                        .setItems(List.of(
                                expectedSubscriptionItem(
                                        "Промокоды и акции",
                                        NotificationType.STORE_PUSH_GENERAL_ADVERTISING,
                                        general
                                ),
                                expectedSubscriptionItem(
                                        "Персональные предложения",
                                        NotificationType.STORE_PUSH_PERSONAL_ADVERTISING,
                                        personal
                                ),
                                expectedSubscriptionItem(
                                        "Вопросы и ответы",
                                        NotificationType.STORE_PUSH_UGC,
                                        ugc
                                ),
                                expectedSubscriptionItem(
                                        "Прямые трансляции",
                                        NotificationType.STORE_PUSH_LIVE_STREAM,
                                        live
                                )
                        ))));
    }

    private GetSubscriptionsResponse.SubscriptionItem expectedSubscriptionItem(String title,
                                                                               NotificationType type,
                                                                               Status status) {
        return new GetSubscriptionsResponse.SubscriptionItem()
                .setEntity(Entity.SUBSCRIPTION)
                .setLead(new GetSubscriptionsResponse.Lead().setTitle(title))
                .setId(type.getId())
                .setStatus(status)
                .setType(SettingType.PUSH)
                .setUpdated(clockProvider.clock().instant().toEpochMilli())
                .setTrail(new GetSubscriptionsResponse.Trail().setType(TrailType.SWITCH))
                .setStyle(GetSubscriptionsResponse.Style.MEDIUM);
    }

    private Subscription subscription(String uuid, NotificationType type, SubscriptionStatus status) {
        return new Subscription(new Uuid(uuid), NotificationTransportType.PUSH, type, status);
    }

    private UpdateSubscriptionsRequest.Subscription updatedSubscription(Entity entity,
                                                                        Integer id,
                                                                        Status status,
                                                                        SettingType type,
                                                                        Long updated) {
        return new UpdateSubscriptionsRequest.Subscription()
                .setEntity(entity)
                .setId(id)
                .setStatus(status)
                .setType(type)
                .setUpdated(updated);
    }

    private UpdateSubscriptionsRequest.Subscription updatedSubscription(Integer id, Status status) {
        return updatedSubscription(Entity.SUBSCRIPTION, id, status, SettingType.PUSH, 0L);
    }
}
