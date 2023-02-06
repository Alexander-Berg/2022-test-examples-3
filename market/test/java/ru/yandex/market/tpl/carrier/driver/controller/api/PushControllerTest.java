package ru.yandex.market.tpl.carrier.driver.controller.api;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.tpl.carrier.core.domain.push.subscription.PushSubscription;
import ru.yandex.market.tpl.carrier.core.domain.push.subscription.PushSubscriptionRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.common.xiva.subscribe.XivaSubscriptionTvmClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
class PushControllerTest extends BaseDriverApiIntTest {

    private final XivaSubscriptionTvmClient xivaSubscriptionTvmClient;
    private final TestUserHelper testUserHelper;

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @BeforeEach
    void setUp() {
        Mockito.when(xivaSubscriptionTvmClient.subscribe(Mockito.any()))
                .thenReturn(true);
        Mockito.when(xivaSubscriptionTvmClient.unsubscribe(Mockito.any()))
                .thenReturn(true);
    }

    @SneakyThrows
    @Test
    void shouldSubscribeCorrectly() {
        testUserHelper.findOrCreateUser(UID);

        mockMvc.perform(post("/api/push/subscription")
                .header("Tpl-Uuid", "uuid")
                .header("Tpl-PushToken", "token")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .param("appName", "ru.yandex.market.carrier.app")
        ).andExpect(status().isNoContent());

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();
        Assertions.assertThat(subscriptions).hasSize(1);
        Mockito.verify(xivaSubscriptionTvmClient, Mockito.atLeastOnce()).subscribe(Mockito.any());
    }

    @SneakyThrows
    @Test
    void shouldSubscribeAndUnsubscribeCorrectly() {
        testUserHelper.findOrCreateUser(UID);

        mockMvc.perform(post("/api/push/subscription")
                .header("Tpl-Uuid", "uuid")
                .header("Tpl-PushToken", "token")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .param("appName", "ru.yandex.market.carrier.app")
        ).andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/push/subscription")
                .header("Tpl-Uuid", "uuid")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .param("appName", "ru.yandex.market.carrier.app")
        ).andExpect(status().isNoContent());


        Mockito.verify(xivaSubscriptionTvmClient, Mockito.atLeastOnce()).subscribe(Mockito.any());
        Mockito.verify(xivaSubscriptionTvmClient, Mockito.atLeastOnce()).unsubscribe(Mockito.any());

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();
        Assertions.assertThat(subscriptions).hasSize(1);
        Assertions.assertThat(subscriptions.get(0).isDeleted()).isTrue();

    }

    @SneakyThrows
    @Test
    void shouldUnsubscribeCorrectlyIfSubscriptionWasNotCreated() {
        testUserHelper.findOrCreateUser(UID);

        mockMvc.perform(delete("/api/push/subscription")
                .header("Tpl-Uuid", "uuid")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .param("appName", "ru.yandex.market.carrier.app")
        ).andExpect(status().isNoContent());


        Mockito.verify(xivaSubscriptionTvmClient, Mockito.atLeastOnce()).unsubscribe(Mockito.any());

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();
        Assertions.assertThat(subscriptions).isEmpty();

    }

}
