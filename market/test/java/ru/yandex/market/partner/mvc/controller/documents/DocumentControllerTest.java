package ru.yandex.market.partner.mvc.controller.documents;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientContractInfo;
import ru.yandex.market.common.balance.xmlrpc.model.ContractType;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.yadoc.YaDocClient;
import ru.yandex.yadoc.client.model.DocumentMeta;
import ru.yandex.yadoc.client.model.DocumentStatus;
import ru.yandex.yadoc.client.model.DocumentsContractsRequest;
import ru.yandex.yadoc.client.model.DocumentsMeta;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;

/**
 * Тесты для {@link DocumentController}
 */
@DbUnitDataSet(before = "DocumentControllerTest.before.csv")
class DocumentControllerTest extends FunctionalTest {

    @Autowired
    private YaDocClient yaDocClient;

    @Autowired
    @Qualifier("jacksonMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private BalanceService balanceService;

    @Test
    void testGetMeta() throws Exception {
        Mockito.doReturn(new DocumentsMeta()
                .addDocumentsItem(new DocumentMeta()
                        .contractId(10L)
                        .partyId(10L)
                        .docDate(OffsetDateTime.of(
                                2020, 10, 2,
                                0, 0, 0, 0,
                                OffsetDateTime.now().getOffset())
                        ).docId(1L)
                        .docType(DocumentMeta.DocTypeEnum.ACT)
                        .docNumber("1ACT")
                ).addDocumentsItem(new DocumentMeta()
                        .contractId(11L)
                        .partyId(11L)
                        .docDate(OffsetDateTime.of(
                                2020, 10, 2,
                                0, 0, 0, 0,
                                OffsetDateTime.now().getOffset())
                        ).docId(2L)
                        .docType(DocumentMeta.DocTypeEnum.INV)
                        .docNumber("1INV")
                )
        ).when(yaDocClient).getDocumentsMeta(Mockito.eq(new DocumentsContractsRequest()
                .contractIds(List.of(10L, 11L))
                .dateFrom(LocalDate.of(2020, 10, 1))
                .dateTo(LocalDate.of(2020, 10, 31))
                .addExcludeStatusesItem(DocumentStatus.REVERSED))
        );

        Mockito.doReturn(List.of(new ClientContractInfo.ClientContractInfoBuilder()
                .withId(10)
                .withExternalId("10/20")
                .build()
        )).when(balanceService).getClientContracts(Mockito.eq(10L), Mockito.eq(ContractType.GENERAL));
        Mockito.doReturn(List.of(new ClientContractInfo.ClientContractInfoBuilder()
                .withId(11)
                .withExternalId("ОФ/11")
                .build()
        )).when(balanceService).getClientContracts(Mockito.eq(10L), Mockito.eq(ContractType.SPENDABLE));

        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "campaigns/1/documents/meta?date_from=2020-10-01&date_to=2020-10-31"
        );

        JsonNode result = objectMapper.readValue(response.getBody(), ObjectNode.class).get("result");
        List<DocumentDTO> docs = objectMapper.readValue(
                objectMapper.writeValueAsString(result),
                new TypeReference<List<DocumentDTO>>() {
                }
        );

        assertThat(
                docs,
                Matchers.containsInAnyOrder(
                        MbiMatchers.<DocumentDTO>newAllOfBuilder()
                                .add(allOf(
                                        DocumentDTOMatcher.hasDocId(1),
                                        DocumentDTOMatcher.hasDocNumber("1ACT"),
                                        DocumentDTOMatcher.hasDocType(DocumentTypeDTO.ACT),
                                        DocumentDTOMatcher.hasDocDate(LocalDate.of(2020, 10, 2)),
                                        DocumentDTOMatcher.hasContractExternalId("10/20"),
                                        DocumentDTOMatcher.hasContractType(PartnerContractTypeDTO.INCOME)
                                )).build(),
                        MbiMatchers.<DocumentDTO>newAllOfBuilder()
                                .add(allOf(
                                        DocumentDTOMatcher.hasDocId(2),
                                        DocumentDTOMatcher.hasDocNumber("1INV"),
                                        DocumentDTOMatcher.hasDocType(DocumentTypeDTO.INV),
                                        DocumentDTOMatcher.hasDocDate(LocalDate.of(2020, 10, 2)),
                                        DocumentDTOMatcher.hasContractExternalId("ОФ/11"),
                                        DocumentDTOMatcher.hasContractType(PartnerContractTypeDTO.OUTCOME)
                                )).build()
                )
        );
    }

    @Test
    void getDocumentsWithBalanceExceptions() {
        Mockito.doReturn(new DocumentsMeta())
                .when(yaDocClient).getDocumentsMeta(Mockito.any(DocumentsContractsRequest.class));
        Mockito.doThrow(new IllegalStateException())
                .when(balanceService).getClientContracts(Mockito.eq(10L), Mockito.eq(ContractType.SPENDABLE));
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "campaigns/1/documents/meta?date_from=2020-10-01&date_to=2020-10-31"
        );
        String expected = "[]";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testNoContractsNoException() {
        Assertions.assertDoesNotThrow(() -> FunctionalTestHelper.get(
                baseUrl + "campaigns/2/documents/meta?date_from=2020-10-01&date_to=2020-10-31"
        ));
    }

    @Test
    void testSubsidyStatuses() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "campaigns/1/documents/subsidy/statuses?year=2019"
        );

        //language=json
        String expected = "" +
                "{\n" +
                "  \"documentStatuses\": {\n" +
                "    \"JANUARY\": \"CLOSED\",\n" +
                "    \"MARCH\": \"OBSOLETE\",\n" +
                "    \"FEBRUARY\": \"OBSOLETE\"\n" +
                "  }\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testSubsidyStatusesByYears() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "campaigns/1/documents/subsidy/statuses-by-years"
        );

        //language=json
        String expected = "" +
                "{\n" +
                "  \"statusesByYears\": {\n" +
                "    \"2019\": {\n" +
                "      \"documentStatuses\": {\n" +
                "        \"JANUARY\": \"CLOSED\",\n" +
                "        \"MARCH\": \"OBSOLETE\",\n" +
                "        \"FEBRUARY\": \"OBSOLETE\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"2020\": {\n" +
                "      \"documentStatuses\": {\n" +
                "        \"JANUARY\": \"CLOSED\",\n" +
                "        \"MARCH\": \"PENDING\",\n" +
                "        \"FEBRUARY\": \"PENDING\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }
}
