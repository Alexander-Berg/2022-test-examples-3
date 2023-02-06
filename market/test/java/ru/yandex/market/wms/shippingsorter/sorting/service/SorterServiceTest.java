package ru.yandex.market.wms.shippingsorter.sorting.service;

import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.common.model.dto.TransportUnitId;
import ru.yandex.market.wms.common.model.dto.TransportUnitLocation;
import ru.yandex.market.wms.common.model.dto.TransportUnitTrackingDTO;
import ru.yandex.market.wms.common.model.enums.TransportUnitStatus;
import ru.yandex.market.wms.common.spring.servicebus.vendor.VendorProvider;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.entity.Sorter;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterServiceTest extends IntegrationTest {

    @Autowired
    private SorterService sorterService;

    @ParameterizedTest
    @MethodSource("findSorterByCurrentLocationTestArgs")
    @DatabaseSetup("/sorting/service/sorter/before.xml")
    @ExpectedDatabase(value = "/sorting/service/sorter/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void findSorterByCurrentLocationTest(String sorterExit, Sorter expectedSorter) {
        Sorter sorter = sorterService.findActiveSorterByConveyorLoc(sorterExit);

        Assertions.assertEquals(expectedSorter, sorter);
    }

    @ParameterizedTest
    @CsvSource({"SR3_CH-20,No active sorter found for location SR3_CH-20", "SR4_CH-07,No active sorter found for " +
            "location SR4_CH-07"})
    @DatabaseSetup("/sorting/service/sorter/before.xml")
    @ExpectedDatabase(value = "/sorting/service/sorter/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void findSorterByCurrentLocationTestThrowRuntimeException(String sorterExit, String expectedMessage) {
        RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class,
                () -> sorterService.findActiveSorterByConveyorLoc(sorterExit));

        Assertions.assertEquals(expectedMessage, runtimeException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("isCurrentLocationCheckpointTestArgs")
    @DatabaseSetup("/sorting/service/sorter/before.xml")
    @ExpectedDatabase(value = "/sorting/service/sorter/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void isCurrentLocationCheckpointTest(TransportUnitTrackingDTO dto, boolean expected) {
        boolean isCurrentLocationCheckpoint = sorterService.isCurrentLocationCheckpoint(dto);

        Assertions.assertEquals(expected, isCurrentLocationCheckpoint);
    }

    private static Stream<Arguments> findSorterByCurrentLocationTestArgs() {
        Sorter expectedSorter1 = Sorter.builder()
                .sorterId("CONS-1")
                .enabled(true)
                .sorterZone("SSORT_ZONE")
                .externalSorterZone("SHIPPING1")
                .locPattern("SR1.*")
                .automation("SCHAEFER")
                .build();

        Sorter expectedSorter2 = Sorter.builder()
                .sorterId("CONS-2")
                .enabled(true)
                .sorterZone("SSORT_ZN_2")
                .externalSorterZone("SHIPPING2")
                .locPattern("SR2.*")
                .automation("SCHAEFER")
                .build();

        return Stream.of(
                Arguments.of("SR1_CH-07", expectedSorter1),
                Arguments.of("SR2_CH-44", expectedSorter2)
        );
    }

    private static Stream<Arguments> isCurrentLocationCheckpointTestArgs() {
        TransportUnitTrackingDTO dto1 = TransportUnitTrackingDTO.builder()
                .externalZoneName("SHIPPING1")
                .vendorProvider(VendorProvider.SCHAEFER)
                .status(TransportUnitStatus.NOTIFICATION)
                .currentLocation(TransportUnitLocation.of("SR1_TP-01"))
                .transportUnitId(TransportUnitId.of("P208839291"))
                .build();

        TransportUnitTrackingDTO dto2 = TransportUnitTrackingDTO.builder()
                .externalZoneName("SHIPPING1")
                .vendorProvider(VendorProvider.SCHAEFER)
                .status(TransportUnitStatus.NOTIFICATION)
                .currentLocation(TransportUnitLocation.of("SR2_TP-01"))
                .transportUnitId(TransportUnitId.of("P208839291"))
                .build();

        TransportUnitTrackingDTO dto3 = TransportUnitTrackingDTO.builder()
                .externalZoneName("SHIPPING1")
                .vendorProvider(VendorProvider.SCHAEFER)
                .status(TransportUnitStatus.NOTIFICATION)
                .transportUnitId(TransportUnitId.of("P208839291"))
                .build();

        TransportUnitTrackingDTO dto4 = TransportUnitTrackingDTO.builder()
                .externalZoneName("SHIPPING3")
                .vendorProvider(VendorProvider.SCHAEFER)
                .status(TransportUnitStatus.NOTIFICATION)
                .transportUnitId(TransportUnitId.of("P208839291"))
                .build();

        return Stream.of(
                Arguments.of(dto1, true),
                Arguments.of(dto2, false),
                Arguments.of(dto3, false),
                Arguments.of(dto4, false)
        );
    }
}
