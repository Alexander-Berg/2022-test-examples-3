package ru.yandex.market.tpl.core.domain.push;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.push.subscription.PushSubscriptionRepository;
import ru.yandex.market.tpl.core.domain.push.subscription.PushSubscriptionServiceImpl;
import ru.yandex.market.tpl.core.domain.push.subscription.model.PushSubscribeRequest;
import ru.yandex.market.tpl.core.domain.push.subscription.model.PushSubscription;
import ru.yandex.market.tpl.core.domain.push.subscription.model.PushSubscriptionPlatform;
import ru.yandex.market.tpl.core.domain.push.subscription.model.PushUnsubscribeRequest;
import ru.yandex.market.tpl.core.external.xiva.XivaClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class PushSubscriptionServiceImplTest {

    private final PushSubscriptionServiceImpl pushSubscriptionServiceImpl;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final XivaClient xivaClient;

    @Test
    void subscribe() {
        assertThat(pushSubscriptionServiceImpl.subscribe(new PushSubscribeRequest(
                "12345", "my_uuid", 12345L, "my_token", PushSubscriptionPlatform.APNS,
                "my_app"
        ))).isTrue();
        assertThat(pushSubscriptionRepository.findAll().get(0).isDeleted()).isFalse();
    }

    @Test
    void subscribeWithSamePushTokenUnsubscribesOldSubscription() {
        String pushToken = "my_token";
        String firstXivaUserId = "12345";
        String secondXivaUserId = "12346";
        pushSubscriptionServiceImpl.subscribe(new PushSubscribeRequest(
                firstXivaUserId, "my_uuid", 12345L, pushToken, PushSubscriptionPlatform.APNS,
                "my_app"
        ));
        pushSubscriptionServiceImpl.subscribe(new PushSubscribeRequest(
                secondXivaUserId, "my_uuid", 12345L, pushToken, PushSubscriptionPlatform.APNS,
                "my_app"
        ));

        verify(xivaClient).unsubscribe(argThat((pur) -> Objects.equals(firstXivaUserId, pur.getXivaUserId())));
        assertThat(
                pushSubscriptionRepository.findAllByPushTokenAndDeleted(pushToken, true).get(0)
        ).extracting(PushSubscription::getXivaUserId).isEqualTo(firstXivaUserId);
        assertThat(
                pushSubscriptionRepository.findAllByPushTokenAndDeleted(pushToken, false).get(0)
        ).extracting(PushSubscription::getXivaUserId).isEqualTo(secondXivaUserId);
    }

    @Test
    void unsubscribe() {
        String xivaUserId = "12345";
        String uuid = "my_uuid";
        pushSubscriptionServiceImpl.subscribe(new PushSubscribeRequest(
                xivaUserId, uuid, 12345L, "my_token", PushSubscriptionPlatform.APNS,
                "my_app"
        ));
        assertThat(pushSubscriptionServiceImpl.unsubscribe(new PushUnsubscribeRequest(xivaUserId, uuid))).isTrue();
        assertThat(pushSubscriptionRepository.findAll().get(0).isDeleted()).isTrue();
    }

}
