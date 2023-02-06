package ru.yandex.market.loyalty.back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponDto;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.events.ForceCreateCouponEventDto;
import ru.yandex.market.loyalty.api.model.events.ForceEmmitCouponEventDto;
import ru.yandex.market.loyalty.api.model.events.LoginEventDto;
import ru.yandex.market.loyalty.api.model.events.LoyaltyEvent;
import ru.yandex.market.loyalty.api.model.events.SubscriptionEventDto;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.back.config.MarketLoyaltyBack;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.dao.coupon.CouponNotificationDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.coupon.CouponNotifyRecord;
import ru.yandex.market.loyalty.core.model.trigger.event.LoginEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.SubscriptionEvent;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.event.EventWithIdentity;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coupon.CouponNotificationService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerFastEvaluatorService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerManagementService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;
import ru.yandex.market.pers.notify.model.NotificationType;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.api.model.LoyaltyDateFormat.LOYALTY_DATE_FORMAT;
import static ru.yandex.market.loyalty.api.model.identity.Identity.Type.UID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * @author maratik
 */
@TestFor(EventController.class)
public class EventControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    private final SimpleDateFormat loyaltyDateFormat = new SimpleDateFormat(LOYALTY_DATE_FORMAT);

    private static final String EMAIL = "email@yandex-team.ru";
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private TriggerManagementService triggerManagementService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CouponNotificationDao notificationDao;
    @Autowired
    private CouponNotificationService notificationService;

    @Autowired
    @MarketLoyaltyBack
    private ObjectMapper objectMapper;

    @Test
    public void shouldProcessLoginEvent() {
        long uid = 512L;

        LoginEventDto request = new LoginEventDto();
        request.setPlatform(MarketPlatform.BLUE);
        request.setRegion(214L);
        request.setUid(uid);
        marketLoyaltyClient.processEvent(request);

        List<TriggerEvent> events = triggerEventDao.getNotProcessed(Duration.ZERO);
        assertThat(events, hasSize(1));
        TriggerEvent event = events.get(0);
        assertTrue(event instanceof LoginEvent);
        Identity<?> identity = ((EventWithIdentity) event).getIdentity();
        assertEquals(uid, identity.getValue());
        assertEquals(UID, identity.getType());
    }

    @Test
    public void shouldFailIfUidNotSpecifiedInLoginEvent() {
        LoginEventDto request = new LoginEventDto();
        request.setPlatform(MarketPlatform.BLUE);
        request.setRegion(214L);
        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.processEvent(request)
        );
        assertThat(exception.getMessage(), allOf(
                startsWith("Validation failed"),
                containsString("on field 'uid': rejected value [null]")
        ));
    }

    @Test
    public void shouldFailIfPlatformNotSpecifiedInLoginEvent() {
        LoginEventDto request = new LoginEventDto();
        request.setUid(123L);
        request.setRegion(214L);
        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.processEvent(request)
        );
        assertThat(exception.getMessage(), allOf(
                startsWith("Validation failed"),
                containsString("on field 'platform': rejected value [null]")
        ));
    }

    @Test
    public void shouldProcessSubscriptionEvent() {
        SubscriptionEventDto request = new SubscriptionEventDto();
        request.setEmail(EMAIL);
        request.setPlatform(MarketPlatform.BLUE);
        request.setNotificationType(NotificationType.ADVERTISING);
        marketLoyaltyClient.processEvent(request);

        List<TriggerEvent> events = triggerEventDao.getNotProcessed(Duration.ZERO);
        assertThat(events, hasSize(1));
        TriggerEvent event = events.get(0);
        assertTrue(event instanceof SubscriptionEvent);
        assertEquals(EMAIL, ((SubscriptionEvent) event).getEmail());
        assertNull(((SubscriptionEvent) event).getLastUnsubscribeDate());
    }

    @Test
    public void shouldSaveLastUnsubscribeDateInSubscriptionEvent() throws Exception {
        String lastUnsubscribeDate = "20-02-2017 15:40:00";

        SubscriptionEventDto request = new SubscriptionEventDto();
        request.setEmail(EMAIL);
        request.setPlatform(MarketPlatform.BLUE);
        request.setNotificationType(NotificationType.ADVERTISING);
        request.setLastUnsubscribeDate(loyaltyDateFormat.parse(lastUnsubscribeDate));
        assertThat(objectMapper.writeValueAsString(request), containsString(lastUnsubscribeDate));

        marketLoyaltyClient.processEvent(request);

        List<TriggerEvent> events = triggerEventDao.getNotProcessed(Duration.ZERO);
        assertThat(events, hasSize(1));
        TriggerEvent event = events.get(0);
        assertTrue(event instanceof SubscriptionEvent);
        assertEquals(EMAIL, ((SubscriptionEvent) event).getEmail());
        assertEquals(lastUnsubscribeDate,
                loyaltyDateFormat.format(((SubscriptionEvent) event).getLastUnsubscribeDate()));
    }

    @Test
    public void shouldSuccessEvaluateSubscription() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createSubscriptionTrigger(promo);

        SubscriptionEventDto request = generateSubscriptionEventDto(MarketPlatform.BLUE);

        assertTrue(evaluate(request));
    }

    @Test
    public void shouldNotSuccessEvaluateWhenRestrictionForbidIt() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        triggersFactory.createSubscriptionTrigger(promo);

        SubscriptionEventDto request = generateSubscriptionEventDto(MarketPlatform.RED);

        assertFalse(evaluate(request));
    }

    @Test
    public void shouldProcessForceCreateCoupon() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        ForceCreateCouponEventDto request = generateForceCreateCouponEventDto(promo.getId(), "someKey");

        CouponDto response = processWithForceCouponCreation(request);
        assertEquals(CouponStatus.ACTIVE, response.getStatus());
        assertThat(response.getCode(), is(not(emptyOrNullString())));
    }

    @Test
    public void shouldFailForceCreateCouponOnInactivePromo() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStatus(PromoStatus.INACTIVE)
        );

        triggersFactory.createForceCreateCouponTrigger(promo);

        ForceCreateCouponEventDto request = generateForceCreateCouponEventDto(promo.getId(), "someKey");

        mockMvc.perform(post("/event/" + LoyaltyEvent.FORCE_CREATE_COUPON + "/process")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andDo(log())
                .andReturn();
    }

    @Test
    public void shouldReturnSameCouponOnSecondForceCreateCouponWithSameUniqueKey() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        String uniqueKey = "someKey";

        CouponDto firstResponse = processWithForceCouponCreation(
                generateForceCreateCouponEventDto(promo.getId(), uniqueKey)
        );
        CouponDto secondResponse = processWithForceCouponCreation(
                generateForceCreateCouponEventDto(promo.getId(), uniqueKey)
        );
        assertEquals(firstResponse.getCode(), secondResponse.getCode());
    }

    @Test
    public void shouldReturnDifferentCouponsOnForceCreateCouponWithDifferentUniqueKeys() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        CouponDto firstResponse = processWithForceCouponCreation(
                generateForceCreateCouponEventDto(promo.getId(), "someKey")
        );
        CouponDto secondResponse = processWithForceCouponCreation(
                generateForceCreateCouponEventDto(promo.getId(), "anotherKey")
        );
        assertNotEquals(firstResponse.getCode(), secondResponse.getCode());
    }

    @Test
    public void shouldFailIfUniqueKeyIsEmptyOnForceCreateCoupon() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        ForceCreateCouponEventDto request = generateForceCreateCouponEventDto(promo.getId(), "");

        mockMvc.perform(post("/event/" + LoyaltyEvent.FORCE_CREATE_COUPON + "/process")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void shouldResendCouponOnForceCreateCoupon() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        triggersFactory.createForceCreateCouponTrigger(promo);
        ForceCreateCouponEventDto request = generateForceCreateCouponEventDto(promo.getId(), "someKey");
        processWithForceCouponCreation(request);

        List<CouponNotifyRecord> records = notificationDao.getCouponsWithoutNotifications(10);
        assertEquals(1, records.size());

        notificationService.notifyUsersAboutCouponActivation();
        records = notificationDao.getCouponsWithoutNotifications(10);
        assertEquals(0, records.size());

        request.setResending(true);
        processWithForceCouponCreation(request);
        records = notificationDao.getCouponsWithoutNotifications(10);
        assertEquals(1, records.size());
    }

    @Test
    public void shouldFailForceEmmitCouponOnInactivePromo() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStatus(PromoStatus.INACTIVE)
        );

        triggersFactory.createForceEmmitCouponTrigger(promo);

        ForceEmmitCouponEventDto request = generateForceEmmitCouponEventDto(promo.getId(), "someKey");

        mockMvc.perform(post("/event/" + LoyaltyEvent.FORCE_EMMIT_COUPON + "/process")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andDo(log())
                .andReturn();
    }

    @Test
    public void shouldReturnSameCouponOnSecondForceEmmitCouponWithSameUniqueKey() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        String uniqueKey = "someKey";

        CouponDto firstResponse = processWithForceEmmitCreation(
                generateForceEmmitCouponEventDto(promo.getId(), uniqueKey)
        );
        CouponDto secondResponse = processWithForceEmmitCreation(
                generateForceEmmitCouponEventDto(promo.getId(), uniqueKey)
        );
        assertEquals(firstResponse.getCode(), secondResponse.getCode());
    }

    @Test
    public void shouldReturnDifferentCouponsOnForceEmmitCouponWithDifferentUniqueKeys() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        CouponDto firstResponse = processWithForceEmmitCreation(
                generateForceEmmitCouponEventDto(promo.getId(), "someKey")
        );
        CouponDto secondResponse = processWithForceEmmitCreation(
                generateForceEmmitCouponEventDto(promo.getId(), "anotherKey")
        );
        assertNotEquals(firstResponse.getCode(), secondResponse.getCode());
    }

    @Test
    public void shouldFailIfUniqueKeyIsEmptyOnForceEmmitCoupon() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        ForceEmmitCouponEventDto request = generateForceEmmitCouponEventDto(promo.getId(), "");

        mockMvc.perform(post("/event/" + LoyaltyEvent.FORCE_EMMIT_COUPON + "/process")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void shouldProcessForceEmmitCoupon() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        ForceEmmitCouponEventDto request = generateForceEmmitCouponEventDto(promo.getId(), "someKey");

        CouponDto response = processWithForceEmmitCreation(request);
        assertEquals(CouponStatus.ACTIVE, response.getStatus());
        assertThat(response.getCode(), is(not(emptyOrNullString())));
    }

    @Test
    public void shouldCacheEvaluateForSomeTime() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createSubscriptionTrigger(promo);
        //warmUp cache
        assertTrue(evaluate(generateSubscriptionEventDto(MarketPlatform.BLUE)));

        triggerManagementService.getTriggers().forEach(
                trigger -> triggerManagementService.removeTrigger(trigger.getId())
        );
        //cached trigger
        assertTrue(evaluate(generateSubscriptionEventDto(MarketPlatform.BLUE)));

        Thread.sleep(1100 * TriggerFastEvaluatorService.EXPIRE_SECOND_COUNT);
        assertFalse(evaluate(generateSubscriptionEventDto(MarketPlatform.BLUE)));
    }

    @Test
    public void shouldProcessForceCreateCouponViaClient() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        triggersFactory.createForceCreateCouponTrigger(promo);

        CouponDto response = marketLoyaltyClient.forceCreateCouponProcess(
                generateForceCreateCouponEventDto(
                        promo.getId(),
                        "someKeyValue"
                )
        );

        assertEquals(CouponStatus.ACTIVE, response.getStatus());
        assertThat(response.getCode(), is(not(emptyOrNullString())));
    }

    private static ForceCreateCouponEventDto generateForceCreateCouponEventDto(long id, String uniqueKey) {
        ForceCreateCouponEventDto request = new ForceCreateCouponEventDto();
        request.setEmail("email");
        request.setPromoId(id);
        request.setUniqueKey(uniqueKey);
        return request;
    }

    private static ForceEmmitCouponEventDto generateForceEmmitCouponEventDto(long id, String uniqueKey) {
        ForceEmmitCouponEventDto request = new ForceEmmitCouponEventDto();
        request.setPromoId(id);
        request.setUniqueKey(uniqueKey);
        return request;
    }

    private static SubscriptionEventDto generateSubscriptionEventDto(MarketPlatform platform) {
        SubscriptionEventDto request = new SubscriptionEventDto();
        request.setEmail("email");
        request.setPlatform(platform);
        request.setNotificationType(NotificationType.ADVERTISING);
        return request;
    }

    private boolean evaluate(SubscriptionEventDto request) throws Exception {
        return Boolean.parseBoolean(mockMvc.perform(post("/event/" + LoyaltyEvent.SUBSCRIPTION + "/evaluate")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(log())
                .andReturn().getResponse().getContentAsString());
    }

    private CouponDto processWithForceCouponCreation(ForceCreateCouponEventDto request) throws Exception {
        String response = mockMvc.perform(post("/event/" + LoyaltyEvent.FORCE_CREATE_COUPON + "/process")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(log())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, CouponDto.class);
    }

    private CouponDto processWithForceEmmitCreation(ForceEmmitCouponEventDto request) throws Exception {
        String response = mockMvc.perform(post("/event/" + LoyaltyEvent.FORCE_EMMIT_COUPON + "/process")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(log())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, CouponDto.class);
    }
}
