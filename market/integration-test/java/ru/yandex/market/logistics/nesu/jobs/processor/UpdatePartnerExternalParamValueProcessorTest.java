package ru.yandex.market.logistics.nesu.jobs.processor;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.RemovePartnerExternalParamData;
import ru.yandex.market.logistics.nesu.jobs.model.UpdatePartnerExternalParamValueData;
import ru.yandex.market.logistics.nesu.jobs.model.UpdatePartnerExternalParamValuePayload;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тесты DbQueue-таски для обновления external params у партнера в LMS")
class UpdatePartnerExternalParamValueProcessorTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private UpdatePartnerExternalParamValueProcessor updatePartnerExternalParamValueProcessor;

    @AfterEach
    void noMoreInteractionsCheck() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Пустой пэйлоад (нет запросов в LMS)")
    void emptyPayload() {
        updatePartnerExternalParamValueProcessor.processPayload(new UpdatePartnerExternalParamValuePayload(
            "123",
            List.of(),
            List.of()
        ));
    }

    @Test
    @DisplayName("Парамеры в LMS обновляются")
    void paramsUpdated() {
        updatePartnerExternalParamValueProcessor.processPayload(new UpdatePartnerExternalParamValuePayload(
            "123",
            List.of(
                new UpdatePartnerExternalParamValueData(1L, PartnerExternalParamType.DESCRIPTION, "desc"),
                new UpdatePartnerExternalParamValueData(12L, PartnerExternalParamType.IS_DROPOFF, "true"),
                new UpdatePartnerExternalParamValueData(1L, PartnerExternalParamType.IS_DROPOFF, "false"),
                new UpdatePartnerExternalParamValueData(1L, PartnerExternalParamType.CAN_SELL_MEDICINE, "true")
            ),
            List.of(
                new RemovePartnerExternalParamData(100L, PartnerExternalParamType.IS_DROPOFF),
                new RemovePartnerExternalParamData(100L, PartnerExternalParamType.DROPSHIP_EXPRESS),
                new RemovePartnerExternalParamData(200L, PartnerExternalParamType.ASSESSED_VALUE_TOTAL_CHECK)
            )
        ));

        verify(lmsClient).addOrUpdatePartnerExternalParams(
            eq(1L),
            argThat(list -> list.containsAll(List.of(
                new PartnerExternalParamRequest(PartnerExternalParamType.DESCRIPTION, "desc"),
                new PartnerExternalParamRequest(PartnerExternalParamType.IS_DROPOFF, "false"),
                new PartnerExternalParamRequest(PartnerExternalParamType.CAN_SELL_MEDICINE, "true")
            )))
        );

        verify(lmsClient).addOrUpdatePartnerExternalParams(
            12L,
            List.of(new PartnerExternalParamRequest(PartnerExternalParamType.IS_DROPOFF, "true"))
        );

        verify(lmsClient).deleteParamsForPartnerByTypes(
            100L,
            List.of(PartnerExternalParamType.IS_DROPOFF, PartnerExternalParamType.DROPSHIP_EXPRESS)
        );
        verify(lmsClient).deleteParamsForPartnerByTypes(
            200L,
            List.of(PartnerExternalParamType.ASSESSED_VALUE_TOTAL_CHECK)
        );
    }
}
