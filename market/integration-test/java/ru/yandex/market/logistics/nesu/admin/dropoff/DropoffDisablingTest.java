package ru.yandex.market.logistics.nesu.admin.dropoff;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.nesu.admin.model.response.AdminDropoffDisablingNewDto;
import ru.yandex.market.logistics.nesu.base.AbstractDropoffDisablingTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Тесты на создание заявки на отключение дропофа")
class DropoffDisablingTest extends AbstractDropoffDisablingTest {

    @Override
    @Nonnull
    protected ResultActions createDisablingDropoffRequest(
        Long logisticPointId,
        LocalDateTime startClosingDate,
        LocalDateTime closingDate
    ) throws Exception {
        return mockMvc.perform(
            post("/admin/dropoff-disabling")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    buildRequest(
                        logisticPointId,
                        startClosingDate,
                        closingDate
                    )
                ))
        );
    }

    @Nonnull
    private AdminDropoffDisablingNewDto buildRequest(
        Long logisticPointId,
        LocalDateTime startClosingDateTime,
        LocalDateTime closingDateTime
    ) {
        return AdminDropoffDisablingNewDto.builder()
            .logisticPointId(logisticPointId)
            .startClosingDate(startClosingDateTime)
            .closingDate(closingDateTime)
            .reasonId(AbstractDropoffDisablingTest.REASON.getAdminId())
            .build();
    }

    @Nonnull
    @Override
    protected String getNotFoundReasonMessage() {
        return "Failed to find [DROPOFF_DISABLING_REASON] with ids [1]";
    }

    @Override
    @Nonnull
    protected String getObjectName() {
        return "adminDropoffDisablingNewDto";
    }

    @Override
    @Nonnull
    protected ResultMatcher responseContent() {
        return MockMvcResultMatchers.content().string("1");
    }
}
