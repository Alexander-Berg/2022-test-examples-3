package ru.yandex.market.billing.distribution.imports;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.distribution.imports.dao.DistributionPartnerDao;
import ru.yandex.market.billing.distribution.imports.dao.HardcodedClidDao;
import ru.yandex.market.billing.distribution.share.model.DistributionPartner;
import ru.yandex.market.billing.distribution.share.model.DistributionPartnerSegment;
import ru.yandex.market.billing.tasks.distribution.DistributionClient;
import ru.yandex.market.billing.tasks.distribution.DistributionPlaceClient;
import ru.yandex.market.billing.tasks.distribution.DistributionReportResponseParser;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ClidImportServiceTest extends FunctionalTest {

    @Autowired
    private DistributionPartnerDao distributionPartnerDao;

    @Autowired
    private DistributionPartnerDao distributionClientsPgDao;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private HardcodedClidDao hardcodedClidDao;

    @BeforeEach
    void init() {
        hardcodedClidDao = new HardcodedClidDao(namedParameterJdbcTemplate);
    }

    @Test
    @DbUnitDataSet(
            before = "ClidImportServiceTest.testImportDistributionClients.before.csv",
            after = "ClidImportServiceTest.testImportDistributionClients.after.csv"
    )
    void testImportFromClients() throws IOException {
        DistributionPlaceClient placeClient = mock(DistributionPlaceClient.class);
        ClidImportService clidImportService = spy(new ClidImportService(
                distributionPartnerDao,
                distributionClientsPgDao,
                environmentService,
                mock(DistributionClient.class),
                placeClient,
                new DistributionReportResponseParser(),
                hardcodedClidDao
        ));
        DistributionClient.Response reportResponse = new ObjectMapper().readerFor(DistributionClient.Response.class)
                .readValue(this.getClass().getResource("distributionReportResponse.json"));
        when(clidImportService.requestData(1046)).thenReturn(reportResponse);
        DistributionClient.Response havingContractResponse =
                new ObjectMapper().readerFor(DistributionClient.Response.class)
                        .readValue(this.getClass().getResource("distributionReportResponseClids.json"));
        when(clidImportService.requestClidsHavingContracts(1046)).thenReturn(havingContractResponse);
        for (int softId : ClidImportService.ALLOWED_SOFT_IDS) {
            if (softId != 1046) {
                mockEmptyResponse(clidImportService, softId);
            }
        }

        when(placeClient.getAllClids()).thenReturn(getClids());

        clidImportService.importDistributionClients();

        verify(placeClient).getAllClids();
        verifyNoMoreInteractions(placeClient);
    }

    private void mockEmptyResponse(ClidImportService service, int softId) throws IOException{
        DistributionClient.Response reportEmptyResponse =
                new ObjectMapper().readerFor(DistributionClient.Response.class)
                        .readValue(this.getClass().getResource("distributionReportEmptyResponse.json"));
        when(service.requestData(softId)).thenReturn(reportEmptyResponse);
        when(service.requestClidsHavingContracts(softId)).thenReturn(reportEmptyResponse);
    }

    private static List<DistributionPartner> getClids() {
        return List.of(
                DistributionPartner.builder()
                        .setClid(101L)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setPlaceType("Купонный агрегатор")
                        .setName("Сто первый")
                        .setUrl("101.ru")
                        .setUniqueVisitors("100-500")
                        .setStatus(0)
                        .build(),

                DistributionPartner.builder()
                        .setClid(102L)
                        .setPartnerSegment(DistributionPartnerSegment.MARKETING)
                        .setPlaceType("Instagram блог")
                        .setName("Сто второй")
                        .setUrl("102.ru")
                        .setUniqueVisitors("100-500")
                        .setStatus(0)
                        .build(),

                DistributionPartner.builder()
                        .setClid(103L)
                        .setPartnerSegment(null)
                        .setName("Сто третий")
                        .setUrl("103.ru")
                        .setUniqueVisitors("1000-5000")
                        .setStatus(1)
                        .build(),

                DistributionPartner.builder()
                        .setClid(120L)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .build(),

                // place_type и partner_segment не будет импортирован,
                // так как clid содержится в списке явно заданных клидов
                DistributionPartner.builder()
                        .setClid(201L)
                        .setPartnerSegment(DistributionPartnerSegment.MARKETING)
                        .setPlaceType("Twitch канал")
                        .setName("Двести первый")
                        .setUrl("201.ru")
                        .setUniqueVisitors("1000-5000")
                        .setStatus(1)
                        .build()
        );
    }
}
