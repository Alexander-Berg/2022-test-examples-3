package ru.yandex.market.ff.controller.api;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.service.MarketIdClientService;
import ru.yandex.market.ff.service.util.excel.acts.PrimaryRefundDivergenceActBuilder;
import ru.yandex.market.ff.service.util.excel.acts.PrimaryUnredeemedDivergenceActBuilder;
import ru.yandex.market.ff.service.util.excel.acts.email.DivergenceActMailService;
import ru.yandex.market.ff.service.util.excel.acts.misc.DataLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест для {@link UnredeemedDivergenceActController}.
 */
public class UnredeemedDivergenceActControllerTest extends MvcIntegrationTest {

    @Autowired
    private UnredeemedDivergenceActController unredeemedDivergenceActController;

    @Autowired
    private PrimaryUnredeemedDivergenceActBuilder primaryUnredeemedActBuilder;

    @Autowired
    private PrimaryRefundDivergenceActBuilder primaryRefundActBuilder;

    @Autowired
    private DataLoader dataLoader;

    private DivergenceActMailService mailServiceMock;
    private MarketIdClientService marketIdClientService;

    @BeforeEach
    public void setMocks() {
        mailServiceMock = mock(DivergenceActMailService.class);
        marketIdClientService = mock(MarketIdClientService.class);
        ReflectionTestUtils.setField(unredeemedDivergenceActController,
                "divergenceActMailService", mailServiceMock);
        ReflectionTestUtils.setField(dataLoader, "marketIdClientService", marketIdClientService);
        when(marketIdClientService.getLegalNameByPartner(anyLong(), anyString()))
                .thenReturn("Боксберри");
    }

