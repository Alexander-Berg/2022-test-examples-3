package ru.yandex.market.logistics.iris.service.item.put;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Service;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifierWithChangedFields;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTOWithChangedFields;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyBooleanKey;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyIntegerKey;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyService;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyStringKey;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

public class PutReferenceItemsServiceTest extends AbstractContextualTest {

    private final String REQUEST_ID = "TestRequestId";

    @Autowired
    private PutReferenceItemsService putReferenceItemsService;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @SpyBean
    private SystemPropertyService systemPropertyService;

    @Captor
    private ArgumentCaptor<List<Item>> itemsCaptor;

    @Captor
    private ArgumentCaptor<Partner> partnersCaptor;

    @Before
    public void init() {
        RequestContextHolder.createContext(Optional.of(REQUEST_ID));
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/put_reference_items_service/items.xml")
    public void shouldSuccessPutReferenceItems() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();
        doReturn("145,147").when(systemPropertyService).getStringProperty(SystemPropertyStringKey.FULFILLMENT_WAREHOUSE_IDS);

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(2)).putReferenceItems(itemsCaptor.capture(), partnersCaptor.capture());
        List<List<Item>> captorItemsResult = itemsCaptor.getAllValues();
        List<Partner> captorPartnerResults = partnersCaptor.getAllValues();

