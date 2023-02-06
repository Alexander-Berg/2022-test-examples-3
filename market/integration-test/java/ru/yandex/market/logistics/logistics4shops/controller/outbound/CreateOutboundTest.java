package ru.yandex.market.logistics.logistics4shops.controller.outbound;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.OutboundApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OutboundsCreateRequest;
import ru.yandex.market.logistics.logistics4shops.utils.ModelFactory;

import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Создание отправки")
@DatabaseSetup("/controller/outbound/create/prepare.xml")
@ParametersAreNonnullByDefault
class CreateOutboundTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Успешно сохранить отправки")
    @ExpectedDatabase(
        value = "/controller/outbound/create/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void save() {
        OutboundsCreateRequest request = new OutboundsCreateRequest().outbounds(List.of(
            ModelFactory.outbound(1L, "ya-id" + 1L, "ext-id" + 1L, List.of("100101", "100103")),
            ModelFactory.outbound(2L, "ya-id" + 2L, "ext-id" + 2L, List.of("100104"))
        ));

        apiOperation(request).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Nonnull
    private OutboundApi.CreateOutboundsOper apiOperation(OutboundsCreateRequest request) {
        return apiClient.outbound().createOutbounds().body(request);
    }
}
