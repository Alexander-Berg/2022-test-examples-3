package ru.yandex.market.logistics.nesu.api.deliveryoptions;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.api.model.deliveryoptions.SenderDeliveryOptionsFilter;
import ru.yandex.market.logistics.nesu.base.AbstractDeliveryOptionsSearchTest;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilter;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createScheduleDayDtoSetWithSize;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск вариантов доставки в Open API")
class ApiDeliveryOptionsSearchTest extends AbstractDeliveryOptionsSearchTest {

    private static final long PARTNER_ID = 100;

    @Autowired
    private BlackboxService blackboxService;
    @Autowired
    private MbiApiClient mbiApiClient;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);

        authHolder.mockAccess(mbiApiClient, PARTNER_ID);
    }

    @Test
    @DisplayName("Не указан сендер")
    void noSender() throws Exception {
        search(defaultFilter(), null)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                fieldError("senderId", "must not be null", "senderDeliveryOptionsFilter", "NotNull")
            ));
    }

    @Test
    @DisplayName("Недоступный пользователю магазин")
    void inaccessibleShop() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, PARTNER_ID);

        search(defaultFilter())
            .andExpect(status().isForbidden())
            .andExpect(content().string(""));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Замена склада")
    @MethodSource
    @DatabaseSetup(
        value = "/repository/delivery-options/api_warehouse_substitution.xml",
        type = DatabaseOperation.INSERT
    )
    void withSubstitution(
        @SuppressWarnings("unused") String name,
        Long oldWarehouseId,
        Long newWarehouseId
    ) throws Exception {
        when(lmsClient.getLogisticsPoint(newWarehouseId)).thenReturn(Optional.of(
            warehouseBuilder(newWarehouseId, null).businessId(42L).schedule(createScheduleDayDtoSetWithSize(6)).build()
        ));
        search(defaultFilter().andThen(withSenderWarehouse(oldWarehouseId))).andExpect(status().isOk());
        verify(lmsClient).getLogisticsPoint(newWarehouseId);
    }

    @Nonnull
    private static Stream<Arguments> withSubstitution() {
        return Stream.of(
            Arguments.of("Склад был заменен", SENDER_WAREHOUSE_ID, 400L),
            Arguments.of("Нет замены для склада, есть по businessId", 2L, 2L),
            Arguments.of("Нет замены по businessId, есть по складу", 4L, 4L),
            Arguments.of("Нет замены по businessId, и складу", 5L, 5L)
        );
    }

    @Nonnull
    @Override
    protected String optionFilterObjectName() {
        return "senderDeliveryOptionsFilter";
    }

    @Nonnull
    @Override
    protected ResultActions search(@Nonnull Consumer<DeliveryOptionsFilter> filterAdjuster, @Nullable Long senderId)
        throws Exception {
        SenderDeliveryOptionsFilter filter = new SenderDeliveryOptionsFilter();
        filterAdjuster.accept(filter);
        filter.setSenderId(senderId);
        return mockMvc.perform(request(HttpMethod.PUT, "/api/delivery-options", filter)
            .headers(authHolder.authHeaders()));
    }
}
