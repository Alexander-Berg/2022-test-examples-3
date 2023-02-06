package ru.yandex.market.logistics.cte.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.annotation.ExpectedDatabases;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.logistics.cte.base.MvcIntegrationTest;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemIdentifier;
import ru.yandex.market.logistics.cte.client.enums.SupplyItemIdentifierType;
import ru.yandex.market.logistics.cte.converters.SupplyDtoToSupplyConverter;
import ru.yandex.market.logistics.cte.converters.SupplyItemDtoToSupplyItemConverter;
import ru.yandex.market.logistics.cte.repo.SupplyItemRepository;
import ru.yandex.market.logistics.cte.util.ResettableSequenceStyleGenerator;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mdm.http.MdmCommon;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;

@ContextConfiguration(classes = {SupplyDtoToSupplyConverter.class, SupplyItemDtoToSupplyItemConverter.class})
class SupplyControllerTest extends MvcIntegrationTest {

    private static final String SHOP_SKU_1 = "SHOPSKU1";
    private static final Long SUPPLIER_ID = 1L;
    private static final LocalDate NOW = LocalDate.parse("2020-01-01");

    @Autowired
    SupplyController supplyController;

    @Autowired
    private DeliveryParams deliveryParams;

    @Autowired
    private SupplyItemRepository repository;

    @Autowired
    private LomClient lomClient;

    @AfterEach
    void invalidateCache() {
        reset(deliveryParams);
    }