        assertSoftly(assertions -> {
            assertions.assertThat(captorItemsResult.size()).isEqualTo(2);

            List<Item> firstItemsRequest = captorItemsResult.get(0);
            assertions.assertThat(firstItemsRequest).isNotNull();
            assertions.assertThat(firstItemsRequest.get(0).getUnitId()).isEqualTo(new UnitId(null, 2L, "1"));


            List<Item> secondItemsRequest = new ArrayList<>(captorItemsResult.get(1));
            assertions.assertThat(secondItemsRequest).isNotNull();

            secondItemsRequest.sort(Comparator.comparing((Item item) -> item.getUnitId().getVendorId()));

            assertions.assertThat(secondItemsRequest.get(0).getUnitId()).isEqualTo(new UnitId(null, 1L, "1"));
            assertions.assertThat(secondItemsRequest.get(1).getUnitId()).isEqualTo(new UnitId(null, 2L, "2"));

            assertions.assertThat(secondItemsRequest.get(0).getInboundServices()).isNull();

            assertions.assertThat(captorPartnerResults.size()).isEqualTo(2);
            assertions.assertThat(captorPartnerResults.get(0).getId()).isEqualTo(145L);
            assertions.assertThat(captorPartnerResults.get(1).getId()).isEqualTo(147L);
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/put_reference_items_service/items_measurement_state_is_true.xml")
    public void shouldSuccessPutReferenceItemsWhenAlreadyMeasuredIsTrue() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();
        itemIdentifiersWithChangedFields.get(0).setChangedFields(Collections.singletonList("measurement_state"));

        doReturn("145,147").when(systemPropertyService).getStringProperty(SystemPropertyStringKey.FULFILLMENT_WAREHOUSE_IDS);

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(2)).putReferenceItems(itemsCaptor.capture(), partnersCaptor.capture());
        List<List<Item>> captorItemsResult = itemsCaptor.getAllValues();
        List<Partner> captorPartnerResults = partnersCaptor.getAllValues();

        assertSoftly(assertions -> {
            assertions.assertThat(captorItemsResult.size()).isEqualTo(2);

            List<Item> firstItemsRequest = captorItemsResult.get(0);
            assertions.assertThat(firstItemsRequest).isNotNull();
            assertions.assertThat(firstItemsRequest.get(0).getUnitId()).isEqualTo(new UnitId(null, 2L, "1"));


            List<Item> secondItemsRequest = new ArrayList<>(captorItemsResult.get(1));
            assertions.assertThat(secondItemsRequest).isNotNull();

            secondItemsRequest.sort(Comparator.comparing((Item item) -> item.getUnitId().getVendorId()));

            assertions.assertThat(secondItemsRequest.get(0).getUnitId()).isEqualTo(new UnitId(null, 1L, "1"));
            assertions.assertThat(secondItemsRequest.get(1).getUnitId()).isEqualTo(new UnitId(null, 2L, "2"));

            assertions.assertThat(secondItemsRequest.get(0).getInboundServices())
                    .isEqualTo(Collections.singletonList(
                            new Service.ServiceBuilder(ServiceType.NO_MEASURE_ITEM).setIsOptional(true).build())
                    );

            assertions.assertThat(captorPartnerResults.size()).isEqualTo(2);
            assertions.assertThat(captorPartnerResults.get(0).getId()).isEqualTo(145L);
            assertions.assertThat(captorPartnerResults.get(1).getId()).isEqualTo(147L);
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/put_reference_items_service/items_measurement_state_is_false.xml")
    public void shouldSuccessPutReferenceItemsWhenAlreadyMeasuredIsFalse() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();
        itemIdentifiersWithChangedFields.get(0).setChangedFields(Collections.singletonList("measurement_state"));

        doReturn("145,147").when(systemPropertyService).getStringProperty(SystemPropertyStringKey.FULFILLMENT_WAREHOUSE_IDS);

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(2)).putReferenceItems(itemsCaptor.capture(), partnersCaptor.capture());
        List<List<Item>> captorItemsResult = itemsCaptor.getAllValues();
        List<Partner> captorPartnerResults = partnersCaptor.getAllValues();

        assertSoftly(assertions -> {
            assertions.assertThat(captorItemsResult.size()).isEqualTo(2);

            List<Item> firstItemsRequest = captorItemsResult.get(0);
            assertions.assertThat(firstItemsRequest).isNotNull();
            assertions.assertThat(firstItemsRequest.get(0).getUnitId()).isEqualTo(new UnitId(null, 2L, "1"));


            List<Item> secondItemsRequest = new ArrayList<>(captorItemsResult.get(1));
            assertions.assertThat(secondItemsRequest).isNotNull();

            secondItemsRequest.sort(Comparator.comparing((Item item) -> item.getUnitId().getVendorId()));

            assertions.assertThat(secondItemsRequest.get(0).getUnitId()).isEqualTo(new UnitId(null, 1L, "1"));
            assertions.assertThat(secondItemsRequest.get(1).getUnitId()).isEqualTo(new UnitId(null, 2L, "2"));

            assertions.assertThat(secondItemsRequest.get(0).getInboundServices())
                    .isEqualTo(Collections.singletonList(
                            new Service.ServiceBuilder(ServiceType.MEASURE_ITEM).setIsOptional(true).build())
                    );

            assertions.assertThat(captorPartnerResults.size()).isEqualTo(2);
            assertions.assertThat(captorPartnerResults.get(0).getId()).isEqualTo(145L);
            assertions.assertThat(captorPartnerResults.get(1).getId()).isEqualTo(147L);
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/put_reference_items_service/items.xml")
    public void shouldSuccessPutReferenceItemsForOneWarehouse() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();
        doReturn("145").when(systemPropertyService).getStringProperty(SystemPropertyStringKey.FULFILLMENT_WAREHOUSE_IDS);

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(1)).putReferenceItems(itemsCaptor.capture(), partnersCaptor.capture());
        List<List<Item>> captorItemsResult = itemsCaptor.getAllValues();
        List<Partner> captorPartnerResults = partnersCaptor.getAllValues();

        assertSoftly(assertions -> {
            assertions.assertThat(captorItemsResult.size()).isEqualTo(1);

            List<Item> firstItemsRequest = captorItemsResult.get(0);
            assertions.assertThat(firstItemsRequest).isNotNull();
            assertions.assertThat(firstItemsRequest.get(0).getUnitId()).isEqualTo(new UnitId(null, 2L, "1"));

            assertions.assertThat(captorPartnerResults.size()).isEqualTo(1);
            assertions.assertThat(captorPartnerResults.get(0).getId()).isEqualTo(145L);
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/put_reference_items_service/items.xml")
    public void shouldNotPutReferenceItemsIfFulfillmentWarehouseIdsPropertyIsEmpty() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();
        doReturn("").when(systemPropertyService).getStringProperty(SystemPropertyStringKey.FULFILLMENT_WAREHOUSE_IDS);

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(0)).putReferenceItems(any(), any());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/put_reference_items_service/items.xml")
    public void shouldNotPutReferenceItemsWithEmptyIdentifiers() throws GatewayApiException {
        putReferenceItemsService.put(Collections.emptyList());

        Mockito.verify(fulfillmentClient, times(0)).putReferenceItems(any(), any());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/put_reference_items_service/items.xml")
    public void shouldNotPutReferenceItemsWithUnknownIdentifiers() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = Collections.singletonList(
                new ItemIdentifierWithChangedFields(ItemIdentifier.of("10023", "9999"), Collections.emptyList())
        );

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(0)).putReferenceItems(any(), any());
    }

    @Test
    public void shouldNotPutReferenceItemsWithEmptyDatabase() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(0)).putReferenceItems(any(), any());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/put_reference_items_service/items.xml")
    public void shouldNotAsyncPutReferenceItemsWithEmptyIdentifiers() throws GatewayApiException {
        putReferenceItemsService.putAsync(Collections.emptyList());

        Mockito.verify(fulfillmentClient, times(0)).putReferenceItems(any(), any());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/put_reference_items_service/items.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/put_reference_items_service/items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "classpath:fixtures/expected/put_reference_items_service/1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessProducePutReferenceItemsJob() {
        doReturn(Integer.MAX_VALUE).when(systemPropertyService)
                .getIntegerProperty(SystemPropertyIntegerKey.PUT_REFERENCE_ITEMS_EXECUTION_DELAY_SECONDS);

        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();
        putReferenceItemsService.putAsync(itemIdentifiersWithChangedFields);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/put_reference_items_service/items.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/put_reference_items_service/items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "classpath:fixtures/expected/put_reference_items_service/2.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessProduceFailedPutReferenceItemsJos() {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();
        putReferenceItemsService.putFailedAsync(itemIdentifiersWithChangedFields);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/put_reference_items_service/items_partially_inserted.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/put_reference_items_service/after_partial_insert_items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessProduceFailedPutReferenceItemsJobWithPartOfItems() {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();
        putReferenceItemsService.putFailedAsync(itemIdentifiersWithChangedFields);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/put_reference_items_service/all_items_inserted.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/put_reference_items_service/all_items_inserted.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotProduceFailedPutReferenceItemsJobWhenAllItemsAlreadyInserted() {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();
        putReferenceItemsService.putFailedAsync(itemIdentifiersWithChangedFields);
    }

    @Test
    @DatabaseSetup({
            "classpath:fixtures/setup/put_reference_items_service/items.xml",
            "classpath:fixtures/setup/put_reference_items_service/queue_tasks.xml"
    })
    @ExpectedDatabase(value = "classpath:fixtures/setup/put_reference_items_service/items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "classpath:fixtures/expected/put_reference_items_service/queue_tasks.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotConsumePutReferenceItemsTaskIfConsumerDisabled() {
        doReturn(true).when(systemPropertyService).getBooleanProperty(SystemPropertyBooleanKey.DISABLE_PUT_REFERENCE_ITEMS_CONSUMER);
    }

    @Test
    @DatabaseSetup({
            "classpath:fixtures/setup/put_reference_items_service/items.xml"
    })
    public void shouldOncePutReferenceItemsIfReferenceIndexEquals() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = Collections.singletonList(
                new ItemIdentifierWithChangedFields(ItemIdentifier.of("10", "10"), Collections.emptyList())
        );

        doReturn(true).when(systemPropertyService).getBooleanProperty(SystemPropertyBooleanKey.ENABLED_COMPARISON_ITEMS_BEFORE_PUT);
        doReturn("173").when(systemPropertyService).getStringProperty(SystemPropertyStringKey.FULFILLMENT_WAREHOUSE_IDS);

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(1)).putReferenceItems(any(), any());
    }

    @Test
    @DatabaseSetup({
            "classpath:fixtures/setup/put_reference_items_service/items.xml"
    })
    public void shouldTwicePutReferenceItemsIfReferenceIndexesAreNotEquals() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = Collections.singletonList(
                new ItemIdentifierWithChangedFields(ItemIdentifier.of("10", "10"), Collections.emptyList())
        );

        doReturn(true).when(systemPropertyService).getBooleanProperty(SystemPropertyBooleanKey.ENABLED_COMPARISON_ITEMS_BEFORE_PUT);
        doReturn("173,174").when(systemPropertyService).getStringProperty(SystemPropertyStringKey.FULFILLMENT_WAREHOUSE_IDS);

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(2)).putReferenceItems(any(), any());
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/put_reference_items_service/items.xml")
    public void shouldNotPutReferenceItemsIfFulfillmentWarehouseIdsPropertyIsEmptyAndCompareIndexEnabled() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = getItemIdentifiersWithChangedFields();
        doReturn(true).when(systemPropertyService).getBooleanProperty(SystemPropertyBooleanKey.ENABLED_COMPARISON_ITEMS_BEFORE_PUT);
        doReturn("").when(systemPropertyService).getStringProperty(SystemPropertyStringKey.FULFILLMENT_WAREHOUSE_IDS);

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(0)).putReferenceItems(any(), any());
    }

