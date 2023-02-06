package ru.yandex.market.ff.event.processor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.ReturnFlowType;
import ru.yandex.market.ff.event.LrmReturnEventConsumer;
import ru.yandex.market.ff.model.bo.SupplierSkuKeyWithReturnReason;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.entity.ReturnBoxEntity;
import ru.yandex.market.ff.model.entity.ReturnEntity;
import ru.yandex.market.ff.model.entity.ReturnItemEntity;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.implementation.ReturnEntityService;
import ru.yandex.market.ff.service.lrm.LrmEventProcessorProvider;
import ru.yandex.market.ff.service.lrm.LrmService;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.lrm.event_model.ReturnEvent;
import ru.yandex.market.logistics.lrm.event_model.ReturnEventType;
import ru.yandex.market.logistics.lrm.event_model.payload.OrderItemInfo;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnBox;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnCommittedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnItem;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnReasonType;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSource;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatusChangedPayload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.ff.client.enums.RegistryUnitIdType.CIS;
import static ru.yandex.market.ff.client.enums.RegistryUnitIdType.IMEI;
import static ru.yandex.market.ff.client.enums.RegistryUnitIdType.SERIAL_NUMBER;
import static ru.yandex.market.ff.client.enums.RegistryUnitIdType.UIT;
import static ru.yandex.market.ff.client.enums.RegistryUnitIdType.values;
import static ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY;

class LrmReturnEventConsumerTest {

    private static final String SKU = "sku123";
    private static final long SUPPLIER_ID = 123;
    private static final String SERIAL_NUMBER_NODE_NAME = "sn";

    private LrmReturnEventConsumer consumer;
    private ReturnEntityService returnEntityService;
    private ConcreteEnvironmentParamService concreteEnvironmentParamService;
    private LrmService lrmService;
    private ReturnsApi returnsApi;
    private LrmEventProcessorProvider lrmEventProcessorProvider;
    private LrmReturnCommittedEventProcessor lrmReturnCommittedEventProcessor;
    private LrmReturnStatusChangedEventProcessor lrmReturnStatusChangedEventProcessor;


    @BeforeEach
    void init() {
        returnEntityService = Mockito.mock(ReturnEntityService.class);
        returnsApi = Mockito.mock(ReturnsApi.class);
        concreteEnvironmentParamService = Mockito.mock(ConcreteEnvironmentParamService.class);
        lrmService = new LrmService(returnsApi, concreteEnvironmentParamService);
        lrmReturnCommittedEventProcessor = new LrmReturnCommittedEventProcessor(returnEntityService, lrmService);
        lrmReturnStatusChangedEventProcessor = new LrmReturnStatusChangedEventProcessor(returnEntityService);
        lrmEventProcessorProvider = new LrmEventProcessorProvider(
                List.of(lrmReturnCommittedEventProcessor, lrmReturnStatusChangedEventProcessor));
        consumer = new LrmReturnEventConsumer(lrmEventProcessorProvider);
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(concreteEnvironmentParamService);
    }

    @Test
    void shouldSkipOtherThanReturnCommitedEvents() {
        consumer.accept(List.of(
                ReturnEvent.builder()
                        .eventType(ReturnEventType.CUSTOMER_ORDER_ITEMS_CHANGED)
                        .build(),
                ReturnEvent.builder()
                        .eventType(ReturnEventType.CUSTOMER_ORDER_ITEMS_CHANGED)
                        .build()
        ));
        Mockito.verify(returnEntityService, Mockito.never()).save(any());
    }

    @Test
    void shouldConvertEventAndCallRepositorySave() {
        consumer.accept(List.of(getReturnEvent()));
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(returnEntityService, Mockito.times(1)).save(captor.capture());

        ReturnEntity actual = (ReturnEntity) captor.getValue().get(0);
        ReturnEntity expected = getReturnEntityExpected();
        assertThat(actual.getExternalId()).isEqualTo(expected.getExternalId());
        assertThat(actual.getOrderExternalId()).isEqualTo(expected.getOrderExternalId());
        assertThat(actual.getBoxes().size()).isEqualTo(expected.getBoxes().size());
        assertThat(actual.getItems().size()).isEqualTo(expected.getItems().size());

        ReturnBoxEntity actualBox = actual.getBoxes().iterator().next();
        ReturnBoxEntity expectedBox = expected.getBoxes().iterator().next();
        assertThat(actualBox.getExternalId()).isEqualTo(expectedBox.getExternalId());

        compareReturnEntity(actual.getItems().iterator().next(), expected.getItems().iterator().next());
    }

