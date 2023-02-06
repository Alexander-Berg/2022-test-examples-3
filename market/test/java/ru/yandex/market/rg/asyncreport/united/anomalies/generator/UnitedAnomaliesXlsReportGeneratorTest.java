package ru.yandex.market.rg.asyncreport.united.anomalies.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.feed.supplier.report.UnitedReportsInformationService;
import ru.yandex.market.core.fulfillment.FulfillmentWorkflowService;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.core.partner.PartnerCommonInfoService;
import ru.yandex.market.core.partner.PartnerNameHelper;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.supplier.SupplierExposedActService;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.ff.client.FulfillmentWorkflowClient;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTOContainer;
import ru.yandex.market.ff.client.dto.ShopRequestDocumentDTO;
import ru.yandex.market.ff.client.dto.additionalSupplies.AdditionalSuppliesDiscrepancyDTO;
import ru.yandex.market.ff.client.dto.additionalSupplies.AdditionalSuppliesStatusDTO;
import ru.yandex.market.ff.client.dto.additionalSupplies.AdditionalSuppliesStatusListDTO;
import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.rg.asyncreport.united.anomalies.UnitedAnomaliesParams;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnitedAnomaliesXlsReportGeneratorTest extends FunctionalTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy/HH:mm:ss");

    private ObjectMapper objectMapper;
    private UnitedAnomaliesRowsSupplier rowsSupplier;

    @Autowired
    private SupplierExposedActService supplierExposedActService;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private PartnerContractService supplierContactService;
    @Autowired
    private PartnerPlacementProgramService partnerPlacementProgramService;
    @Autowired
    private DeliveryInfoService deliveryInfoService;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private PartnerNameHelper partnerNameHelper;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private PartnerCommonInfoService partnerCommonInfoService;
    @Autowired
    private EnvironmentService environmentService;

    private NamedParameterJdbcTemplate yqlNamedParameterJdbcTemplate;
    private FulfillmentWorkflowClient fulfillmentWorkflowClient;
    private final TestableClock clock = new TestableClock();

    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DefaultPrettyPrinter.Indenter indenter =
                new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
    }

    public void initXlsGenerationOnlyTest() {
        clock.setFixed(Instant.parse("2022-01-08T12:00:00Z"), ZoneOffset.UTC);
        rowsSupplier = mock(UnitedAnomaliesRowsSupplier.class);
        when(rowsSupplier.getClock()).thenReturn(clock);
    }

    public void initFullGenerationTest() {
        clock.setFixed(Instant.parse("2022-01-08T12:00:00Z"), ZoneOffset.UTC);
        fulfillmentWorkflowClient = mock(FulfillmentWorkflowClient.class);
        yqlNamedParameterJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        UnitedReportsInformationService unitedReportsInformationService =
                new UnitedReportsInformationService(
                        supplierExposedActService,
                        supplierContactService,
                        businessService,
                        partnerPlacementProgramService,
                        partnerNameHelper,
                        supplierService,
                        campaignService,
                        partnerCommonInfoService,
                        environmentService
                );
        rowsSupplier = new UnitedAnomaliesRowsSupplier(
                unitedReportsInformationService,
                new FulfillmentWorkflowService(fulfillmentWorkflowClient),
                deliveryInfoService,
                campaignService,
                yqlNamedParameterJdbcTemplate,
                clock,
                null
        );
    }

    @Test
    public void testXlsGenerationOnly() throws IOException, InvalidFormatException {
        initXlsGenerationOnlyTest();

        List<UnitedAnomaliesRow> anomaliesRows = objectMapper.readValue(
                StringTestUtil.getString(this.getClass(), "UnitedAnomaliesXlsReportGeneratorTest.anomalies.json"),
                new TypeReference<List<UnitedAnomaliesRow>>() {
                });

        when(rowsSupplier.getRows(any())).thenReturn(anomaliesRows.stream());

        UnitedAnomaliesXlsReportGenerator unitedAnomaliesXlsReportGenerator =
                new UnitedAnomaliesXlsReportGenerator(rowsSupplier);

        Path tempFilePath = Files.createTempFile("UnitedAnomaliesXlsReportGeneratorTest",".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedAnomaliesXlsReportGenerator.generate(new UnitedAnomaliesParams(10614661L), output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expectedXls = new XSSFWorkbook(Objects.requireNonNull(
                getClass().getResourceAsStream("UnitedAnomaliesXlsReportGeneratorTest.expected.xlsx")
        ));

        ExcelTestUtils.assertEquals(
                expectedXls,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    private ShopRequestDTO createRequest(
            long reqId,
            long serviceId,
            long shopId,
            String reqDate
    ) {
        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(reqId);
        shopRequestDTO.setServiceId(serviceId);
        shopRequestDTO.setShopId(shopId);
        shopRequestDTO.setRequestedDate(LocalDateTime.parse(reqDate, DATE_FORMATTER));
        return shopRequestDTO;
    }

    private ShopRequestDTOContainer createRequestContainerPage(
            int totalPages,
            int page,
            List<ShopRequestDTO> requests
    ) {
        ShopRequestDTOContainer request = new ShopRequestDTOContainer(totalPages, page, 1);
        requests.forEach(request::addRequest);
        return request;
    }

    private AdditionalSuppliesStatusDTO createAdditionalSuppliesStatus(
            long reqId, int acceptable, int unacceptable, String createDate
    ) {
        AdditionalSuppliesStatusDTO additionalSuppliesStatusDTO = new AdditionalSuppliesStatusDTO();
        additionalSuppliesStatusDTO.setRequestId(reqId);
        if (createDate != null) {
            additionalSuppliesStatusDTO.setDocuments(
                    List.of(new ShopRequestDocumentDTO(
                            10614661L,
                            reqId,
                            DocumentType.SECONDARY_RECEPTION_ACT,
                            LocalDateTime.parse(createDate, DATE_FORMATTER),
                            ""
                    )));
        } else {
            additionalSuppliesStatusDTO.setDocuments(
                    List.of()
            );
        }
        AdditionalSuppliesDiscrepancyDTO discrepancyDTO = new AdditionalSuppliesDiscrepancyDTO();
        discrepancyDTO.setAcceptable(acceptable);
        discrepancyDTO.setUnacceptable(unacceptable);
        additionalSuppliesStatusDTO.setDiscrepancy(discrepancyDTO);
        return additionalSuppliesStatusDTO;
    }

    @Test
    @DbUnitDataSet(before = "UnitedAnomaliesTest.before.csv")
    public void testFullGeneration() throws IOException, InvalidFormatException {
        initFullGenerationTest();

        UnitedAnomaliesParams params = new UnitedAnomaliesParams(10614661L);

        UnitedAnomaliesXlsReportGenerator unitedAnomaliesXlsReportGenerator =
                new UnitedAnomaliesXlsReportGenerator(rowsSupplier);

        long reqId1 = 1001L;
        long reqId2 = 1002L;
        long reqId3 = 1003L;
        long reqId4 = 1004L;

        long supplierId1 = 501L;
        long supplierId2 = 502L;

        when(fulfillmentWorkflowClient.getRequests(any())).thenReturn(
                createRequestContainerPage(
                        4, 0, List.of(
                                createRequest(reqId1, 1001L, supplierId1, "03.01.2022/12:00:00"),
                                createRequest(0L, 0L, 0L, "03.01.2022/12:00:00"))),
                createRequestContainerPage(
                        4, 1, List.of(
                                createRequest(reqId2, 1002L, supplierId1, "03.01.2022/12:00:00"))),
                createRequestContainerPage(
                        4, 3, List.of(
                                createRequest(reqId4, 1004L, supplierId2, "03.01.2022/12:00:00"))),
                new ShopRequestDTOContainer()
        );
        when(fulfillmentWorkflowClient.getAdditionalSuppliesStatuses(any())).thenReturn(
                new AdditionalSuppliesStatusListDTO(List.of(
                        createAdditionalSuppliesStatus(reqId1, 4, 4, "05.01.2022/12:00:00"),
                        createAdditionalSuppliesStatus(reqId2, 4, 4, "05.01.2022/12:00:00"),
                        createAdditionalSuppliesStatus(reqId4, 4, 4, "05.01.2022/12:00:00")
                ))
        );
        RowMapper<UnitedAnomaliesRowsSupplier.YtWmsInfo> rowMapper = any();
        when(yqlNamedParameterJdbcTemplate.query(any(), rowMapper))
                .thenReturn(List.of(
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId1,
                                supplierId1
                        ),
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId2,
                                supplierId1
                        ),
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId3,
                                supplierId2
                        ),
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId4,
                                supplierId2
                        )
                ));

        Path tempFilePath = Files.createTempFile("UnitedAnomaliesFullGeneration", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedAnomaliesXlsReportGenerator.generate(params, output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expectedXls = new XSSFWorkbook(Objects.requireNonNull(
                getClass().getResourceAsStream(
                        "UnitedAnomaliesFullTest.expected.xlsx"
                )
        ));

        ExcelTestUtils.assertEquals(
                expectedXls,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DbUnitDataSet(before = "UnitedAnomaliesTest.before.csv")
    public void testFullGenerationWithNull() throws IOException, InvalidFormatException {
        initFullGenerationTest();

        UnitedAnomaliesParams params = new UnitedAnomaliesParams(10614661L);

        UnitedAnomaliesXlsReportGenerator unitedAnomaliesXlsReportGenerator =
                new UnitedAnomaliesXlsReportGenerator(rowsSupplier);

        long reqId1 = 1001L;
        long reqId2 = 1002L;
        long reqId3 = 1003L;
        long reqId4 = 1004L;

        long supplierId1 = 501L;
        long supplierId2 = 502L;

        when(fulfillmentWorkflowClient.getRequests(any())).thenReturn(
                createRequestContainerPage(
                        4, 0, List.of(
                                createRequest(reqId1, 1001L, supplierId1, "03.01.2022/12:00:00"))),
                createRequestContainerPage(
                        4, 1, List.of(
                                createRequest(reqId2, 1002L, supplierId1, "03.01.2022/12:00:00"))),
                createRequestContainerPage(
                        4, 2, List.of(
                                createRequest(reqId3, 1003L, supplierId2, "03.01.2022/12:00:00"))),
                createRequestContainerPage(
                        4, 3, List.of(
                                createRequest(reqId4, 1004L, supplierId2, "03.01.2022/12:00:00"))),
                new ShopRequestDTOContainer()
        );
        when(fulfillmentWorkflowClient.getAdditionalSuppliesStatuses(any())).thenReturn(
                new AdditionalSuppliesStatusListDTO(List.of(
                        createAdditionalSuppliesStatus(reqId1, 4, 4, "05.01.2022/12:00:00")
                ))
        );
        RowMapper<UnitedAnomaliesRowsSupplier.YtWmsInfo> rowMapper = any();
        when(yqlNamedParameterJdbcTemplate.query(any(), rowMapper))
                .thenReturn(List.of(
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId1,
                                supplierId1
                        ),
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId2,
                                supplierId1
                        ),
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId3,
                                supplierId2
                        ),
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId4,
                                supplierId2
                        )
                ));

        Path tempFilePath = Files.createTempFile("UnitedAnomaliesFullGeneration", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedAnomaliesXlsReportGenerator.generate(params, output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expectedXls = new XSSFWorkbook(Objects.requireNonNull(
                getClass().getResourceAsStream(
                        "UnitedAnomaliesFullTestNullAdditionalSupply.expected.xlsx"
                )
        ));

        ExcelTestUtils.assertEquals(
                expectedXls,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    @Test
    @DbUnitDataSet(before = "UnitedAnomaliesTest.before.csv")
    public void testFullGenerationWithoutSecondaryReceptionAct() throws IOException, InvalidFormatException {
        initFullGenerationTest();

        UnitedAnomaliesParams params = new UnitedAnomaliesParams(10614661L);

        UnitedAnomaliesXlsReportGenerator unitedAnomaliesXlsReportGenerator =
                new UnitedAnomaliesXlsReportGenerator(rowsSupplier);

        long reqId1 = 1001L;
        long reqId2 = 1002L;
        long reqId3 = 1003L;
        long reqId4 = 1004L;

        long supplierId1 = 501L;
        long supplierId2 = 502L;

        when(fulfillmentWorkflowClient.getRequests(any())).thenReturn(
                createRequestContainerPage(
                        4, 0, List.of(
                                createRequest(reqId1, 1001L, supplierId1, "03.01.2022/12:00:00"))),
                createRequestContainerPage(
                        4, 1, List.of(
                                createRequest(reqId2, 1002L, supplierId1, "03.01.2022/12:00:00"))),
                createRequestContainerPage(
                        4, 2, List.of(
                                createRequest(reqId3, 1003L, supplierId2, "03.01.2022/12:00:00"))),
                createRequestContainerPage(
                        4, 3, List.of(
                                createRequest(reqId4, 1004L, supplierId2, "03.01.2022/12:00:00"))),
                new ShopRequestDTOContainer()
        );
        when(fulfillmentWorkflowClient.getAdditionalSuppliesStatuses(any())).thenReturn(
                new AdditionalSuppliesStatusListDTO(List.of(
                        createAdditionalSuppliesStatus(reqId1, 4, 4, null),
                        createAdditionalSuppliesStatus(reqId2, 4, 4, "05.01.2022/12:00:00"),
                        createAdditionalSuppliesStatus(reqId3, 4, 4, null),
                        createAdditionalSuppliesStatus(reqId4, 4, 4, "05.01.2022/12:00:00")
                ))
        );
        RowMapper<UnitedAnomaliesRowsSupplier.YtWmsInfo> rowMapper = any();
        when(yqlNamedParameterJdbcTemplate.query(any(), rowMapper))
                .thenReturn(List.of(
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId1,
                                supplierId1
                        ),
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId2,
                                supplierId1
                        ),
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId3,
                                supplierId2
                        ),
                        new UnitedAnomaliesRowsSupplier.YtWmsInfo(
                                4L,
                                reqId4,
                                supplierId2
                        )
                ));

        Path tempFilePath = Files.createTempFile("UnitedAnomaliesFullGeneration", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            unitedAnomaliesXlsReportGenerator.generate(params, output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expectedXls = new XSSFWorkbook(Objects.requireNonNull(
                getClass().getResourceAsStream(
                        "UnitedAnomaliesFullTestWithoutSecondReceptionAct.expected.xlsx"
                )
        ));

        ExcelTestUtils.assertEquals(
                expectedXls,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }
}
