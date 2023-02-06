package ru.yandex.market.notifier.senders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.client.entity.PushApiOrder;
import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.ff4shops.api.model.CourierDto;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.ff4shops.client.FF4ShopsClient;
import ru.yandex.market.notifier.jobs.zk.impl.LocalOrder;
import ru.yandex.market.notifier.jobs.zk.processors.AbstractEventProcessor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff4shops.api.model.ElectronicAcceptCodeStatus.OK_SHOW;

public class PushMessageSenderTest {

    final PushMessageSender pushMessageSender = new PushMessageSender();

    @Test
    public void shouldParseShipmentDateForPush() throws IOException {
        //given:
        String orderWithShipmentDate = IOUtils.toString(
                this.getClass().getResourceAsStream("/files/local-order-from-notifier.xml"),
                StandardCharsets.UTF_8
        );
        //when:
        LocalOrder localOrder = pushMessageSender.parseOrder(orderWithShipmentDate, ChannelType.PUSH);

        //then:
        assertNotNull(localOrder);
        assertNotNull(localOrder.getWrappedOrder());
        assertNotNull(localOrder.getWrappedOrder().getDelivery());
        List<Parcel> parcels = localOrder.getWrappedOrder().getDelivery().getParcels();
        assertThat(parcels, hasSize(1));
        assertEquals(parcels.get(0).getShipmentDate(), LocalDate.of(2020, 1, 30));
    }

    @Test
    public void testOrderStatusWithCourierInfo() throws IOException, PermanentIssueException {
        DeliveryChannel channel = new DeliveryChannel(ChannelType.PUSH, "10281764");
        String orderWithShipmentDate = IOUtils.toString(
                this.getClass().getResourceAsStream("/files/local-order-from-notifier.xml"),
                StandardCharsets.UTF_8
        );
        Map<String, String> messageProperties = new HashMap<>();
        CourierDto courierDto = CourierDto.builder()
                .setFirstName("Тест").setLastName("Тестов")
                .setElectronicAcceptCodeRequired(true)
                .setElectronicAcceptCodeStatus(OK_SHOW)
                .setElectronicAcceptanceCertificateCode("123-456")
                .setPhoneNumber("+7999999999").setPhoneExtension("123").setVehicleNumber("тт123т").build();
        messageProperties.put(PushMessageSender.ORDER_KEY, orderWithShipmentDate);
        PushApi pushClient = mock(PushApi.class);
        FF4ShopsClient ff4ShopsClient = mock(FF4ShopsClient.class);
        when(ff4ShopsClient.getCourier(anyLong())).thenReturn(courierDto);
        pushMessageSender.setFf4ShopsClient(ff4ShopsClient);
        pushMessageSender.setPushClient(pushClient);
        pushMessageSender.send(AbstractEventProcessor.PUSH_API_STATUS_CHANGE_TYPE, channel, messageProperties);

        ArgumentCaptor<PushApiOrder> captor = ArgumentCaptor.forClass(PushApiOrder.class);
        verify(pushClient).orderStatus(anyLong(), captor.capture(), anyBoolean(), any(), any(), isNull());
        PushApiOrder pushApiOrder = captor.getValue();
        Courier pushApiCourier = pushApiOrder.getDelivery().getCourier();
        assertEquals(pushApiOrder.getElectronicAcceptanceCertificateCode(), "123-456");
        assertEquals(pushApiCourier.getVehicleNumber(), "тт123т");
        assertEquals(pushApiCourier.getFullName(), "Тестов Тест");
        assertEquals(pushApiCourier.getPhone(), "+7999999999");
        assertEquals(pushApiCourier.getPhoneExtension(), "123");
    }

    @Test
    public void testShootingOrderSkipping() throws IOException, PermanentIssueException {
        DeliveryChannel channel = new DeliveryChannel(ChannelType.PUSH, "10281764");
        String order = IOUtils.toString(
                this.getClass().getResourceAsStream("/files/shooting-order-for-push-api.xml"),
                StandardCharsets.UTF_8
        );
        Map<String, String> messageProperties = new HashMap<>();
        messageProperties.put(PushMessageSender.ORDER_KEY, order);
        PushApi pushClient = mock(PushApi.class);
        FF4ShopsClient ff4ShopsClient = mock(FF4ShopsClient.class);
        pushMessageSender.setPushClient(pushClient);
        pushMessageSender.setFf4ShopsClient(ff4ShopsClient);
        reset(pushClient, ff4ShopsClient);

        pushMessageSender.send(AbstractEventProcessor.PUSH_API_STATUS_CHANGE_TYPE, channel, messageProperties);

        verifyNoInteractions(pushClient, ff4ShopsClient);
    }
}
