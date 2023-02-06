package ru.yandex.market.core.delivery;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link LmsPartnerServiceInfoConverter}.
 */
class LmsPartnerServiceInfoConverterTest {

    private static final String SITE_URL = "https://partner.ru";
    private static final String LOGO_URL = SITE_URL + "/logo.png";
    private static final long BALANCE_CLIENT_ID = 5;
    private static final int RATING = 2;

    @Test
    void testConvertType() {
        final Set<DeliveryServiceType> allTypes = Set.of(DeliveryServiceType.values());
        final Set<DeliveryServiceType> matchedTypes = Arrays.stream(PartnerType.values())
                .map(partnerType -> {
                    try {
                        return LmsPartnerServiceInfoConverter.INSTANCE.convertType(partnerType);
                    } catch (final Exception ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Collection<DeliveryServiceType> missedTypes = CollectionUtils.subtract(allTypes, matchedTypes);
        assertThat(
                "Found delivery service types which are not matched for partner types: " + missedTypes,
                missedTypes, empty()
        );
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("testConvertData")
    void testConvert(PartnerStatus partnerStatus, DeliveryServiceMarketStatus marketStatus) {
         var partnerResponse =
                random(BusinessWarehouseResponse.Builder.class)
                        .partnerStatus(partnerStatus)
                        .partnerType(PartnerType.FULFILLMENT)
                        .domain(SITE_URL)
                        .logoUrl(LOGO_URL)
                        .billingClientId(BALANCE_CLIENT_ID)
                        .rating(RATING)
                        .build();

        final DeliveryServiceInfo deliveryServiceInfo =
                LmsPartnerServiceInfoConverter.INSTANCE.convert(partnerResponse);

        checkObjectFields(deliveryServiceInfo, partnerResponse, marketStatus);
        checkExplicitFields(deliveryServiceInfo);
    }

    @Test
    void testExternalParams() {
        var partnerResponse =
                random(BusinessWarehouseResponse.Builder.class)
                        .partnerParams(List.of(
                                new PartnerExternalParam(PartnerExternalParamType.DROPSHIP_EXPRESS.name(), "", "true"),
                                new PartnerExternalParam(PartnerExternalParamType.MARKET_PICKUP_AVAILABLE.name(),
                                               "",
                                                   "true"),
                                new PartnerExternalParam(PartnerExternalParamType.IS_COMMON.name(), "", "true")
                        ))
                        .partnerType(PartnerType.DELIVERY) // Не критично какой тип, проверяем параметры
                        .rating(RATING)
                        .billingClientId(BALANCE_CLIENT_ID)
                        .build();

        final DeliveryServiceInfo deliveryServiceInfo =
                LmsPartnerServiceInfoConverter.INSTANCE.convert(partnerResponse);

        assertTrue(deliveryServiceInfo.isPickupAvailable());
        assertTrue(deliveryServiceInfo.isExpress());
        assertTrue(deliveryServiceInfo.isCommon());
    }

    static Stream<Arguments> testConvertData() {
        return Stream.of(
                Arguments.of(PartnerStatus.ACTIVE, DeliveryServiceMarketStatus.ON),
                Arguments.of(PartnerStatus.TESTING, DeliveryServiceMarketStatus.ON),
                Arguments.of(PartnerStatus.INACTIVE, DeliveryServiceMarketStatus.OFF),
                Arguments.of(PartnerStatus.FROZEN, DeliveryServiceMarketStatus.OFF)
        );
    }

    private static void checkObjectFields(
            DeliveryServiceInfo deliveryServiceInfo,
            BusinessWarehouseResponse partnerResponse,
            DeliveryServiceMarketStatus marketStatus
    ) {
        assertEquals(
                partnerResponse.getPartnerId(),
                deliveryServiceInfo.getId().longValue(),
                "Id was not converted correctly");
        assertEquals(
                partnerResponse.getName(),
                deliveryServiceInfo.getHumanReadableId(),
                "Name was not converted correctly"
        );
        assertEquals(
                partnerResponse.getLogisticsPointName(),
                deliveryServiceInfo.getName(),
                "Human readable name was not passed correctly"
        );
        assertEquals(
                marketStatus,
                deliveryServiceInfo.getMarketStatus(),
                "Status was not converted correctly"
        );

        assertEquals(
                partnerResponse.getLogoUrl(),
                deliveryServiceInfo.getLogo(),
                "Delivery service logo was not matched correctly"
        );
        assertEquals(
                partnerResponse.getDomain(),
                deliveryServiceInfo.getUrl(),
                "Delivery service URL was not matched correctly"
        );
        assertEquals(
                partnerResponse.getRating(),
                deliveryServiceInfo.getRating(),
                "Delivery service rating was not matched correctly"
        );
        assertEquals(
                partnerResponse.getBillingClientId(),
                deliveryServiceInfo.getBalanceClientId(),
                "Delivery service balance client id was not passed correctly"
        );
    }

    private static void checkExplicitFields(DeliveryServiceInfo deliveryServiceInfo) {
        assertEquals(
                SITE_URL,
                deliveryServiceInfo.getUrl(),
                "Delivery service URL was not passed correctly"
        );
        assertEquals(
                LOGO_URL,
                deliveryServiceInfo.getLogo(),
                "Delivery service logo was not passed correctly"
        );
        assertEquals(
                RATING,
                deliveryServiceInfo.getRating(),
                "Delivery service rating was not passed correctly"
        );
        assertEquals(
                BALANCE_CLIENT_ID,
                deliveryServiceInfo.getBalanceClientId(),
                "Delivery service balance client id was not passed correctly"
        );
        assertEquals(
                DeliveryServiceType.FULFILLMENT,
                deliveryServiceInfo.getType(),
                "Delivery service type was not passed correctly"
        );
    }

}
