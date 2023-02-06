package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.dto.CreateRegistryDTO;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.entity.LogisticUnit;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.repository.LogisticUnitRepository;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.service.EnvironmentParamService;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Функциональный тест для метода submitRegistryRequest из {@link RequestUploadController}.
 */
@SuppressWarnings("AnnotationUseStyle")
@DatabaseSetup("classpath:controller/upload-request-registry/before.xml")
class RequestUploadRegistryControllerTest extends MvcIntegrationTest {

    private static final String FILE_URL = "http://localhost:8080/file";

    @Autowired
    private EnvironmentParamService environmentParamService;

    @Autowired
    @Qualifier(value = "jsonMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private RequestItemRepository requestItemRepository;

    @Autowired
    private LogisticUnitRepository logisticUnitRepository;

    @BeforeEach
    void init() throws MalformedURLException {
        doNothing().when(mdsFfwfS3Client).upload(any(), any());
        when(mdsFfwfS3Client.getUrl(any())).thenReturn(new URL(FILE_URL));
        environmentParamService.clearCache();
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithoutRequiredField() throws Exception {
        String data = getJsonFromFile("registry-without-required-field.json");

        MvcResult mvcResult = doPostReturnSupplyNoFile(data)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
            equalTo("{\"message\":\"type must not be null\"}"));
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithoutMetaForItem() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-without-meta-for-item.json",
            "{\"message\":\"There are no article or count for some items\"," +
                "\"type\":\"REQUIRED_FIELDS_NOT_PROVIDED_FOR_ITEM\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithoutRequiredFieldInMetaForItem() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-without-required-field-in-meta-for-item.json",
            "{\"message\":\"There are no article or count for some items\"," +
                "\"type\":\"REQUIRED_FIELDS_NOT_PROVIDED_FOR_ITEM\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyForInvalidDate() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-for-invalid-date.json",
            "{\"message\":\"Request date must be after 2018-01-01T07:10:10Z. " +
                    "Current date: 2017-12-31T17:00:00Z\",\"type\":\"INVALID_REQUEST_DATE\"}");
    }

