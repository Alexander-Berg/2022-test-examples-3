package ru.yandex.market.wms.common.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.lgw.dto.LifetimeIndicator;
import ru.yandex.market.wms.common.model.dto.SkuAndPackDTO;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.service.GoldSkuService;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.ShelfLifesDto;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.UnitIdDto;
import ru.yandex.market.wms.common.spring.servicebus.model.request.PushMeasurementShelfLifesRequest;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class GoldSkuServiceTest extends IntegrationTest {

    @Autowired
    private GoldSkuService goldSkuService;

    @Autowired
    private SecurityDataProvider securityDataProvider;

    @Autowired
    private ServicebusClient servicebusClient;

    @Test
    @DatabaseSetup("/db/service/goldSku/before.xml")
    @ExpectedDatabase(value = "/db/service/goldSku/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void createAndUpdateGoldSkus() {

        Collection<SkuAndPackDTO> mockSkus = List.of(
                buildSkuAndPackDTO("465852",
                        "ROV0000000000000000358",
                        "MAN_0358",
                        1638841312L,
                        30),
                buildSkuAndPackDTO("465852",
                        "ROV0000000000000000359",
                        "MAN_0359",
                        1638841312L,
                        40),
                buildSkuAndPackDTO("465852",
                        "ROV0000000000000000360",
                        "MAN_0360",
                        1638841312L,
                        30)
        );

        goldSkuService.createOrUpdateGoldSkus(mockSkus);
    }

    @Test
    @DatabaseSetup("/db/service/goldSku/before.xml")
    void reportSkuShelfLife() {

        Set<SkuId> mockSkuIds = Set.of(
                SkuId.of("465852", "ROV0000000000000000359"),
                SkuId.of("465852", "ROV0000000000000000360"));
        String mockUser = securityDataProvider.getUser();

        goldSkuService.reportSkuShelfLife(mockSkuIds, mockUser, "test");

        ArgumentCaptor<PushMeasurementShelfLifesRequest> requestCaptor =
                ArgumentCaptor.forClass(PushMeasurementShelfLifesRequest.class);
        PushMeasurementShelfLifesRequest expectedRequest = PushMeasurementShelfLifesRequest.builder()
                .shelfLifes(
                        List.of(
                                ShelfLifesDto.builder()
                                        .unitId(UnitIdDto.builder()
                                                .article("MAN_0359")
                                                .vendorId(465852L)
                                                .id("MAN_0359")
                                                .build())
                                        .shelfLife(112)
                                        .operatorId(mockUser)
                                        .build(),
                                ShelfLifesDto.builder()
                                        .unitId(UnitIdDto.builder()
                                                .article("MAN_0360")
                                                .vendorId(465852L)
                                                .id("MAN_0360")
                                                .build())
                                        .shelfLife(113)
                                        .operatorId(mockUser)
                                        .build()))
                .build();
        verify(servicebusClient, times(1)).pushMeasurementShelfLifes(requestCaptor.capture());
        Assertions.assertEquals(expectedRequest, requestCaptor.getValue());
    }

    private SkuAndPackDTO buildSkuAndPackDTO(String storerKey,
                                             String sku,
                                             String manufacturerSku,
                                             Long version,
                                             Integer toExpireDays) {
        SkuAndPackDTO dto = new SkuAndPackDTO();
        dto.setShelflifeindicator(LifetimeIndicator.TRACK_LIFETIME.getValue());
        dto.setStorerkey(storerKey);
        dto.setSku(sku);
        dto.setManufacturersku(manufacturerSku);
        dto.setGoldLifetime(toExpireDays);
        dto.setGoldLifetimeUpdated(Timestamp.from(Instant.ofEpochMilli(version)));
        return dto;
    }
}
