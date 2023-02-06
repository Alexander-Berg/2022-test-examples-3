package ru.yandex.market.tpl.tms.sqs;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.*;
import ru.yandex.market.tpl.tms.service.external.sqs.OrderFlowStatusHistorySender;
import ru.yandex.market.tpl.tms.service.external.sqs.OrderFlowStatusHistorySendingService;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class OrderFlowStatusHistorySendingTest extends TplTmsAbstractTest {

    private final OrderFlowStatusHistorySendingService service;
    private final OrderGenerateService orderGenerateService;
    private final OrderCommandService orderCommandService;
    private final OrderFlowStatusHistoryRepository orderFlowStatusHistoryRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final ConfigurationService configurationService;

    @MockBean
    private final OrderFlowStatusHistorySender orderFlowStatusHistorySender;

    private Order order;

    @BeforeEach
    void setUp() {
        this.order = orderGenerateService.createOrder();
        Mockito.doReturn(Optional.of(2L))
                .when(configurationProviderAdapter).getValueAsLong(
                        ConfigurationProperties.ORDER_FLOW_STATUS_HISTORY_SENDING_BATCH_SIZE
                );

        Mockito.doNothing().when(orderFlowStatusHistorySender).send(Mockito.any(OrderFlowStatusHistory.class));
    }

    @Test
    void setLesNum() {
        addStatus(OrderFlowStatus.TRANSMITTED_TO_RECIPIENT);
        addStatus(OrderFlowStatus.DELIVERED_TO_RECIPIENT);

        service.setLesNum();

        List<Long> lesNums = fetchStatuses().stream()
                .map(OrderFlowStatusHistory::getLesNum)
                .collect(Collectors.toList());
        Assertions.assertEquals(List.of(1L, 2L, 3L), lesNums);
    }

    @Test
    void sendToLes() {
        configurationService.updateValue(
                ConfigurationProperties.ORDER_FLOW_STATUS_HISTORY_SENDING_LAST_SENDED_LES_NUM.getName(),
                1L
        );

        addStatus(OrderFlowStatus.TRANSMITTED_TO_RECIPIENT);
        addStatus(OrderFlowStatus.DELIVERED_TO_RECIPIENT);
        fillLesNum();

        service.sendHistory();

        Mockito.verify(orderFlowStatusHistorySender, Mockito.times(2))
                .send(Mockito.any(OrderFlowStatusHistory.class));


        //TODO дописать ассерт на последний отправленный ид MARKETTPL-9097
    }

    private void addStatus(OrderFlowStatus status) {
        orderCommandService.forceUpdateFlowStatus(new OrderCommand.UpdateFlowStatus(
                order.getId(),
                status
        ));
    }

    private List<OrderFlowStatusHistory> fetchStatuses() {
        return orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId());
    }

    private void fillLesNum() {
        long lesNum = 0L;
        for (OrderFlowStatusHistory status : fetchStatuses()) {
            status.setLesNum(++lesNum);
            orderFlowStatusHistoryRepository.save(status);
        }
    }

}
