package ru.yandex.market.pers.author.socialecom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;
import ru.yandex.market.pers.author.mock.mvc.socialecom.SubscriptionMvcMocks;
import ru.yandex.market.pers.author.socialecom.dto.SubscriptionDto;
import ru.yandex.market.pers.author.socialecom.model.Subscription;
import ru.yandex.market.pers.author.socialecom.model.UserType;
import ru.yandex.market.pers.author.socialecom.service.SubscriptionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SubscriptionControllerTest extends PersAuthorTest {

    private static final String UID = "123";
    private static final String SUB_UID = "123";

    @Autowired
    private SubscriptionService service;

    @Autowired
    private SubscriptionMvcMocks mvcMocks;

    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testGetUserSubscriptions() {
        Subscription sub1 = createSubscription(UID, "321", UserType.BUSINESS);
        Subscription sub2 = createSubscription(UID, "344", UserType.BUSINESS);
        Subscription sub3 = createSubscription(UID, "399", UserType.BRAND);

        service.subscribe(sub1);
        service.subscribe(sub2);
        service.subscribe(sub3);

        assertEquals(3, mvcMocks.getUserSubscriptionsCountUID("123").getCount());
        DtoPager<SubscriptionDto> result = mvcMocks.getUserSubscriptions(String.valueOf(123L), UserType.UID.getName());
        assertEquals(3, result.getData().size());
    }

    @Test
    public void testDeleteAndGetUserSubscriptions() {

        mvcMocks.subscribeUsingUID(UID, "321", UserType.BUSINESS.getName());
        mvcMocks.subscribeUsingUID(UID, "344", UserType.BUSINESS.getName());
        mvcMocks.subscribeUsingUID(UID, "399", UserType.BRAND.getName());
        mvcMocks.subscribeUsingUID(UID, "900", UserType.BRAND.getName());
        mvcMocks.subscribeUsingUID(UID, "1000", UserType.BUSINESS.getName());

        assertEquals(5, mvcMocks.getUserSubscriptionsCountUID("123").getCount());
        DtoPager<SubscriptionDto> result = mvcMocks.getUserSubscriptions(String.valueOf(123L), UserType.UID.getName());
        assertEquals(5, result.getData().size());

        mvcMocks.deleteSubscription(UID, "321", UserType.BUSINESS.getName());

        assertEquals(4, mvcMocks.getUserSubscriptionsCountUID("123").getCount());
        result = mvcMocks.getUserSubscriptions(String.valueOf(123L), UserType.UID.getName());
        assertEquals(4, result.getData().size());
    }

    @Test
    public void testGetSubscribers() {
        Subscription sub1 = createSubscription("321", SUB_UID, UserType.BUSINESS);
        Subscription sub2 = createSubscription("344", SUB_UID, UserType.BUSINESS);
        Subscription sub3 = createSubscription("399", SUB_UID, UserType.BRAND);

        service.subscribe(sub1);
        service.subscribe(sub2);
        service.subscribe(sub3);
        assertEquals(2, mvcMocks.getSubscribersCountUID(SUB_UID, UserType.BUSINESS.getName()).getCount());
        DtoPager<SubscriptionDto> result = mvcMocks.getSubscribers(SUB_UID, UserType.BUSINESS.getName());
        assertEquals(2, result.getData().size());
    }

    @Test
    public void testDeleteAndGetUserSubscribers() {

        mvcMocks.subscribeUsingUID("321", SUB_UID, UserType.BUSINESS.getName());
        mvcMocks.subscribeUsingUID("344", SUB_UID, UserType.BUSINESS.getName());
        mvcMocks.subscribeUsingUID("399", SUB_UID, UserType.BUSINESS.getName());
        mvcMocks.subscribeUsingUID("901", SUB_UID, UserType.BUSINESS.getName());
        mvcMocks.subscribeUsingUID("213", SUB_UID, UserType.BUSINESS.getName());

        assertEquals(5, mvcMocks.getSubscribersCountUID(SUB_UID, UserType.BUSINESS.getName()).getCount());
        DtoPager<SubscriptionDto> result = mvcMocks.getSubscribers(SUB_UID, UserType.BUSINESS.getName());
        assertEquals(5, result.getData().size());

        mvcMocks.deleteSubscription("321", SUB_UID, UserType.BUSINESS.getName());

        assertEquals(4, mvcMocks.getSubscribersCountUID(SUB_UID, UserType.BUSINESS.getName()).getCount());
        result = mvcMocks.getSubscribers(SUB_UID, UserType.BUSINESS.getName());
        assertEquals(4, result.getData().size());
    }

    @Test
    public void testIllegalUserType() {
        mvcMocks.getUserSubscriptionsAndExpect4xx("123");
    }

    @Test
    public void testHasSubscription() {
        mvcMocks.subscribeUsingUID("321", SUB_UID, UserType.BUSINESS.getName());
        mvcMocks.subscribeUsingUID("344", SUB_UID, UserType.BUSINESS.getName());

        SubscriptionDto sub1 = mvcMocks.hasSubscriptionUsingUID("321", SUB_UID, UserType.BUSINESS.getName());
        SubscriptionDto sub2 = mvcMocks.hasSubscriptionAndExpectNotFound("321", SUB_UID, UserType.BRAND.getName());

        assertNotNull(sub1);
        assertNull(sub2);
    }

    @BeforeEach
    public void cleanUp() {
        jdbcTemplate.execute("delete from se.subscription");
    }

    private Subscription createSubscription(String uid, String subId, UserType subType) {
        return new Subscription(UserType.UID, uid, subId, subType);
    }
}
