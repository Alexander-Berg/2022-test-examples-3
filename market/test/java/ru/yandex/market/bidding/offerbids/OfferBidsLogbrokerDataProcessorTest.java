package ru.yandex.market.bidding.offerbids;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.market.bidding.ExchangeProtos;
import ru.yandex.market.bidding.service.AdminService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.bidding.offerbids.OfferBidsTestUtils.createMessageBatch;
import static ru.yandex.market.bidding.offerbids.OfferBidsTestUtils.createParcel;

/**
 * Тесты для {@link OfferBidsLogbrokerDataProcessor}
 */
@RunWith(MockitoJUnitRunner.class)
public class OfferBidsLogbrokerDataProcessorTest {

    @Mock
    private AdminService adminService;

    private OfferBidsLogbrokerDataProcessor dataProcessor;

    private MockParcelConsumer parcelConsumer = spy(MockParcelConsumer.class);

    @Before
    public void beforeEach() {
        dataProcessor = new OfferBidsLogbrokerDataProcessor(parcelConsumer, adminService);
    }

    @Test
    @DisplayName("Тест на то, что сообщения не будут обрабатываться, если флаг выключен")
    public void testWithFlagProcessOfferBidsReplyOff() {
        when(adminService.isProcessOfferBidsReply()).thenReturn(false);
        dataProcessor.process(mock(MessageBatch.class));

        verify(adminService).isProcessOfferBidsReply();
        verifyNoMoreInteractions(adminService);
        verifyZeroInteractions(parcelConsumer);
    }

    @Test
    @DisplayName("Тест на то, что парсель будет зачитан, если флаг включен")
    public void testWithFlagProcessOfferBidsReplyOn() {
        when(adminService.isProcessOfferBidsReply()).thenReturn(true);
        dataProcessor = spy(dataProcessor);

        var parcel = createParcel();
        var parcelBytes = parcel.toByteArray();
        var messageBatch = createMessageBatch(parcelBytes);

        dataProcessor.process(messageBatch);

        verify(dataProcessor).process(messageBatch);
        verify(dataProcessor).processData(parcelBytes);
        verifyNoMoreInteractions(dataProcessor);

        verify(parcelConsumer).accept(eq(parcel));
    }

    @Test
    @DisplayName("Тест на то, что мы упадем, если в лб передадут не парсель")
    public void testWithError() {
        when(adminService.isProcessOfferBidsReply()).thenReturn(true);

        var messageBatch = createMessageBatch("SomeInvalidParcel".getBytes());
        Assertions.assertThrows(
                RuntimeException.class,
                () -> dataProcessor.process(messageBatch)
        );
    }

    private static class MockParcelConsumer implements Consumer<ExchangeProtos.Parcel> {
        public MockParcelConsumer() {
        }

        @Override
        public void accept(ExchangeProtos.Parcel parcel) {
            // заглушка, т.к. мокито не умеет мокать лямбды
            // mock(() -> {}); бросит ошибку
        }
    }
}
