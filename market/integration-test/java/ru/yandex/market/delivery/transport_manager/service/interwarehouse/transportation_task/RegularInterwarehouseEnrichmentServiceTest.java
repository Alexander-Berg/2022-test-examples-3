package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task;

import java.time.Duration;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation.RegularInterwarehouseEnrichmentService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerTransportDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;

public class RegularInterwarehouseEnrichmentServiceTest extends AbstractContextualTest {

    @Autowired
    private RegularInterwarehouseEnrichmentService enrichmentService;

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DatabaseSetup("/repository/interwarehouse/regular_xdoc.xml")
    @ExpectedDatabase(
        value = "/repository/interwarehouse/after/after_enrichment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testEnrichment() {
        initLmsMock();
        enrichmentService.enrichTransportation(102L);
    }

    private void initLmsMock() {
        PartnerTransportDto first =
            PartnerTransportDto.newBuilder()
                .id(1L)
                .partner(PartnerResponse.newBuilder().id(1).build())
                .logisticsPointFrom(LogisticsPointResponse.newBuilder().id(1L).build())
                .logisticsPointTo(LogisticsPointResponse.newBuilder().id(2L).build())
                .duration(Duration.ofMinutes(40))
                .palletCount(10)
                .price(500L)
                .build();

        Mockito.when(lmsClient.getPartnerTransport(Mockito.any())).thenReturn(List.of(first));

        Mockito.when(lmsClient.searchPartnerApiSettingsMethods(Mockito.any()))
            .thenReturn(List.of(
                SettingsMethodDto.newBuilder().partnerId(1L)
                        .method("putMovement").settingsApiId(1L).active(true).build(),
                SettingsMethodDto.newBuilder().partnerId(1L)
                        .method("getMovementStatusHistory").settingsApiId(2L).active(true).build()
            ));
        Mockito.when(lmsClient.searchPartnerApiSettings(Mockito.any()))
            .thenReturn(List.of(
                SettingsApiDto.newBuilder().partnerId(1L).id(1L).apiType(ApiType.DELIVERY).build(),
                SettingsApiDto.newBuilder().partnerId(1L).id(2L).apiType(ApiType.DELIVERY).build()
            ));
    }

}
