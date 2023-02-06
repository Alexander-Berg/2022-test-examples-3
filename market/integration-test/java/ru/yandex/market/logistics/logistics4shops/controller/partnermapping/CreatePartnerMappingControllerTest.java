package ru.yandex.market.logistics.logistics4shops.controller.partnermapping;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.PartnerMappingApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.CreatePartnerMappingRequest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.PartnerType;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationError;
import ru.yandex.market.logistics.logistics4shops.utils.ModelFactory;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Тест создания соответствий партнеров в lms и mbi")
@ParametersAreNonnullByDefault
class CreatePartnerMappingControllerTest extends AbstractIntegrationTest {
    private static final long MBI_PARTNER_ID = 123L;
    private static final long LMS_PARTNER_ID = 456L;

    @Test
    @DisplayName("Успешное создание соответствия")
    @ExpectedDatabase(
        value = "/controller/partnermapping/new_mapping.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createSuccess() {
        var response = apiCreateMapping(buildRequest(MBI_PARTNER_ID, LMS_PARTNER_ID, PartnerType.DROPSHIP))
            .executeAs(validatedWith(shouldBeCode(HttpStatus.OK.value())));
        softly.assertThat(response)
            .usingRecursiveComparison()
            .isEqualTo(ModelFactory.dropshipPartnerMappingDto(MBI_PARTNER_ID, LMS_PARTNER_ID));
    }

    @Test
    @DisplayName("Соответствие уже существует, типы партнера в старом и новом совпадают")
    @DatabaseSetup("/controller/partnermapping/new_mapping.xml")
    @ExpectedDatabase(
        value = "/controller/partnermapping/new_mapping.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void returnOnDuplicate() {
        var response = apiCreateMapping(buildRequest(MBI_PARTNER_ID, LMS_PARTNER_ID, PartnerType.DROPSHIP))
            .executeAs(validatedWith(shouldBeCode(HttpStatus.ALREADY_REPORTED.value())));
        softly.assertThat(response)
            .usingRecursiveComparison()
            .isEqualTo(ModelFactory.dropshipPartnerMappingDto(MBI_PARTNER_ID, LMS_PARTNER_ID));
    }

    @Test
    @DisplayName("Соответствие уже существует, типы партнера в старом и новом не совпадают")
    @DatabaseSetup("/controller/partnermapping/new_mapping.xml")
    @ExpectedDatabase(
        value = "/controller/partnermapping/new_mapping.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void thrownOnDuplicate() {
        ValidationError error = apiCreateMapping(
            buildRequest(MBI_PARTNER_ID, LMS_PARTNER_ID, PartnerType.DROPSHIP_BY_SELLER)
        )
            .execute(validatedWith(shouldBeCode(HttpStatus.BAD_REQUEST.value())))
            .as(ValidationError.class);
        softly.assertThat(error.getMessage())
            .isEqualTo("Requested partner type DROPSHIP_BY_SELLER is different from old partner type DROPSHIP");
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ошибка: вернуть BAD_REQUEST, если в реквесте создания отстутствует одно из полей")
    void failOnEmptyField(String name, CreatePartnerMappingRequest request, String field) {
        ValidationError error = apiCreateMapping(request)
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
                buildRequest(null, LMS_PARTNER_ID, PartnerType.DROPSHIP),
                "mbiPartnerId"
            ),
            Arguments.of(
                "Пустой lms_partner_id",
                buildRequest(MBI_PARTNER_ID, null, PartnerType.DROPSHIP),
                "lmsPartnerId"
            ),
            Arguments.of(
                "Пустой тип партнера",
                buildRequest(MBI_PARTNER_ID, LMS_PARTNER_ID, null),
                "partnerType"
            )
        );
    }

    @Nonnull
    private PartnerMappingApi.CreatePartnerMappingOper apiCreateMapping(CreatePartnerMappingRequest request) {
        return apiClient.partnerMapping().createPartnerMapping().body(request);
    }

    @Nonnull
    private static CreatePartnerMappingRequest buildRequest(
        @Nullable Long mbiPartnerId,
        @Nullable Long lmsPartnerId,
        @Nullable PartnerType partnerType
    ) {
        return new CreatePartnerMappingRequest()
            .mbiPartnerId(mbiPartnerId)
            .lmsPartnerId(lmsPartnerId)
            .partnerType(partnerType);
    }
}
