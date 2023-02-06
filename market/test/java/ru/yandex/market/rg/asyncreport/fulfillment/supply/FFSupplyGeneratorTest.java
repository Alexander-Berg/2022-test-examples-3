package ru.yandex.market.rg.asyncreport.fulfillment.supply;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampUnitedOffer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.asyncreport.exception.EmptyReportException;
import ru.yandex.market.core.feed.validation.result.XlsTestUtils;
import ru.yandex.market.core.fulfillment.supply.FFSupplyGenerator;
import ru.yandex.market.core.fulfillment.supply.FFSupplyTemplateParams;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsFilter;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsRow;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsRowWarehouse;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtStorage;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.RequestItemDTO;
import ru.yandex.market.ff.client.dto.RequestItemDTOContainer;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.model.search.filter.PartnerSupplyPlan;
import ru.yandex.market.mbi.datacamp.model.search.filter.ResultContentStatus;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.feed.validation.result.XlsTestUtils.buildExpectedMap;


class FFSupplyGeneratorTest extends AbstractFFSupplyGeneratorTest {

    public static final Function<Integer, Map<XlsTestUtils.CellInfo, String>> PELMESHKA_CELL_INFO = (demandCount) ->
            buildExpectedMap(1,
                    "Pelmeshka1234",
                    "Pelmeshka iz gribov",
                    "6789, 9876",
                    Optional.ofNullable(demandCount).map(String::valueOf).orElse("0"),
                    "10",
                    "Some test comment"
            );
    public static final Function<Integer, Map<XlsTestUtils.CellInfo, String>> VARENNICK_CELL_INFO_FUNCTION = (row) ->
            buildExpectedMap(row,
                    "Varennik123",
                    "Varennik iz kartoshki",
                    "",
                    "0",
                    "10",
                    "Some test comment"
            );
    public static final Function<Integer, Map<XlsTestUtils.CellInfo, String>> TARELKA_CELL_INFO_FUNCTION = (row) ->
            buildExpectedMap(row,
                    "ikea-plate",
                    "Тарелка Ikea 26см",
                    "123456789",
                    "0",
                    "203",
                    "в упак. 3 шт."
            );
    public static final Function<Integer, Map<XlsTestUtils.CellInfo, String>> CHEBUREK_CELL_INFO_FUNCTION = (row) ->
            buildExpectedMap(row,
                    "z_cheburek007",
                    "Cheburek s myasom",
                    "1234, 4321",
                    "0",
                    "15",
                    ""
            );

    public static final Map<XlsTestUtils.CellInfo, String> BAKINGS =
            ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                    .putAll(PELMESHKA_CELL_INFO.apply(0))
                    .putAll(VARENNICK_CELL_INFO_FUNCTION.apply(2))
                    .putAll(CHEBUREK_CELL_INFO_FUNCTION.apply(3))
                    .build();

    private static final long PARTNER_ID = 10103L;

    @Autowired
    private FFSupplyGenerator ffSupplyGenerator;
    @Autowired
    private SalesDynamicsYtStorage salesDynamicsYtStorage;
    @Autowired
    private FulfillmentWorkflowClientApi fulfillmentWorkflowClientApi;
    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;
    @Autowired
    private SaasService saasService;

    public static final String REQUEST_JSON = "{\n" +
            " \"entityId\": 10103,\n" +
            " \"warehouseId\": 172,\n" +
            " \"categoryId\": [123, 456],\n" +
            " \"vendor\": [\"vendor1\", \"vendor2\"],\n" +
            " \"deficitOnly\": true, \n" +
            " \"textQuery\": \"поиск\" \n" +
            "}";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @BeforeEach
    void init() throws IOException {
        DataCampUnitedOffer.UnitedOffer pelmeshkaOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/pelmeshkaApprovedOffer.json",
                getClass()
        );

