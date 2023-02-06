package ru.yandex.market.wms.shippingsorter.sorting.service;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.core.base.request.BoxInfoRequest;
import ru.yandex.market.wms.core.base.response.BoxInfoResponse;
import ru.yandex.market.wms.core.base.response.Carrier;
import ru.yandex.market.wms.core.base.response.OperationDay;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxInfo;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.api.internal.CoreIntegrationService;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class CoreIntegrationServiceTest extends IntegrationTest {

    @Autowired
    private CoreIntegrationService coreIntegrationService;

    @Autowired
    @MockBean
    protected CoreClient coreClient;

    @Test
    public void getBoxInfoByBoxIdTest() {
        String boxId = "P000000501";
        BoxInfoResponse boxInfoResponse = new BoxInfoResponse(
                new BigDecimal("0.100"),
                new BigDecimal("10.0"),
                new BigDecimal("15.0"),
                new BigDecimal("3.0"),
                new Carrier("10987", "DPD"),
                new OperationDay(3981)
        );
        BoxInfo expectedBoxInfo = BoxInfo.builder()
                .boxWeight(100)
                .boxWidth(new BigDecimal("10.0"))
                .boxHeight(new BigDecimal("15.0"))
                .boxLength(new BigDecimal("3.0"))
                .carrierCode("10987")
                .carrierName("DPD")
                .operationDayId(3981L)
                .build();

        Mockito.when(coreClient.getBoxInfo(new BoxInfoRequest(boxId)))
                .thenReturn(boxInfoResponse);

        BoxInfo boxInfo = coreIntegrationService.getBoxInfoByBoxId(boxId);

        Assertions.assertEquals(expectedBoxInfo, boxInfo);
    }
}
