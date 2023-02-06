package ru.yandex.market.delivery.transport_manager.facade.routing;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationAdditionalData;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationRoutingConfig;
import ru.yandex.market.delivery.transport_manager.domain.enums.DimensionsClass;

class GetRoutingRequestFacadeTest {
    private GetRoutingRequestFacade facade;
    private static final Transportation TRANSPORTATION_WITH_ROUTING_CONFIG = new Transportation();
    private static final Transportation TRANSPORTATION_WITH_NULL_ROUTING_CONFIG = new Transportation();
    private static final Transportation TRANSPORTATION_WITH_NULL_ADDITIONAL_DATA = new Transportation();

    @BeforeAll
    static void beforeAll() {
        TransportationRoutingConfig routingConfig = new TransportationRoutingConfig(
            true,
            DimensionsClass.MEDIUM_SIZE_CARGO,
            1.1D,
            false,
            "DEFAULT"
        );
        TransportationAdditionalData additionalData = new TransportationAdditionalData(routingConfig);
        TRANSPORTATION_WITH_ROUTING_CONFIG.setAdditionalData(additionalData);
        TRANSPORTATION_WITH_NULL_ROUTING_CONFIG.setAdditionalData(
            new TransportationAdditionalData(null)
        );
    }

    @BeforeEach
    void setUp() {
        facade = new GetRoutingRequestFacade(null, null, null, null);
    }

    @MethodSource("getRoutingConfigFieldTestParams")
    @ParameterizedTest
    void getRoutingConfigField(
        Transportation transportation,
        boolean enabled,
        double expectedVolume,
        String locationGroupTag,
        boolean excludeFromLocationGroup
    ) {
        Assertions.assertEquals(
            facade.getRoutingConfigField(transportation, TransportationRoutingConfig::isEnabled, false),
            enabled
        );
        Assertions.assertEquals(
            facade.getRoutingConfigField(
                transportation,
                TransportationRoutingConfig::getExpectedVolume,
                0D
            ),
            expectedVolume
        );
        Assertions.assertEquals(
            facade.getRoutingConfigField(
                transportation,
                TransportationRoutingConfig::getLocationGroupTag,
                null
            ),
            locationGroupTag
        );
        Assertions.assertEquals(
            facade.getRoutingConfigField(
                transportation,
                TransportationRoutingConfig::isExcludeFromLocationGroup,
                false
            ),
            excludeFromLocationGroup
        );
    }

    static Stream<Arguments> getRoutingConfigFieldTestParams() {
        return Stream.of(
            Arguments.of(TRANSPORTATION_WITH_ROUTING_CONFIG, true, 1.1D, "DEFAULT", false),
            Arguments.of(TRANSPORTATION_WITH_NULL_ROUTING_CONFIG, false, 0D, null, false),
            Arguments.of(TRANSPORTATION_WITH_NULL_ADDITIONAL_DATA, false, 0D, null, false)
        );
    }

    @Test
    void getDimensionsClass() {
        Assertions.assertEquals(
            ru.yandex.market.tpl.core.external.routing.api.DimensionsClass.MEDIUM_SIZE_CARGO,
            facade.getDimensionsClass(TRANSPORTATION_WITH_ROUTING_CONFIG)
        );
    }

    @Test
    void getDimensionsClassNullConfig() {
        Assertions.assertEquals(
            ru.yandex.market.tpl.core.external.routing.api.DimensionsClass.REGULAR_CARGO,
            facade.getDimensionsClass(TRANSPORTATION_WITH_NULL_ROUTING_CONFIG)
        );
    }

    @Test
    void getDimensionsClassNullAdditionalData() {
        Assertions.assertEquals(
            ru.yandex.market.tpl.core.external.routing.api.DimensionsClass.REGULAR_CARGO,
            facade.getDimensionsClass(TRANSPORTATION_WITH_NULL_ADDITIONAL_DATA)
        );
    }
}
