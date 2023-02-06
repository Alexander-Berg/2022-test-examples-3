package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferDetails;
import ru.yandex.market.wms.common.spring.domain.dto.TransferDetailDto;
import ru.yandex.market.wms.common.spring.dto.TransferKeyDto;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.logistics.server.converter.TransferDetailsConverter;
import ru.yandex.market.wms.servicebus.api.internal.api.client.impl.WmsApiClientImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.xmlunit.diff.ElementSelectors.byName;
import static org.xmlunit.diff.ElementSelectors.byNameAndText;
import static org.xmlunit.diff.ElementSelectors.byXPath;
import static org.xmlunit.diff.ElementSelectors.conditionalBuilder;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class GetTransferDetailsControllerTest extends IntegrationTest {
    @Qualifier("localValidatorFactoryBean")
    @Autowired
    private Validator validator;
    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private TransferDetailsConverter transferDetailsConverter;

    @MockBean(name = "wmsApiClientImpl")
    @Autowired
    private WmsApiClientImpl wmsApiClientImpl;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(wmsApiClientImpl);
    }

    @Test
    void getTransferDetails() throws Exception {
        setupTransferDetails("api/logistics/server/getTransferDetails/transfer-details.json");
        final String content = mockMvc.perform(MockMvcRequestBuilders.post("/api/logistic/getTransferDetails")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/getTransferDetails/ok/request.xml")))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTransferDetails(getFileContent("api/logistics/server/getTransferDetails/ok/response.xml"), content);
    }

    @Test
    void getTransferDetails_badRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/logistic/getTransferDetails")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/getTransferDetails/badRequest/request.xml")))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .xml(getFileContent("api/logistics/server/getTransferDetails/badRequest/response.xml")))
                .andReturn();
    }

    @Test
    void getTransferDetailsWithDuplicatedIdentity() throws Exception {
        setupTransferDetails("api/logistics/server/getTransferDetails/transfer-details-duplicated-identity.json");
        final String content = mockMvc.perform(MockMvcRequestBuilders.post("/api/logistic/getTransferDetails")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/getTransferDetails/ok/request.xml")))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTransferDetails(getFileContent("api/logistics/server/getTransferDetails/ok/response.xml"), content);
    }

    @Test
    void getTransferDetailsOfPartiallyFailedTransfer() throws Exception {
        setupTransferDetails("api/logistics/server/getTransferDetails/transfer-details-partially-failed.json");
        final String content = mockMvc.perform(MockMvcRequestBuilders.post("/api/logistic/getTransferDetails")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/getTransferDetails/ok/request.xml")))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTransferDetails(
                getFileContent("api/logistics/server/getTransferDetails/ok/response-with-partially-failed-details.xml"),
                content);
    }

    @Test
    void validateConverter() {
        ArrayList<TransferDetailDto> transferDetailDtos = Lists.newArrayList(
                TransferDetailDto.builder()
                        .manufacturerSku("ROV0000000000000002222")
                        .toSku("ROV0000000000000002222")
                        .toStorerKey("10264169")
                        .toQty(1.0F)
                        .fromQty(0.0F)
                        .build(),
                TransferDetailDto.builder()
                        .manufacturerSku("ROV0000000000000002228")
                        .toSku("ROV0000000000000002228")
                        .toStorerKey("10264169")
                        .toQty(0.0F)
                        .fromQty(1.0F)
                        .build());
        TransferDetails transferDetails =
                transferDetailsConverter.convert(new ResourceId("ya", "pa"), transferDetailDtos);
        Set<ConstraintViolation<TransferDetails>> violations = validator.validate(transferDetails);
        assertions.assertThat(violations).isEmpty();
    }

    private void setupTransferDetails(String jsonFile) {
        lenient().when(wmsApiClientImpl.getTransferDetails(any(TransferKeyDto.class)))
                .thenAnswer(invocation -> {
                    TransferKeyDto transferKeyDto = invocation.getArgument(0);
                    assertNotNull(transferKeyDto);
                    assertNotNull(transferKeyDto.getTransferKey());
                    return objectMapper
                            .readValue(getFileContent(jsonFile), new TypeReference<List<TransferDetailDto>>() {
                            });
                });
    }

    private void assertTransferDetails(String expected, String actual) {
        final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .withNodeMatcher(new DefaultNodeMatcher(conditionalBuilder()
                        .whenElementIsNamed("instance")
                        .thenUse(byXPath("./partialIds/partialId/value", byNameAndText))
                        .elseUse(byName)
                        .build()
                ))
                .checkForSimilar()
                .ignoreWhitespace()
                .ignoreComments()
                .build();
        assertThat(diff.hasDifferences())
                .as(diff.toString())
                .isFalse();
    }
}