    @Test
    @DatabaseSetup({
            "classpath:fixtures/setup/put_reference_items_service/not_valid_items.xml"
    })
    @ExpectedDatabase(value = "classpath:fixtures/expected/put_reference_items_service/3.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotPutReferenceItemsIfAfterValidatingThereIsNoItems() throws GatewayApiException {
        List<ItemIdentifierWithChangedFields> itemIdentifiersWithChangedFields = Collections.singletonList(
                new ItemIdentifierWithChangedFields(ItemIdentifier.of("10", "10"), Collections.emptyList())
        );

        doReturn(true).when(systemPropertyService).getBooleanProperty(SystemPropertyBooleanKey.ENABLED_COMPARISON_ITEMS_BEFORE_PUT);
        doReturn("173,174").when(systemPropertyService).getStringProperty(SystemPropertyStringKey.FULFILLMENT_WAREHOUSE_IDS);

        putReferenceItemsService.put(itemIdentifiersWithChangedFields);

        Mockito.verify(fulfillmentClient, times(0)).putReferenceItems(any(), any());
    }

    private List<ItemIdentifierWithChangedFields> getItemIdentifiersWithChangedFields() {
        return Stream.of(
                        new ItemIdentifierWithChangedFields(ItemIdentifier.of("1", "1"), Collections.emptyList()),
                        new ItemIdentifierWithChangedFields(ItemIdentifier.of("2", "1"), Collections.emptyList()),
                        new ItemIdentifierWithChangedFields(ItemIdentifier.of("2", "2"), Collections.emptyList())
                )
                .collect(Collectors.toList());
    }


}
