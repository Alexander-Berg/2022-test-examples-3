package ru.yandex.market.wms.shippingsorter.sorting.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.common.model.dto.TransportUnitId;
import ru.yandex.market.wms.common.model.dto.TransportUnitLocation;
import ru.yandex.market.wms.common.model.dto.TransportUnitTrackingDTO;
import ru.yandex.market.wms.common.model.enums.TransportUnitStatus;
import ru.yandex.market.wms.common.spring.servicebus.vendor.VendorProvider;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class TransportUnitTrackingServiceTest extends IntegrationTest {

    @Autowired
    private TransportUnitTrackingService transportUnitTrackingService;

    @Test
    @DatabaseSetup("/sorting/service/transport-unit-tracking/before.xml")
    void fillExternalZoneForSecondLevelShippingsorter() {
        TransportUnitTrackingDTO unitTrackingDTO = TransportUnitTrackingDTO.builder()
                .currentLocation(TransportUnitLocation.of("SR2_CH-42"))
                .transportUnitId(TransportUnitId.builder().id("P00001").build())
                .status(TransportUnitStatus.FINISHED)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        TransportUnitTrackingDTO expectedUnitTrackingDTO = TransportUnitTrackingDTO.builder()
                .currentLocation(TransportUnitLocation.of("SR2_CH-42"))
                .transportUnitId(TransportUnitId.builder().id("P00001").build())
                .status(TransportUnitStatus.FINISHED)
                .vendorProvider(VendorProvider.SCHAEFER)
                .externalZoneName("SHIPPING2")
                .build();

        Assertions.assertNull(unitTrackingDTO.getExternalZoneName());

        transportUnitTrackingService.fillExternalZone(unitTrackingDTO);

        Assertions.assertEquals(expectedUnitTrackingDTO, unitTrackingDTO);
    }

    @Test
    @DatabaseSetup("/sorting/service/transport-unit-tracking/before.xml")
    void fillExternalZoneForSchaefer() {
        TransportUnitTrackingDTO unitTrackingDTO = TransportUnitTrackingDTO.builder()
                .currentLocation(TransportUnitLocation.of("SR1_CH-04"))
                .transportUnitId(TransportUnitId.builder().id("P00001").build())
                .status(TransportUnitStatus.FINISHED)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        TransportUnitTrackingDTO expectedUnitTrackingDTO = TransportUnitTrackingDTO.builder()
                .currentLocation(TransportUnitLocation.of("SR1_CH-04"))
                .transportUnitId(TransportUnitId.builder().id("P00001").build())
                .status(TransportUnitStatus.FINISHED)
                .vendorProvider(VendorProvider.SCHAEFER)
                .externalZoneName("SHIPPING1")
                .build();

        Assertions.assertNull(unitTrackingDTO.getExternalZoneName());

        transportUnitTrackingService.fillExternalZone(unitTrackingDTO);

        Assertions.assertEquals(expectedUnitTrackingDTO, unitTrackingDTO);
    }

    @Test
    @DatabaseSetup("/sorting/service/transport-unit-tracking/before.xml")
    void fillExternalZoneAlreadyFilled() {
        TransportUnitTrackingDTO unitTrackingDTO = TransportUnitTrackingDTO.builder()
                .currentLocation(TransportUnitLocation.of("SR1_CH-04"))
                .transportUnitId(TransportUnitId.builder().id("P00001").build())
                .status(TransportUnitStatus.FINISHED)
                .vendorProvider(VendorProvider.SCHAEFER)
                .externalZoneName("SHIPPING_FILLED")
                .build();

        TransportUnitTrackingDTO expectedUnitTrackingDTO = TransportUnitTrackingDTO.builder()
                .currentLocation(TransportUnitLocation.of("SR1_CH-04"))
                .transportUnitId(TransportUnitId.builder().id("P00001").build())
                .status(TransportUnitStatus.FINISHED)
                .vendorProvider(VendorProvider.SCHAEFER)
                .externalZoneName("SHIPPING_FILLED")
                .build();

        Assertions.assertEquals(expectedUnitTrackingDTO, unitTrackingDTO);

        transportUnitTrackingService.fillExternalZone(unitTrackingDTO);

        Assertions.assertEquals(expectedUnitTrackingDTO, unitTrackingDTO);
    }
}
