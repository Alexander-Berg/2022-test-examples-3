package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.tpl.common.logbroker.producer.LogbrokerProducerFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.TICKET_SYSTEM_FLOW_ENABLED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AcceptanceControllerTest extends BaseApiControllerTest {

    @MockBean
    Clock clock;

    TestControllerCaller caller;
    SortingCenter sortingCenter;

    @MockBean
    LogbrokerProducerFactory logbrokerProducerFactory;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedUser(sortingCenter, UID);
        testFactory.setSortingCenterProperty(sortingCenter, TICKET_SYSTEM_FLOW_ENABLED, true);
        caller = new TestControllerCaller(mockMvc);

        mockLogbrokerBeans();
    }

    private void mockLogbrokerBeans() {
        var writeAckFuture = new CompletableFuture<ProducerWriteResponse>();
        writeAckFuture.complete(new ProducerWriteResponse(1, 1, true));

        var asyncProducer = Mockito.mock(AsyncProducer.class);
        when(asyncProducer.write(any(), anyLong())).thenReturn(writeAckFuture);
        when(logbrokerProducerFactory.createProducer("/topic")).thenReturn(asyncProducer);
    }

    @Test
    @SneakyThrows
    @DisplayName("отправка сообщения начала приемки в электронную очередь")
    void startAcceptance() {
        caller.startAcceptance("ticket-id").andExpect(status().is2xxSuccessful());
    }

    @Test
    @SneakyThrows
    @DisplayName("отправка сообщения окончания приемки в электронную очередь")
    void finishAcceptance() {
        caller.finishAcceptance("ticket-id").andExpect(status().is2xxSuccessful());
    }
}
