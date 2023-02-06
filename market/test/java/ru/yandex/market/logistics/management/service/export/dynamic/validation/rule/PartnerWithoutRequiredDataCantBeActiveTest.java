package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.domain.entity.Address;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;

@ParametersAreNonnullByDefault
@DisplayName("Партнёр без marketId не может быть в статусе ACTIVE")
class PartnerWithoutRequiredDataCantBeActiveTest extends AbstractValidationRuleTest<Partner> {

    @Nonnull
    @Override
    ValidationRule getRule() {
        return new PartnerWithoutRequiredDataCantBeActive();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("partnerArguments")
    @DisplayName("Проверка валидации статуса партнёра")
    void testValidationRule(String displayName, Partner partner, ValidationStatus status, @Nullable String error) {
        assertValidationResult(partner, status, error);
    }

    @Nonnull
    public static Stream<? extends Arguments> partnerArguments() {
        return Stream.of(
            Arguments.of(
                "У партнёра проставлен marketId, статус ACTIVE",
                createPartner(345L, PartnerStatus.ACTIVE),
                ValidationStatus.OK,
                null
            ),
            Arguments.of(
                "У партнёра не проставлен marketId, статус не ACTIVE",
                createPartner(null, PartnerStatus.INACTIVE),
                ValidationStatus.OK,
                null
            ),
            Arguments.of(
                "У партнёра нет marketId, статус ACTIVE, валидация срабатывает",
                createPartner(null, PartnerStatus.ACTIVE),
                ValidationStatus.FAILED,
                "Partner can't be ACTIVE with empty fields [marketId], partnerId 123"
            ),
            Arguments.of(
                "У партнёра нет marketId, статус ACTIVE, валидация срабатывает",
                createPartner(null, PartnerStatus.ACTIVE),
                ValidationStatus.FAILED,
                "Partner can't be ACTIVE with empty fields [marketId], partnerId 123"
            ),
            Arguments.of(
                "У партнёра есть marketId, дропшип, нет адреса, статус ACTIVE, валидация срабатывает",
                createDropship(12345L, PartnerStatus.ACTIVE),
                ValidationStatus.FAILED,
                "Partner can't be ACTIVE with empty fields [street, house, postCode], partnerId 123"
            ),
            Arguments.of(
                "У партнёра есть marketId, дропшип, нет индекса, статус ACTIVE, валидация срабатывает",
                createDropship(12345L, PartnerStatus.ACTIVE, "Иванова", "27к1стр2", null),
                ValidationStatus.FAILED,
                "Partner can't be ACTIVE with empty fields [postCode], partnerId 123"
            ),
            Arguments.of(
                "У партнёра есть marketId, дропшип, нет номера дома, статус ACTIVE, валидация срабатывает",
                createDropship(12345L, PartnerStatus.ACTIVE, "Иванова", null, "656038"),
                ValidationStatus.FAILED,
                "Partner can't be ACTIVE with empty fields [house], partnerId 123"
            ),
            Arguments.of(
                "У партнёра есть marketId, дропшип, нет улицы, статус ACTIVE, валидация срабатывает",
                createDropship(12345L, PartnerStatus.ACTIVE, null, "27к1стр2", "656038"),
                ValidationStatus.FAILED,
                "Partner can't be ACTIVE with empty fields [street], partnerId 123"
            ),
            Arguments.of(
                "У партнёра нет marketId, дропшип, нет нужных полей адреса, статус ACTIVE, валидация срабатывает",
                createDropship(null, PartnerStatus.ACTIVE, null, null, null),
                ValidationStatus.FAILED,
                "Partner can't be ACTIVE with empty fields [marketId, street, house, postCode], partnerId 123"
            ),
            Arguments.of(
                "У партнёра нет marketId, дропшип, нет дома, статус ACTIVE, валидация срабатывает",
                createDropship(null, PartnerStatus.ACTIVE, "Иванова", null, "656038"),
                ValidationStatus.FAILED,
                "Partner can't be ACTIVE with empty fields [marketId, house], partnerId 123"
            ),
            Arguments.of(
                "У партнёра нет marketId, дропшип, нет дома, статус INACTIVE, валидация не срабатывает",
                createDropship(null, PartnerStatus.INACTIVE, "Иванова", null, "656038"),
                ValidationStatus.OK,
                null
            ),
            Arguments.of(
                "У партнёра есть marketId, дропшип, нет адреса, статус INACTIVE, валидация не срабатывает",
                createDropship(12345L, PartnerStatus.INACTIVE),
                ValidationStatus.OK,
                null
            )
        );
    }

    @Nonnull
    private static Partner createPartner(@Nullable Long marketId, PartnerStatus status) {
        return new Partner()
            .setId(123L)
            .setMarketId(marketId)
            .setStatus(status);
    }

    @Nonnull
    private static Partner createDropship(@Nullable Long marketId, PartnerStatus status) {
        return createPartner(marketId, status)
            .setPartnerType(PartnerType.DROPSHIP);
    }

    @Nonnull
    private static Partner createDropship(
        @Nullable Long marketId,
        PartnerStatus status,
        @Nullable String street,
        @Nullable String house,
        @Nullable String postCode
    ) {
        return createDropship(marketId, status)
            .setLogisticsPoints(
                Set.of(
                    new LogisticsPoint()
                        .setId(1L)
                        .setAddress(
                            new Address()
                                .setStreet(street)
                                .setHouse(house)
                                .setPostCode(postCode)
                        )
                        .setActive(true)
                        .setType(PointType.WAREHOUSE),
                    new LogisticsPoint()
                        .setId(2L)
                        .setAddress(
                            new Address()
                                .setStreet("street")
                                .setHouse("house")
                                .setPostCode("postCode")
                        )
                        .setActive(false)
                        .setType(PointType.WAREHOUSE),
                    new LogisticsPoint()
                        .setId(3L)
                        .setAddress(
                            new Address()
                                .setStreet("street")
                                .setHouse("house")
                                .setPostCode("postCode")
                        )
                        .setActive(true)
                        .setType(PointType.PICKUP_POINT)
                )
            );
    }
}
