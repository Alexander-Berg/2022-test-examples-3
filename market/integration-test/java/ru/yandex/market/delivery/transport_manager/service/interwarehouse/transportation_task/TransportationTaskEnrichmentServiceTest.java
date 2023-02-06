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
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTaskStatus;
import ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task.enrichment.TransportationTaskEnrichmentService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerTransportDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;
import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MasterDataService;
import ru.yandex.market.mdm.http.MdmCommon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TransportationTaskEnrichmentServiceTest extends AbstractContextualTest {
    @Autowired
    private TransportationTaskEnrichmentService enrichmentService;

    @Autowired
    private TransportationTaskStatusService transportationTaskStatusService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MasterDataService masterDataService;

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/transportation_tasks.xml",
        "/repository/transportation_task/transport_metadata.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_enrichment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void enrich() {
        transportationTaskStatusService.setStatus(1L, TransportationTaskStatus.ENRICHING);

        initLmsMock();
        initMasterDataMock();

        enrichmentService.enrichTransportationTask(1L);
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

        PartnerTransportDto second =
            PartnerTransportDto.newBuilder()
                .id(2L)
                .partner(PartnerResponse.newBuilder().id(5).build())
                .logisticsPointFrom(LogisticsPointResponse.newBuilder().id(1L).build())
                .logisticsPointTo(LogisticsPointResponse.newBuilder().id(2L).build())
                .duration(Duration.ofMinutes(50))
                .palletCount(100)
                .price(700L)
                .build();
        Mockito.when(lmsClient.getPartnerTransport(Mockito.any())).thenReturn(
            List.of(first, second)
        );

        Mockito.when(lmsClient.searchPartnerApiSettingsMethods(Mockito.any()))
            .thenReturn(List.of(
                SettingsMethodDto.newBuilder().partnerId(1L)
                        .method("putMovement").settingsApiId(1L).active(true).build(),
                SettingsMethodDto.newBuilder().partnerId(5L)
                        .method("putMovement").settingsApiId(2L).active(true).build(),
                SettingsMethodDto.newBuilder().partnerId(1L)
                        .method("getMovementStatusHistory").settingsApiId(3L).active(true).build(),
                SettingsMethodDto.newBuilder().partnerId(5L)
                        .method("getMovementStatusHistory").settingsApiId(4L).active(true).build()
            ));

        Mockito.when(lmsClient.searchPartnerApiSettings(Mockito.any()))
                .thenReturn(List.of(
                        SettingsApiDto.newBuilder().partnerId(1L).id(1L).apiType(ApiType.DELIVERY).build(),
                        SettingsApiDto.newBuilder().partnerId(5L).id(2L).apiType(ApiType.DELIVERY).build(),
                        SettingsApiDto.newBuilder().partnerId(1L).id(3L).apiType(ApiType.DELIVERY).build(),
                        SettingsApiDto.newBuilder().partnerId(5L).id(4L).apiType(ApiType.DELIVERY).build()
                ));
    }

    private void initMasterDataMock() {
        when(masterDataService.searchSskuMasterData(any())
        ).thenReturn(MasterDataProto.SearchSskuMasterDataResponse.newBuilder().addSskuMasterData(
            MdmCommon.SskuMasterData.newBuilder().build()
        ).build());

        when(masterDataService.searchMskuMasterData(any())
        ).thenReturn(MasterDataProto.SearchMskuMasterDataResponse.newBuilder().addMskuMasterData(
            MdmCommon.MskuMasterData.newBuilder().build()
        ).build());
    }
}
