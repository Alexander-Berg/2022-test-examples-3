package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.OutboundRegistry;
import ru.yandex.market.wms.common.model.dto.OutboundRegister;
import ru.yandex.market.wms.common.model.dto.RegisterUnit;
import ru.yandex.market.wms.common.model.enums.OutboundRegisterType;
import ru.yandex.market.wms.common.model.enums.RegisterUnitType;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.IntegrationTestConfig;
import ru.yandex.market.wms.servicebus.api.external.logistics.server.converter.OutboundRegisterToRegistryConverter;
import ru.yandex.market.wms.servicebus.api.external.logistics.server.exception.LogisticApiExceptionHandler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpringBootTest(classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@Import(LogisticApiExceptionHandler.class)
class GetOutboundControllerTest extends IntegrationTest {

    private static final List<OutboundRegister> OUTBOUND_REGISTERS = makeOutboundRegistersResponse();

    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;

    @Autowired
    protected ObjectMapper objectMapper;

    @Qualifier("localValidatorFactoryBean")
    @Autowired
    private Validator validator;

    @Autowired
    private OutboundRegisterToRegistryConverter registryConverter;

    @Autowired
    private Environment env;

    @BeforeEach
    public void setupWireMock() {
        WireMock.configureFor(env.getProperty("wiremock.server.port", Integer.class));
    }

    @Test
    void getOutboundTest() throws Exception {
        setupOutbound("outbound-775325", "api/logistics/server/getOutbound/happy/api-order-response.json");
        setupOutboundRegisters("outbound-775325", "api/logistics/server/getOutbound/happy/api-registers-response.json");

        mockMvc.perform(post("/api/logistic/getOutbound")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/getOutbound/happy/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/getOutbound/happy/response.xml")))
                .andReturn();
    }

    @Test
    void getAutoOutbound() throws Exception {
        setupOutbound("outbound-12345", "api/logistics/server/getOutbound/auto/api-order-response.json");
        setupOutboundRegistry("outbound-12345", "api/logistics/server/getOutbound/auto/api-registry-response.json");
        when(dbConfigService.getConfigAsBoolean("GET_OUTBOUND_UNIFIED")).thenReturn(true);

        mockMvc.perform(post("/api/logistic/getOutbound")
                        .contentType(MediaType.TEXT_XML)
                        .accept(MediaType.TEXT_XML)
                        .content(getFileContent("api/logistics/server/getOutbound/auto/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/getOutbound/auto/response.xml")))
                .andReturn();
    }

    @Test
    /* Проверка того что при возвращении параметра hasLifeTime из API
    * он корректно передается в ответ getOutbound */
    void getOutboundTestInterstoreItemShouldHaveShelfLife() throws Exception {
        setupOutbound("outbound-775325", "api/logistics/server/getOutbound/interstore/api-order-response.json");
        setupOutboundRegistry("outbound-775325",
                "api/logistics/server/getOutbound/interstore/api-registry-response.json");

        mockMvc.perform(post("/api/logistic/getOutbound")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/getOutbound/interstore/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/getOutbound/interstore/response.xml")))
                .andReturn();
    }

    @Test
    void getOutboundRegisterWithEmptyBoxParentId() throws Exception {
        setupOutbound("outbound-775325", "api/logistics/server/getOutbound/empty-parent-id/api-order-response.json");
        setupOutboundRegisters("outbound-775325",
                "api/logistics/server/getOutbound/empty-parent-id/api-registers-response.json");

        mockMvc.perform(post("/api/logistic/getOutbound")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/getOutbound/empty-parent-id/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(
                        getFileContent("api/logistics/server/getOutbound/empty-parent-id/response.xml")))
                .andReturn();
    }

    @Test
    void validateRegistryItem() {
        for (OutboundRegister register : OUTBOUND_REGISTERS) {
            OutboundRegistry res = registryConverter.convert(register, new ResourceId("as", "s"));
            Set<ConstraintViolation<OutboundRegistry>> validatesRegistry = validator.validate(res);
            assertions.assertThat(validatesRegistry).isEmpty();
        }
    }

    @Test
    public void uitAndCisInfoShouldBePresentInResponse() throws Exception {
        setupOutbound("outbound-124345", "api/logistics/server/getOutbound/cis-uit/api-order-response.json");
        setupOutboundRegistry("outbound-124345", "api/logistics/server/getOutbound/cis-uit/api-outbound-response.json");

        mockMvc.perform(post("/api/logistic/getOutbound")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/getOutbound/cis-uit/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/getOutbound/cis-uit/response.xml")))
                .andReturn();

    }

    private void setupOutbound(String externalOrderKey, String fileName) {
        final String testUrl = "/api/INFOR_SCPRD_wmwhse1/shipments/external-order-key/" + externalOrderKey;
        stubFor(get(urlEqualTo(testUrl))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FileContentUtils.getFileContent(fileName))));
    }

    private void setupOutboundRegistry(String externalOrderKey, String fileName) {
        final String testUrl = "/api/INFOR_SCPRD_ENTERPRISE/outbound/registry/" + externalOrderKey;
        stubFor(get(urlEqualTo(testUrl))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FileContentUtils.getFileContent(fileName))));
    }

    private void setupOutboundRegisters(String externalOrderKey, String fileName) {
        final String testUrl = "/api/INFOR_SCPRD_wmwhse1/outbound-registers/" + externalOrderKey;
        stubFor(get(urlEqualTo(testUrl))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FileContentUtils.getFileContent(fileName))));
    }

    private static List<OutboundRegister> makeOutboundRegistersResponse() {
        List<OutboundRegister> registers = new ArrayList<>();
        List<RegisterUnit> units1 = new ArrayList<>();
        units1.add(RegisterUnit.builder()
                .externOrderKey("outbound-775325")
                .unitKey("B100")
                .amount(1)
                .type(RegisterUnitType.BOX)
                .parentUnitKey("DP101")
                .cargoTypes(Collections.singletonList(100))
                .build());
        units1.add(RegisterUnit.builder()
                .externOrderKey("outbound-775325")
                .unitKey("DP101")
                .amount(1)
                .type(RegisterUnitType.PALLET)
                .cargoTypes(Collections.singletonList(100))
                .build());
        units1.add(RegisterUnit.builder()
                .externOrderKey("outbound-775325")
                .unitKey("DP102")
                .amount(3)
                .type(RegisterUnitType.ITEM)
                .parentUnitKey("DP101")
                .manufacturerSku("MANSKU1")
                .storerKey("Storer1")
                .editDate(Instant.parse("2020-08-17T21:00:00Z"))
                .cargoTypes(Collections.singletonList(100))
                .build());
        units1.add(RegisterUnit.builder()
                .externOrderKey("outbound-775325")
                .unitKey("DP103")
                .amount(7)
                .type(RegisterUnitType.ITEM)
                .manufacturerSku("MANSKU2")
                .storerKey("Storer2")
                .sku("sku1")
                .editDate(Instant.parse("2020-08-16T21:00:00Z"))
                .cargoTypes(Collections.singletonList(100))
                .build());
        units1.add(RegisterUnit.builder()
                .externOrderKey("outbound-775325")
                .unitKey("DP104")
                .amount(5)
                .type(RegisterUnitType.ITEM)
                .manufacturerSku("MANSKU2")
                .storerKey("Storer2")
                .sku("sku2")
                .editDate(Instant.parse("2020-08-16T21:00:00Z"))
                .cargoTypes(Collections.singletonList(100))
                .build());
        List<RegisterUnit> units2 = new ArrayList<>();
        units2.add(RegisterUnit.builder()
                .externOrderKey("outbound-775325")
                .unitKey("DP102")
                .amount(1)
                .type(RegisterUnitType.PALLET)
                .cargoTypes(Collections.singletonList(100))
                .build());
        registers.add(OutboundRegister.builder()
                .registerKey("1111")
                .externRegisterKey("606")
                .externOrderKey("outbound-775325")
                .type(OutboundRegisterType.FACTUAL)
                .addDate(Instant.parse("2020-12-20T07:00:00Z"))
                .registerUnits(units1)
                .build());
        registers.add(OutboundRegister.builder()
                .registerKey("2222")
                .externRegisterKey("607")
                .externOrderKey("outbound-775325")
                .type(OutboundRegisterType.FACTUAL)
                .addDate(Instant.parse("2020-12-20T07:00:00Z"))
                .registerUnits(units2)
                .build());
        return registers;
    }
}
