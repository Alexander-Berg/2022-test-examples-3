package ru.yandex.market.vendors.analytics.platform.controller.partner.subscription;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.utils.DateUtils;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "PartnerSubscriptionControllerTest.before.csv")
class PartnerSubscriptionControllerTest extends FunctionalTest {

    @MockBean(name = "clock")
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(DateUtils.toInstant(LocalDate.of(2020, 7, 3)));
    }

    @Test
    @DisplayName("Не нужно предлагать подписаться: партнёр уже подписан")
    void vendorSubscribed() {
        var response = needShowSubscriptionRequest(1, 1);
        assertEquals("false", response);
    }

    @Test
    @DisplayName("Вендор не подписан")
    void vendorNotSubscribed() {
        var response = needShowSubscriptionRequest(2, 1);
        assertEquals("true", response);
    }

    @Test
    @DisplayName("Пользователь отказался давно")
    void rejectLongTimeAgo() {
        var response = needShowSubscriptionRequest(2, 2);
        assertEquals("true", response);
    }

    @Test
    @DisplayName("Пользователь отказался недавно")
    void rejectRecently() {
        var response = needShowSubscriptionRequest(2, 3);
        assertEquals("false", response);
    }

    @Test
    @DisplayName("Подписать партнёра")
    @DbUnitDataSet(after = "PartnerSubscriptionControllerTest.subscribePartner.after.csv")
    void subscribePartner() {
        subscribePartner(4, 10002, true);
    }

    @Test
    @DisplayName("Попытка повторно подписать партнёра на рассылки")
    @DbUnitDataSet(after = "PartnerSubscriptionControllerTest.before.csv")
    void subscribePartnerAgain() {
        subscribePartner(1, 10002, true);
    }

    @Test
    @DisplayName("Не подписать партнёра")
    @DbUnitDataSet(after = "PartnerSubscriptionControllerTest.doNotSubscribePartner.after.csv")
    void doNotSubscribePartner() {
        subscribePartner(4, 10003, false);
    }

    private String needShowSubscriptionRequest(long partnerId, long userId) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("partners", "{partnerId}", "subscription", "show")
                .queryParam("uid", userId)
                .buildAndExpand(partnerId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private void subscribePartner(long partnerId, long userId, Boolean accepted) {
        var body = "{\"advertisingAgreement\": " + accepted + "}";
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("partners", "{partnerId}", "subscription")
                .queryParam("uid", userId)
                .buildAndExpand(partnerId)
                .toUriString();
        FunctionalTestHelper.postForJson(url, body);
    }
}