    @Test
    void generateUnredeemedDivergenceActTest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/unredeemed/send-act")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"supplierId\": \"106\"," +
                                "\"registryType\": \"UNPAID\"," +
                                "\"consignorRequestId\": \"1123_T\"," +
                                "\"actDate\": \"2020-06-18\"," +
                                "\"acceptanceDate\": \"18.06.2020 10:00\"," +
                                "\"systemPrintTime\": \"19.06.2020 11:18\"," +
                                "\"warehouseAddress\": \"140126, Московская область, Раменский городской округ, " +
                                "Логистический технопарк Софьино, строение 3/1\"," +
                                " \"actDataRows\": [" +
                                "{" +
                                "\"orderId\": \"12w4r\"," +
                                "\"trackId\": \"gtm1HY5\"," +
                                "\"divergenceDescr\": \"DELIVERED\"," +
                                "\"boxesAmountWithDivergence\": 1" +
                                "}" +
                                "]" +
                                "}")
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response.getContentAsByteArray());
        assertFalse(response.getContentAsString().isEmpty());
        assertEquals(response.getHeader("Content-disposition"),
                "attachment; filename=Акт расхождений (Первичная приемка) к Акту No 1123_T ОТ 2020-06-18.xlsx");
        assertEquals(response.getHeader("Content-Type"), "application/octet-stream");
        verify(marketIdClientService).getLegalNameByPartner(anyLong(), anyString());
        verify(mailServiceMock, never()).sendEmail(any(Workbook.class), anyString(), anyString());
    }

    //    Невыкупы
    @Test
    void generateDivergenceActAndSendToEmailsTest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/unredeemed/send-act")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"sendToEmails\": true," +
                                "\"supplierId\": \"106\"," +
                                "\"registryType\": \"UNPAID\"," +
                                "\"consignorRequestId\": \"1123_T\"," +
                                "\"actDate\": \"2020-06-18\"," +
                                "\"acceptanceDate\": \"18.06.2020 10:00\"," +
                                "\"systemPrintTime\": \"19.06.2020 11:18\"," +
                                "\"warehouseAddress\": \"140126, Московская область, Раменский городской округ, " +
                                "Логистический технопарк Софьино, строение 3/1\"," +
                                " \"actDataRows\": [" +
                                "{" +
                                "\"orderId\": \"12w4r\"," +
                                "\"trackId\": \"gtm1HY5\"," +
                                "\"divergenceDescr\": \"DELIVERED\"," +
                                "\"boxesAmountWithDivergence\": 1" +
                                "}" +
                                "]" +
                                "}")
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response.getContentAsByteArray());
        assertFalse(response.getContentAsString().isEmpty());
        assertEquals(response.getHeader("Content-disposition"),
                "attachment; filename=Акт расхождений (Первичная приемка) к Акту No 1123_T ОТ 2020-06-18.xlsx");
        assertEquals(response.getHeader("Content-Type"), "application/octet-stream");
        verify(marketIdClientService).getLegalNameByPartner(anyLong(), anyString());
        verify(mailServiceMock).sendEmail(any(Workbook.class), anyString(), anyString());
    }

    @Test
    void generateDivergenceActWithNumberOfBoxesExceptionTest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/unredeemed/send-act")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"supplierId\": \"106\"," +
                                "\"registryType\": \"UNPAID\"," +
                                "\"consignorRequestId\": \"1123_T\"," +
                                "\"actDate\": \"2020-06-18\"," +
                                "\"acceptanceDate\": \"18.06.2020 10:00\"," +
                                "\"systemPrintTime\": \"19.06.2020 11:18\"," +
                                "\"warehouseAddress\": \"140126, Московская область, Раменский городской округ, " +
                                "Логистический технопарк Софьино, строение 3/1\"," +
                                " \"actDataRows\": [" +
                                "{" +
                                "\"orderId\": \"12w4r\"," +
                                "\"trackId\": \"gtm1HY5\"," +
                                "\"divergenceDescr\": \"DELIVERED\"," +
                                "\"boxesAmountWithDivergence\": 0" +
                                "}" +
                                "]" +
                                "}")
        )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"message\":" +
                        "\"actDataRows[0].boxesAmountWithDivergence must be greater than or equal to 1\"}"))
                .andReturn();
        verify(marketIdClientService, never()).getLegalNameByPartner(anyLong(), anyString());
        verify(mailServiceMock, never()).sendEmail(any(Workbook.class), anyString(), anyString());
    }

    @Test
    void sendDivergenceActToEmailsWithEmptyDataFieldsTest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/unredeemed/send-act")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"supplierId\": \"106\"," +
                                "\"consignorRequestId\": \"\"," +
                                "\"actDate\": \"\"," +
                                "\"acceptanceDate\": \"\"," +
                                "\"systemPrintTime\": \"\"," +
                                "\"warehouseAddress\": \"\"," +
                                " \"actDataRows\": [" +
                                "{" +
                                "\"orderId\": \"\"," +
                                "\"trackId\": \"\"," +
                                "\"divergenceDescr\": \"DELIVERED\"" +
                                "}" +
                                "]" +
                                "}")
        ).andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
        verify(marketIdClientService, never()).getLegalNameByPartner(anyLong(), anyString());
        verify(mailServiceMock, never()).sendEmail(any(Workbook.class), anyString(), anyString());
    }

    @Test
    void sendDivergenceActToEmailsWithWrongDivergenceDescrTest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/unredeemed/send-act")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"supplierId\": \"106\"," +
                                "\"registryType\": \"UNPAID\"," +
                                " \"actDataRows\": [" +
                                "{" +
                                "\"divergenceDescr\": \"\"" +
                                "}" +
                                "]" +
                                "}")
        ).andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
        verify(marketIdClientService, never()).getLegalNameByPartner(anyLong(), anyString());
        verify(mailServiceMock, never()).sendEmail(any(Workbook.class), anyString(), anyString());
    }

    @Test
    void sendDivergenceActToEmailsWithWrongRegistryTypeTest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/unredeemed/send-act")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"supplierId\": \"106\"," +
                                "\"registryType\": \"UNKNOWN\"," +
                                " \"actDataRows\": [" +
                                "{" +
                                "\"divergenceDescr\": \"DELIVERED\"" +
                                "}" +
                                "]" +
                                "}")
        ).andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
        verify(marketIdClientService, never()).getLegalNameByPartner(anyLong(), anyString());
        verify(mailServiceMock, never()).sendEmail(any(Workbook.class), anyString(), anyString());
    }

    // Возвраты
    @Test
    void generateRefundDivergenceActAndSendToEmailsTest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/unredeemed/send-act")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"sendToEmails\": true," +
                                "\"supplierId\": \"106\"," +
                                "\"registryType\": \"REFUND\"," +
                                "\"consignorRequestId\": \"1123_T\"," +
                                "\"actDate\": \"2020-06-18\"," +
                                "\"acceptanceDate\": \"18.06.2020 10:00\"," +
                                "\"systemPrintTime\": \"19.06.2020 11:18\"," +
                                "\"warehouseAddress\": \"140126, Московская область, Раменский городской округ, " +
                                "Логистический технопарк Софьино, строение 3/1\"," +
                                " \"actDataRows\": [" +
                                "{" +
                                "\"trackId\": \"gtm1HY5\"," +
                                "\"divergenceDescr\": \"NOT_SUPPLIED\"," +
                                "\"boxesAmountWithDivergence\": 1" +
                                "}" +
                                "]" +
                                "}")
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response.getContentAsByteArray());
        assertFalse(response.getContentAsString().isEmpty());
        assertEquals(response.getHeader("Content-disposition"),
                "attachment; filename=Акт расхождений (Первичная приемка) к Акту клиентский возврат No 1123_T ОТ " +
                        "2020-06-18.xlsx");
        assertEquals(response.getHeader("Content-Type"), "application/octet-stream");
        verify(marketIdClientService).getLegalNameByPartner(anyLong(), anyString());
        verify(mailServiceMock).sendEmail(any(Workbook.class), anyString(), anyString());
    }
}