    @Test
    void shouldGroupAndConvertEventAndCallRepositorySave() {
        consumer.accept(List.of(getReturnEventWithMultipleReturnItem()));
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(returnEntityService, Mockito.times(1)).save(captor.capture());

        ReturnEntity actual = (ReturnEntity) captor.getValue().get(0);
        ReturnEntity expected = getReturnEntityExpectedWithMultipleItems();
        assertThat(actual.getExternalId()).isEqualTo(expected.getExternalId());
        assertThat(actual.getOrderExternalId()).isEqualTo(expected.getOrderExternalId());
        assertThat(actual.getBoxes().size()).isEqualTo(expected.getBoxes().size());
        assertThat(actual.getItems().size()).isEqualTo(expected.getItems().size());

        ReturnBoxEntity actualBox = actual.getBoxes().iterator().next();
        ReturnBoxEntity expectedBox = expected.getBoxes().iterator().next();
        assertThat(actualBox.getExternalId()).isEqualTo(expectedBox.getExternalId());


        Iterator<ReturnItemEntity> actualIterator = actual.getItems()
                .stream()
                .sorted(Comparator.comparing(ReturnItemEntity::getReturnReason))
                .collect(Collectors.toList())
                .iterator();

        Iterator<ReturnItemEntity> expectedIterator = expected.getItems()
                .stream()
                .sorted(Comparator.comparing(ReturnItemEntity::getReturnReason))
                .collect(Collectors.toList())
                .iterator();

        compareReturnEntity(actualIterator.next(), expectedIterator.next());
        compareReturnEntity(actualIterator.next(), expectedIterator.next());
    }

    @Test
    void shouldConvertEventAndCallRepositorySaveWithNullSupplier() {
        consumer.accept(List.of(getReturnEventWithNullSupplier()));
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(returnEntityService, Mockito.times(1)).save(captor.capture());

        ReturnEntity actual = (ReturnEntity) captor.getValue().get(0);
        ReturnEntity expected = getReturnEntityNullSupplierExpected();
        assertThat(actual.getExternalId()).isEqualTo(expected.getExternalId());
        assertThat(actual.getOrderExternalId()).isEqualTo(expected.getOrderExternalId());
        assertThat(actual.getBoxes().size()).isEqualTo(expected.getBoxes().size());
        assertThat(actual.getItems().size()).isEqualTo(expected.getItems().size());

        ReturnBoxEntity actualBox = actual.getBoxes().iterator().next();
        ReturnBoxEntity expectedBox = expected.getBoxes().iterator().next();
        assertThat(actualBox.getExternalId()).isEqualTo(expectedBox.getExternalId());

        compareReturnEntity(actual.getItems().iterator().next(), expected.getItems().iterator().next());
    }

    @Test
    void shouldAcceptDataContainNulls() {
        consumer.accept(null);
        consumer.accept(List.of());
        consumer.accept(List.of(ReturnEvent.builder().build()));
        consumer.accept(List.of(ReturnEvent.builder().eventType(ReturnEventType.RETURN_COMMITTED)
                .payload(
                        new ReturnCommittedPayload())
                .build()));
        consumer.accept(List.of(ReturnEvent.builder().eventType(ReturnEventType.RETURN_COMMITTED)
                .payload(
                        new ReturnCommittedPayload()
                                .setBoxes(List.of())
                                .setItems(List.of())
                )
                .build()));
        consumer.accept(List.of(ReturnEvent.builder()
                .payload(
                        new ReturnCommittedPayload()
                                .setBoxes(List.of(ReturnBox.builder().build()))
                                .setItems(List.of(ReturnItem.builder().build()))
                )
                .build()));
    }

