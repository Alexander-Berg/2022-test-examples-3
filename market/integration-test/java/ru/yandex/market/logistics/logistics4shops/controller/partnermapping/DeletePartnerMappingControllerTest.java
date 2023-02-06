package ru.yandex.market.logistics.logistics4shops.controller.partnermapping;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.PartnerMappingApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.DeletePartnerMappingRequest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationError;

import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Тест удаления соответствий партнеров в lms и mbi")
@ParametersAreNonnullByDefault
class DeletePartnerMappingControllerTest extends AbstractIntegrationTest {
    private static final long MBI_PARTNER_ID = 123L;
    private static final long LMS_PARTNER_ID = 456L;

    @Test
    @DisplayName("Успешное удаление соответствия")
    @DatabaseSetup("/controller/partnermapping/new_mapping.xml")
    @ExpectedDatabase(
        value = "/controller/partnermapping/after/empty_mappings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteSuccess() {
        apiDeleteMapping(buildRequest(MBI_PARTNER_ID, LMS_PARTNER_ID))
            .execute(validatedWith(shouldBeCode(HttpStatus.OK.value())));
    }

    @Test
    @DisplayName("Удаление несуществующего соответствия")
    @ExpectedDatabase(
        value = "/controller/partnermapping/after/empty_mappings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteNonPresent() {
        apiDeleteMapping(buildRequest(MBI_PARTNER_ID, LMS_PARTNER_ID))
            .execute(validatedWith(shouldBeCode(HttpStatus.OK.value())));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Вернуть BAD_REQUEST, если в реквесте удаления отстутствует одно из полей")
    void failOnEmptyField(@SuppressWarnings("unused") String name, DeletePartnerMappingRequest request, String field) {
        ValidationError error = apiDeleteMapping(request)
            .execute(validatedWith(shouldBeCode(HttpStatus.BAD_REQUEST.value())))
            .as(ValidationError.class);
        softly.assertThat(error.getErrors().get(0).getField()).isEqualTo(field);
        softly.assertThat(error.getErrors().get(0).getMessage()).isEqualTo("must not be null");
    }

    @Nonnull
    private static Stream<Arguments> failOnEmptyField() {
        return Stream.of(
            Arguments.of(
                "Пустой mbi_partner_id",
                buildRequest(null, LMS_PARTNER_ID),
                "mbiPartnerId"
            ),
            Arguments.of(
                "Пустой lms_partner_id",
                buildRequest(MBI_PARTNER_ID, null),
                "lmsPartnerId"
            )
        );
    }

    @Nonnull
    private PartnerMappingApi.DeletePartnerMappingOper apiDeleteMapping(DeletePartnerMappingRequest request) {
        return apiClient.partnerMapping().deletePartnerMapping().body(request);
    }

    @Nonnull
    private static DeletePartnerMappingRequest buildRequest(
        @Nullable Long mbiPartnerId,
        @Nullable Long lmsPartnerId
    ) {
        return new DeletePartnerMappingRequest().mbiPartnerId(mbiPartnerId).lmsPartnerId(lmsPartnerId);
    }
}
