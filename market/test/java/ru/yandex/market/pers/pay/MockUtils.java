package ru.yandex.market.pers.pay;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableLong;
import org.mockito.ArgumentCaptor;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.pers.pay.model.ContentPrice;
import ru.yandex.market.pers.pay.model.PersPayEntity;
import ru.yandex.market.pers.pay.model.PersPayEntityState;
import ru.yandex.market.pers.pay.model.PersPayEntityType;
import ru.yandex.market.pers.pay.model.PersPayUser;
import ru.yandex.market.pers.pay.model.PersPayer;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.model.PersPaymentBuilder;
import ru.yandex.market.pers.pay.model.dto.PaymentEntityStateEventDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.pay.model.PersPayUserType.UID;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 19.03.2021
 */
public class MockUtils {

    public static final String TEST_PAYER = "test";

    public static void mockConsumer(LogbrokerClientFactory clientFactory,
                                    List<ConsumerReadResponse> responses) throws Exception {
        SyncConsumer consumer = mock(SyncConsumer.class);
        when(clientFactory.syncConsumer(any())).thenReturn(consumer);

        MutableBoolean isInitCalled = new MutableBoolean(false);
        MutableLong lastCommit = new MutableLong();
        when(consumer.init()).then(invocation -> {
            isInitCalled.setTrue();
            return null;
        });

        Iterator<ConsumerReadResponse> responseIterator = responses.iterator();
        when(consumer.read()).then(invocation -> {
            assertTrue(isInitCalled.getValue());

            if (!responseIterator.hasNext()) {
                return null;
            }
            ConsumerReadResponse next = responseIterator.next();
            assertEquals(next.getCookie() - 1, (long) lastCommit.getValue());
            return next;
        });

        doAnswer(invocation -> {
            lastCommit.setValue(invocation.getArgument(0));
            return null;
        }).when(consumer).commit(anyLong());
    }

    public static ArgumentCaptor<byte[]> mockLogbrokerWrite(LogbrokerClientFactory logbrokerClientFactory) throws InterruptedException {
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);

        AsyncProducer asyncProducer = mock(AsyncProducer.class);
        when(logbrokerClientFactory.asyncProducer(any())).thenReturn(asyncProducer);
        when(asyncProducer.init())
            .thenReturn(CompletableFuture.completedFuture(new ProducerInitResponse(1, "", 1, "")));
        when(asyncProducer.write(messageCaptor.capture()))
            .thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, true)));

        return messageCaptor;
    }

    public static List<String> parseCapturedMessagesRaw(ArgumentCaptor<byte[]> messageCaptor) {
        return messageCaptor.getAllValues().stream()
            .map(String::new)
            .collect(Collectors.toList());
    }

    public static PersPaymentBuilder testPay(long modelId, long userId) {
        return PersPayment.builderModelGradeUid(modelId, userId)
            .payer(PersPayerType.MARKET, TEST_PAYER)
            .amount(1);
    }

    public static PersPaymentBuilder testPhotoPay(long modelId, long userId) {
        return PersPayment.builderModelGradeUid(modelId, userId)
            .entity(PersPayEntityType.MODEL_GRADE_PHOTO, String.valueOf(modelId))
            .payer(PersPayerType.MARKET, TEST_PAYER)
            .amount(1);
    }

    public static String testPayKey(long modelId, long userId) {
        return testPay(modelId, userId).getPayKey();
    }

    public static ContentPrice testPrice(long modelId, int amount) {
        return testPrice(modelId, amount, TEST_PAYER);
    }

    public static ContentPrice testPrice(long modelId, int amount, String payerId) {
        ContentPrice result = new ContentPrice();
        result.setEntity(new PersPayEntity(PersPayEntityType.MODEL_GRADE, modelId));
        result.setPayer(new PersPayer(PersPayerType.MARKET, payerId));
        result.setAmount(amount);
        result.setAmountCharge(BigDecimal.valueOf(amount));
        return result;
    }

    public static PaymentEntityStateEventDto createModelGradeDto(long modelId,
                                                                 long userId,
                                                                 PersPayEntityState state,
                                                                 String contentId,
                                                                 long timestamp) {
        return new PaymentEntityStateEventDto(
            new PersPayUser(UID, userId),
            new PersPayEntity(PersPayEntityType.MODEL_GRADE, modelId),
            state,
            contentId,
            timestamp
        );
    }

    public static PaymentEntityStateEventDto createModelGradePhotoDto(long modelId,
                                                                      long userId,
                                                                      PersPayEntityState state,
                                                                      String contentId,
                                                                      long timestamp) {
        return new PaymentEntityStateEventDto(
            new PersPayUser(UID, userId),
            new PersPayEntity(PersPayEntityType.MODEL_GRADE_PHOTO, modelId),
            state,
            contentId,
            timestamp
        );
    }

    public static PaymentEntityStateEventDto createModelVideoDto(long modelId,
                                                                 long userId,
                                                                 PersPayEntityState state,
                                                                 String contentId,
                                                                 long timestamp) {
        return new PaymentEntityStateEventDto(
            new PersPayUser(UID, userId),
            new PersPayEntity(PersPayEntityType.MODEL_VIDEO, modelId),
            state,
            contentId,
            timestamp
        );
    }
}
