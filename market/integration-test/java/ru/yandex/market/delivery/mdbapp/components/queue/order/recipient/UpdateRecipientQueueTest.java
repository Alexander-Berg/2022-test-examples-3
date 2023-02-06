package ru.yandex.market.delivery.mdbapp.components.queue.order.recipient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import steps.UpdateRecipientSteps;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.UpdateOrderDeliveryDateDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.recipient.PersonDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.recipient.UpdateRecipientDto;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.delivery.mdbapp.configuration.queue.UpdateRecipientQueue;
import ru.yandex.market.delivery.mdbapp.integration.converter.UpdateRecipientPayloadConverter;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderRecipientRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.delivery.mdbapp.testutils.MockUtils.prepareMockServer;

public class UpdateRecipientQueueTest extends AllMockContextualTest {

    @Autowired
    @Qualifier(value = UpdateRecipientQueue.UPDATE_RECIPIENT_QUEUE_PRODUCER)
    private QueueProducer<UpdateRecipientDto> updateRecipientQueueProducer;

    @Autowired
    private TaskLifecycleListener taskListener;

    private CountDownLatch countDownLatch;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    private MockRestServiceServer checkouterMockServer;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private UpdateRecipientPayloadConverter recipientPayloadConverter;

    @Before
    public void setUp() {
        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);

        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        countDownLatch = new CountDownLatch(1);
        mockedTaskListener.setFinishedLatch(countDownLatch);
    }

    @Test
    public void testEnqueueOldProcessWithFakeId() throws Exception {
        UpdateRecipientDto updateRecipientDto = UpdateRecipientSteps
            .createUpdateRecipientDto(UpdateOrderDeliveryDateDto.FAKE_CHANGE_REQUEST_ID);

        updateRecipientQueueProducer.enqueue(EnqueueParams.create(updateRecipientDto));
        countDownLatch.await(2, TimeUnit.SECONDS);

        UpdateOrderRecipientRequestDto recipientRequestDto = createUpdateRecipientRequest(updateRecipientDto);
        doReturn(createUpdateRecipientResponse()).when(lomClient).updateOrderRecipient(recipientRequestDto);

        assertions(updateRecipientDto, UpdateOrderDeliveryDateDto.FAKE_CHANGE_REQUEST_ID);

        verify(lomClient).updateOrderRecipient(recipientRequestDto);
    }

    @Test
    public void testEnqueueInLom() throws Exception {
        prepareMockServer(checkouterMockServer, "/orders/7000/change-requests/88585");

        UpdateRecipientDto updateRecipientDto = UpdateRecipientSteps.createUpdateRecipientDto();
        updateRecipientQueueProducer.enqueue(EnqueueParams.create(updateRecipientDto));
        countDownLatch.await(2, TimeUnit.SECONDS);

        UpdateOrderRecipientRequestDto recipientRequestDto = createUpdateRecipientRequest(updateRecipientDto);
        doReturn(createUpdateRecipientResponse()).when(lomClient).updateOrderRecipient(recipientRequestDto);

        checkouterMockServer.verify();

        verify(lomClient).updateOrderRecipient(recipientRequestDto);
    }

    private UpdateOrderRecipientRequestDto createUpdateRecipientRequest(UpdateRecipientDto updateRecipientDto) {
        return recipientPayloadConverter.toRecipientRequestDto(updateRecipientDto);
    }

    private ChangeOrderRequestDto createUpdateRecipientResponse() {
        return ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ChangeOrderRequestType.RECIPIENT)
            .status(ChangeOrderRequestStatus.PROCESSING)
            .build();
    }

    private void assertions(UpdateRecipientDto payload, long requestUpdateId) {
        softly.assertThat(payload).isNotNull();
        softly.assertThat(payload).extracting(
            UpdateRecipientDto::getOrderId,
            UpdateRecipientDto::getParcelId,
            UpdateRecipientDto::getUpdateRequestId,
            UpdateRecipientDto::getPartnerId,
            UpdateRecipientDto::getPhone,
            UpdateRecipientDto::getEmail,
            UpdateRecipientDto::isDataGathered,
            UpdateRecipientDto::getTrackCode
        ).as("updateRecipientDto fields must be equal").contains(
            UpdateRecipientSteps.ORDER_ID,
            UpdateRecipientSteps.PARCEL_ID,
            requestUpdateId,
            UpdateRecipientSteps.PARTNER_ID,
            UpdateRecipientSteps.PHONE,
            UpdateRecipientSteps.EMAIL,
            UpdateRecipientSteps.DATA_GATHERED,
            UpdateRecipientSteps.TRACK_CODE
        );
        softly.assertThat(payload.getPerson()).extracting(
            PersonDto::getFirstName,
            PersonDto::getLastName,
            PersonDto::getMiddleName
        ).as("personDto fields must be equal").contains(
            UpdateRecipientSteps.FIRST_NAME,
            UpdateRecipientSteps.LAST_NAME,
            UpdateRecipientSteps.MIDDLE_NAME
        );
    }
}
