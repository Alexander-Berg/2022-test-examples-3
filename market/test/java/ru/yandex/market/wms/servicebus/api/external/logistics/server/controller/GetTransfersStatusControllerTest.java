package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Validator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.wms.common.model.enums.TransferStatus;
import ru.yandex.market.wms.common.spring.domain.dto.TransferDto;
import ru.yandex.market.wms.common.spring.dto.TransferKeyDto;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.logistics.server.converter.TransferStatusConverter;
import ru.yandex.market.wms.servicebus.api.internal.api.client.impl.WmsApiClientImpl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class GetTransfersStatusControllerTest extends IntegrationTest {

    @Qualifier("localValidatorFactoryBean")
    @Autowired
    private Validator validator;
    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private TransferStatusConverter transferStatusConverter;

    @MockBean(name = "wmsApiClientImpl")
    @Autowired
    private WmsApiClientImpl wmsApiClientImpl;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(wmsApiClientImpl);
    }

    @Test
    void getTransfersStatus() throws Exception {
        setupTransfer("api/logistics/server/getTransfersStatus/transfers.json");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/logistic/getTransfersStatus")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/getTransfersStatus/ok/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/getTransfersStatus/ok/response.xml")))
                .andReturn();
    }

    @Test
    void getTransfersStatus_badRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/logistic/getTransfersStatus")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/getTransfersStatus/badRequest/request.xml")))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .xml(getFileContent("api/logistics/server/getTransfersStatus/badRequest/response.xml")))
                .andReturn();
    }

    @Test
    void validateConverter() {
        Instant now = Instant.now();
        ArrayList<TransferDto> transferDtos = Lists.newArrayList(
                TransferDto.builder()
                        .status(TransferStatus.UNKNOWN)
                        .editDate(new Timestamp(now.toEpochMilli())).build(),
                TransferDto.builder()
                        .status(TransferStatus.CREATED)
                        .editDate(new Timestamp(now.plus(Duration.ofMinutes(10)).toEpochMilli())).build(),
                TransferDto.builder()
                        .status(TransferStatus.IN_PROCESSING)
                        .editDate(new Timestamp(now.plus(Duration.ofMinutes(20)).toEpochMilli())).build(),
                TransferDto.builder()
                        .status(TransferStatus.SUCCESS)
                        .editDate(new Timestamp(now.plus(Duration.ofMinutes(30)).toEpochMilli())).build());
        transferDtos.forEach(transferDto -> {
            assertions.assertThat(validator
                    .validate(transferStatusConverter.convertToStatus(new ResourceId("ya", "pa"), transferDto)))
                    .isEmpty();
        });
    }

    private void setupTransfer(String jsonFile) {
        lenient().when(wmsApiClientImpl.getTransfers(any()))
                .thenAnswer(invocation -> {
                    List<TransferKeyDto> transferKeyDto = invocation.getArgument(0);
                    assertNotNull(transferKeyDto);
                    assertFalse(transferKeyDto.isEmpty());
                    return objectMapper
                            .readValue(getFileContent(jsonFile), new TypeReference<List<TransferDto>>() { });
                });
    }
}