    @Test
    void shouldGetTheBoxByBoxId() {
        String extBoxId = "box1";
        ReturnBoxEntity box1 = getBox(1L, extBoxId);
        ReturnBoxEntity box2 = getBox(2L, "box2");
        Map<String, ReturnBoxEntity> boxes = Map.of(extBoxId, box1, "box2", box2);
        assertThat(lrmReturnCommittedEventProcessor.getBox(boxes, getReturnItemWithBox(extBoxId))).isEqualTo(box1);
    }

    @Test
    void shouldGetNullByNonExistentBoxId() {
        String extBoxId = "box1";
        ReturnBoxEntity box1 = getBox(1L, extBoxId);
        ReturnBoxEntity box2 = getBox(2L, "box2");
        Map<String, ReturnBoxEntity> boxes = new LinkedHashMap<>();
        boxes.put(extBoxId, box1);
        boxes.put("box2", box2);
        assertThat(lrmReturnCommittedEventProcessor.getBox(boxes,
                getReturnItemWithBox("non-existent boxId"))).isNull();
    }

    @Test
    void shouldReturnNullWhenNoBoxesProvided() {
        Map<String, ReturnBoxEntity> boxes = Map.of();
        assertThat(lrmReturnCommittedEventProcessor.getBox(boxes, getReturnItemWithBox("someBoxId"))).isNull();
    }

    private ReturnBoxEntity getBox(long id, String externalBoxId) {
        return ReturnBoxEntity.builder().id(id).externalId(externalBoxId).build();
    }

    @Test
    void shouldntFailOnNullValues() {
        var instancesWithNulls = new HashMap<String, String>();
        instancesWithNulls.put("key with null value", null);
        instancesWithNulls.put(null, "null key value");
        List<OrderItemInfo> orderItems = getOrderItemInfos(instancesWithNulls);
        List<ReturnItem> items = getReturnItemInfos(instancesWithNulls);

        Mockito.when(concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns()).thenReturn(Set.of());
        lrmReturnCommittedEventProcessor.convertInstances(items, orderItems, getSupplierSkuKeyWithReturnReason());
    }

    @Test
    void shouldntFailOnNullSupplierId() {
        var instancesWithNulls = new HashMap<String, String>();
        instancesWithNulls.put("key with null value", null);
        instancesWithNulls.put(null, "null key value");
        List<OrderItemInfo> orderItems = getOrderItemInfos(instancesWithNulls);
        List<ReturnItem> items = getReturnItemInfos(instancesWithNulls);

        Mockito.when(concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns()).thenReturn(Set.of());
        lrmReturnCommittedEventProcessor.convertInstances(items, orderItems, getSupplierSkuKeyWithReturnReason());
    }

    @Test
    void shouldAcceptAllRegistryUnitIdTypesWhenNoTypesSpecifiedAtEnvParam() {
        Mockito.when(concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns()).thenReturn(Set.of());
        Map<String, String> instances = Arrays.stream(values())
                .map(RegistryUnitIdType::getValue)
                .collect(Collectors.toMap(Function.identity(), it -> "some value for " + it));
        var regUnitId = lrmReturnCommittedEventProcessor.convertInstances(
                getReturnItemInfos(instances), getOrderItemInfos(instances),
                getSupplierSkuKeyWithReturnReason()
        );
        assertThat(regUnitId.iterator().next().getUnitIdTypesSet())
                .containsExactlyInAnyOrder(values());
    }

    @Test
    void shouldAcceptOnlySpecifiedRegistryUnitIdTypes() {
        Mockito.when(concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns())
                .thenReturn(Set.of(CIS, IMEI));
        Map<String, String> instances = Arrays.stream(values())
                .map(RegistryUnitIdType::getValue)
                .collect(Collectors.toMap(Function.identity(), it -> "some value for " + it));
        var regUnitId = lrmReturnCommittedEventProcessor.convertInstances(
                getReturnItemInfos(instances), getOrderItemInfos(instances),
                getSupplierSkuKeyWithReturnReason()
        );
        assertThat(regUnitId.iterator().next().getUnitIdTypesSet())
                .containsExactlyInAnyOrder(CIS, IMEI);
    }

