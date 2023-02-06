package ru.yandex.market.pers.notify.service;

import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.CouponDto;
import ru.yandex.market.loyalty.api.model.CouponParamName;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.events.SubscriptionEventDto;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.test.MockedDbTest;


import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 06.10.17
 */
public class MarketLoyaltyServiceTest extends MockedDbTest {
    private static final Long GRADE_PROMO_ID = 0L;
    private static final long THANKS_FOR_SUBSCRIPTION_PROMO_ID = 4L;

    @Autowired
    private MarketLoyaltyService marketLoyaltyService;
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;

    @Test
    public void generateAndActivateCouponThanksForOrderNotNull() throws Exception {
        when(marketLoyaltyClient.generateAndActivateCoupon(anyString(), anyObject(), anyLong()))
            .thenReturn(new CouponDto(UUID.randomUUID().toString(), CouponStatus.ACTIVE));
        CouponDto couponDto = marketLoyaltyService.generateAndActivateCouponThanksForOrder(1L);
        assertNotNull(couponDto);
        assertNotNull(couponDto.getCode());
    }

    @Test
    public void generateCouponForGradeCouponNotNull() throws Exception {
        when(marketLoyaltyClient.generateCoupon(any()))
            .thenReturn(new CouponDto(UUID.randomUUID().toString(), CouponStatus.INACTIVE));
        CouponDto couponDto = marketLoyaltyService.generateCouponForGrade("my-email@test.ru", 1L, null);
        assertNotNull(couponDto);
        assertNotNull(couponDto.getCode());
    }

    @Test
    public void generateCouponForGradeCouponVerifyHttpParams() throws Exception {
        String email = "my-email@test.ru";
        long uid = 2L;
        marketLoyaltyService.generateCouponForGrade(email, 1L, uid);
        verify(marketLoyaltyClient).generateCoupon(argThat(new ArgumentMatcher<CouponCreationRequest>() {
            @Override
            public boolean matches(Object argument) {
                CouponCreationRequest request = (CouponCreationRequest) argument;
                return request.getPromoId() == GRADE_PROMO_ID
                    && !request.isForceActivation()
                    && new Uid(uid).equals(request.getIdentity())
                    && email.equals(request.getParams().get(CouponParamName.USER_EMAIL));
            }
        }));
    }

    @Test
    public void generateAndActivateCouponThanksForSubscriptionNotNull() throws Exception {
        when(marketLoyaltyClient.generateCoupon(any(CouponCreationRequest.class)))
            .thenReturn(new CouponDto(UUID.randomUUID().toString(), CouponStatus.INACTIVE));
        CouponDto couponDto = marketLoyaltyService.generateAndActivateCouponThanksForSubscription(
            "my-email@test.ru", "mall", "213", null);
        assertNotNull(couponDto);
        assertNotNull(couponDto.getCode());
    }

    @Test
    public void generateAndActivateCouponThanksForSubscriptionVerifyHttpParams() throws Exception {
        String email = "my-email@test.ru";
        String adsLocation = "mall";
        String regionId = "213";
        long uid = 2L;
        marketLoyaltyService.generateAndActivateCouponThanksForSubscription(
            email, adsLocation, regionId, new Uid(uid));
        verify(marketLoyaltyClient).generateCoupon(argThat(new ArgumentMatcher<CouponCreationRequest>() {
            @Override
            public boolean matches(Object argument) {
                CouponCreationRequest request = (CouponCreationRequest) argument;
                return request.getPromoId() == THANKS_FOR_SUBSCRIPTION_PROMO_ID
                    && request.isForceActivation()
                    && new Uid(uid).equals(request.getIdentity())
                    && email.equals(request.getParams().get(CouponParamName.USER_EMAIL))
                    && adsLocation.equals(request.getParams().get(CouponParamName.USER_ADS_LOCATION))
                    && regionId.equals(request.getParams().get(CouponParamName.USER_REGION_ID));
            }
        }));
    }

    @Test
    @Disabled
    public void processSubscribeEventForStoreAdvertising() {
        String email = "my-email@test.ru";
        long region = 214L;
        marketLoyaltyService.processSubscriptionEvent(NotificationType.STORE_ADVERTISING, email, region, null, null);
        verify(marketLoyaltyClient).processEvent(argThat(allOf(
            isA(SubscriptionEventDto.class),
            hasProperty("notificationType", equalTo(NotificationType.STORE_ADVERTISING)),
            hasProperty("region", equalTo(region)),
            hasProperty("email", equalTo(email)),
            hasProperty("platform", equalTo(MarketPlatform.BLUE))
        )));
    }

    @Test
    public void shouldNotCallLoyaltyForAdvertisingEvent() {
        String email = "my-email@test.ru";
        long region = 214L;
        marketLoyaltyService.processSubscriptionEvent(NotificationType.ADVERTISING, email, region, null, null);
        verifyZeroInteractions(marketLoyaltyClient);
    }
}
