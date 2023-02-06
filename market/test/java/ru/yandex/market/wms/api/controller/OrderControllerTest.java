package ru.yandex.market.wms.api.controller;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetup(value = "/shipment-order/before/setup.xml")
@SpringBootTest(
        classes = {IntegrationTestConfig.class},
        properties = {"warehouse-timezone = Asia/Yekaterinburg"}
)
public class OrderControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/after/after-created-orders.xml")
    @ExpectedDatabase(value = "/shipment-order/after/after-created-orders.xml", assertionMode = NON_STRICT_UNORDERED)
    public void updateOrderTestHappyPath() throws Exception {
        createOrder(
                "shipment-order/request/update-order.json",
                "shipment-order/response/updated-order.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/before/before-cancel-order.xml")
    @ExpectedDatabase(value = "/shipment-order/after/after-cancel-order-1.xml", assertionMode = NON_STRICT_UNORDERED)
    public void cancelOrderHappyPathWhenOneOrder() throws Exception {
        String externOrderKey = "22899370";
        mockMvc.perform(post("/ENTERPRISE/shipments/external-order-key/" + externOrderKey + "/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/before/before-cancel-order.xml")
    @ExpectedDatabase(value = "/shipment-order/after/after-cancel-order-2.xml", assertionMode = NON_STRICT_UNORDERED)
    public void cancelOrderHappyPathWhenTwoOrders() throws Exception {
        String externOrderKey = "22899375";
        mockMvc.perform(post("/ENTERPRISE/shipments/external-order-key/" + externOrderKey + "/cancel"))
                .andExpect(status().isOk());
    }

    //todo remove when MARKETWMS-7648 will be merged
    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/before/before-cancel-order.xml")
    @ExpectedDatabase(value = "/shipment-order/after/after-cancel-order-1.xml", assertionMode = NON_STRICT_UNORDERED)
    public void cancelOrderHappyPath() throws Exception {
        mockMvc.perform(post("/ENTERPRISE/shipments/0000039466/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/before/before-cancel-order.xml")
    @ExpectedDatabase(value = "/shipment-order/before/before-cancel-order.xml", assertionMode = NON_STRICT_UNORDERED)
    public void cancelOrderWithWrongExternOrderKey() throws Exception {
        mockMvc.perform(post("/ENTERPRISE/shipments/22899371/cancel"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"22899370", "22899375", "22899385"})
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/before/before-cancel-order-with-record-in-pickdetail.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-cancel-order-with-record-in-pickdetail.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void cancelOrderThrowsCannonCancel(String externOrderKey) throws Exception {
        MvcResult mvcResult = mockMvc
                .perform(post("/ENTERPRISE/shipments/external-order-key/" + externOrderKey + "/cancel"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("Cannot cancel this Order.");
    }

    //todo remove when MARKETWMS-7648 will be merged
    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/before/before-cancel-order-with-record-in-pickdetail.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-cancel-order-with-record-in-pickdetail.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void cancelOrderThrowsCannonCancel() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/shipments/0000039466/cancel"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("Cannot cancel this Order.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"22899370", "22899375", "23899375", "23909375"})
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/before/before-cancel-order-wrong-statuses.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-cancel-order-wrong-statuses.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void cancelOrderIsNotCancelable(String externOrderKey) throws Exception {
        MvcResult mvcResult = mockMvc
                .perform(post("/ENTERPRISE/shipments/external-order-key/" + externOrderKey + "/cancel"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("Order is not cancelable.");
    }

    //todo remove when MARKETWMS-7648 will be merged
    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/before/before-cancel-order-wrong-statuses.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-cancel-order-wrong-statuses.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void cancelOrderIsNotCancelable() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/shipments/0000039466/cancel"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("Order is not cancelable.");
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @ExpectedDatabase(value = "/shipment-order/before/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createOrderTestIncorrectRequest() throws Exception {
        mockMvc.perform(post("/ENTERPRISE/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @ExpectedDatabase(value = "/shipment-order/before/common.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createOrderTestWithNotExistingStorer() throws Exception {
        createOrder(
                "shipment-order/request/create-order.json",
                "shipment-order/response/create-order-with-not-existing-storer.json",
                Collections.emptyList(),
                status().isBadRequest()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer.xml")
    @ExpectedDatabase(value = "/shipment-order/before/before-with-storer.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createOrderTestGetItemBatchWithoutExistingCarrier() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("shipment-order/request/create-order.json")))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("Storer/Sku is not valid");
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createOrderTestGetItemBatchWithoutExistingSku() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("shipment-order/request/create-order.json")))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("Storer/Sku is not valid");
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-one-sku.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier-one-sku.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createOrderTestGetItemBatchWithoutExistingOneSku() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("shipment-order/request/create-order.json")))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("Storer/Sku is not valid");
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier-all-sku.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(value = "/shipment-order/after/after-created-orders.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createOrderTestHappyPath() throws Exception {
        createOrder(
                "shipment-order/request/create-order.json",
                "shipment-order/response/created-order.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier-all-sku.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(value = "/shipment-order/after/after-created-orders-anomaly-withdrawal.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void createOrderTestAnomalyWithdrawal() throws Exception {
        createOrder(
                "shipment-order/request/create-order-anomaly-withdrawal.json",
                "shipment-order/response/created-order-anomaly-withdrawal.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-nonsort-sku.xml")
    @ExpectedDatabase(
            value = "/shipment-order/after/after-created-order-ignore-nonsort.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createOrderIgnoreNonSort() throws Exception {
        createOrder(
                "shipment-order/request/create-order-ignore-nonsort.json",
                "shipment-order/response/create-order-ignore-nonsort.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-force-nonsort.xml")
    @ExpectedDatabase(
            value = "/shipment-order/after/after-created-order-force-nonsort.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createOrderForceNonSort() throws Exception {
        createOrder(
                "shipment-order/request/create-order-force-nonsort.json",
                "shipment-order/response/create-order-force-nonsort.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-nonsort-sku.xml")
    @ExpectedDatabase(
            value = "/shipment-order/after/after-created-order-nonsort-height.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createNonSortOrderHeightExceeded() throws Exception {
        createOrder(
                "shipment-order/request/create-order-nonsort-height.json",
                "shipment-order/response/create-order-nonsort-height.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-nonsort-sku.xml")
    @ExpectedDatabase(
            value = "/shipment-order/after/after-created-order-nonsort-width.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createNonSortOrderWidthExceeded() throws Exception {
        createOrder(
                "shipment-order/request/create-order-nonsort-width.json",
                "shipment-order/response/create-order-nonsort-width.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-nonsort-sku.xml")
    @ExpectedDatabase(
            value = "/shipment-order/after/after-created-order-nonsort-length.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createNonSortOrderLengthExceeded() throws Exception {
        createOrder(
                "shipment-order/request/create-order-nonsort-length.json",
                "shipment-order/response/create-order-nonsort-length.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-nonsort-sku.xml")
    @ExpectedDatabase(
            value = "/shipment-order/after/after-created-order-nonsort-weight.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createNonSortOrderWeightExceeded() throws Exception {
        createOrder(
                "shipment-order/request/create-order-nonsort-weight.json",
                "shipment-order/response/create-order-nonsort-weight.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    // в заказе конв товары - заказ конв
    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/split-enabled/before.xml")
    @ExpectedDatabase(
            value = "/shipment-order/after/split-enabled/create-order-conv.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createConvOrderWhenSplitEnabled() throws Exception {
        createOrder(
                "shipment-order/request/split-enabled/create-order-conv.json",
                "shipment-order/response/split-enabled/create-order-conv.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    // в заказе конв и нонконв товары - заказ нонконв
    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup(value = "/shipment-order/before/split-enabled/before.xml", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/shipment-order/after/split-enabled/create-order-conv-nonconv.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createConvAndNonConvOrderWhenSplitEnabled() throws Exception {
        createOrder(
                "shipment-order/request/split-enabled/create-order-conv-nonconv.json",
                "shipment-order/response/split-enabled/create-order-conv-nonconv.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    // в заказе КГТ товары - заказ КГТ
    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup(value = "/shipment-order/before/split-enabled/before.xml", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/shipment-order/after/split-enabled/create-order-oversize.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createOversizeOrderWhenSplitEnabled() throws Exception {
        createOrder(
                "shipment-order/request/split-enabled/create-order-oversize.json",
                "shipment-order/response/split-enabled/create-order-oversize.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    // в заказе конв, нонконв и КГТ товары - заказ разделяется на КГТ и конв + нонконв
    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup(value = "/shipment-order/before/split-enabled/before.xml", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/shipment-order/after/split-enabled/create-order-conv-nonconv-oversize.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createConvAndNonConvAndOversizeOrderWhenSplitEnabled() throws Exception {
        createOrder(
                "shipment-order/request/split-enabled/create-order-conv-nonconv-oversize.json",
                "shipment-order/response/split-enabled/create-order-conv-nonconv-oversize.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
        getOrderByOriginOrderKey("0000039466", "split-enabled/created-order-conv-nonconv-oversize");
    }

    // в заказе конв, нонконв и КГТ товары, но СД однопосылочная - заказ не разделяется
    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup(value = "/shipment-order/before/split-enabled/before.xml", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/shipment-order/after/split-enabled/create-order-conv-nonconv-oversize-ds-sp.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createConvAndNonConvAndOversizeOrderWhenSplitEnabledButDsSingleParcel() throws Exception {
        createOrder(
                "shipment-order/request/split-enabled/create-order-conv-nonconv-oversize-ds-sp.json",
                "shipment-order/response/split-enabled/create-order-conv-nonconv-oversize-ds-sp.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
        getOrderByOriginOrderKey("0000039466", "split-enabled/created-order-conv-nonconv-oversize-ds-sp");
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier-all-sku.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
            value = "/shipment-order/after/after-created-loadtest-orders.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createOrderTestLoadTestOrder() throws Exception {
        createOrder(
                "shipment-order/request/create-loadtest-order.json",
                "shipment-order/response/created-loadtest-order.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier.xml")
    @ExpectedDatabase(value = "/shipment-order/after/after-put-outbound.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createOrderTestPutOutboundSuccess() throws Exception {
        createOrder(
                "shipment-order/request/put-outbound.json",
                "shipment-order/response/put-outbound.json",
                Collections.emptyList(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier.xml")
    @DatabaseSetup("/shipment-order/after/after-put-outbound.xml")
    @ExpectedDatabase(
            value = "/shipment-order/after/after-update-put-outbound-not-recreate.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createOrderTestUpdatePutOutboundNotRecreateTypeSuccess() throws Exception {
        createOrder(
                "shipment-order/request/update-put-outbound-not-recreate-type.json",
                "shipment-order/response/updated-put-outbound-not-recreate-type.json",
                Collections.emptyList(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier.xml")
    @DatabaseSetup("/shipment-order/after/before-update-put-outbound.xml")
    @ExpectedDatabase(
            value = "/shipment-order/after/after-update-put-outbound.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createOrderTestUpdatePutOutboundSuccess() throws Exception {
        createOrder(
                "shipment-order/request/update-put-outbound.json",
                "shipment-order/response/updated-put-outbound.json",
                Collections.emptyList(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier.xml")
    @DatabaseSetup("/shipment-order/before/before-outbound-with-details.xml")
    @ExpectedDatabase(
            value = "/shipment-order/after/after-update-outbound-with-details-by-put-outbound.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createOrderTestUpdateOutboundWithDetailsByPutOutboundSuccess() throws Exception {
        createOrder(
                "shipment-order/request/update-outbound-with-details-put-outbound.json",
                "shipment-order/response/updated-outbound-with-details-put-outbound.json",
                Collections.emptyList(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/map-carrier/common.xml")
    @DatabaseSetup("/map-carrier/1/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabase(value = "/map-carrier/1/after-created-orders.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createOrderTestPutMappedOrderHappyPath() throws Exception {
        createOrder(
                "map-carrier/1/create-order-request.json",
                "map-carrier/1/created-order-response.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/map-carrier/common.xml")
    @DatabaseSetup("/map-carrier/3/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabase(value = "/map-carrier/3/after-created-orders.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createOrderTestPutMappedOrderOutboundAutoHappyPath() throws Exception {
        createOrder(
                "map-carrier/3/create-order-request.json",
                "map-carrier/3/created-order-response.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-3-flow-types.xml")
    @ExpectedDatabase(value = "/shipment-order/before/before-with-3-flow-types.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/shipment-order/after/after-created-3-flow-types.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void postOrderWith3FlowTypes() throws Exception {
        createOrder("shipment-order/request/create-order.json",
                "shipment-order/response/created-order-3-flow-types.json",
                getOrderIgnoreFields(),
                status().isOk());
    }

    @Test
    @DatabaseSetup("/map-carrier/common.xml")
    @DatabaseSetup("/map-carrier/2/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabase(value = "/map-carrier/2/after-created-orders.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createOrderTestStorerTypeShouldBeConsidered() throws Exception {
        createOrder(
                "map-carrier/2/create-order-request.json",
                "map-carrier/2/created-order-response.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier-all-sku.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(value = "/shipment-order/after/after-created-orders.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createOrderTestWithShipmentDateTime() throws Exception {
        createOrder(
                "shipment-order/request/create-order-with-shipmentdatetime.json",
                "shipment-order/response/created-order-with-shipmentdatetime.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/before/shipmentdatetime-null-scenario.xml")
    @ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier-all-sku.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(value = "/shipment-order/after/after-created-orders.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createOrderTestWithShipmentDateTimeIfNull() throws Exception {
        createOrder(
                "shipment-order/request/create-order.json",
                "shipment-order/response/created-order-with-shipmentdatetime-2.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup("/shipment-order/before/before-create-outbound-auction-order.xml")
    @ExpectedDatabase(value = "/shipment-order/after/after-create-outbound-auction-order.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void createOutboundAuctionOrderHappyPath() throws Exception {
        createOrder(
                "shipment-order/request/create-outbound-auction-order.json",
                "shipment-order/response/create-outbound-auction-order.json",
                getOrderIgnoreFields(),
                status().isOk()
        );
    }

    @Test
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @DatabaseSetup("/shipment-order/before/before-get-order.xml")
    public void getOrderByExternalOrderKeyTestSuccess() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders
                        .get("/ENTERPRISE/shipments/external-order-key/22899370")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        JsonAssertUtils
                .assertFileNonExtensibleEquals("shipment-order/response/get-order-response.json",
                        mvcResult.getResponse().getContentAsString());
    }

    @Test
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    public void getOrderByExternalOrderKeyTestNoOrders() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders
                        .get("/ENTERPRISE/shipments/external-order-key/10101010")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        Assertions.assertEquals("", mvcResult.getResponse().getContentAsString());
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup(value = "/shipment-order/after/after-order-with-multi-items.xml", type = DatabaseOperation.INSERT)
    public void getOrderByOriginOrderKeyTestWithMultiItemsBomsAndPack() throws Exception {
        String originOrderKey = "0000001057";
        String orderKey = "0000001047";

        MvcResult mvcResult =
                mockMvc.perform(get("/ENTERPRISE/shipments/" + originOrderKey)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn();
        JsonAssertUtils.assertFileEquals("get-order/response/" + orderKey + ".json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup(value = "/shipment-order/after/after-order-with-bom.xml", type = DatabaseOperation.INSERT)
    public void getOrderByOriginOrderKeyTestWithBom() throws Exception {
        String originOrderKey = "0000010879";
        String orderKey = "0000000879";

        MvcResult mvcResult =
                mockMvc.perform(get("/ENTERPRISE/shipments/" + originOrderKey)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn();
        JsonAssertUtils.assertFileEquals("get-order/response/" + orderKey + ".json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }

    @Test
    @DatabaseSetup(value = "/shipment-order/after/after-order-with-bom.xml", type = DatabaseOperation.INSERT)
    public void getOrderByOriginOrderKeyTestWhenIdNotFound() throws Exception {
        mockMvc.perform(get("/ENTERPRISE/shipments/fakeid").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Order not found: fakeid")));
    }

    @Test
    public void getOrderByOriginOrderKeyTest404WhenIdNotFound() throws Exception {
        mockMvc.perform(get("/ENTERPRISE/shipments/fakeid").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Order not found: fakeid")));
    }

    @Test
    @DatabaseSetup(value = "/shipment-order/before/fake-parcel/settings.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(
            value = "/shipment-order/before/fake-parcel/shipped-cancelled-not-picked.xml",
            type = DatabaseOperation.INSERT
    )
    public void getOrderWithNonsortSippedCancelledAndNotPickedWithFake() throws Exception {
        getOrderByOriginOrderKey("0018682523", "fake/real-and-fake-shipped-cancelled-not-picked");
    }

    @Test
    @DatabaseSetup(value = "/shipment-order/before/fake-parcel/settings.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(
            value = "/shipment-order/before/fake-parcel/shipped-cancelled-not-picked-with-stretch.xml",
            type = DatabaseOperation.INSERT
    )
    public void getOrderWithNonsortSippedCancelledAndNotPickedWithFakeWithStretch() throws Exception {
        getOrderByOriginOrderKey("0018682523", "fake/real-and-fake-shipped-cancelled-not-picked");
    }

    @Test
    @DatabaseSetup(value = "/shipment-order/before/fake-parcel/settings.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(
            value = "/shipment-order/before/fake-parcel/one-detail-not-picked.xml",
            type = DatabaseOperation.INSERT
    )
    public void getOrderByOriginOrderKeyTestOrderTwoItemsInDetailNotPickedWithFake() throws Exception {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01");
    }

    @Test
    @DatabaseSetup(
            value = "/shipment-order/before/fake-parcel/one-detail-not-picked.xml",
            type = DatabaseOperation.INSERT
    )
    public void getOrderByOriginOrderKeyTestOrderTwoItemsInDetailNotPickedWithoutFake() throws Exception {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01-NO-FAKE");
    }

    @Test
    @DatabaseSetup(value = "/shipment-order/before/fake-parcel/settings.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(
            value = "/shipment-order/before/fake-parcel/one-detail-partial-packed.xml",
            type = DatabaseOperation.INSERT
    )
    public void getOrderByOriginOrderKeyTestOrderTwoItemsInDetailPartialPackedWithFake() throws Exception {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01-PARTIAL");
    }

    @Test
    @DatabaseSetup(
            value = "/shipment-order/before/fake-parcel/one-detail-partial-packed.xml",
            type = DatabaseOperation.INSERT
    )
    public void getOrderByOriginOrderKeyTestOrderTwoItemsInDetailPartialPackedWithNoFake() throws Exception {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01-PARTIAL-NO-FAKE");
    }

    @Test
    @DatabaseSetup(
            value = "/shipment-order/before/fake-parcel/one-detail-partial-canceled.xml",
            type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(value = "/shipment-order/before/fake-parcel/settings.xml", type = DatabaseOperation.INSERT)
    public void getOrderByOriginOrderKeyTestOrderTwoItemsWithAdjustedQty() throws Exception {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01-PARTIAL-CANCELED");
    }

    @Test
    @DatabaseSetup(value = "/shipment-order/before/fake-parcel/no-picks.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/shipment-order/before/fake-parcel/settings.xml", type = DatabaseOperation.INSERT)
    public void getOrderByOriginOrderKeyTestOrderTwoItemsNoPicksFakeFlagEnabled() throws Exception {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01-NO-PICKS");
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @DatabaseSetup(value = "/shipment-order/after/after-order-with-identities.xml", type = DatabaseOperation.INSERT)
    public void getOrderByOriginOrderKeyTestWithIdentities() throws Exception {
        String originOrderKey = "0000002048";
        String orderKey = "0000001048";

        MvcResult mvcResult =
                mockMvc.perform(get("/ENTERPRISE/shipments/" + originOrderKey)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn();
        JsonAssertUtils.assertFileEquals("get-order/response/" + orderKey + ".json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }

    @Test
    @DatabaseSetup(
            value = "/shipment-order/after/after-order-simple-with-pack-and-pallet.xml",
            type = DatabaseOperation.INSERT
    )
    public void getOrderByOriginOrderKeyTestWithPackAndPallet() throws Exception {
        String originOrderKey = "0000081223";
        String orderKey = "0000081213";

        MvcResult mvcResult =
                mockMvc.perform(get("/ENTERPRISE/shipments/" + originOrderKey)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn();
        JsonAssertUtils.assertFileEquals("get-order/response/" + orderKey + ".json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }

    @Test
    @DatabaseSetup(value = "/shipment-order/after/after-order-simple-with-pack.xml", type = DatabaseOperation.INSERT)
    public void getOrderByOriginOrderKeyTestWithPack() throws Exception {
        String originOrderKey = "0000081113";
        String orderKey = "0000000410";

        MvcResult mvcResult =
                mockMvc.perform(get("/ENTERPRISE/shipments/" + originOrderKey)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn();
        JsonAssertUtils.assertFileEquals("get-order/response/" + orderKey + ".json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }

    @Test
    @DatabaseSetup(value = "/shipment-order/before/before-with-pack-61.xml", type = DatabaseOperation.INSERT)
    public void getOrderByOriginOrderKeyTestWithPack61() throws Exception {
        String originOrderKey = "0000081113";
        String orderKey = "0000000461";

        MvcResult mvcResult =
                mockMvc.perform(get("/ENTERPRISE/shipments/" + originOrderKey)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn();
        JsonAssertUtils.assertFileEquals("get-order/response/0000000461.json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }

    @Test
    @DatabaseSetup(value = "/shipment-order/before/before-with-pack-cancelled.xml", type = DatabaseOperation.INSERT)
    public void getOrderByOriginOrderKeyTestWithPackWhenCancelled() throws Exception {
        String originOrderKey = "0000081113";
        String orderKey = "0000000461";

        MvcResult mvcResult =
                mockMvc.perform(get("/ENTERPRISE/shipments/" + originOrderKey)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn();
        JsonAssertUtils.assertFileEquals("get-order/response/0000000461-cancelled.json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }

    @Test
    @DatabaseSetup(value = "/shipment-order/after/after-order-simple-without-pack.xml", type = DatabaseOperation.INSERT)
    public void getOrderByOriginOrderKeyTestWithoutPack() throws Exception {
        String originOrderKey = "0000010881";
        String orderKey = "0000000881";

        MvcResult mvcResult =
                mockMvc.perform(get("/ENTERPRISE/shipments/" + originOrderKey)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn();
        JsonAssertUtils.assertFileEquals("get-order/response/" + orderKey + ".json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }

    private void getOrderByOriginOrderKey(String originOrderKey, String response) throws Exception {
        MvcResult mvcResult =
                mockMvc.perform(get("/ENTERPRISE/shipments/" + originOrderKey).accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                        .andReturn();
        JsonAssertUtils.assertFileEquals("shipment-order/response/" + response + ".json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }

    private void createOrder(String requestFileName,
                             String responseFileName,
                             List<String> ignoreFields,
                             ResultMatcher status) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFileName)))
                .andExpect(status)
                .andReturn();
        JsonAssertUtils.assertFileNonExtensibleEquals(responseFileName,
                mvcResult.getResponse().getContentAsString(),
                ignoreFields);
    }

    private List<String> getOrderIgnoreFields() {
        return List.of(
                "ordersid",
                "orderdetails[**].orderdetailid"
        );
    }
}