    @Test
    void shouldAcceptSerialNumberRegistryUnitIdType() {
        Mockito.when(concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns())
                .thenReturn(Set.of(SERIAL_NUMBER));

        Map<String, String> instances = new HashMap<>();
        instances.put(SERIAL_NUMBER_NODE_NAME, "23879/10486963");

        var regUnitId = lrmReturnCommittedEventProcessor.convertInstances(
                getReturnItemInfos(instances), getOrderItemInfos(instances),
                getSupplierSkuKeyWithReturnReason()
        );

        assertThat(regUnitId.iterator().next().getParts().iterator().next().getType())
                .isEqualTo(SERIAL_NUMBER);

        assertThat(regUnitId.iterator().next().getParts().iterator().next().getValue())
                .isEqualTo("23879/10486963");
    }

    @Test
    void shouldReturnEmptyListForEmptyList() {
        Set<RegistryUnitIdType> instanceTypes = Set.of(RegistryUnitIdType.CIS, RegistryUnitIdType.IMEI);
        Mockito.when(concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns())
                .thenReturn(instanceTypes);
        Map<String, String> instances = getInstances(EnumSet.complementOf(EnumSet.copyOf(instanceTypes)));

        var regUnitId = lrmReturnCommittedEventProcessor.convertInstances(
                getReturnItemInfos(instances), getOrderItemInfos(instances), getSupplierSkuKeyWithReturnReason());
        assertThat(regUnitId).isEmpty();
    }

    @Test
    void shouldMergeIdentifiersWhenEmptyOrderItems() {
        Mockito.when(concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns()).thenReturn(Set.of());

        var regUnitId = lrmReturnCommittedEventProcessor.convertInstances(
                getReturnItemInfos(List.of(getInstances(EnumSet.copyOf(Set.of(CIS, IMEI))))),
                getOrderItemInfos(List.of(Map.of())),
                getSupplierSkuKeyWithReturnReason()
        );

        assertThat(regUnitId.iterator().next().getUnitIdTypesSet())
                .containsExactlyInAnyOrder(CIS, IMEI);
    }

    @Test
    void shouldMergeIdentifiersWhenEmptyItems() {
        Mockito.when(concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns()).thenReturn(Set.of());

        var regUnitId = lrmReturnCommittedEventProcessor.convertInstances(
                getReturnItemInfos(List.of(Map.of())),
                getOrderItemInfos(List.of(getInstances(EnumSet.copyOf(Set.of(CIS, IMEI))))),
                getSupplierSkuKeyWithReturnReason()
        );

        assertThat(regUnitId.iterator().next().getUnitIdTypesSet())
                .containsExactlyInAnyOrder(CIS, IMEI);
    }

    @Test
    void shouldMergeIdentifiersWhenNullItems() {
        Mockito.when(concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns()).thenReturn(Set.of());

        var list = new ArrayList<Map<String, String>>();
        list.add(null);
        var regUnitId = lrmReturnCommittedEventProcessor.convertInstances(
                getReturnItemInfos(list),
                getOrderItemInfos(List.of(getInstances(EnumSet.copyOf(Set.of(CIS, IMEI))))),
                getSupplierSkuKeyWithReturnReason()
        );

        assertThat(regUnitId.iterator().next().getUnitIdTypesSet())
                .containsExactlyInAnyOrder(CIS, IMEI);
    }

    @Test
    void shouldMergeIdentifiersWhenMultipleOrderItems() {
        Mockito.when(concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns()).thenReturn(Set.of());

        var regUnitId = lrmReturnCommittedEventProcessor.convertInstances(
                getReturnItemInfos(List.of(Map.of())),
                getOrderItemInfosMultiple(List.of(List.of(getInstances(EnumSet.copyOf(Set.of(CIS, IMEI)))),
                        List.of(getInstances(EnumSet.copyOf(Set.of(UIT)))))),
                getSupplierSkuKeyWithReturnReason()
        );

        Iterator<RegistryUnitId> iterator = regUnitId.iterator();
        assertThat(iterator.next().getUnitIdTypesSet())
                .containsExactlyInAnyOrder(CIS, IMEI);
        assertThat(iterator.next().getUnitIdTypesSet())
                .containsExactlyInAnyOrder(UIT);
    }

