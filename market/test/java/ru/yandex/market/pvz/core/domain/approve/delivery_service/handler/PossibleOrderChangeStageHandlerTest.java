package ru.yandex.market.pvz.core.domain.approve.delivery_service.handler;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.possibleOrderChanges.PossibleOrderChangeRequest;
import ru.yandex.market.logistics.management.entity.response.possibleOrderChanges.PossibleOrderChangeDto;
import ru.yandex.market.logistics.management.entity.response.possibleOrderChanges.PossibleOrderChangeGroup;
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PossibleOrderChangeStageHandler.CHECKPOINT_STATUS_TO;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PossibleOrderChangeStageHandler.ORDER_CHANGE_METHOD;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PossibleOrderChangeStageHandler.ORDER_CHANGE_TYPES;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PossibleOrderChangeStageHandler.POSSIBLE_ORDER_CHANGE_ENABLED;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PossibleOrderChangeStageHandler.REDELIVERY_ORDER_CHANGE_TYPES;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PossibleOrderChangeStageHandlerTest {

    private static final long DELIVERY_SERVICE_ID = 48455L;

    @MockBean
    private LMSClient lmsClient;

    private final PossibleOrderChangeStageHandler possibleOrderChangeStageHandler;

    @Test
    void setupPossibleOrderChange() {
        when(lmsClient.getPartnerPossibleOrderChanges(ORDER_CHANGE_TYPES)).thenReturn(List.of());

        possibleOrderChangeStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));

        verify(lmsClient, times(1))
                .createPossibleOrderChange(PossibleOrderChangeRequest.builder()
                        .partnerId(DELIVERY_SERVICE_ID)
                        .type(PossibleOrderChangeType.RECIPIENT)
                        .method(ORDER_CHANGE_METHOD)
                        .checkpointStatusTo(CHECKPOINT_STATUS_TO)
                        .enabled(POSSIBLE_ORDER_CHANGE_ENABLED)
                        .build());
    }

    @Test
    void setupPossibleOrderChangeWhenOtherPartnersExist() {
        when(lmsClient.getPartnerPossibleOrderChanges(ORDER_CHANGE_TYPES)).thenReturn(List.of(
                new PossibleOrderChangeGroup(DELIVERY_SERVICE_ID + 1, List.of(
                        PossibleOrderChangeDto.builder()
                                .id(1L)
                                .partnerId(DELIVERY_SERVICE_ID + 1)
                                .method(ORDER_CHANGE_METHOD)
                                .type(PossibleOrderChangeType.RECIPIENT)
                                .build()
                )),
                new PossibleOrderChangeGroup(DELIVERY_SERVICE_ID + 2, List.of(
                        PossibleOrderChangeDto.builder()
                                .id(2L)
                                .partnerId(DELIVERY_SERVICE_ID + 2)
                                .method(ORDER_CHANGE_METHOD)
                                .type(PossibleOrderChangeType.RECIPIENT)
                                .build()
                ))
        ));

        possibleOrderChangeStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));

        verify(lmsClient, times(1))
                .createPossibleOrderChange(PossibleOrderChangeRequest.builder()
                        .partnerId(DELIVERY_SERVICE_ID)
                        .type(PossibleOrderChangeType.RECIPIENT)
                        .method(ORDER_CHANGE_METHOD)
                        .checkpointStatusTo(CHECKPOINT_STATUS_TO)
                        .enabled(POSSIBLE_ORDER_CHANGE_ENABLED)
                        .build());
    }

    @Test
    void possibleOrderChangeAlreadyExists() {
        when(lmsClient.getPartnerPossibleOrderChanges(any())).thenReturn(
                List.of(
                        new PossibleOrderChangeGroup(DELIVERY_SERVICE_ID,
                                Stream.concat(ORDER_CHANGE_TYPES.stream(), REDELIVERY_ORDER_CHANGE_TYPES.stream())
                                        .map(t -> PossibleOrderChangeDto.builder()
                                                .id(1L)
                                                .partnerId(DELIVERY_SERVICE_ID)
                                                .method(ORDER_CHANGE_METHOD)
                                                .type(t)
                                                .build())
                                        .collect(Collectors.toList())),
                        new PossibleOrderChangeGroup(DELIVERY_SERVICE_ID + 2,
                                Stream.concat(ORDER_CHANGE_TYPES.stream(), REDELIVERY_ORDER_CHANGE_TYPES.stream())
                                        .map(t -> PossibleOrderChangeDto.builder()
                                                .id(1L)
                                                .partnerId(DELIVERY_SERVICE_ID + 2)
                                                .method(ORDER_CHANGE_METHOD)
                                                .type(t)
                                                .build())
                                        .collect(Collectors.toList()))
                ));

        possibleOrderChangeStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));

        verify(lmsClient, never()).createPossibleOrderChange(any());
    }

    @Test
    void internalLmsErrorWhileGet() {
        when(lmsClient.getPartnerPossibleOrderChanges(any()))
                .thenThrow(new HttpTemplateException(500, ""));

        assertThatThrownBy(() -> possibleOrderChangeStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")))
                .isExactlyInstanceOf(HttpTemplateException.class);
    }

    @Test
    void internalLmsErrorWhileCreate() {
        when(lmsClient.createPossibleOrderChange(any())).thenThrow(new HttpTemplateException(500, ""));

        assertThatThrownBy(() -> possibleOrderChangeStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")))
                .isExactlyInstanceOf(HttpTemplateException.class);

        verify(lmsClient, times(1))
                .createPossibleOrderChange(PossibleOrderChangeRequest.builder()
                        .partnerId(DELIVERY_SERVICE_ID)
                        .type(any())
                        .method(ORDER_CHANGE_METHOD)
                        .checkpointStatusTo(CHECKPOINT_STATUS_TO)
                        .enabled(POSSIBLE_ORDER_CHANGE_ENABLED)
                        .build());
    }
}
