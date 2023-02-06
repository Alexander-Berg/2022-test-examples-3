package ru.yandex.market.logistics.lrm.api.returns;

import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi.CancelReturnsOper;
import ru.yandex.market.logistics.lrm.client.model.CancelReturnsRequest;
import ru.yandex.market.logistics.lrm.client.model.CancelReturnsResponse;
import ru.yandex.market.logistics.lrm.client.model.NotFoundError;
import ru.yandex.market.logistics.lrm.client.model.ResourceType;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Отмена возврата")
class ReturnCancelTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/database/api/returns/cancel/before/setup.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/cancel/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        CancelReturnsResponse response = cancelReturns(new CancelReturnsRequest().returnIds(Set.of(1L)))
            .execute(validatedWith(shouldBeCode(SC_OK)))
            .as(CancelReturnsResponse.class);
        softly.assertThat(response.getReturnIds()).containsExactly(1L);
        softly.assertThat(response.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Успех, несколько возвратов")
    @DatabaseSetup("/database/api/returns/cancel/before/setup.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/cancel/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successSeveralReturns() {
        CancelReturnsResponse response = cancelReturns(new CancelReturnsRequest().returnIds(Set.of(1L, 2L, 3L)))
            .execute(validatedWith(shouldBeCode(SC_OK)))
            .as(CancelReturnsResponse.class);
        softly.assertThat(response.getReturnIds()).containsExactly(1L);
        softly.assertThat(response.getErrors())
            .extracting(ValidationViolation::getMessage)
            .containsExactly(
                "Cannot cancel return with id 2 in status IN_TRANSIT",
                "Failed to find RETURN with ids [3]"
            );
    }

    @Test
    @DisplayName("Возвраты не найдены")
    void notFound() {
        NotFoundError error = cancelReturns(new CancelReturnsRequest().returnIds(Set.of(1L, 2L)))
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error.getIds()).containsExactly(1L, 2L);
        softly.assertThat(error.getResourceType()).isEqualTo(ResourceType.RETURN);
        softly.assertThat(error.getMessage()).isNotNull();
    }

    @Test
    @DisplayName("Возврат в пути")
    @DatabaseSetup("/database/api/returns/cancel/before/setup_in_transit.xml")
    void inTransit() {
        CancelReturnsResponse response = cancelReturns(new CancelReturnsRequest().returnIds(Set.of(1L)))
            .execute(validatedWith(shouldBeCode(SC_OK)))
            .as(CancelReturnsResponse.class);

        softly.assertThat(response.getReturnIds()).isEmpty();
        softly.assertThat(response.getErrors())
            .extracting(ValidationViolation::getMessage)
            .containsExactly("Cannot cancel return with id 1 in status IN_TRANSIT");
    }

    @Nonnull
    private CancelReturnsOper cancelReturns(CancelReturnsRequest cancelReturnsRequest) {
        return apiClient.returns().cancelReturns().body(cancelReturnsRequest);
    }
}
