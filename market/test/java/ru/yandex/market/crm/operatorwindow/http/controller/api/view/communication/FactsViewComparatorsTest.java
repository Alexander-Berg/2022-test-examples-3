package ru.yandex.market.crm.operatorwindow.http.controller.api.view.communication;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.crm.operatorwindow.http.controller.util.EnumValueView;

import static org.mockito.Mockito.when;

public class FactsViewComparatorsTest {

    private static final String BRAND1 = "brand1";
    private static final String BRAND2 = "brand2";
    private static final Long ORDER_ID1 = 100002L;
    private static final Long ORDER_ID2 = 100001L;
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    private static final FactEoTicketView FACT_1 = createEoTicketFact(1, BRAND1, null, true, 0, NOW);
    private static final FactEoTicketView FACT_2 = createEoTicketFact(2, null, ORDER_ID2, true, 0, NOW);
    private static final FactEoTicketView FACT_3 = createEoTicketFact(3, BRAND2, ORDER_ID2, null, 3, NOW);
    private static final FactEoTicketView FACT_4 = createEoTicketFact(4, null, ORDER_ID1, null, 0, NOW);
    private static final FactEoTicketView FACT_5 = createEoTicketFact(5, BRAND2, ORDER_ID2, true, 3, NOW.plusHours(1));
    private static final FactEoTicketView FACT_6 = createEoTicketFact(6, BRAND1, ORDER_ID2, false, 0, NOW);
    private static final FactEoTicketView FACT_7 = createEoTicketFact(7, BRAND2, ORDER_ID2, true, 0, NOW);
    private static final FactEoTicketView FACT_8 = createEoTicketFact(8, BRAND2, ORDER_ID2, true, 5, NOW);

    private static FactEoTicketView createEoTicketFact(int index,
                                                       String brand,
                                                       Long orderId,
                                                       Boolean fcr,
                                                       long csi,
                                                       OffsetDateTime createdAt) {
        FactEoTicketView ticket = Mockito.mock(FactEoTicketView.class);
        when(ticket.getBrand()).thenReturn(new EnumValueView(brand, null));
        when(ticket.getOrderId()).thenReturn(orderId);
        when(ticket.getFcr()).thenReturn(fcr);
        when(ticket.getCsi()).thenReturn(csi);
        when(ticket.getCreated()).thenReturn(createdAt);
        when(ticket.toString()).thenReturn("fact" + index);
        return ticket;
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSorting() {
        List<FactEoTicketView> actual = Arrays.asList(FACT_1, FACT_2, FACT_3, FACT_4, FACT_5, FACT_6, FACT_7, FACT_8);
        actual.sort(FactsViewComparators.getEoTicketFactComparator(BRAND2, ORDER_ID2));
        Assertions.assertEquals(List.of(FACT_5, FACT_3, FACT_8, FACT_7, FACT_6, FACT_2, FACT_1, FACT_4), actual);
    }

    @Test
    public void testSortingWhenBrandIsNull() {
        List<FactEoTicketView> actual = Arrays.asList(FACT_1, FACT_3, FACT_4);
        actual.sort(FactsViewComparators.getEoTicketFactComparator(null, ORDER_ID2));
        Assertions.assertEquals(List.of(FACT_3, FACT_1, FACT_4), actual);
    }

    @Test
    public void testSortingWhenOrderIdIsNull() {
        List<FactEoTicketView> actual = Arrays.asList(FACT_1, FACT_2, FACT_3);
        actual.sort(FactsViewComparators.getEoTicketFactComparator(BRAND2, null));
        Assertions.assertEquals(List.of(FACT_3, FACT_1, FACT_2), actual);
    }
}
