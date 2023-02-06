package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.converter.lms.PartnerExternalParamLmsConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.embedded.Address;
import ru.yandex.market.logistics.lom.entity.embedded.Recipient;
import ru.yandex.market.logistics.lom.entity.enums.DeliveryType;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.PostalCodeValidator;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@DisplayName("Валидация наличия почтового индекса")
@ParametersAreNonnullByDefault
class PostalCodeValidatorTest extends AbstractTest {
    private static final long PARTNER_ID = 10L;
    private final PostalCodeValidator postalCodeValidator =
        new PostalCodeValidator(new PartnerExternalParamLmsConverter());

    @DisplayName("Валидация наличия почтового индекса")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    void validate(String caseName, Order order, ValidateAndEnrichContext context, Boolean validationPassed) {
        ValidateAndEnrichResults result = postalCodeValidator.validateAndEnrich(order, context);
        softly.assertThat(result.isValidationPassed()).isEqualTo(validationPassed);
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
            Quadruple.of(
                "Тип доставки не POST",
                new Order().setDeliveryType(DeliveryType.PICKUP),
                createContextWithNoFlag(),
                true
            ),
            Quadruple.of(
                "У партнера есть флаг и проверка проходит",
                createOrder("630090"),
                createContextWithFlag(),
                true
            ),
            Quadruple.of(
                "У партнера есть флаг, но проверка не проходит",
                createOrder(null),
                createContextWithFlag(),
                false
            ),
            Quadruple.of(
                "У партнера нет флага, проверка проходит независимо от наличия почтового индекса",
                createOrder(null),
                createContextWithNoFlag(),
                true
            ),
            Quadruple.of(
                "У партнера нет флага, проверка проходит независимо от наличия почтового индекса",
                createOrder("630090"),
                createContextWithNoFlag(),
                true
            )
        )
            .map(q -> Arguments.of(q.getFirst(), q.getSecond(), q.getThird(), q.getFourth()));
    }

    @Nonnull
    private static Order createOrder(@Nullable String postalCode) {
        return new Order()
            .setDeliveryType(DeliveryType.POST)
            .setId(1L)
            .setRecipient(new Recipient().setAddress(new Address().setZipCode(postalCode)));
    }

    @Nonnull
    private static ValidateAndEnrichContext createContextWithNoFlag() {
        return new ValidateAndEnrichContext()
            .setPartners(List.of(
                PartnerResponse.newBuilder().id(PARTNER_ID).partnerType(PartnerType.DELIVERY).build()
            ));
    }

    @Nonnull
    private static ValidateAndEnrichContext createContextWithFlag() {
        return new ValidateAndEnrichContext()
            .setPartners(
                List.of(
                    PartnerResponse.newBuilder()
                        .id(PARTNER_ID)
                        .partnerType(PartnerType.DELIVERY)
                        .params(List.of(new PartnerExternalParam(
                            "POSTAL_CODE_NEEDED",
                            "Postal code must be present",
                            "1"
                        )))
                        .build())
            );
    }
}