    @Test
    void shouldFillReturnFlowType() {
        consumer.accept(List.of(getReturnEventWithNullSupplierAndReturnSource()));
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(returnEntityService, Mockito.times(1)).save(captor.capture());

        ReturnEntity actual = (ReturnEntity) captor.getValue().get(0);

        assertThat(actual.getType()).isEqualTo(ReturnFlowType.CUSTOMER_RETURN);
    }

    @Test
    void shouldConvertReturnStatusChangedEventAndCallRepositorySave() {
        consumer.accept(List.of(getReturnStatusChangedEvent()));
        ArgumentCaptor<ReturnStatus> statusCaptor = ArgumentCaptor.forClass(ReturnStatus.class);
        ArgumentCaptor<Long> returnIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Instant> timeCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(returnEntityService, Mockito.times(1))
                .updateReturnStatus(statusCaptor.capture(), returnIdCaptor.capture(), timeCaptor.capture());

        ReturnStatus actualStatus = statusCaptor.getValue();
        Long actualReturnId = returnIdCaptor.getValue();

        assertThat(ReturnStatus.CREATED).isEqualTo(actualStatus);
        assertThat(actualReturnId).isEqualTo(111L);
    }



    @Nonnull
    private Map<String, String> getInstances(EnumSet<RegistryUnitIdType> registryUnitIdTypes) {
        return registryUnitIdTypes.stream()
                .map(RegistryUnitIdType::getValue)
                .collect(Collectors.toMap(Function.identity(), it -> "some value for " + it));
    }

    private void compareReturnEntity(ReturnItemEntity actual, ReturnItemEntity expected) {
        assertThat(actual.getArticle()).isEqualTo(expected.getArticle());
        assertThat(actual.getSupplierId()).isEqualTo(expected.getSupplierId());
        assertThat(actual.getReturnReason()).isEqualTo(expected.getReturnReason());
        assertThat(actual.getReasonType()).isEqualTo(expected.getReasonType());
        assertThat(actual.getInstances()).isEqualTo(expected.getInstances());
        assertThat(actual.getBox().getExternalId()).isEqualTo(expected.getBox().getExternalId());
    }

    private ReturnEvent getReturnEvent() {
        return ReturnEvent.builder()
                .eventType(ReturnEventType.RETURN_COMMITTED)
                .id(1L)
                .created(DateTime.now().toDate().toInstant())
                .returnId(1L)
                .orderExternalId("orderExtId")
                .payload(new ReturnCommittedPayload()
                        .setOrderItemsInfo(getOrderItemInfos(
                                Map.of("CIS", "2489571_item1_cis1", "UIT", "2489571_item1_uit1")
                        ))
                        .setExternalId("return-id-1")
                        .setBoxes(List.of(ReturnBox.builder().externalId("box1").build()))
                        .setItems(List.of(
                                ReturnItem.builder()
                                        .vendorCode(SKU)
                                        .supplierId(SUPPLIER_ID)
                                        .boxExternalId("box1")
                                        .returnReason("return reason")
                                        .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                        .build()))
                        .setSource(ReturnSource.CANCELLATION)
                )
                .build();
    }

    private ReturnEvent getReturnStatusChangedEvent() {
        return ReturnEvent.builder()
                .eventType(ReturnEventType.RETURN_STATUS_CHANGED)
                .id(1L)
                .created(DateTime.now().toDate().toInstant())
                .returnId(111L)
                .orderExternalId("orderExtId")
                .payload(new ReturnStatusChangedPayload().setStatus(ReturnStatus.CREATED))
                .build();
    }

