package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;
import ru.yandex.market.sdk.userinfo.service.NoSideEffectUserService;
import ru.yandex.mj.generated.client.b2bclients.model.MultiOrderPaymentInvoiceDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.B2B_MULTI_ORDER_PAYMENT_INVOICE_ZIP_URL;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.MULTI_ORDER_ITEMS;
import static ru.yandex.market.crm.core.services.trigger.MessageTypes.BUSINESS_ORDER_MULTI_ORDER_PAYMENT_INVOICE_ZIP;
import static ru.yandex.market.crm.triggers.services.logbroker.consumers.MessageMatchers.messagesMatcher;

@ExtendWith(MockitoExtension.class)
public class B2bMultiOrderPaymentInvoiceConsumerTest {

    private B2bMultiOrderPaymentInvoiceConsumer consumer;
    private Gson objectMapper;
    @Mock
    private MessageSender messageSender;
    @Mock
    private LogTypesResolver logTypes;
    @Mock
    private CheckouterClient checkouterClient;
    @Mock
    private NoSideEffectUserService noSideEffectUserService;

    @BeforeEach
    void setUp() {
        when(logTypes.getLogIdentifier("b2bmultiorder.events"))
                .thenReturn(new LogIdentifier("null/null", LBInstallation.LOGBROKER));

        Order order = new Order();
        Buyer buyer = new Buyer();
        buyer.setUid(1234567L);
        order.setBuyer(buyer);

        when(checkouterClient.getOrder(any(), any()))
                .thenReturn(order);

        when(noSideEffectUserService.isNoSideEffectUid(1234567L)).thenReturn(false);

        objectMapper = new Gson();
        consumer = new B2bMultiOrderPaymentInvoiceConsumer(logTypes, messageSender, checkouterClient,
                noSideEffectUserService);
    }

    @Test
    public void testMultiorderEvents() throws IOException {
        InputStream eventStream = getClass().getResourceAsStream("b2b_multiorder_payment_invoice.json");
        byte[] eventBytes = IOUtils.toByteArray(Objects.requireNonNull(eventStream));
        Assertions.assertNotNull(eventBytes);

        transformAndAccept(eventBytes);

        MultiOrderPaymentInvoiceDto event = objectMapper.fromJson(new String(eventBytes, StandardCharsets.UTF_8),
                MultiOrderPaymentInvoiceDto.class);

        UidBpmMessage expectedMessage = new UidBpmMessage(
                BUSINESS_ORDER_MULTI_ORDER_PAYMENT_INVOICE_ZIP,
                Uid.asPuid(1234567L),
                Map.of(
                        B2B_MULTI_ORDER_PAYMENT_INVOICE_ZIP_URL, event.getPaymentInvoiceUrl()
                ),
                Map.of(
                        B2B_MULTI_ORDER_PAYMENT_INVOICE_ZIP_URL, event.getPaymentInvoiceUrl(),
                        MULTI_ORDER_ITEMS, event.getOrdersIds()
                )
        );

        verify(messageSender).send(argThat(messagesMatcher(expectedMessage)));
    }


    private void transformAndAccept(byte[] eventBytes) {
        List<MultiOrderPaymentInvoiceDto> rows = consumer.transform(eventBytes);
        Assertions.assertNotNull(rows);
        consumer.accept(rows);
    }
}