    @Test
    void submitReturnSupplyForInvalidDateWithIgnore() throws Exception {
        String data = getJsonFromFile("registry-for-invalid-date-with-ignore.json");

        doPostReturnSupplyNoFile(data)
                .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithPalletWithoutPalletId() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-with-pallet-without-pallet-id.json",
            "{\"message\":\"There are some pallets without pallet id\"," +
                "\"type\":\"NO_PALLET_ID_FOR_PALLET\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithBoxWithoutBoxId() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-with-box-without-box-id.json",
            "{\"message\":\"There are some boxes without box id\"," +
                "\"type\":\"NO_BOX_ID_FOR_BOX\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithPalletInPallet() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-with-pallet-in-pallet.json",
            "{\"message\":\"There is pallet inside of pallet in request\",\"type\":\"PALLET_INSIDE_OF_PALLET\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithBoxInBox() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-with-box-in-box.json",
            "{\"message\":\"There is pallet or box inside of box in request\"," +
                "\"type\":\"PALLET_OR_BOX_INSIDE_OF_BOX\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithLogisticUnitInsideOfItem() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-item-inside-item.json",
            "{\"message\":\"There are some logistic units inside of item\"," +
                "\"type\":\"LOGISTIC_UNIT_INSIDE_OF_ITEM\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithMoreThanOneOrderIdForRequestItem() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-with-more-than-one-order-id-for-item.json",
            "{\"message\":\"There are some request items with more than one order id\"," +
                "\"type\":\"MORE_THAN_ONE_ORDER_ID_FOR_REQUEST_ITEM\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithoutBoxesInOrder() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-without-boxesInOrder.json",
            "{\"message\":\"There are some orders without boxesInOrder\"," +
                "\"type\":\"NO_BOXES_IN_ORDER_FOR_ORDER\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithBoxesInOrderForBoxWithTwoOrders() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-with-boxes-in-order-for-box-with-two-orders.json",
            "{\"message\":\"There are some logistic units with boxesInOrder specified and incorrect " +
                "orders count (0 or more than 1)\",\"type\":" +
                "\"BOXES_IN_ORDER_FOR_LOGISTIC_UNIT_WITH_INCORRECT_ORDER_COUNT_VALIDATION_STRATEGY\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithBoxesInOrderForUnitWithoutOrders() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-with-boxes-in-order-for-box-without-orders.json",
            "{\"message\":\"There are some logistic units with boxesInOrder specified and incorrect " +
                "orders count (0 or more than 1)\",\"type\":" +
                "\"BOXES_IN_ORDER_FOR_LOGISTIC_UNIT_WITH_INCORRECT_ORDER_COUNT_VALIDATION_STRATEGY\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithItemWithoutSourceFulfillmentId() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-with-item-without-sourceFulfillmentId.json",
            "{\"message\":\"There are some items without sourceFulfillmentId\"," +
                "\"type\":\"NO_SOURCE_FULFILLMENT_ID_FOR_ITEM\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithItemWithoutIds() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-with-item-without-ids.json",
                "{\"message\":\"There are some logistic unit without ids\"," +
                        "\"type\":\"NO_IDS_FOR_LOGISTIC_UNIT\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithItemWithoutSupplierId() throws Exception {
        assertCreateRegistryErrorForAnyReturnSupplyType("registry-with-item-without-supplier-id.json",
                "{\"message\":\"There are some items without supplier id\"," +
                        "\"type\":\"NO_SUPPLIER_ID_FOR_ITEM\"}");
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitReturnSupplyWithItemWithNotExistingSupplierId() throws Exception {
        String data = getJsonFromFile("registry-with-item-with-not-existing-supplier-id.json");
        CreateRegistryDTO dto = objectMapper.readValue(data, CreateRegistryDTO.class);
        Set<RequestType> requestTypes =
                EnumSet.of(RequestType.VALID_UNREDEEMED, RequestType.INVALID_UNREDEEMED, RequestType.CUSTOMER_RETURN);
        for (RequestType requestType : requestTypes) {
            dto.setType(requestType);
            MvcResult mvcResult = doPostReturnSupplyNoFile(objectMapper.writeValueAsString(dto))
                    .andExpect(status().isNotFound())
                    .andReturn();

            assertThat(mvcResult.getResponse().getContentAsString(),
                    equalTo("{\"message\":\"Failed to find [SUPPLIER] with id [20]\"," +
                            "\"resourceType\":\"SUPPLIER\",\"identifier\":\"20\"}"));
        }
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitUnredeemedRegistryWithoutOrderIdForBoxId() throws Exception {
        assertCreateRegistryErrorForAnyType("registry-with-box-without-order-id.json",
            "{\"message\":\"There are some boxes without order id\",\"type\":\"NO_ORDER_ID_FOR_BOX\"}",
            EnumSet.of(RequestType.VALID_UNREDEEMED, RequestType.INVALID_UNREDEEMED));
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/before.xml", assertionMode = NON_STRICT)
    void submitUnredeemedRegistryWithoutItems() throws Exception {
        assertCreateRegistryErrorForAnyType("registry-without-items.json",
            "{\"message\":\"There are no items in registry\",\"type\":\"NO_ITEMS_IN_REGISTRY\"}",
            EnumSet.of(RequestType.VALID_UNREDEEMED, RequestType.INVALID_UNREDEEMED));
    }

    private void assertCreateRegistryErrorForAnyReturnSupplyType(String inputFile,
                                                                 String errorMessage) throws Exception {
        assertCreateRegistryErrorForAnyType(inputFile, errorMessage,
            EnumSet.of(RequestType.VALID_UNREDEEMED, RequestType.INVALID_UNREDEEMED, RequestType.CUSTOMER_RETURN));
    }

    private void assertCreateRegistryErrorForAnyType(String inputFile, String errorMessage,
                                                     EnumSet<RequestType> types) throws Exception {
        String data = getJsonFromFile(inputFile);
        CreateRegistryDTO dto = objectMapper.readValue(data, CreateRegistryDTO.class);
        for (RequestType requestType : types) {
            dto.setType(requestType);
            MvcResult mvcResult = doPostReturnSupplyNoFile(objectMapper.writeValueAsString(dto))
                .andExpect(status().isBadRequest())
                .andReturn();

            assertThat(mvcResult.getResponse().getContentAsString(), equalTo(errorMessage));
        }
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/" +
        "after-submit-correct-return-registry-without-order-id.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void submitCustomerReturnWithoutOrderIdForBoxId() throws Exception {
        String data = getJsonFromFile("registry-with-box-without-order-id.json");

        doPostReturnSupplyNoFile(data)
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/" +
            "after-submit-correct-return-registry-with-items-on-high-level.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void submitReturnRegistryWithItemsOnHighLevel() throws Exception {
        String data = getJsonFromFile("registry-with-items-on-high-level.json");

        doPostReturnSupplyNoFile(data)
                .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/" +
        "after-submit-correct-customer-return-registry-without-max-receipt-date-for-box.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void submitCustomerReturnWithoutMaxReceiptDateForBox() throws Exception {
        String data = getJsonFromFile("registry-without-max-receipt-date-for-box.json");

        doPostReturnSupplyNoFile(data)
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/" +
        "after-submit-correct-return-registry-without-items.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void submitCustomerReturnWithoutItems() throws Exception {
        String data = getJsonFromFile("registry-without-items.json");

        doPostReturnSupplyNoFile(data)
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/" +
        "after-submit-correct-registry-with-same-items-in-different-json-parts.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void submitCorrectReturnSupplyWithTwoSameItemsOnTwoSameBoxesButInDifferentJsonParts() throws Exception {
        String data =
            getJsonFromFile("registry-submit-correct-with-two-same-items-in-different-json-parts.json");

        MvcResult mvcResult = doPostReturnSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("registry-create-response.json", mvcResult);
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/" +
        "after-submit-correct-unredeemed-registry-with-incorrect-box.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void submitCorrectUnredeemedWithIncorrectBox() throws Exception {
        String data =
            getJsonFromFile("registry-submit-correct-unredeemed-with-incorrect-box.json");

        doPostReturnSupplyNoFile(data)
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/" +
        "after-submit-correct-customer-return-registry-with-incorrect-box.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void submitCorrectCustomerReturnWithIncorrectBox() throws Exception {
        String data = getJsonFromFile(
            "registry-submit-correct-customer-return-with-incorrect-box.json");

        doPostReturnSupplyNoFile(data)
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/" +
        "after-submit-correct-registry-with-correct-and-incorrect-item-in-same-order.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void submitCorrectReturnSupplyWithCorrectAndIncorrectItemInSameOrderAndInDifferentBoxes() throws Exception {
        String data =
            getJsonFromFile("registry-submit-correct-with-correct-and-incorrect-item-in-same-order.json");

        doPostReturnSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/" +
            "after-submit-correct-registry-with-different-meta-for-two-same-items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void submitCorrectReturnSupplyWithDifferentMetaForTwoSameItems() throws Exception {
        String data =
                getJsonFromFile("registry-submit-correct-with-different-meta-for-two-same-items.json");

        doPostReturnSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/" +
            "after-submit-correct-registry-with-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void submitCorrectReturnSupplyWithCis() throws Exception {
        String data = getJsonFromFile("registry-with-cis.json");

        doPostReturnSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Тест проверяет отсутствие N + 1 при сохранении большого количества айтемов и грузомест в поставке.
     * В случае, если каждый айтем и грузоместо сохраняется одним запросом в БД, количество запросов было бы 36.
     */
    @Test
    @JpaQueriesCount(20)
    void submitReturnRegistryForManyItemsAndUnits() throws Exception {
        String data = getJsonFromFile("registry-with-many-items-and-units.json");

        doPostReturnSupplyNoFile(data)
                .andExpect(status().isOk());

        List<Long> itemIds = requestItemRepository.findAllByRequestIdOrderById(2).stream()
                .map(RequestItem::getId)
                .collect(Collectors.toList());
        assertions.assertThat(itemIds).hasSize(21);
        for (int i = 1; i <= 21; i++) {
            assertions.assertThat(itemIds).contains((long) i);
        }
        List<Long> logisticUnitIds = logisticUnitRepository.findAllByRequestId(2).stream()
                .map(LogisticUnit::getId)
                .collect(Collectors.toList());
        assertions.assertThat(logisticUnitIds).hasSize(6);
        for (int i = 1; i <= 6; i++) {
            assertions.assertThat(logisticUnitIds).contains((long) i);
        }
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request-registry/after-save-real-supplier.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testSaveRealSupplierOnUploadRequestRegistry() throws Exception {
        doPostReturnSupplyNoFile(getJsonFromFile("save-real-supplier-request.json"))
                .andExpect(status().isOk())
                .andReturn();

    }

    private ResultActions doPostReturnSupplyNoFile(final String data) throws Exception {
        return mockMvc.perform(
            post("/upload-request/registry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(data)
        ).andDo(print());
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(getJsonFromFile(filename), response.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    private String getJsonFromFile(final String name) throws IOException {
        return FileContentUtils.getFileContent("controller/upload-request-registry/" + name);
    }
}
