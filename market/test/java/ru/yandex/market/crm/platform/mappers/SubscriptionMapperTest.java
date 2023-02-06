package ru.yandex.market.crm.platform.mappers;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.models.Subscription;

public class SubscriptionMapperTest {
    private SubscriptionMapper mapper;

    @Before
    public void setUp() {
        mapper = new SubscriptionMapper();
    }

    @Test
    public void testTskvWithTwoRequiredFieldsAndValidEmail() {
        String subscriptionLine =
                "tskv\t" +
                        "email=test@mail.ru\t" +
                        "subscription_type=1";

        Collection<Subscription> actual = mapper.apply(subscriptionLine.getBytes());
        Assert.assertEquals(1, actual.size());

        Subscription actualSubscription = actual.iterator().next();
        Subscription expectedSubscription = Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, "test@mail.ru"))
                .setType(1L)
                .setEmailValid(true)
                .setUserIds(UserIds.newBuilder().build())
                .setId("1")
                .build();

        Assert.assertEquals(expectedSubscription, actualSubscription);
    }

    @Test
    public void testTskvWithTwoRequiredFieldsAndInvalidEmail() {
        String subscriptionLine =
                "tskv\t" +
                        "email=test\t" +
                        "subscription_type=1";

        Collection<Subscription> actual = mapper.apply(subscriptionLine.getBytes());
        Assert.assertEquals(1, actual.size());

        Subscription actualSubscription = actual.iterator().next();
        Subscription expectedSubscription = Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, "test"))
                .setType(1L)
                .setEmailValid(false)
                .setUserIds(UserIds.newBuilder().build())
                .setId("1")
                .build();

        Assert.assertEquals(expectedSubscription, actualSubscription);
    }

    @Test
    public void testTskvWithoutStatusModificationDateInResult() {
        String subscriptionLine =
                "tskv\t" +
                        "email=test@mail.ru\t" +
                        "subscription_type=1\t" +
                        "modification_date=2015-12-02 12:20:00.0\t" +
                        "confirmed_date=2015-12-02 12:20:01.0";

        Collection<Subscription> actual = mapper.apply(subscriptionLine.getBytes());
        Assert.assertEquals(1, actual.size());

        Subscription actualSubscription = actual.iterator().next();
        Subscription expectedSubscription = Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, "test@mail.ru"))
                .setType(1L)
                .setEmailValid(true)
                .setModificationDate("2015-12-02 12:20:01.0")
                .setUserIds(UserIds.newBuilder().build())
                .setId("1")
                .build();

        Assert.assertEquals(expectedSubscription, actualSubscription);
    }


    @Test
    public void testTskvWithStatusModificationDateInResultFromConfirmedDate() {
        String subscriptionLine =
                "tskv\t" +
                        "email=test@mail.ru\t" +
                        "subscription_type=1\t" +
                        "modification_date=2015-12-02 12:20:00.0\t" +
                        "confirmed_date=2015-12-02 12:20:01.0\t" +
                        "subscription_status=1";

        Collection<Subscription> actual = mapper.apply(subscriptionLine.getBytes());
        Assert.assertEquals(1, actual.size());

        Subscription actualSubscription = actual.iterator().next();
        Subscription expectedSubscription = Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, "test@mail.ru"))
                .setType(1L)
                .setEmailValid(true)
                .setModificationDate("2015-12-02 12:20:01.0")
                .setStatusModificationDate("2015-12-02 12:20:01.0")
                .setStatus(Subscription.Status.SUBSCRIBED)
                .setUserIds(UserIds.newBuilder().build())
                .setActive(true)
                .setId("1")
                .build();

        Assert.assertEquals(expectedSubscription, actualSubscription);
    }

    @Test
    public void testTskvWithStatusModificationDateInResultFromUnsubscribedDate() {
        String subscriptionLine =
                "tskv\t" +
                        "email=test@mail.ru\t" +
                        "subscription_type=1\t" +
                        "modification_date=2015-12-02 12:20:00.0\t" +
                        "confirmed_date=2015-12-02 12:20:01.0\t" +
                        "unsubscribe_date=2015-12-02 12:20:10.0\t" +
                        "subscription_status=2";

        Collection<Subscription> actual = mapper.apply(subscriptionLine.getBytes());
        Assert.assertEquals(1, actual.size());

        Subscription actualSubscription = actual.iterator().next();
        Subscription expectedSubscription = Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, "test@mail.ru"))
                .setType(1L)
                .setEmailValid(true)
                .setModificationDate("2015-12-02 12:20:01.0")
                .setStatusModificationDate("2015-12-02 12:20:10.0")
                .setStatus(Subscription.Status.UNSUBSCRIBED)
                .setUserIds(UserIds.newBuilder().build())
                .setId("1")
                .build();

        Assert.assertEquals(expectedSubscription, actualSubscription);
    }

    @Test
    public void testTskvWithParameterFromModelId() {
        String subscriptionLine =
                "tskv\t" +
                        "email=test@mail.ru\t" +
                        "subscription_type=1\t" +
                        "modification_date=2015-12-02 12:20:00.0\t" +
                        "confirmed_date=2015-12-02 12:20:01.0\t" +
                        "unsubscribe_date=2015-12-02 12:20:10.0\t" +
                        "subscription_status=2\t" +
                        "subscription_parameter_price_4=100\t" +
                        "subscription_parameter_platform_5=DESKTOP\t" +
                        "subscription_parameter_modelId_15=101010";

        Collection<Subscription> actual = mapper.apply(subscriptionLine.getBytes());
        Assert.assertEquals(1, actual.size());

        Subscription actualSubscription = actual.iterator().next();
        Subscription expectedSubscription = Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, "test@mail.ru"))
                .setType(1L)
                .setEmailValid(true)
                .setModificationDate("2015-12-02 12:20:01.0")
                .setStatusModificationDate("2015-12-02 12:20:10.0")
                .setStatus(Subscription.Status.UNSUBSCRIBED)
                .addOtherParams(Subscription.Parameter.newBuilder().setKey("price").setValue("100").build())
                .setPlatform("DESKTOP")
                .setParameter("101010")
                .setUserIds(UserIds.newBuilder().build())
                .setId("1$101010")
                .build();

        Assert.assertEquals(expectedSubscription, actualSubscription);
    }

    @Test
    public void testTskvWithoutParameter() {
        String subscriptionLine =
                "tskv\t" +
                        "email=test@mail.ru\t" +
                        "subscription_type=48\t" +
                        "modification_date=2015-12-02 12:20:00.0\t" +
                        "confirmed_date=2015-12-02 12:20:01.0\t" +
                        "unsubscribe_date=2015-12-02 12:20:10.0\t" +
                        "subscription_status=2\t" +
                        "subscription_parameter_price_4=100\t" +
                        "subscription_parameter_platform_5=DESKTOP\t" +
                        "subscription_parameter_modelId_15=101010";

        Collection<Subscription> actual = mapper.apply(subscriptionLine.getBytes());
        Assert.assertEquals(1, actual.size());

        Subscription actualSubscription = actual.iterator().next();
        Subscription expectedSubscription = Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, "test@mail.ru"))
                .setType(48L)
                .setEmailValid(true)
                .setModificationDate("2015-12-02 12:20:01.0")
                .setStatusModificationDate("2015-12-02 12:20:10.0")
                .setStatus(Subscription.Status.UNSUBSCRIBED)
                .addOtherParams(Subscription.Parameter.newBuilder().setKey("modelId").setValue("101010").build())
                .addOtherParams(Subscription.Parameter.newBuilder().setKey("price").setValue("100").build())
                .setPlatform("DESKTOP")
                .setUserIds(UserIds.newBuilder().build())
                .setId("48")
                .build();

        Assert.assertEquals(expectedSubscription, actualSubscription);
    }

    @Test
    public void testTskvWithParameterFromQuestionId() {
        String subscriptionLine =
                "tskv\t" +
                        "email=test@mail.ru\t" +
                        "subscription_type=48\t" +
                        "modification_date=2015-12-02 12:20:00.0\t" +
                        "confirmed_date=2015-12-02 12:20:01.0\t" +
                        "unsubscribe_date=2015-12-02 12:20:10.0\t" +
                        "subscription_status=2\t" +
                        "puid=2010\t" +
                        "subscription_parameter_price_4=100\t" +
                        "subscription_parameter_platform_5=DESKTOP\t" +
                        "subscription_parameter_questionId_15=19\t" +
                        "subscription_parameter_modelId_=101010";

        Collection<Subscription> actual = mapper.apply(subscriptionLine.getBytes());
        Assert.assertEquals(1, actual.size());

        Subscription actualSubscription = actual.iterator().next();
        Subscription expectedSubscription = Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, "test@mail.ru"))
                .setType(48L)
                .setEmailValid(true)
                .setModificationDate("2015-12-02 12:20:01.0")
                .setStatusModificationDate("2015-12-02 12:20:10.0")
                .setStatus(Subscription.Status.UNSUBSCRIBED)
                .addOtherParams(Subscription.Parameter.newBuilder().setKey("modelId").setValue("101010").build())
                .addOtherParams(Subscription.Parameter.newBuilder().setKey("price").setValue("100").build())
                .setPlatform("DESKTOP")
                .setParameter("19")
                .setUserIds(UserIds.newBuilder().setPuid(2010).build())
                .setId("48$19")
                .build();

        Assert.assertEquals(expectedSubscription, actualSubscription);
    }
}
