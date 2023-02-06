package ru.yandex.market.tpl.core.service.delivery.settings;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.possibleOrderChanges.PossibleOrderChangeRequest;
import ru.yandex.market.logistics.management.entity.response.possibleOrderChanges.PossibleOrderChangeDto;
import ru.yandex.market.logistics.management.entity.response.possibleOrderChanges.PossibleOrderChangeGroup;
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod;
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.service.delivery.settings.PossibleOrderChangesCreator.ORDER_CHANGE_TYPES;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PossibleOrderChangesCreatorTest {
    private static final long DELIVERY_SERVICE_ID = 48455L;

    @MockBean
    private LMSClient lmsClient;

    private final PossibleOrderChangesCreator possibleOrderChangesCreator;

    @Test
    void setupPossibleOrderChange() {
        when(lmsClient.getPartnerPossibleOrderChanges(ORDER_CHANGE_TYPES)).thenReturn(List.of());

        possibleOrderChangesCreator.create(DELIVERY_SERVICE_ID);

        verify(lmsClient, times(1))
                .createPossibleOrderChange(PossibleOrderChangeRequest.builder()
                        .partnerId(DELIVERY_SERVICE_ID)
                        .type(PossibleOrderChangeType.RECIPIENT)
                        .method(PossibleOrderChangeMethod.PARTNER_API)
                        .checkpointStatusTo(49)
                        .enabled(true)
                        .build());
    }

    @Test
    void setupPossibleOrderChangeWhenOtherPartnersExist() {
        when(lmsClient.getPartnerPossibleOrderChanges(ORDER_CHANGE_TYPES)).thenReturn(List.of(
                new PossibleOrderChangeGroup(DELIVERY_SERVICE_ID + 1, List.of(
                        PossibleOrderChangeDto.builder()
                                .id(1L)
                                .partnerId(DELIVERY_SERVICE_ID + 1)
                                .method(PossibleOrderChangeMethod.PARTNER_API)
                                .type(PossibleOrderChangeType.RECIPIENT)
                                .build()
                )),
                new PossibleOrderChangeGroup(DELIVERY_SERVICE_ID + 2, List.of(
                        PossibleOrderChangeDto.builder()
                                .id(2L)
                                .partnerId(DELIVERY_SERVICE_ID + 2)
                                .method(PossibleOrderChangeMethod.PARTNER_API)
                                .type(PossibleOrderChangeType.RECIPIENT)
                                .build()
                ))
        ));

        possibleOrderChangesCreator.create(DELIVERY_SERVICE_ID);

        verify(lmsClient, times(1))
                .createPossibleOrderChange(PossibleOrderChangeRequest.builder()
                        .partnerId(DELIVERY_SERVICE_ID)
                        .type(PossibleOrderChangeType.RECIPIENT)
                        .method(PossibleOrderChangeMethod.PARTNER_API)
                        .checkpointStatusTo(49)
                        .enabled(true)
                        .build());
    }

    @Test
    void possibleOrderChangeAlreadyExists() {
        when(lmsClient.getPartnerPossibleOrderChanges(ORDER_CHANGE_TYPES)).thenReturn(
                List.of(
                        new PossibleOrderChangeGroup(DELIVERY_SERVICE_ID,
                                ORDER_CHANGE_TYPES.stream()
                                        .map(t -> PossibleOrderChangeDto.builder()
                                                .id(1L)
                                                .partnerId(DELIVERY_SERVICE_ID)
                                                .method(PossibleOrderChangeMethod.PARTNER_API)
                                                .type(t)
                                                .build())
                                        .collect(Collectors.toList())),
                        new PossibleOrderChangeGroup(DELIVERY_SERVICE_ID + 2,
                                ORDER_CHANGE_TYPES.stream()
                                        .map(t -> PossibleOrderChangeDto.builder()
                                                .id(1L)
                                                .partnerId(DELIVERY_SERVICE_ID + 2)
                                                .method(PossibleOrderChangeMethod.PARTNER_API)
                                                .type(t)
                                                .build())
                                        .collect(Collectors.toList()))
                ));

        possibleOrderChangesCreator.create(DELIVERY_SERVICE_ID);

        verify(lmsClient, never()).createPossibleOrderChange(any());
    }

    @Test
    void internalLmsErrorWhileGet() {
        when(lmsClient.getPartnerPossibleOrderChanges(ORDER_CHANGE_TYPES))
                .thenThrow(new HttpTemplateException(500, ""));

        assertThatThrownBy(() -> possibleOrderChangesCreator.create(DELIVERY_SERVICE_ID))
                .isExactlyInstanceOf(HttpTemplateException.class);
    }

    @Test
    void internalLmsErrorWhileCreate() {
        when(lmsClient.createPossibleOrderChange(any())).thenThrow(new HttpTemplateException(500, ""));

        assertThatThrownBy(() -> possibleOrderChangesCreator.create(DELIVERY_SERVICE_ID))
                .isExactlyInstanceOf(HttpTemplateException.class);

        verify(lmsClient, times(1))
                .createPossibleOrderChange(PossibleOrderChangeRequest.builder()
                        .partnerId(DELIVERY_SERVICE_ID)
                        .type(any())
                        .method(PossibleOrderChangeMethod.PARTNER_API)
                        .checkpointStatusTo(49)
                        .enabled(true)
                        .build());
    }

}