        DataCampUnitedOffer.UnitedOffer varennikMboOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/varennikOffer.json",
                getClass()
        );
        DataCampUnitedOffer.UnitedOffer cheburekMboOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/cheburekApprovedOffer.json",
                getClass()
        );

        when(dataCampShopClient.searchBusinessOffers(any()))
                .thenReturn(SearchBusinessOffersResult.builder()
                        .setOffers(List.of(
                                pelmeshkaOffer,
                                varennikMboOffer,
                                cheburekMboOffer
                        ))
                        .build());

        RequestItemDTOContainer requestItemDTOContainer = new RequestItemDTOContainer();
        requestItemDTOContainer
                .addItem(generateRequestItem("Pelmeshka1234", List.of("123456", "123567"), "Some test comment",
                        BigDecimal.TEN));
        requestItemDTOContainer
                .addItem(generateRequestItem("Varennik123", Collections.emptyList(), "Some test comment",
                        BigDecimal.TEN));
        requestItemDTOContainer.addItem(
                generateRequestItem("ikea-plate", List.of("123456789"), "в упак. 3 шт.", BigDecimal.valueOf(203)));
        requestItemDTOContainer
                .addItem(generateRequestItem("z_cheburek007", List.of("1234", "4321"), "", BigDecimal.valueOf(15)));

        when(fulfillmentWorkflowClientApi.getLastRequestItems(any()))
                .thenReturn(requestItemDTOContainer);

        List<SalesDynamicsRow> salesDynamicsRows = Collections.singletonList(
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("Pelmeshka1234")
                        .addWarehouse(147, SalesDynamicsRowWarehouse.builder()
                                .withDemandDynamicRecWeek2(13L)
                                .withAvailable(Boolean.TRUE)
                                .build())
                        .build()
        );
        doAnswer(invocation -> {
            Consumer<Iterator<SalesDynamicsRow>> consumer = invocation.getArgument(2);
            consumer.accept(salesDynamicsRows.iterator());
            return null;
        }).when(salesDynamicsYtStorage).getSalesDynamicsReport(eq(new SalesDynamicsFilter(PARTNER_ID)), any(), any());

        mockMboDeliveryParamsClient("searchFulfillmentSskuParams.allavailable.data.json");
    }

    @Test
    void testSuccessWithoutWarehouse() {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1000);
        ffSupplyGenerator.xlsFFSupplyReport(
                FFSupplyTemplateParams.newBuilder().withPartnerId(PARTNER_ID).build(), output);

        assertSheet(4, output, BAKINGS);
    }

    @Test
    void testSuccessWithNotExistentWarehouse() {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1000);
        assertThrows(EmptyReportException.class, () -> ffSupplyGenerator.xlsFFSupplyReport(
                FFSupplyTemplateParams.newBuilder()
                        .withPartnerId(PARTNER_ID)
                        .withWarehouseId(404L)
                        .build(), output));
    }

    @Test
    void testSuccessWith145Warehouse() {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1000);
        ffSupplyGenerator.xlsFFSupplyReport(
                FFSupplyTemplateParams.newBuilder()
                        .withPartnerId(PARTNER_ID)
                        .withWarehouseId(145L)
                        .build(), output);

        assertSheet(4, output, BAKINGS);
    }

    @Test
    void testEmptyReportWith145WarehouseAndDeficit() {
        assertThrows(EmptyReportException.class, () -> ffSupplyGenerator.xlsFFSupplyReport(
                FFSupplyTemplateParams.newBuilder()
                        .withPartnerId(PARTNER_ID)
                        .withWarehouseId(145L)
                        .withDeficitOnly(true)
                        .build(), new ByteArrayOutputStream(1000)));
    }

    @Test
    void testSuccessWith147Warehouse() {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1000);
        ffSupplyGenerator.xlsFFSupplyReport(
                FFSupplyTemplateParams.newBuilder()
                        .withPartnerId(PARTNER_ID)
                        .withWarehouseId(147L)
                        .build(), output);

        int expectedDemandCount = 13;
        assertSheet(2, output, PELMESHKA_CELL_INFO.apply(expectedDemandCount));
    }

    @Test
    void testBarcodesFromCatalogAndPrevShipment() {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1000);
        ffSupplyGenerator.xlsFFSupplyReport(
                FFSupplyTemplateParams.newBuilder()
                        .withPartnerId(PARTNER_ID)
                        .withWarehouseId(145L)
                        .build(), output);

        // строки отчета (заголовок - 1):
        // 2. штрихкоды из каталога (когда есть отличающиеся каталожные и из последней поставки)
        // 4. штрихкоды из последней поставки (когда нет каталожных)
        // 3. когда нет ни каталожных ни из последней поставки
        assertSheet(4, output, BAKINGS);
    }

    @Test
    void uCatFFSupplyGenerator_dataCampStroller_ok() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DataCampUnitedOffer.UnitedOffer tarelkaUnitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/tarelkaUnitedOffer.json",
                getClass()
        );
        when(dataCampShopClient.searchBusinessOffers(
                eq(SearchBusinessOffersRequest.builder()
                        .setPartnerId(10103L)
                        .setBusinessId(102L)
                        .addSupplyPlan(PartnerSupplyPlan.WILL_SUPPLY)
                        .addCategoryId(123L)
                        .addVendors(List.of("Ikea", "Emal"))
                        .setPageRequest(SeekSliceRequest.firstN(1000))
                        .setScanLimit(1001)
                        .addResultContentStatuses(List.of(
                                ResultContentStatus.HAS_CARD_MARKET,
                                ResultContentStatus.HAS_CARD_PARTNER
                        ))
                        .setWithRetry(true)
                        .build()
                )
        )).thenReturn(SearchBusinessOffersResult.builder()
                .setOffers(List.of(tarelkaUnitedOffer))
                .build()
        );
        ffSupplyGenerator.xlsFFSupplyReport(
                FFSupplyTemplateParams.newBuilder()
                        .withPartnerId(10103L)
                        .withCategoryIds(List.of(123))
                        .withVendors(List.of("Ikea", "Emal"))
                        .build(), output);
        assertSheet(2, output, TARELKA_CELL_INFO_FUNCTION.apply(1));
    }

    @Test
    void testSuccessWith147WarehouseAndCategoriesAndVendorsAndTextQuery() {
        ArgumentCaptor<SearchBusinessOffersRequest> captor =
                ArgumentCaptor.forClass(SearchBusinessOffersRequest.class);
        when(saasService.searchBusinessOffers(any()))
                .thenReturn(SaasSearchResult.builder()

                        .setOffers(List.of(
                                SaasOfferInfo.newBuilder()
                                        .addOfferId("1")
                                        .build(),
                                SaasOfferInfo.newBuilder()
                                        .addOfferId("2")
                                        .build(),
                                SaasOfferInfo.newBuilder()
                                        .addOfferId("3")
                                        .build()
                        ))
                        .build());

        ByteArrayOutputStream output = new ByteArrayOutputStream(1000);
        ffSupplyGenerator.xlsFFSupplyReport(
                FFSupplyTemplateParams.newBuilder()
                        .withPartnerId(PARTNER_ID)
                        .withWarehouseId(147L)
                        .withCategoryIds(List.of(123, 456))
                        .withVendors(List.of("vendor1", "vendor2"))
                        .withTextQuery("поиск")
                        .build(), output);

        verify(dataCampShopClient).searchBusinessOffers(captor.capture());
        SearchBusinessOffersRequest request = captor.getValue();
        assertEquals(10103, request.getPartnerId());
        assertTrue(request.getOfferIds().containsAll(List.of("1", "2", "3")));
        // assertEquals(2, request.getMarketCategoryIds().size());
        // assertEquals(2, request.getVendors().size());
        // assertTrue(request.getVendors().containsAll(List.of("vendor1", "vendor2")));
        //assertEquals("поиск", request.getText());
        //assertTrue(request.getMarketCategoryIds().containsAll(List.of(123, 456)));
    }

    @Test
    @DisplayName("Десериализация параметров")
    void testFFSupplyTemplateParams() throws IOException {
        FFSupplyTemplateParams params = MAPPER.readValue(REQUEST_JSON, FFSupplyTemplateParams.class);
        assertEquals(10103, params.getPartnerId());
        assertEquals(172, params.getWarehouseId());
        assertEquals(2, params.getCategoryIds().size());
        assertEquals(2, params.getVendors().size());
        assertTrue(params.deficitOnly());
        assertEquals("поиск", params.getTextQuery());
        assertTrue(params.getVendors().containsAll(List.of("vendor1", "vendor2")));
        assertTrue(params.getCategoryIds().containsAll(List.of(123, 456)));
    }

    @Test
    @DisplayName("Cериализация параметров")
    void testFFSupplyTemplateParamsSerialize() throws IOException {
        String json = MAPPER.writeValueAsString(
                FFSupplyTemplateParams.newBuilder()
                        .withPartnerId(PARTNER_ID)
                        .withWarehouseId(172)
                        .withCategoryIds(List.of(123, 456))
                        .withVendors(List.of("vendor1", "vendor2"))
                        .withDeficitOnly(true)
                        .withTextQuery("поиск")
                        .build());
        JSONAssert.assertEquals(REQUEST_JSON, json, JSONCompareMode.LENIENT);
    }

    @Nonnull
    private RequestItemDTO generateRequestItem(String shopSku, List<String> barcodes, String comment,
                                               BigDecimal supplyPrice) {
        RequestItemDTO requestItemDTO = new RequestItemDTO();
        requestItemDTO.setArticle(shopSku);
        requestItemDTO.setBarcodes(barcodes);
        requestItemDTO.setComment(comment);
        requestItemDTO.setSupplyPrice(supplyPrice);
        requestItemDTO.setCount(10);
        return requestItemDTO;
    }
}