    @BeforeEach
    public void resetSequences() {
        ResettableSequenceStyleGenerator.Companion.resetAllInstances();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(
                    value = "classpath:controller/after_second_supply_item_creation_with_name_and_warehouse.xml",
                    assertionMode = NON_STRICT_UNORDERED)
    })
    public void createSupplyItem_created() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/create_second_supply_item_request_with_name_and_warehouse.json",
                "controller/create_second_supply_item_response_with_name_and_warehouse.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(
                    value = "classpath:controller/after_second_supply_item_creation_with_name_and_warehouse.xml",
                    assertionMode = NON_STRICT_UNORDERED)
    })
    public void createSupplyItemBatch_created() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/10/items:batch",
                "controller/create_second_supply_item_batch_request.json",
                "controller/create_second_supply_item_batch_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/system_property_is_quality_group_matrix_enabled.xml"),
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/quality_group_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/supply-controller/with-attributes/after_creation.xml",
                    assertionMode = NON_STRICT)
    })
    public void createSupplyItemWithAttributeIds_created() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/supply-controller/with-attributes/create_second_supply_item_with_attributes_request.json",
                "controller/supply-controller/with-attributes/create_second_supply_item_with_attributes_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:controller/supply-controller/" +
                    "for-market-trade/system_properties_for_market_trade.xml"),
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/quality_group_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/supply-controller/for-market-trade/after_creation.xml",
                    assertionMode = NON_STRICT)
    })
    public void createSupplyItemForMarketTrade() throws Exception {

        PageResult<OrderDto> orderDto = getOrderDto();
        when(lomClient.searchOrders(any(), any())).thenReturn(orderDto);

        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/supply-controller/" +
                        "for-market-trade/create_second_supply_item_with_attributes_request.json",
                "controller/supply-controller/" +
                        "for-market-trade/create_second_supply_item_with_attributes_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:controller/supply-controller/" +
                    "for-market-trade-with-categories/system_properties_for_market_trade.xml"),
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/quality_group_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/supply-controller/" +
                    "for-market-trade-with-categories/after_creation.xml",
                    assertionMode = NON_STRICT)
    })
    public void createSupplyItemForMarketTradeWithExcludedCategory() throws Exception {

        PageResult<OrderDto> orderDto = getOrderDto();
        when(lomClient.searchOrders(any(), any())).thenReturn(orderDto);

        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/supply-controller/" +
                        "for-market-trade-with-categories/create_second_supply_item_with_attributes_request.json",
                "controller/supply-controller/" +
                        "for-market-trade-with-categories/create_second_supply_item_with_attributes_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_supply_creation_unpaid.xml",
                    assertionMode = NON_STRICT),
            @ExpectedDatabase(value = "classpath:controller/supply-controller/evaluate/evaluate_supply_item.xml",
                    assertionMode = NON_STRICT)
    })
    public void createSupplyItemCreated() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/supply-controller/evaluate/evaluate_supply_item_request.json",
                "controller/supply-controller/evaluate/evaluate_supply_item_response.json",
                OK
        );
    }

    @Test
    public void shouldFailWhenBothShopSkuAndExternalSkuAreEmpty() throws Exception {
        testEndpointPutStatus(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/should_fail_both_shop_sku_external_sku_empty_request.json",
                BAD_REQUEST
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/canonical_category_before.xml"),
            @DatabaseSetup("classpath:repository/resupply_matrix.xml"),
            @DatabaseSetup("classpath:controller/after_supply_creation.xml")
    })
    public void evaluateForMissingMBOMapping() throws Exception {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                buildEmptyMappingResponse();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/supply-controller/evaluate/evaluateForMissingMBOMapping_request.json",
                "controller/supply-controller/evaluate/evaluateForMissingMBOMapping_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/system_property_is_quality_group_matrix_enabled.xml"),
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:repository/qmatrix_group.xml"),
            @DatabaseSetup("classpath:repository/quality_attribute_inclusion.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/after_supply_creation.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    public void evaluateForMissingMBOMappingWithQualityGroups() throws Exception {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                buildEmptyMappingResponse();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/supply-controller/evaluate/evaluateForMissingMBOMappingWithQualityGroup_request.json",
                "controller/supply-controller/evaluate/evaluateForMissingMBOMappingWithQualityGroup_response.json",
                OK
        );
    }

    @Test
    public void shouldFailExternalSkuIsEmptyButMarketShopSkuIsEmpty() throws Exception {
        testEndpointPutStatus(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/should_fail_when_have_external_sku_but_market_shop_sku_empty_request.json",
                BAD_REQUEST
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_second_and_first_supply_item_creation.xml",
                    assertionMode = NON_STRICT_UNORDERED)
    })
    public void createSupplyItem_modified() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/create_second_supply_item_request.json",
                "controller/create_second_supply_item_response.json",
                OK
        );
    }

    @Test
    @Rollback()
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/after_supply_creation.xml"),
            @DatabaseSetup("classpath:controller/before_supply_item_attributes_update.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_supply_creation.xml",
                    assertionMode = NON_STRICT),
            @ExpectedDatabase(value = "classpath:controller/after_supply_item_attributes_update.xml",
                    assertionMode = NON_STRICT)
    })
    public void supplyItemAttributesUpdate() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/create_second_supply_item_request.json",
                "controller/create_second_supply_item_response.json",
                OK
        );
        assertThat(Objects.requireNonNull(repository.findById(1L).orElse(null)).getIdentifiers())
                .usingRecursiveComparison().isEqualTo(
                        Set.of(new SupplyItemIdentifier("1134243234", SupplyItemIdentifierType.UIT)));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/after_supply_creation.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_1p_supply_item_creation.xml",
                    assertionMode = NON_STRICT)
    })
    public void createSupplyItemWith1PVendor_happyPath() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid3",
                "controller/create_1p_supply_item_request.json",
                "controller/create_1p_supply_item_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/after_supply_creation.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_expired_item_creation.xml",
                    assertionMode = NON_STRICT)
    })
    public void createExpiredSupplyItem() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid4",
                "controller/create_expired_item_request.json",
                "controller/create_expired_item_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/after_supply_creation.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_item_with_null_values_creation.xml",
                    assertionMode = NON_STRICT)
    })
    public void createSupplyItemWithNullValues() throws Exception {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse mappingResponse =
                buildMappingResponse(false);
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(mappingResponse);

        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid5",
                "controller/create_item_with_null_values_request.json",
                "controller/create_item_with_null_values_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/after_supply_creation.xml"),
            @DatabaseSetup("classpath:controller/before_supply_item_attributes_update.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_supply_creation.xml",
                    assertionMode = NON_STRICT),
            @ExpectedDatabase(value = "classpath:controller/after_supply_item_attributes_update_text_uit.xml",
                    assertionMode = NON_STRICT)
    })
    public void updateSupplyItemUITIsSavedIfUITValueItIsNotOnlyDigits() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/10/items/uuid2",
                "controller/create_second_supply_item_request_text_uit.json",
                "controller/create_second_supply_item_response_text_uit.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/before_update_supply_item.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_update_supply_item.xml",
                    assertionMode = NON_STRICT),
            @ExpectedDatabase(value = "classpath:controller/dbqueue_success_both_1_2.xml",
                    assertionMode = NON_STRICT, connection = "dbqueueDatabaseConnection")
    })
    public void updateSupplyItemsWithUitsUsingUuidHappyPath() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/items/",
                "controller/update_supply_item_uit_using_uuid_request.json",
                "controller/update_supply_item_uit_using_uuid_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/before_update_supply_item_not_full.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_update_supply_item_not_full.xml",
                    assertionMode = NON_STRICT),
            @ExpectedDatabase(value = "classpath:controller/dbqueue_success_only_one.xml",
                    assertionMode = NON_STRICT, connection = "dbqueueDatabaseConnection")
    })
    public void updateOnlyExistingSupplyItems() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/items/",
                "controller/update_supply_item_uit_using_uuid_request.json",
                "controller/update_supply_item_uit_using_uuid_response_not_full.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/before_update_supply_item.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_update_supply_item_no_updates.xml",
                    assertionMode = NON_STRICT),
            @ExpectedDatabase(value = "classpath:controller/dbqueue_success_empty.xml",
                    assertionMode = NON_STRICT, connection = "dbqueueDatabaseConnection")
    })
    public void ifWeHaveNoSuchSupplyItemsNoUpdatesEmptyResponse() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/items/",
                "controller/update_supply_item_uit_using_uuid_request_empty_response.json",
                "controller/update_supply_item_uit_using_uuid_empty_response.json",
                OK
        );
    }

    @Test
    public void shouldFailIfUuidWillBeNullWhenUpdateSupplyItems() throws Exception {
        testEndpointPutStatus(
                "/logistic_services/supplies/items/",
                "controller/update_supply_item_uit_using_uuid_null_uuid.json",
                BAD_REQUEST
        );
    }

    @Test
    public void shouldFailIfUitWillBeNullWhenUpdateSupplyItems() throws Exception {
        testEndpointPutStatus(
                "/logistic_services/supplies/items/",
                "controller/update_supply_item_uit_using_uuid_null_uit.json",
                BAD_REQUEST
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/before_update_supply_item.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_update_supply_item.xml",
                    assertionMode = NON_STRICT),
            @ExpectedDatabase(value = "classpath:controller/dbqueue_success_both_1_2.xml",
                    assertionMode = NON_STRICT, connection = "dbqueueDatabaseConnection")
    })
    public void updateSupplyItemsWhenPayloadConsistsNotOnlyFromPredefinedFields() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/items/",
                "controller/update_supply_item_uit_using_uuid_extra_fields.json",
                "controller/update_supply_item_uit_using_uuid_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/before_update_supply_item.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_update_supply_item-with-additional-fields.xml",
                    assertionMode = NON_STRICT_UNORDERED),
            @ExpectedDatabase(value = "classpath:controller/dbqueue_success_both_1_2.xml",
                    assertionMode = NON_STRICT, connection = "dbqueueDatabaseConnection")
    })
    public void updateSupplyItemsWhenPayloadConsistsWithAdditionalFields() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/items/",
                "controller/update_supply_item_uit_using_uuid_with_additional_fields.json",
                "controller/update_supply_item_uit_using_uuid_with_additional_fields_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/empty.xml"),
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:service/quality_attribute_after.xml",
                    assertionMode = NON_STRICT)
    })
    public void addQualityAttribute() throws Exception {
        testEndpointPostStatus(
                "/logistic_services/quality_attribute/",
                "controller/add_quality_attribute_request.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/quality_attribute_before.xml"),
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:service/quality_attribute_after.xml",
                    assertionMode = NON_STRICT)
    })
    public void addQualityAttributeWithDuplicateNameAndRefId() throws Exception {
        testEndpointPostStatus(
                "/logistic_services/quality_attribute/",
                "controller/add_quality_attribute_request.json",
                CONFLICT
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/quality_attribute_before.xml"),
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:service/quality_attribute_after.xml",
                    assertionMode = NON_STRICT)
    })
    public void addQualityAttributeWithNullValues() throws Exception {
        testEndpointPostStatus(
                "/logistic_services/quality_attribute/",
                "controller/add_quality_attribute_request_with_null_values.json",
                BAD_REQUEST
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/before_update_supply_item_type_unpaid.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_update_supply_item_type_unpaid.xml",
                    assertionMode = NON_STRICT),
            @ExpectedDatabase(value = "classpath:controller/dbqueue_success_both_1_2_unpaid.xml",
                    assertionMode = NON_STRICT, connection = "dbqueueDatabaseConnection")
    })
    public void updateSupplyItemsButNotCreateTicketIfItemTypeIsUnpaid() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/items/",
                "controller/update_supply_item_uit_using_uuid_request.json",
                "controller/update_supply_item_uit_using_uuid_response.json",
                OK
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:repository/group.xml"),
            @DatabaseSetup("classpath:service/canonical_category_before.xml"),
            @DatabaseSetup("classpath:controller/before_update_supply_item_type_null.xml"),
            @DatabaseSetup("classpath:controller/quality_matrix_with_attributes.xml")
    })
    @ExpectedDatabases({
            @ExpectedDatabase(value = "classpath:controller/after_update_supply_item_type_null.xml",
                    assertionMode = NON_STRICT),
            @ExpectedDatabase(value = "classpath:controller/dbqueue_failure_both_1_2_type_null.xml",
                    assertionMode = NON_STRICT, connection = "dbqueueDatabaseConnection")
    })
    public void createTaskIfNoRegistryTypePresented() throws Exception {
        testEndpointPut(
                "/logistic_services/supplies/items/",
                "controller/update_supply_item_uit_using_uuid_request.json",
                "controller/update_supply_item_uit_using_uuid_response.json",
                OK
        );
    }

    private MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse buildMappingResponse(
            boolean hasWeightDimensionsInfoInMdm) {
        return MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                .addFulfilmentInfo(setDimensionsIfRequired(hasWeightDimensionsInfoInMdm, createWeightDimensions(),
                        MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(SUPPLIER_ID.intValue())
                                .setShopSku(SHOP_SKU_1)
                )
                        .build())
                .build();
    }

    private MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse buildEmptyMappingResponse() {
        return MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder().build();
    }

    private MboMappingsForDelivery.OfferFulfilmentInfo.Builder setDimensionsIfRequired(
            boolean required,
            MdmCommon.WeightDimensionsInfo dimensions,
            MboMappingsForDelivery.OfferFulfilmentInfo.Builder builder) {
        if (!required) {
            return builder;
        }
        builder.setGoldenWeightDimensionsInfo(dimensions);
        return builder;
    }

    private MdmCommon.WeightDimensionsInfo createWeightDimensions() {
        return MdmCommon.WeightDimensionsInfo.newBuilder()
                .build();
    }

    private PageResult<OrderDto> getOrderDto() {
        WaybillSegmentDto.ShipmentDto shipmentDto = Mockito.mock(WaybillSegmentDto.ShipmentDto.class);
        when(shipmentDto.getDate()).thenReturn(NOW);

        WaybillSegmentDto waybillSegmentDto = Mockito.mock(WaybillSegmentDto.class);
        when(waybillSegmentDto.getShipment()).thenReturn(shipmentDto);

        OrderDto orderDto = Mockito.mock(OrderDto.class);
        when(orderDto.getWaybill()).thenReturn(List.of(waybillSegmentDto));

        PageResult<OrderDto> objectPageResult = new PageResult<>();
        return objectPageResult.setData(List.of(orderDto));
    }
}
