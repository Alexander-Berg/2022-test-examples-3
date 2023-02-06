package ru.yandex.market.wms.shippingsorter.sorting.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxId;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxInfo;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxStatus;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.LocationId;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.PackStationId;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.SorterCreateDTO;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.service.SorterOrderService;
import ru.yandex.market.wms.shippingsorter.sorting.utils.JsonAssertUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterOrdersControllerOrderServiceTest extends IntegrationTest {

    @MockBean
    @Autowired
    private SorterOrderService sorterOrderService;

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-order-management/common.xml")
    public void testInternalErrorWhenSorterOrderServiceReturnExceptionAtCreation() throws Exception {
        SorterCreateDTO sorterCreateDTO = SorterCreateDTO.builder()
                .boxId(BoxId.of("P123456789"))
                .boxInfo(BoxInfo.builder()
                        .boxWeight(1001)
                        .boxWidth(new BigDecimal("10.0"))
                        .boxHeight(new BigDecimal("15.0"))
                        .boxLength(new BigDecimal("3.0"))
                        .carrierName("rier")
                        .carrierCode("123456")
                        .operationDayId(18262L)
                        .boxStatus(
                                BoxStatus.builder()
                                        .isBoxDropped(false)
                                        .isBoxLoaded(false)
                                        .isBoxShipped(false)
                                        .build()
                        )
                        .build())
                .packStationId(PackStationId.of("PACK-TAB03"))
                .zone("SSORT_ZONE")
                .build();

        Mockito.when(sorterOrderService
                .createSorterOrder(sorterCreateDTO))
                .thenThrow(new RuntimeException("500 Internal error occurred."));

        assertApiCallFail(
                "sorting/controller/sorter-order-management/internal-error/request.json",
                "sorting/controller/sorter-order-management/internal-error/response.json",
                post("/sorting/sorter-orders")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-order-management/common.xml")
    public void testInternalErrorWhenSorterOrderServiceReturnExceptionAtRecreation() throws Exception {

        Mockito.when(sorterOrderService
                .recreateSorterOrder(BoxId.of("P123456789"), LocationId.of("ESORTEXIT")))
                .thenThrow(new RuntimeException("500 Internal error occurred."));

        assertApiCallFail(
                "sorting/controller/sorter-order-management/recreation-internal-error/request.json",
                "sorting/controller/sorter-order-management/recreation-internal-error/response.json",
                post("/sorting/sorter-orders/recreate")
        );
    }

    private void assertApiCallFail(String requestFile, String responseFile, MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(requestFile)))
                .andExpect(status().isInternalServerError())
                .andReturn();
        if (responseFile != null) {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                    mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        }
    }
}
