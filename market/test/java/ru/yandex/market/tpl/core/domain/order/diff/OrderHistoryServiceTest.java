package ru.yandex.market.tpl.core.domain.order.diff;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.difference.AddressOrderDifference;
import ru.yandex.market.tpl.core.domain.order.difference.OrderHistoryService;
import ru.yandex.market.tpl.core.domain.order.difference.PersonalRecipientOrderDifference;
import ru.yandex.market.tpl.core.domain.order.difference.RecipientDataOrderDifference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_ORDER_HISTORY_ENABLED;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderHistoryServiceTest {
    private static final long orderId = 123L;
    private static final String ADDRESS = "address";
    private static final String NAME = "name";
    private static final String PERSONAL_NAME_ID = "personal-name-id";
    private static final String ANOTHER_NAME = "another name";
    private static final String PHONE = "phone";
    private static final String PERSONAL_PHONE_ID = "personal-phone-id";

    private static final OrderHistoryEvent ADDRESS_CHANGED = OrderHistoryEvent.builder()
            .orderId(orderId)
            .type(OrderEventType.ADDRESS_CHANGED)
            .difference(new AddressOrderDifference(ADDRESS, ADDRESS))
            .build();
    private static final OrderHistoryEvent NAME_CHANGED = OrderHistoryEvent.builder()
            .orderId(orderId)
            .type(OrderEventType.RECIPIENT_DATA_CHANGED)
            .difference(new RecipientDataOrderDifference(NAME, null))
            .build();
    private static final OrderHistoryEvent ANOTHER_NAME_CHANGED = OrderHistoryEvent.builder()
            .orderId(orderId)
            .type(OrderEventType.RECIPIENT_DATA_CHANGED)
            .difference(new RecipientDataOrderDifference(ANOTHER_NAME, null))
            .build();
    private static final OrderHistoryEvent PHONE_CHANGED = OrderHistoryEvent.builder()
            .orderId(orderId)
            .type(OrderEventType.RECIPIENT_DATA_CHANGED)
            .difference(new RecipientDataOrderDifference(null, PHONE))
            .build();
    private static final OrderHistoryEvent NAME_PHONE_CHANGED = OrderHistoryEvent.builder()
            .orderId(orderId)
            .type(OrderEventType.RECIPIENT_DATA_CHANGED)
            .difference(new RecipientDataOrderDifference(NAME, PHONE))
            .build();
    private static final OrderHistoryEvent PERSONAL_NAME_CHANGED = OrderHistoryEvent.builder()
            .orderId(orderId)
            .type(OrderEventType.RECIPIENT_DATA_CHANGED)
            .difference(new PersonalRecipientOrderDifference(PERSONAL_NAME_ID, null))
            .build();
    private static final OrderHistoryEvent PERSONAL_PHONE_CHANGED = OrderHistoryEvent.builder()
            .orderId(orderId)
            .type(OrderEventType.RECIPIENT_DATA_CHANGED)
            .difference(new PersonalRecipientOrderDifference(null, PERSONAL_PHONE_ID))
            .build();
    private static final OrderHistoryEvent PERSONAL_PHONE_NAME_CHANGED = OrderHistoryEvent.builder()
            .orderId(orderId)
            .type(OrderEventType.RECIPIENT_DATA_CHANGED)
            .difference(new PersonalRecipientOrderDifference(PERSONAL_NAME_ID, PERSONAL_PHONE_ID))
            .build();

    private final OrderHistoryService orderHistoryService;

    @MockBean
    private OrderHistoryEventRepository orderHistoryEventRepository;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    public void init() {
        when(configurationProviderAdapter.isBooleanEnabled(eq(IS_ORDER_HISTORY_ENABLED))).thenReturn(true);
    }

    @Test
    public void nameAndPhoneChanged() {
        when(orderHistoryEventRepository.findHistoricalEventsForOrders(eq(List.of(orderId)))).thenReturn(
                List.of(NAME_CHANGED, PHONE_CHANGED)
        );

        var result = orderHistoryService.getOrdersHistory(List.of(orderId));

        var orderHistory = result.get(orderId);
        assertThat(orderHistory).isNotNull();
        assertThat(orderHistory.getPhone()).isEqualTo(PHONE);
        assertThat(orderHistory.getName()).isEqualTo(NAME);
        assertThat(orderHistory.getAddress()).isNull();
    }

    @Test
    public void personaNameAndPhoneChanged() {
        when(orderHistoryEventRepository.findHistoricalEventsForOrders(eq(List.of(orderId)))).thenReturn(
                List.of(PERSONAL_PHONE_CHANGED, PERSONAL_NAME_CHANGED)
        );

        var result = orderHistoryService.getOrdersHistory(List.of(orderId));

        var orderHistory = result.get(orderId);
        assertThat(orderHistory).isNotNull();
        assertThat(orderHistory.getPersonalPhoneId()).isEqualTo(PERSONAL_PHONE_ID);
        assertThat(orderHistory.getPersonalNameId()).isEqualTo(PERSONAL_NAME_ID);
        assertThat(orderHistory.getAddress()).isNull();
    }

    @Test
    public void allChanged() {
        when(orderHistoryEventRepository.findHistoricalEventsForOrders(eq(List.of(orderId)))).thenReturn(
                List.of(ADDRESS_CHANGED, NAME_CHANGED, PHONE_CHANGED)
        );

        var result = orderHistoryService.getOrdersHistory(List.of(orderId));

        var orderHistory = result.get(orderId);
        assertThat(orderHistory).isNotNull();
        assertThat(orderHistory.getPhone()).isEqualTo(PHONE);
        assertThat(orderHistory.getName()).isEqualTo(NAME);
        assertThat(orderHistory.getAddress()).isEqualTo(ADDRESS);
    }

    @Test
    public void nameChangedSeveralTimes() {
        when(orderHistoryEventRepository.findHistoricalEventsForOrders(eq(List.of(orderId)))).thenReturn(
                List.of(NAME_CHANGED, ANOTHER_NAME_CHANGED, ANOTHER_NAME_CHANGED)
        );

        var result = orderHistoryService.getOrdersHistory(List.of(orderId));

        var orderHistory = result.get(orderId);
        assertThat(orderHistory).isNotNull();
        assertThat(orderHistory.getPhone()).isNull();
        assertThat(orderHistory.getName()).isEqualTo(NAME);
        assertThat(orderHistory.getAddress()).isNull();
    }

    @Test
    public void namePhoneChanged() {
        when(orderHistoryEventRepository.findHistoricalEventsForOrders(eq(List.of(orderId)))).thenReturn(
                List.of(NAME_PHONE_CHANGED)
        );

        var result = orderHistoryService.getOrdersHistory(List.of(orderId));

        var orderHistory = result.get(orderId);
        assertThat(orderHistory).isNotNull();
        assertThat(orderHistory.getPhone()).isEqualTo(PHONE);
        assertThat(orderHistory.getName()).isEqualTo(NAME);
        assertThat(orderHistory.getAddress()).isNull();
    }

    @Test
    public void personalNamePhoneChanged() {
        when(orderHistoryEventRepository.findHistoricalEventsForOrders(eq(List.of(orderId)))).thenReturn(
                List.of(PERSONAL_PHONE_NAME_CHANGED)
        );

        var result = orderHistoryService.getOrdersHistory(List.of(orderId));

        var orderHistory = result.get(orderId);
        assertThat(orderHistory).isNotNull();
        assertThat(orderHistory.getPersonalNameId()).isEqualTo(PERSONAL_NAME_ID);
        assertThat(orderHistory.getPersonalPhoneId()).isEqualTo(PERSONAL_PHONE_ID);
        assertThat(orderHistory.getAddress()).isNull();
    }

    @Test
    public void personalAndNonPersonalNamePhoneChanged() {
        when(orderHistoryEventRepository.findHistoricalEventsForOrders(eq(List.of(orderId)))).thenReturn(
                List.of(PERSONAL_PHONE_NAME_CHANGED, NAME_PHONE_CHANGED)
        );

        var result = orderHistoryService.getOrdersHistory(List.of(orderId));

        var orderHistory = result.get(orderId);
        assertThat(orderHistory).isNotNull();
        assertThat(orderHistory.getPersonalNameId()).isEqualTo(PERSONAL_NAME_ID);
        assertThat(orderHistory.getPersonalPhoneId()).isEqualTo(PERSONAL_PHONE_ID);
        assertThat(orderHistory.getPhone()).isEqualTo(PHONE);
        assertThat(orderHistory.getName()).isEqualTo(NAME);
        assertThat(orderHistory.getAddress()).isNull();
    }
}
