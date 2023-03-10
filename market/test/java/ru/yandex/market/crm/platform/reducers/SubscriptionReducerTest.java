package ru.yandex.market.crm.platform.reducers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.blackbox.PassportProfile;
import ru.yandex.market.crm.platform.blackbox.PassportProfileProvider;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.models.Subscription;
import ru.yandex.market.crm.platform.models.Subscription.Parameter;
import ru.yandex.market.crm.platform.models.Subscription.Status;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SubscriptionReducerTest {

    private static Subscription subscription() {
        return subscription(UserIds.newBuilder().setPuid(PUID_1));
    }

    private static Subscription subscription(UserIds.Builder userIds) {
        return Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, EMAIL))
                .setType(2L)
                .setEmailValid(true)
                .setModificationDate("2015-12-03 12:20:01.0")
                .setStatusModificationDate("2015-12-03 12:20:10.0")
                .setStatus(Status.SUBSCRIBED)
                .setPlatform("DESKTOP")
                .setUserIds(userIds)
                .setActive(true)
                .build();
    }

    private final static String FACT_ID = "Subscription";
    private final static String EMAIL = "test@mail.ru";

    private final static long PUID_1 = 2011;
    private final static long PUID_2 = 2012;

    private PassportProfileProvider profileProvider;
    private SubscriptionReducer reducer;

    @Before
    public void setUp() {
        profileProvider = mock(PassportProfileProvider.class);
        reducer = new SubscriptionReducer(profileProvider);
    }

    @Test
    public void testSubscriptionsWithSameStatusModificationDatesAndDifferentStatuses() {
        List<Subscription> stored = Collections.singletonList(
                Subscription.newBuilder()
                        .setUid(Uids.create(UidType.EMAIL, EMAIL))
                        .setType(48L)
                        .setEmailValid(true)
                        .setModificationDate("2015-12-02 12:20:01.0")
                        .setStatusModificationDate("2015-12-02 12:20:10.0")
                        .setStatus(Status.SUBSCRIBED)
                        .addOtherParams(Parameter.newBuilder().setKey("modelId").setValue("101010").build())
                        .addOtherParams(Parameter.newBuilder().setKey("price").setValue("100").build())
                        .setPlatform("DESKTOP")
                        .setParameter("19")
                        .setUserIds(UserIds.newBuilder().setPuid(2010).build())
                        .setActive(true)
                        .build()
        );

        List<Subscription> newFacts = Collections.singletonList(
                Subscription.newBuilder()
                        .setUid(Uids.create(UidType.EMAIL, EMAIL))
                        .setType(48L)
                        .setEmailValid(true)
                        .setModificationDate("2015-12-03 12:20:01.0")
                        .setStatusModificationDate("2015-12-02 12:20:10.0")
                        .setStatus(Status.UNSUBSCRIBED)
                        .addOtherParams(Parameter.newBuilder().setKey("modelId").setValue("101010").build())
                        .addOtherParams(Parameter.newBuilder().setKey("price").setValue("100").build())
                        .setPlatform("DESKTOP!")
                        .setParameter("19")
                        .setUserIds(UserIds.newBuilder().setPuid(2011).build())
                        .setActive(false)
                        .build()
        );

        Collection<Subscription> reduced = reduce(stored, newFacts);

        assertEquals(1, reduced.size());

        Subscription expected = Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, EMAIL))
                .setType(48L)
                .setEmailValid(true)
                .setModificationDate("2015-12-03 12:20:01.0")
                .setStatusModificationDate("2015-12-02 12:20:10.0")
                .setStatus(Status.UNSUBSCRIBED)
                .addOtherParams(Parameter.newBuilder().setKey("modelId").setValue("101010").build())
                .addOtherParams(Parameter.newBuilder().setKey("price").setValue("100").build())
                .setPlatform("DESKTOP!")
                .setParameter("19")
                .setUserIds(UserIds.newBuilder().setPuid(2011).build())
                .setActive(false)
                .build();

        assertEquals(expected, reduced.iterator().next());
    }

    @Test
    public void testSubscriptionsWithSameAndDifferentStatusModificationDatesAndStatuses() {
        List<Subscription> stored = Collections.singletonList(
                Subscription.newBuilder()
                        .setUid(Uids.create(UidType.EMAIL, EMAIL))
                        .setType(48L)
                        .setEmailValid(true)
                        .setModificationDate("2015-12-02 12:20:01.0")
                        .setStatusModificationDate("2015-12-02 12:20:10.0")
                        .setStatus(Status.SUBSCRIBED)
                        .addOtherParams(Parameter.newBuilder().setKey("modelId").setValue("101010").build())
                        .addOtherParams(Parameter.newBuilder().setKey("price").setValue("100").build())
                        .setPlatform("DESKTOP")
                        .setParameter("19")
                        .setUserIds(UserIds.newBuilder().setPuid(2010).build())
                        .setActive(true)
                        .build()
        );

        List<Subscription> newFacts = Arrays.asList(
                Subscription.newBuilder()
                        .setUid(Uids.create(UidType.EMAIL, EMAIL))
                        .setType(48L)
                        .setEmailValid(true)
                        .setModificationDate("2015-12-03 12:20:01.0")
                        .setStatusModificationDate("2015-12-03 12:20:10.0")
                        .setStatus(Status.UNSUBSCRIBED)
                        .addOtherParams(Parameter.newBuilder().setKey("modelId").setValue("101010").build())
                        .addOtherParams(Parameter.newBuilder().setKey("price").setValue("100").build())
                        .setPlatform("DESKTOP!")
                        .setParameter("19")
                        .setUserIds(UserIds.newBuilder().setPuid(2011).setUuid("123").build())
                        .setActive(false)
                        .addAllPlaces(Arrays.asList("1", "2", "3", "4"))
                        .build(),

                Subscription.newBuilder()
                        .setUid(Uids.create(UidType.EMAIL, EMAIL))
                        .setType(48L)
                        .setEmailValid(true)
                        .setModificationDate("2015-12-04 12:20:01.0")
                        .setStatusModificationDate("2015-12-03 12:20:10.0")
                        .setStatus(Status.SUBSCRIBED)
                        .addOtherParams(Parameter.newBuilder().setKey("modelId").setValue("101010").build())
                        .addOtherParams(Parameter.newBuilder().setKey("price").setValue("100").build())
                        .addOtherParams(Parameter.newBuilder().setKey("price1").setValue("1001").build())
                        .setPlatform("DESKTOP!!!")
                        .setParameter("19")
                        .setUserIds(UserIds.newBuilder().setPuid(2012).setYandexuid("1010").build())
                        .setActive(true)
                        .addAllPlaces(Arrays.asList("1", "2", "3", "5"))
                        .build()
        );

        Collection<Subscription> reduced = reduce(stored, newFacts);
        assertEquals(1, reduced.size());

        Subscription expected = Subscription.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, EMAIL))
                .setType(48L)
                .setEmailValid(true)
                .setModificationDate("2015-12-04 12:20:01.0")
                .setStatusModificationDate("2015-12-03 12:20:10.0")
                .setStatus(Status.SUBSCRIBED)
                .addOtherParams(Parameter.newBuilder().setKey("modelId").setValue("101010").build())
                .addOtherParams(Parameter.newBuilder().setKey("price").setValue("100").build())
                .addOtherParams(Parameter.newBuilder().setKey("price1").setValue("1001").build())
                .setPlatform("DESKTOP!!!")
                .setParameter("19")
                .setUserIds(UserIds.newBuilder().setPuid(2012).setYandexuid("1010").setUuid("123").build())
                .setActive(true)
                .addAllPlaces(Arrays.asList("1", "2", "3", "4", "5"))
                .build();

        assertEquals(expected, reduced.iterator().next());
    }

    /**
     * ?????? ???????????????????? ?????????? ???????????????? ?? ?????????????? ???????????? puid, ?????????????????????? ?? puid ????????????????????
     * ???????????? ???????????????????????????? ????????????, ???????? puid'???? ?????????????????????? ???????? linked_puid
     */
    @Test
    public void testSaveNewSubscriptionWithLinkedPuid() {
        preparePassportEmail(PUID_1);

        List<Subscription> newFacts = Collections.singletonList(
                subscription()
        );

        Collection<Subscription> saved = reduce(Collections.emptyList(), newFacts);
        assertEquals(1, saved.size());

        Subscription subscription = saved.iterator().next();
        assertEquals(EMAIL, subscription.getUid().getStringValue());
        assertEquals(PUID_1, subscription.getUserIds().getPuid());
        assertEquals(PUID_1, subscription.getLinkedPuid());
    }

    /**
     * ?????? ???????????????????? ???????????????? puid ?????????????? (?? ?????????????? ???????????????????????? ????????????????) ???? ?????????????????? ??
     * ???????????????????? puid, ???????? linked_puid ?????????????????????? ????????????????????
     */
    @Test
    public void testDoNotFillLinkedPuidIfPassportPuidDoesNotMatch() {
        preparePassportEmail(PUID_2);

        List<Subscription> newFacts = Collections.singletonList(
                subscription()
        );

        Collection<Subscription> saved = reduce(Collections.emptyList(), newFacts);
        assertEquals(1, saved.size());

        Subscription subscription = saved.iterator().next();
        assertEquals(EMAIL, subscription.getUid().getStringValue());
        assertEquals(PUID_1, subscription.getUserIds().getPuid());
        assertEquals(PUID_2, subscription.getLinkedPuid());
    }

    /**
     * ?????? ???????????????????? ???????????????? ?? ?????????????????????? puid ???? email ?????????????? ?????? ???????? ?? ?????????????? ???? ?????? puid,
     * ???????????????????????? ???????????????? puid ?? ???????????????????? ???????? linked_puid
     */
    @Test
    public void testFillLinkedPuidIfPuidHasChanged() {
        preparePassportEmail(PUID_1);

        List<Subscription> storedFacts = Collections.singletonList(
                subscription(UserIds.newBuilder())
        );

        List<Subscription> newFacts = Collections.singletonList(
                subscription(UserIds.newBuilder().setPuid(PUID_1))
        );

        Collection<Subscription> saved = reduce(storedFacts, newFacts);
        assertEquals(1, saved.size());

        Subscription subscription = saved.iterator().next();
        assertEquals(EMAIL, subscription.getUid().getStringValue());
        assertEquals(PUID_1, subscription.getUserIds().getPuid());
        assertEquals(PUID_1, subscription.getLinkedPuid());
    }

    /**
     * ?? ???????????? ???????? ?? ???????????????? puid ???????????????? ???? ?????????????????????????? ?? ???????????????????? puid
     * email'??, ???????????????? ???????? linked_puid ???????????????? ????????????????????
     */
    @Test
    public void testFillResetLinkedPuidIfPuidIsChanged() {
        preparePassportEmail(PUID_1);

        List<Subscription> storedFacts = Collections.singletonList(
                subscription().toBuilder()
                        .setLinkedPuid(PUID_1)
                        .build()
        );

        List<Subscription> newFacts = Collections.singletonList(
                subscription(UserIds.newBuilder().setPuid(PUID_2))
        );

        Collection<Subscription> saved = reduce(storedFacts, newFacts);
        assertEquals(1, saved.size());

        Subscription subscription = saved.iterator().next();
        assertEquals(EMAIL, subscription.getUid().getStringValue());
        assertEquals(PUID_2, subscription.getUserIds().getPuid());
        assertEquals(PUID_1, subscription.getLinkedPuid());
    }

    /**
     * ???????? puid ???????????????? ???? ?????????????????? ?????????????????? ???????????????? ?????????????????????? puid ???? ????????????????????
     */
    @Test
    public void testDoNotRecheckLinkedPuidIfPuidDidNotChange() {
        List<Subscription> storedFacts = Collections.singletonList(
                subscription().toBuilder()
                        .setLinkedPuid(PUID_1)
                        .build()
        );

        List<Subscription> newFacts = Collections.singletonList(
                subscription()
        );

        Collection<Subscription> saved = reduce(storedFacts, newFacts);
        assertEquals(1, saved.size());

        Subscription subscription = saved.iterator().next();
        assertEquals(EMAIL, subscription.getUid().getStringValue());
        assertEquals(PUID_1, subscription.getUserIds().getPuid());
        assertEquals(PUID_1, subscription.getLinkedPuid());

        verifyNoInteractions(profileProvider);
    }

    /**
     * ???????? ?? ???????????????? ???? ???????????????? puid ???????????????? ???? ?????? ???????????????????????? ??????????????????????
     * puid ???????????? ???? ??????????????????????
     */
    @Test
    public void testDoNotCheckEmptyPuid() {
        List<Subscription> newFacts = Collections.singletonList(
                subscription(UserIds.newBuilder())
        );

        Collection<Subscription> saved = reduce(Collections.emptyList(), newFacts);
        assertEquals(1, saved.size());

        Subscription subscription = saved.iterator().next();
        assertEquals(EMAIL, subscription.getUid().getStringValue());
        assertEquals(0, subscription.getUserIds().getPuid());
        assertEquals(0, subscription.getLinkedPuid());

        verifyNoInteractions(profileProvider);
    }

    private Collection<Subscription> reduce(List<Subscription> stored, List<Subscription> newFacts) {
        YieldMock collector = new YieldMock();
        reducer.reduce(stored, newFacts, collector);

        return collector.getAdded(FACT_ID);
    }

    private void preparePassportEmail(long puid) {
        when(profileProvider.getProfile(EMAIL)).thenReturn(new PassportProfile(puid, EMAIL));
    }
}
