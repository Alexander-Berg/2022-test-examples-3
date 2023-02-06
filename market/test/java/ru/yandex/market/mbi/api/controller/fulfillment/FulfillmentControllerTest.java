package ru.yandex.market.mbi.api.controller.fulfillment;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.RestTemplateFactory;
import ru.yandex.market.core.delivery.PartnerAndWarehouseInfoResult;
import ru.yandex.market.core.delivery.model.DeliveryWarehouseDTO;
import ru.yandex.market.core.delivery.model.DeliveryWarehouseDTOsContainer;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.mbi.api.client.entity.fulfillment.FulfillmentSupplierFilter;
import ru.yandex.market.mbi.api.client.entity.fulfillment.SupplierInfo;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinkDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinksDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static ru.yandex.market.core.campaign.model.CampaignType.SUPPLIER;

/**
 * Функциональные тесты на {@link ru.yandex.market.mbi.api.controller.FulfillmentController}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "FulfillmentControllerTest.before.csv")
class FulfillmentControllerTest extends FunctionalTest {
    private final SupplierInfo SUPPLIER_1000 = new SupplierInfo.Builder()
            .setId(1000).setName("supplier")
            .setOrganisationName("ООО supplier")
            .setPrepayRequestId(1L)
            .setSupplierType(SupplierType.FIRST_PARTY)
            .setDropship(true)
            .setGoodContentAllowed(true)
            .setNeedContentAllowed(true)
            .setFulfillment(true)
            .setCrossdock(false)
            .setClickAndCollect(false)
            .setDropshipBySeller(false)
            .build();

    private final SupplierInfo SUPPLIER_CROSSDOCK_1001 = new SupplierInfo.Builder()
            .setId(1001).setName("supplier crossdock")
            .setOrganisationName("ООО supplier crossdock")
            .setPrepayRequestId(2L)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setDropship(false)
            .setGoodContentAllowed(true)
            .setNeedContentAllowed(true)
            .setFulfillment(true)
            .setCrossdock(true)
            .setClickAndCollect(false)
            .setDropshipBySeller(false)
            .build();

    private final SupplierInfo SUPPLIER_CNC_1002 = new SupplierInfo.Builder()
            .setId(1002).setName("supplier CNC")
            .setOrganisationName("ООО supplier CNC")
            .setPrepayRequestId(3L)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setDropship(true)
            .setGoodContentAllowed(true)
            .setNeedContentAllowed(true)
            .setFulfillment(false)
            .setCrossdock(false)
            .setClickAndCollect(true)
            .setDropshipBySeller(false)
            .build();

    private final SupplierInfo SUPPLIER_1003 = new SupplierInfo.Builder()
            .setId(1003)
            .setName("supplier")
            .setOrganisationName("ООО supplier CNC")
            .setPrepayRequestId(4L)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setDropship(false)
            .setGoodContentAllowed(true)
            .setNeedContentAllowed(true)
            .setFulfillment(true)
            .setCrossdock(false)
            .setClickAndCollect(false)
            .setDropshipBySeller(false)
            .setBusinessId(10030L)
            .build();

    private final SupplierInfo SUPPLIER_REAL_1100 = new SupplierInfo.Builder()
            .setId(1100).setName("поставщик")
            .setSupplierType(SupplierType.REAL_SUPPLIER)
            .setRsId("000TST1")
            .setDropship(false)
            .setGoodContentAllowed(false)
            .setNeedContentAllowed(false)
            .setFulfillment(true)
            .setCrossdock(false)
            .setClickAndCollect(false)
            .setDropshipBySeller(false)
            .build();

    @Test
    void getSupplierInfoList() {

        List<SupplierInfo> supplierInfoList = mbiApiClient.getSupplierInfoList();
        assertThat(supplierInfoList, hasSize(4));
        assertThat(supplierInfoList, hasItem(samePropertyValuesAs(SUPPLIER_1000)));
        assertThat(supplierInfoList, hasItem(samePropertyValuesAs(SUPPLIER_CROSSDOCK_1001)));
        assertThat(supplierInfoList, hasItem(samePropertyValuesAs(SUPPLIER_CNC_1002)));

        RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
        String response = restTemplate.getForObject("http://localhost:" + port + "/fulfillment/suppliers",
                String.class);
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<supplier-info>\n" +
                        "   <supplier>\n" +
                        "      <suppliers id=\"1000\" name=\"supplier\" organisation-name=\"ООО supplier\" " +
                        "prepay-request-id=\"1\" supplier-type=\"1P\" " +
                        "                 dropship=\"true\" good-content-allowed=\"true\" " +
                        "need-content-allowed=\"true\" " +
                        "                 fulfillment=\"false\" crossdock=\"false\" click-and-collect=\"false\" " +
                        "dropship-by-seller=\"false\"/>\n" +
                        "      <suppliers id=\"1001\" name=\"supplier crossdock\" organisation-name=\"ООО supplier " +
                        "crossdock\" prepay-request-id=\"2\" supplier-type=\"3P\" " +
                        "                 dropship=\"false\" good-content-allowed=\"false\" " +
                        "need-content-allowed=\"false\" " +
                        "                 fulfillment=\"true\" crossdock=\"true\" click-and-collect=\"false\" " +
                        "dropship-by-seller=\"false\"/>\n" +
                        "      <suppliers id=\"1002\" name=\"supplier CNC\" organisation-name=\"ООО supplier CNC\" " +
                        "prepay-request-id=\"3\" supplier-type=\"3P\" " +
                        "                 dropship=\"true\" good-content-allowed=\"false\" " +
                        "need-content-allowed=\"false\" " +
                        "                 fulfillment=\"false\" crossdock=\"false\" click-and-collect=\"true\" " +
                        "dropship-by-seller=\"false\"/>\n" +
                        "      <suppliers id=\"1003\" name=\"supplier\" dropship=\"false\" fulfillment=\"true\" " +
                        "crossdock=\"false\" organisation-name=\"ООО supplier CNC\" prepay-request-id=\"4\" " +
                        "supplier-type=\"3P\" good-content-allowed=\"true\" need-content-allowed=\"true\" " +
                        "business-id=\"10030\" click-and-collect=\"false\" dropship-by-seller=\"false\"/>" +
                        "   </supplier>\n" +
                        "</supplier-info>",
                response
        );
    }

    @Test
    void getSupplierInfoListWithReal() {
        List<SupplierInfo> supplierInfoList = mbiApiClient.getSupplierInfoList(SupplierType.REAL_SUPPLIER);

        assertThat(supplierInfoList, hasSize(1));
        assertThat(supplierInfoList, hasItem(samePropertyValuesAs(SUPPLIER_REAL_1100)));
    }

    @Test
    void getSupplierInfoListP1P() {
        List<SupplierInfo> supplierInfoList = mbiApiClient.getSupplierInfoList(SupplierType.THIRD_PARTY,
                SupplierType.FIRST_PARTY);

        assertThat(supplierInfoList, hasSize(4));
        assertThat(supplierInfoList, hasItem(samePropertyValuesAs(SUPPLIER_1000)));
    }

    @Test
    void getSupplierInfoListAll() {
        List<SupplierInfo> supplierInfoList = mbiApiClient.getSupplierInfoList(
                SupplierType.THIRD_PARTY, SupplierType.FIRST_PARTY, SupplierType.REAL_SUPPLIER);

        assertThat(supplierInfoList, hasSize(5));
        assertThat(supplierInfoList, hasItem(samePropertyValuesAs(SUPPLIER_1000)));
        assertThat(supplierInfoList, hasItem(samePropertyValuesAs(SUPPLIER_REAL_1100)));
    }

    @Test
    void getFFSupplierInfo() {
        SupplierInfo supplierInfo = mbiApiClient.getFulfillmentSupplierInfo(1003);

        assertReflectionEquals(supplierInfo, SUPPLIER_1003);
    }

    @Test
    void getSupplierInfoListByFilter() {
        FulfillmentSupplierFilter filter = new FulfillmentSupplierFilter.Builder()
                .setSupplierIds(List.of(1001L, 1003L))
                .build();

        List<SupplierInfo> supplierInfoList = mbiApiClient.getSupplierInfoListByFilter(filter);

        assertThat(supplierInfoList, hasSize(2));
        assertThat(supplierInfoList, hasItem(samePropertyValuesAs(SUPPLIER_CROSSDOCK_1001)));
        assertThat(supplierInfoList, hasItem(samePropertyValuesAs(SUPPLIER_1003)));
    }

    @Test
    void partnerInfo() {
        PartnerAndWarehouseInfoResult result =
                new PartnerAndWarehouseInfoResult(101, 1000, SUPPLIER,
                        Collections.singletonList(1L));

        List<DeliveryWarehouseDTO> dtos = List.of(new DeliveryWarehouseDTO(101, 1000));

        List<PartnerAndWarehouseInfoResult> resultDtos =
                mbiApiClient.getPartnerInfos(new DeliveryWarehouseDTOsContainer(dtos));

        assertReflectionEquals(List.of(result), resultDtos, ReflectionComparatorMode.LENIENT_ORDER);
    }

    @Test
    void getPartnerLinks() {
        PartnerFulfillmentLinksDTO partnerLinks = mbiApiClient.getPartnerLinks(List.of(101L, 102L));
        Collection<PartnerFulfillmentLinkDTO> links = partnerLinks.getPartnerFulfillmentLinks();
        assertEquals(1, links.size());
        assertEquals(101,
                links
                        .stream()
                        .filter(link -> link.getPartnerId() == 1000)
                        .map(PartnerFulfillmentLinkDTO::getServiceId)
                        .findFirst()
                        .orElse(null));
    }

}