    private ReturnEvent getReturnEventWithMultipleReturnItem() {
        return ReturnEvent.builder()
                .eventType(ReturnEventType.RETURN_COMMITTED)
                .id(1L)
                .created(DateTime.now().toDate().toInstant())
                .returnId(1L)
                .orderExternalId("orderExtId")
                .payload(new ReturnCommittedPayload()
                        .setOrderItemsInfo(getOrderItemInfos(
                                Map.of("CIS", "2489571_item1_cis1", "UIT", "2489571_item1_uit1")
                        ))
                        .setExternalId("return-id-1")
                        .setBoxes(List.of(ReturnBox.builder().externalId("box1").build()))
                        .setItems(List.of(
                                ReturnItem.builder()
                                        .vendorCode(SKU)
                                        .supplierId(SUPPLIER_ID)
                                        .boxExternalId("box1")
                                        .returnReason("return reason")
                                        .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                        .build(),
                                ReturnItem.builder()
                                        .vendorCode(SKU)
                                        .supplierId(SUPPLIER_ID)
                                        .boxExternalId("box1")
                                        .returnReason("return reason1")
                                        .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                        .build(),
                                ReturnItem.builder()
                                        .vendorCode(SKU)
                                        .supplierId(SUPPLIER_ID)
                                        .boxExternalId("box1")
                                        .returnReason("return reason")
                                        .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                        .build()))
                        .setSource(ReturnSource.CLIENT)
                )
                .build();
    }

    private ReturnEvent getReturnEventWithNullSupplier() {
        return ReturnEvent.builder()
                .eventType(ReturnEventType.RETURN_COMMITTED)
                .id(1L)
                .returnId(1L)
                .created(DateTime.now().toDate().toInstant())
                .orderExternalId("orderExtId")
                .payload(new ReturnCommittedPayload()
                        .setOrderItemsInfo(getOrderItemInfosWithNullSupplierId(
                                Map.of("CIS", "2489571_item1_cis1", "UIT", "2489571_item1_uit1")
                        ))
                        .setExternalId("return-id-1")
                        .setBoxes(List.of(ReturnBox.builder().externalId("box1").build()))
                        .setItems(List.of(
                                ReturnItem.builder()
                                        .vendorCode(SKU)
                                        .supplierId(null)
                                        .boxExternalId("box1")
                                        .returnReason("return reason")
                                        .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                        .build()))
                        .setSource(ReturnSource.CLIENT)
                )
                .build();
    }

    @Nonnull
    private ReturnEntity getReturnEntityExpected() {
        var returnEntity = ReturnEntity.builder()
                .externalId("return-id-1")
                .orderExternalId("orderExtId")
                .build();
        ReturnBoxEntity box1 = ReturnBoxEntity.builder().returnEntity(returnEntity).externalId("box1").build();
        var items = Set.of(
                ReturnItemEntity.builder()
                        .returnEntity(returnEntity)
                        .box(box1)
                        .article(SKU)
                        .supplierId(SUPPLIER_ID)
                        .instances(getTestInstances())
                        .returnReason("return reason")
                        .reasonType(BAD_QUALITY)
                        .build()
        );
        returnEntity.setBoxes(Set.of(box1));
        returnEntity.setItems(items);
        return returnEntity;
    }

    @Nonnull
    private ReturnEntity getReturnEntityExpectedWithMultipleItems() {
        var returnEntity = ReturnEntity.builder()
                .externalId("return-id-1")
                .orderExternalId("orderExtId")
                .build();
        ReturnBoxEntity box1 = ReturnBoxEntity.builder().returnEntity(returnEntity).externalId("box1").build();
        var items = Set.of(
                ReturnItemEntity.builder()
                        .returnEntity(returnEntity)
                        .box(box1)
                        .article(SKU)
                        .supplierId(SUPPLIER_ID)
                        .instances(getTestInstances())
                        .returnReason("return reason1")
                        .reasonType(BAD_QUALITY)
                        .build(),
                ReturnItemEntity.builder()
                        .returnEntity(returnEntity)
                        .box(box1)
                        .article(SKU)
                        .supplierId(SUPPLIER_ID)
                        .instances(getTestInstances())
                        .returnReason("return reason")
                        .reasonType(BAD_QUALITY)
                        .build()
        );
        returnEntity.setBoxes(Set.of(box1));
        returnEntity.setItems(items);
        return returnEntity;
    }

    @Nonnull
    private List<RegistryUnitId> getTestInstances() {
        return List.of(lrmService.getRegistryUnit(
                Map.of("CIS", "2489571_item1_cis1", "UIT", "2489571_item1_uit1")));
    }


