package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import Market.DataCamp.DataCampOffer;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportRequest;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.model.unitedcatalog.CopyOffersParams;
import ru.yandex.market.core.config.TestSupplierXlsHelperConfig;
import ru.yandex.market.core.fulfillment.mds.ReportsMdsStorage;
import ru.yandex.market.core.offer.mapping.MarketProtoMboMappingsServiceConfig;
import ru.yandex.market.core.offer.mapping.MboMappingServiceConfig;
import ru.yandex.market.core.param.model.EntityName;
import ru.yandex.market.core.tvm.MbiTvmConfig;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.StrollerClientConfig;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.rg.asyncreport.assortment.AssortmentReportWriteService;
import ru.yandex.market.rg.config.reports.unitedcatalog.MigrationGrpcConfig;
import ru.yandex.market.rg.config.reports.unitedcatalog.MigrationTasksConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@Disabled
@SpringJUnitConfig(classes = {
        MbiTvmConfig.class,
        StrollerClientConfig.class,
        MigrationGrpcConfig.class,
        TestSupplierXlsHelperConfig.class,
        MboMappingServiceConfig.class,
        MarketProtoMboMappingsServiceConfig.class,
        MigrationTasksConfig.class,
        MigrationManualTestConfig.class
})
@TestPropertySource(locations =
        "classpath:ru/yandex/market/rg/asyncreport/unitedcatalog/migration/datacamp-test.properties")
class B2BOffersCopierManualTest {

    @Autowired
    @Qualifier("dataCampShopClient")
    DataCampClient dataCampClient;

    @Autowired
    List<DistributedMigrator> distributedMigrators;

    @Autowired
    EnvironmentService environmentService;

    @Autowired
    ReportsMdsStorage<ReportsType> reportsMdsStorage;

    @Autowired
    ReportsService<ReportsType> reportsService;

    @Autowired
    MigrationTaskStateService migrationTaskStateService;

    @Autowired
    AssortmentReportWriteService assortmentReportWriteService;

    B2BOffersCopier copier;

    @BeforeEach
    void setUp() {
        environmentService.setValue("business.migration.batch.size", "1");
        copier = new B2BOffersCopier(
                reportsMdsStorage, dataCampClient, reportsService, migrationTaskStateService,
                assortmentReportWriteService, null, null,
                Clock.systemDefaultZone(),
                distributedMigrators, environmentService
        );
    }

    @Test
    void generate() throws IOException {
        var params = new CopyOffersParams(11104727, 11062549, 11104726, true);

        when(reportsService.getReportInfo(eq("1"))).thenReturn(ReportInfo.<ReportsType>builder()
                .setId("1")
                .setExtendedState("")
                .setReportRequest(ReportRequest.<ReportsType>builder()
                        .setEntityId(11075352)
                        .setReportType(ReportsType.MIGRATE_OFFERS_TO_UCAT)
                        .setEntityName(EntityName.PARTNER)
                        .setParams(Map.of())
                        .build())
                .setState(ReportState.PROCESSING)
                .setRequestCreatedAt(Instant.now())
                .setStateUpdateAt(Instant.now())
                .build());

        doAnswer(invocation -> {
            Stream<DataCampOffer.Offer> stream = invocation.getArgument(2);
            stream.forEach(o -> {
                try {
                    System.out.println(JsonFormat.printer().print(o));
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            });
            return null;
        }).when(assortmentReportWriteService)
                .writeAssortmentReport(anyLong(), any(), any(), any());

        copier.generate("1", params);
    }
}
