package ru.yandex.market.logistics.nesu.controller.internal;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.nesu.base.AbstractDropoffDisablingTest;
import ru.yandex.market.logistics.nesu.client.model.dropoff.DropoffDisablingRequestDto;
import ru.yandex.market.logistics.nesu.model.entity.DisableDropoffReason;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DisplayName("Создание заявки на отключение дропофа через внутреннее API")
class InternalDropoffDisablingControllerTest extends AbstractDropoffDisablingTest {

    @Override
    @Nonnull
    protected ResultActions createDisablingDropoffRequest(
        Long logisticPointId,
        LocalDateTime startClosingDate,
        LocalDateTime closingDate
    ) throws Exception {
        return mockMvc.perform(
            post("/internal/dropoff-disabling")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    buildRequest(
                        logisticPointId,
                        startClosingDate,
                        closingDate,
                        AbstractDropoffDisablingTest.REASON
                    )
                ))
        );
    }

    @Nonnull
    private DropoffDisablingRequestDto buildRequest(
        Long logisticPointId,
        LocalDateTime startClosingDateTime,
        LocalDateTime closingDateTime,
        DisableDropoffReason reason
    ) {
        return DropoffDisablingRequestDto.builder()
            .logisticPointId(logisticPointId)
            .startClosingDate(startClosingDateTime)
            .closingDate(closingDateTime)
            .reason(reason.getCode())
            .build();
    }

    @Nonnull
    @Override
    protected String getNotFoundReasonMessage() {
        return "Dropoff disabling reason with code UNPROFITABLE not found";
    }

    @Override
    @Nonnull
    protected String getObjectName() {
        return "dropoffDisablingRequestDto";
    }

    @Override
    @Nonnull
    protected ResultMatcher responseContent() {
        return jsonContent("controller/internal/dropoff-disabling/created_request.json");
    }
}