    @Nonnull
    private ReturnEntity getReturnEntityNullSupplierExpected() {
        var returnEntity = ReturnEntity.builder()
                .externalId("return-id-1")
                .orderExternalId("orderExtId")
                .build();
        ReturnBoxEntity box1 = ReturnBoxEntity.builder().returnEntity(returnEntity).externalId("box1").build();
        var items = Set.of(
                ReturnItemEntity.builder()
                        .returnEntity(returnEntity)
                        .box(box1)
                        .article(SKU)
                        .supplierId(null)
                        .instances(getTestInstances())
                        .returnReason("return reason")
                        .reasonType(BAD_QUALITY)
                        .build()
        );
        returnEntity.setBoxes(Set.of(box1));
        returnEntity.setItems(items);
        return returnEntity;
    }

    @Nonnull
    private List<OrderItemInfo> getOrderItemInfos(Map<String, String> instancesWithNulls) {
        return List.of(OrderItemInfo.builder()
                .vendorCode(SKU)
                .supplierId(SUPPLIER_ID)
                .instances(List.of(instancesWithNulls))
                .build());
    }

    @Nonnull
    private List<ReturnItem> getReturnItemInfos(Map<String, String> instancesWithNulls) {
        return List.of(ReturnItem.builder()
                .vendorCode(SKU)
                .supplierId(SUPPLIER_ID)
                .instances(instancesWithNulls)
                .build());
    }

    @Nonnull
    private List<ReturnItem> getReturnItemInfos(List<Map<String, String>> instancesWithNulls) {
        return instancesWithNulls.stream()
                .map(instances ->
                        ReturnItem.builder()
                                .vendorCode(SKU)
                                .supplierId(SUPPLIER_ID)
                                .instances(instances)
                                .build())
                .collect(Collectors.toList());
    }

    @Nonnull
    private List<OrderItemInfo> getOrderItemInfosWithNullSupplierId(Map<String, String> instancesWithNulls) {
        return List.of(OrderItemInfo.builder()
                .vendorCode(SKU)
                .supplierId(null)
                .instances(List.of(instancesWithNulls))
                .build());
    }

    @Nonnull
    private List<OrderItemInfo> getOrderItemInfos(List<Map<String, String>> instancesWithNulls) {
        return instancesWithNulls.stream()
                .map(instances ->
                        OrderItemInfo.builder()
                                .vendorCode(SKU)
                                .supplierId(SUPPLIER_ID)
                                .instances(List.of(instances))
                                .build())
                .collect(Collectors.toList());
    }

    @Nonnull
    private List<OrderItemInfo> getOrderItemInfosMultiple(List<List<Map<String, String>>> instancesWithNulls) {
        return instancesWithNulls.stream()
                .map(instances ->
                        OrderItemInfo.builder()
                                .vendorCode(SKU)
                                .supplierId(SUPPLIER_ID)
                                .instances(instances)
                                .build())
                .collect(Collectors.toList());
    }

    @Nonnull
    private SupplierSkuKeyWithReturnReason getSupplierSkuKeyWithReturnReason() {
        return new SupplierSkuKeyWithReturnReason(SUPPLIER_ID, SKU,
                BAD_QUALITY, "");
    }

    @Nonnull
    private List<ReturnItem> getReturnItemWithBox(String boxId) {
        return List.of(ReturnItem.builder().boxExternalId(boxId).build());
    }

    private ReturnEvent getReturnEventWithNullSupplierAndReturnSource() {
        return ReturnEvent.builder()
                .eventType(ReturnEventType.RETURN_COMMITTED)
                .id(1L)
                .created(DateTime.now().toDate().toInstant())
                .returnId(1L)
                .orderExternalId("orderExtId")
                .payload(new ReturnCommittedPayload()
                        .setOrderItemsInfo(getOrderItemInfosWithNullSupplierId(
                                Map.of("CIS", "2489571_item1_cis1", "UIT", "2489571_item1_uit1")
                        ))
                        .setExternalId("return-id-1")
                        .setBoxes(List.of(ReturnBox.builder().externalId("box1").build()))
                        .setItems(List.of(
                                ReturnItem.builder()
                                        .vendorCode(SKU)
                                        .supplierId(null)
                                        .boxExternalId("box1")
                                        .returnReason("return reason")
                                        .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                        .build()))
                        .setSource(ReturnSource.CLIENT)
                )
                .build();
    }
}
