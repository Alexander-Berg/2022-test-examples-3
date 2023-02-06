package ru.yandex.market.delivery.transport_manager.facade.register;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.ff.client.dto.RegistryUnitDTO;
import ru.yandex.market.ff.client.enums.RegistryUnitType;

class RegisterUnitFetcherFacadeBarcodeFieldTest {
    @ParameterizedTest
    @MethodSource("getBarcodeFieldTestCases")
    void barcodeField(RegistryUnitType unitType, IdType[] idType, @Nullable TransportationType transportationType) {
        RegistryUnitDTO registryUnitDTO = new RegistryUnitDTO();
        registryUnitDTO.setType(unitType);
        Assertions.assertArrayEquals(
            idType,
            RegisterUnitFetcherFacade.barcodeField(registryUnitDTO, transportationType)
        );
    }

    public static Stream<Arguments> getBarcodeFieldTestCases() {
        return Stream.of(
            Arguments.of(
                RegistryUnitType.PALLET,
                new IdType[]{IdType.ORDER_ID, IdType.PALLET_ID},
                TransportationType.ORDERS_OPERATION
            ),
            Arguments.of(
                RegistryUnitType.BOX,
                new IdType[]{IdType.ORDER_ID, IdType.BOX_ID},
                TransportationType.ORDERS_OPERATION
            ),
            Arguments.of(
                RegistryUnitType.BAG,
                new IdType[]{IdType.ORDER_ID, IdType.BAG_ID},
                TransportationType.ORDERS_OPERATION
            ),
            Arguments.of(
                RegistryUnitType.ITEM,
                new IdType[]{IdType.ORDER_ID},
                TransportationType.ORDERS_OPERATION
            ),
            Arguments.of(
                RegistryUnitType.ASSORTMENT,
                new IdType[]{IdType.ORDER_ID},
                TransportationType.ORDERS_OPERATION
            ),
            Arguments.of(
                RegistryUnitType.PALLET,
                new IdType[]{IdType.ORDER_ID, IdType.PALLET_ID},
                null
            ),
            Arguments.of(
                RegistryUnitType.BOX,
                new IdType[]{IdType.ORDER_ID, IdType.BOX_ID},
                null
            ),
            Arguments.of(
                RegistryUnitType.BAG,
                new IdType[]{IdType.ORDER_ID, IdType.BAG_ID},
                null
            ),
            Arguments.of(
                RegistryUnitType.ITEM,
                new IdType[]{IdType.ORDER_ID},
                null
            ),
            Arguments.of(
                RegistryUnitType.ASSORTMENT,
                new IdType[]{IdType.ORDER_ID},
                null
            ),
            Arguments.of(
                RegistryUnitType.PALLET,
                new IdType[]{IdType.PALLET_ID, IdType.ORDER_ID},
                TransportationType.INTERWAREHOUSE
            ),
            Arguments.of(
                RegistryUnitType.BOX,
                new IdType[]{IdType.BOX_ID, IdType.ORDER_ID},
                TransportationType.INTERWAREHOUSE
            ),
            Arguments.of(
                RegistryUnitType.BAG,
                new IdType[]{IdType.BAG_ID, IdType.ORDER_ID},
                TransportationType.INTERWAREHOUSE
            ),
            Arguments.of(
                RegistryUnitType.ITEM,
                new IdType[]{IdType.ORDER_ID},
                TransportationType.INTERWAREHOUSE
            ),
            Arguments.of(
                RegistryUnitType.ASSORTMENT,
                new IdType[]{IdType.ORDER_ID},
                TransportationType.INTERWAREHOUSE
            )
        );
    }
}
