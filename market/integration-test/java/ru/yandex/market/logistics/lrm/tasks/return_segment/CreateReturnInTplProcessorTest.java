package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.LocalDate;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentIdPayload;
import ru.yandex.market.logistics.lrm.queue.processor.CreateReturnInTplProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.tpl.internal.client.TplInternalClient;
import ru.yandex.market.tpl.internal.client.model.clientreturn.ClientReturnCreateDto;
import ru.yandex.market.tpl.internal.client.model.clientreturn.ClientReturnSystemCreated;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Создание возврата в TPL")
class CreateReturnInTplProcessorTest extends AbstractIntegrationTest {

    private static final long RETURN_SEGMENT_ID = 1;

    @Autowired
    private CreateReturnInTplProcessor processor;

    @Autowired
    private TplInternalClient tplInternalClient;

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(tplInternalClient);
    }

    @Test
    @DisplayName("Успешная обработка")
    @DatabaseSetup("/database/tasks/return-segment/create-in-tpl/before/setup.xml")
    void success() {
        mockLms();
        processor.execute(ReturnSegmentIdPayload.builder().returnSegmentId(RETURN_SEGMENT_ID).build());
        verifyTask();
    }

    @Test
    @DisplayName("Ошибка обработки")
    @DatabaseSetup("/database/tasks/return-segment/create-in-tpl/before/setup.xml")
    void fail() {
        mockLms();
        doThrow(new RuntimeException("error message"))
            .when(tplInternalClient)
            .createClientReturn(any(ClientReturnCreateDto.class));
        softly.assertThatThrownBy(
                () -> processor.execute(ReturnSegmentIdPayload.builder().returnSegmentId(RETURN_SEGMENT_ID).build())
            )
            .isInstanceOf(RuntimeException.class)
            .hasMessage("error message");
        verifyTask();
    }

    @Test
    @DisplayName("Не найдена точка")
    @DatabaseSetup("/database/tasks/return-segment/create-in-tpl/before/setup.xml")
    void pointNotFound() {
        softly.assertThatThrownBy(
                () -> processor.execute(ReturnSegmentIdPayload.builder().returnSegmentId(RETURN_SEGMENT_ID).build())
            )
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Unable to create return in tpl: not found pickupPointId, return segment id 1");
        verify(tplInternalClient, never()).createClientReturn(any(ClientReturnCreateDto.class));
    }

    private void mockLms() {
        when(lmsClient.getLogisticsPoint(1234L)).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder()
                .externalId("90")
                .partnerId(467L)
                .build()
        ));
    }

    private void verifyTask() {
        ClientReturnCreateDto request = new ClientReturnCreateDto();
        request.setRequestDate(LocalDate.of(2021, 11, 20));
        request.setBarcode("box-external-id");
        request.setLogisticPointId(1234L);
        request.setPickupPointId(90L);
        request.setSystemCreated(ClientReturnSystemCreated.LRM);
        verify(tplInternalClient).createClientReturn(eq(request));
    }
}
