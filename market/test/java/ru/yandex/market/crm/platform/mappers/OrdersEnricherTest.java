package ru.yandex.market.crm.platform.mappers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.crm.external.personal.PersonalService;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.util.Randoms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrdersEnricherTest {

    private static final String TEST_EMAIL_1 = "test1@ya.ru";
    private static final String TEST_PERSONAL_EMAIL_ID_1 = "test1";
    private static final String TEST_EMAIL_2 = "test2@ya.ru";
    private static final String TEST_PERSONAL_EMAIL_ID_2 = "test2";

    private static final Uid EMAIL_UID_1 = Uids.create(UidType.EMAIL, TEST_EMAIL_1);
    private static final Uid EMAIL_UID_2 = Uids.create(UidType.EMAIL, TEST_EMAIL_2);
    private static final Uid PERSONAL_EMAIL_ID_1 = Uids.create(UidType.PERSONAL_EMAIL_ID, TEST_PERSONAL_EMAIL_ID_1);
    private static final Uid PERSONAL_EMAIL_ID_2 = Uids.create(UidType.PERSONAL_EMAIL_ID, TEST_PERSONAL_EMAIL_ID_2);

    private PersonalService personalService;
    private OrdersEnricher ordersEnricher;

    @BeforeEach
    public void setUp() {
        personalService = Mockito.mock(PersonalService.class);
        ordersEnricher = new OrdersEnricher(personalService);
    }

    @AfterEach
    public void tearDown() {
        reset(personalService);
    }

    @Test
    public void testEmptyOrders() {
        List<Order> result = ordersEnricher.enrich(List.of());
        assertThat(result, empty());
    }

    @Test
    public void testOnlyEmailUid() {
        List<Order> result = ordersEnricher.enrich(List.of(
                createOrder(EMAIL_UID_1)
        ));

        verify(personalService, never()).retrieveEmails(any());
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getUidList(), hasSize(1));
        assertEquals(EMAIL_UID_1, result.get(0).getUid(0));
    }

    @Test
    public void testEmailAndPersonalEmailIdUids() {
        List<Order> result = ordersEnricher.enrich(List.of(
                createOrder(EMAIL_UID_1, PERSONAL_EMAIL_ID_1)
        ));

        verify(personalService, never()).retrieveEmails(any());
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getUidList(), hasSize(2));
        assertEquals(EMAIL_UID_1, result.get(0).getUid(0));
        assertEquals(PERSONAL_EMAIL_ID_1, result.get(0).getUid(1));
    }

    @Test
    public void testOnlyPersonalEmailIdUid() {
        when(personalService.retrieveEmails(Set.of(TEST_PERSONAL_EMAIL_ID_1)))
                .thenReturn(Map.of(TEST_PERSONAL_EMAIL_ID_1, TEST_EMAIL_1));

        List<Order> result = ordersEnricher.enrich(List.of(
                createOrder(PERSONAL_EMAIL_ID_1)
        ));

        verify(personalService, only()).retrieveEmails(any());
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getUidList(), hasSize(2));
        assertEquals(PERSONAL_EMAIL_ID_1, result.get(0).getUid(0));
        assertEquals(EMAIL_UID_1, result.get(0).getUid(1));
    }

    @Test
    public void testMultipleOrders() {
        when(personalService.retrieveEmails(Set.of(TEST_PERSONAL_EMAIL_ID_1, TEST_PERSONAL_EMAIL_ID_2)))
                .thenReturn(Map.of(
                        TEST_PERSONAL_EMAIL_ID_1, TEST_EMAIL_1,
                        TEST_PERSONAL_EMAIL_ID_2, TEST_EMAIL_2
                ));

        List<Order> result = ordersEnricher.enrich(List.of(
                createOrder(EMAIL_UID_1),
                createOrder(PERSONAL_EMAIL_ID_1),
                createOrder(EMAIL_UID_1, PERSONAL_EMAIL_ID_1),
                createOrder(),
                createOrder(PERSONAL_EMAIL_ID_1),
                createOrder(PERSONAL_EMAIL_ID_2)
        ));

        verify(personalService, only()).retrieveEmails(any());
        assertThat(result, hasSize(6));

        assertThat(result.get(0).getUidList(), hasSize(1));
        assertEquals(EMAIL_UID_1, result.get(0).getUid(0));

        assertThat(result.get(1).getUidList(), hasSize(2));
        assertEquals(PERSONAL_EMAIL_ID_1, result.get(1).getUid(0));
        assertEquals(EMAIL_UID_1, result.get(1).getUid(1));

        assertThat(result.get(2).getUidList(), hasSize(2));
        assertEquals(EMAIL_UID_1, result.get(2).getUid(0));
        assertEquals(PERSONAL_EMAIL_ID_1, result.get(2).getUid(1));

        assertThat(result.get(3).getUidList(), empty());

        assertThat(result.get(4).getUidList(), hasSize(2));
        assertEquals(PERSONAL_EMAIL_ID_1, result.get(4).getUid(0));
        assertEquals(EMAIL_UID_1, result.get(4).getUid(1));

        assertThat(result.get(5).getUidList(), hasSize(2));
        assertEquals(PERSONAL_EMAIL_ID_2, result.get(5).getUid(0));
        assertEquals(EMAIL_UID_2, result.get(5).getUid(1));
    }

    private Order createOrder(Uid... uids) {
        var builder = Order.newBuilder();
        builder.setEventId(Randoms.positiveLongValue());
        builder.addAllUid(Arrays.asList(uids));
        return builder.build();
    }
}
