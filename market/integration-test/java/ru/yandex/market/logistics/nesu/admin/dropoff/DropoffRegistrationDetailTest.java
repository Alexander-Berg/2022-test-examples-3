package ru.yandex.market.logistics.nesu.admin.dropoff;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/dropoff/dropoff_registration_state.xml")
@DisplayName("Получение детальной карточки запроса на регистрацию дропоффа")
class DropoffRegistrationDetailTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успех")
    @MethodSource
    void getDetail(
        @SuppressWarnings("unused") String displayName,
        long dropoffRegistrationStateId,
        long dropoffLogisticPointId,
        long dropoffPartnerId,
        String responsePath
    ) throws Exception {
        when(lmsClient.getLogisticsPoint(dropoffLogisticPointId)).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder()
                .id(dropoffLogisticPointId)
                .name(String.format("point%d", dropoffLogisticPointId))
                .build()
        ));

        when(lmsClient.getPartner(dropoffPartnerId)).thenReturn(Optional.of(
            PartnerResponse.newBuilder()
                .id(dropoffPartnerId)
                .name(String.format("partner%d", dropoffPartnerId))
                .build()
        ));

        mockMvc.perform(get(String.format("/admin/dropoff-registration/%d", dropoffRegistrationStateId)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));

        verify(lmsClient).getLogisticsPoint(dropoffLogisticPointId);
        verify(lmsClient).getPartner(dropoffPartnerId);
    }

    @Nonnull
    private static Stream<Arguments> getDetail() {
        return Stream.of(
            Arguments.of(
                "Не заполнен requestId",
                1L,
                100L,
                101L,
                "controller/admin/dropoff-registration/get_detail.json"
            ),
            Arguments.of(
                "Заполнен requestId",
                2L,
                200L,
                201L,
                "controller/admin/dropoff-registration/get_detail_request_id.json"
            ),
            Arguments.of(
                "Ошибка при регистрации",
                3L,
                300L,
                301L,
                "controller/admin/dropoff-registration/get_detail_error.json"
            )
        );
    }

    @Test
    @DisplayName("Ошибка. Запись не найдена")
    void getDetailUnknownId() throws Exception {
        mockMvc.perform(get("/admin/dropoff-registration/4"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [DROPOFF_REGISTRATION_STATE] with ids [4]"));
    }
}
