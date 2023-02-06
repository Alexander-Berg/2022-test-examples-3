package ru.yandex.market.ff4shops.tms.jobs.mbi;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.mbi.feature.model.FeatureStatus;
import ru.yandex.market.ff4shops.partner.dao.PartnerRepository;
import ru.yandex.market.ff4shops.partner.dao.model.PartnerEntity;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseFilter;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class ImportPartnerStatesJobTest extends FunctionalTest {

    @Autowired
    @Qualifier("mbiApiRestTemplate")
    private RestTemplate mbiApiRestTemplate;

    @Autowired
    private ImportPartnerStatesJob importPartnerStatesJob;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private LMSClient lmsClient;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void initMock() {
        BusinessWarehouseResponse businessWarehouseResponse = BusinessWarehouseResponse
                .newBuilder()
                .partnerId(49029L)
                .externalId("123")
                .build();
        mockRestServiceServer = MockRestServiceServer.createServer(mbiApiRestTemplate);
        Mockito.when(lmsClient.getBusinessWarehouses(any(BusinessWarehouseFilter.class), anyInt(), anyInt()))
               .thenReturn(new PageResult<BusinessWarehouseResponse>()
                       .setPage(0)
                       .setData(List.of(businessWarehouseResponse))
                       .setSize(100500)
                       .setTotalElements(1)
                       .setTotalPages(1));
        Mockito.verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DbUnitDataSet(before = "ImportPartnerStatesJobTest.before.csv", after = "ImportPartnerStatesJobTest.after.csv")
    void doJob() {
        partnerRepository.save(new PartnerEntity(
                100L, 100500L, false, FeatureType.DROPSHIP, FeatureStatus.SUCCESS, true, true, null, true
        ));
        expectRequestToMbiApi();
        importPartnerStatesJob.doJob(null);
    }

    private void expectRequestToMbiApi() {
        String response = extractFileContent("ru/yandex/market/ff4shops/tms/jobs/mbi/partners-ff4shops-states-rsp.xml");
        mockRestServiceServer.expect(method(HttpMethod.GET)).andRespond(withSuccess(response, MediaType.APPLICATION_XML));
    }
}
