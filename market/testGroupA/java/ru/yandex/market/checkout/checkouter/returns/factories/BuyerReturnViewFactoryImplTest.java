package ru.yandex.market.checkout.checkouter.returns.factories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.MarketReportSearchService;
import ru.yandex.market.checkout.checkouter.order.archive.ArchiveStorageFutureFactory;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus;
import ru.yandex.market.checkout.checkouter.returns.ReturnFeature;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.checkouter.storage.returns.buyers.BuyerReturnDao;
import ru.yandex.market.checkout.checkouter.storage.returns.buyers.BuyerReturnItemDao;
import ru.yandex.market.checkout.checkouter.storage.returns.buyers.entities.BuyerReturnDeliveryEntity;
import ru.yandex.market.checkout.checkouter.storage.returns.buyers.entities.BuyerReturnEntity;
import ru.yandex.market.checkout.checkouter.storage.returns.buyers.entities.BuyerReturnItemEntity;
import ru.yandex.market.checkout.checkouter.storage.returns.buyers.filters.BuyerReturnFilter;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.BuyerReturnViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.delivery.BuyerReturnCourierDeliveryViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.delivery.BuyerReturnDeliveryViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.delivery.BuyerReturnPickupDeliveryViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.delivery.BuyerReturnPostDeliveryViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.items.BuyerReturnOrderItemViewModel;
import ru.yandex.market.checkout.test.providers.AddressProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

class BuyerReturnViewFactoryImplTest {

    private BuyerReturnViewFactory factory;
    @Mock
    private BuyerReturnDao buyerReturnDao;
    @Mock
    private BuyerReturnItemDao buyerReturnItemDao;
    @Mock
    private MarketReportSearchService reportSearchService;
    @Mock
    private ArchiveStorageFutureFactory archiveStorageFutureFactory;
    @Mock
    private TransactionTemplate readonlyTransactionTemplate;
    @Mock
    private CheckouterFeatureReader reader;

    private BuyerReturnFilter filter;
    private final Long returnId1 = 1L;
    private final Long itemId1 = 10L;
    private final Long itemId2 = 11L;
    private final Long deliveryId1 = 100L;
    private final Long deliveryServiceId1 = 101L;
    private final Long outletId = 1000L;
    private final Long orderItemId = 123L;
    private final Long supplierId = 34555L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        filter = BuyerReturnFilter.builder(123L).build();
        factory = new BuyerReturnViewFactoryImpl(
                buyerReturnDao, buyerReturnItemDao, reportSearchService, archiveStorageFutureFactory,
                readonlyTransactionTemplate, reader);
        when(archiveStorageFutureFactory.buildFutures(Mockito.any(ProceedingJoinPoint.class)))
                .thenReturn(List.of());
        //Mockito.when(buyerReturnItemDao.findByReturnIds(Mockito.anyList())).thenReturn(List.of());
        when(readonlyTransactionTemplate.execute(Mockito.any())).thenReturn(List.of(returnId1));
        when(reader.getBoolean(Mockito.eq(BooleanFeatureType.ENABLE_GETTING_ARCHIVED_RETURNS))).thenReturn(true);
    }

    @Test
    void withEmptyResults() {
        List<BuyerReturnViewModel> results = factory.loadByFilter(filter);
        assertThat(results).isEmpty();
    }

    @Test
    void testConvertWithReturnOrderItemOnly() {
        BuyerReturnEntity returnEntity = initReturnEntity(returnId1);
        List<BuyerReturnItemEntity> items = initReturnOrderItemEntity(returnEntity, itemId1);
        when(buyerReturnItemDao.findByReturnIds(List.of(returnId1))).thenReturn(items);
        when(buyerReturnDao.findPartsByFilter(Mockito.any(BuyerReturnFilter.class)))
                .thenReturn(List.of(returnEntity));

        List<BuyerReturnViewModel> results = factory.loadByFilter(filter);

        assertThat(extractReturnIds(results)).containsOnly(returnId1);
        var aReturn = results.get(0);
        assertTrue(aReturn.isFastReturn());
        assertFalse(aReturn.isLargeSize());
        assertThat(extractItemIds(results)).containsOnly(itemId1);
        assertNotNull(aReturn.getFeatures());
        assertTrue(aReturn.getFeatures().contains(ReturnFeature.SHOP_HOTLINE));
    }

    @Test
    void testPrecisionFoCompensationSums() {
        BuyerReturnEntity returnEntity = initReturnEntity(returnId1);
        List<BuyerReturnItemEntity> items = initReturnOrderItemEntity(returnEntity, itemId1);
        BuyerReturnItemEntity returnItem = items.get(0);
        returnItem.setOrderItemBuyerPrice(new BigDecimal("249"));
        returnItem.setOrderItemCashBackAmount(new BigDecimal("121"));
        returnItem.setCount(6);
        returnItem.setCountAtOrder(6);
        when(buyerReturnItemDao.findByReturnIds(List.of(returnId1))).thenReturn(items);
        when(buyerReturnDao.findPartsByFilter(Mockito.any(BuyerReturnFilter.class)))
                .thenReturn(List.of(returnEntity));

        List<BuyerReturnViewModel> results = factory.loadByFilter(filter);

        BuyerReturnViewModel aReturn = results.get(0);
        assertEquals(aReturn.getBasicCompensationSum(), new BigDecimal(249 * 6 - 121));
        assertEquals(aReturn.getCashbackCompensationSum(), new BigDecimal(121));
    }

    @Test
    void testConvertWithReturnOrderItemAndReturnDeliveryItem() {
        BuyerReturnEntity returnEntity = initReturnEntity(returnId1);
        List<BuyerReturnItemEntity> items = initReturnOrderItemEntity(returnEntity, itemId1);
        items.addAll(initReturnDeliveryItemEntity(returnEntity, itemId2));
        when(buyerReturnItemDao.findByReturnIds(List.of(returnId1))).thenReturn(items);
        when(buyerReturnDao.findPartsByFilter(Mockito.any(BuyerReturnFilter.class)))
                .thenReturn(List.of(returnEntity));

        List<BuyerReturnViewModel> results = factory.loadByFilter(filter);

        assertThat(extractReturnIds(results)).containsOnly(returnId1);
        assertThat(extractItemIds(results)).containsOnly(itemId1);
    }

    @Test
    void testReturnPostDeliveryConvert() {
        BuyerReturnPostDeliveryViewModel expected = new BuyerReturnPostDeliveryViewModel();
        expected.setId(123L);
        expected.setDeliveryServiceId(34662L);
        expected.setStatus(ReturnDeliveryStatus.DELIVERY);
        expected.setStatusUpdateDate(LocalDateTime.now());
        expected.setOutlet(defaultOutlet());
        expected.setPostTrackCode("i am track");

        BuyerReturnEntity returnEntity = initReturnEntity(returnId1);
        returnEntity.setReturnDelivery(initDeliveryEntityByTemplate(expected));
        List<BuyerReturnItemEntity> items = initReturnOrderItemEntity(returnEntity, itemId1);
        mockShopOutlet();
        when(buyerReturnItemDao.findByReturnIds(List.of(returnId1))).thenReturn(items);
        when(buyerReturnDao.findPartsByFilter(Mockito.any(BuyerReturnFilter.class)))
                .thenReturn(List.of(returnEntity));

        List<BuyerReturnViewModel> results = factory.loadByFilter(filter);

        BuyerReturnPostDeliveryViewModel result = (BuyerReturnPostDeliveryViewModel) results.get(0).getDelivery();
        assertCommonDeliveryFields(result, expected);
        assertThat(result.getOutlet().getId()).isEqualTo(expected.getOutlet().getId());
        assertThat(result.getPostTrackCode()).isEqualTo(expected.getPostTrackCode());
    }

    @Test
    void testReturnPickupDeliveryConvert() {
        BuyerReturnPickupDeliveryViewModel expected = new BuyerReturnPickupDeliveryViewModel();
        expected.setId(123L);
        expected.setDeliveryServiceId(34662L);
        expected.setStatus(ReturnDeliveryStatus.DELIVERY);
        expected.setStatusUpdateDate(LocalDateTime.now());
        expected.setOutlet(defaultOutlet());

        BuyerReturnEntity returnEntity = initReturnEntity(returnId1);
        returnEntity.setReturnDelivery(initDeliveryEntityByTemplate(expected));
        List<BuyerReturnItemEntity> items = initReturnOrderItemEntity(returnEntity, itemId1);
        mockShopOutlet();
        when(buyerReturnItemDao.findByReturnIds(List.of(returnId1))).thenReturn(items);
        when(buyerReturnDao.findPartsByFilter(Mockito.any(BuyerReturnFilter.class)))
                .thenReturn(List.of(returnEntity));

        List<BuyerReturnViewModel> results = factory.loadByFilter(filter);

        BuyerReturnPickupDeliveryViewModel result = (BuyerReturnPickupDeliveryViewModel) results.get(0).getDelivery();
        assertCommonDeliveryFields(result, expected);
        assertThat(result.getOutlet().getId()).isEqualTo(expected.getOutlet().getId());
    }

    @Test
    void testReturnCourierDeliveryConvert() {
        BuyerReturnCourierDeliveryViewModel expected = new BuyerReturnCourierDeliveryViewModel();
        expected.setId(123L);
        expected.setDeliveryServiceId(34662L);
        expected.setStatus(ReturnDeliveryStatus.DELIVERY);
        expected.setStatusUpdateDate(LocalDateTime.now());
        expected.setDates(new DeliveryDates(new Date(123L), new Date(345L), LocalTime.MIN, LocalTime.MAX));
        expected.setSenderAddress(AddressProvider.getSenderAddress());

        BuyerReturnEntity returnEntity = initReturnEntity(returnId1);
        returnEntity.setReturnDelivery(initDeliveryEntityByTemplate(expected));
        List<BuyerReturnItemEntity> items = initReturnOrderItemEntity(returnEntity, itemId1);
        when(buyerReturnItemDao.findByReturnIds(List.of(returnId1))).thenReturn(items);
        when(buyerReturnDao.findPartsByFilter(Mockito.any(BuyerReturnFilter.class)))
                .thenReturn(List.of(returnEntity));

        List<BuyerReturnViewModel> results = factory.loadByFilter(filter);

        BuyerReturnCourierDeliveryViewModel result = (BuyerReturnCourierDeliveryViewModel) results.get(0).getDelivery();
        assertCommonDeliveryFields(result, expected);
        assertThat(result.getDates()).isEqualTo(expected.getDates());
        assertThat(result.getSenderAddress()).isEqualTo(AddressProvider.getSenderAddress());
    }

    private void assertCommonDeliveryFields(
            BuyerReturnDeliveryViewModel result,
            BuyerReturnDeliveryViewModel expected
    ) {
        assertThat(result.getId()).isEqualTo(expected.getId());
        assertThat(result.getDeliveryServiceId()).isEqualTo(expected.getDeliveryServiceId());
        assertThat(result.getType()).isEqualTo(expected.getType());
        assertThat(result.getStatus()).isEqualTo(expected.getStatus());
        assertThat(result.getStatusUpdateDate()).isEqualTo(expected.getStatusUpdateDate());
    }

    @Test
    void testConvertWithExceptionOnReport() {
        BuyerReturnEntity returnEntity = initReturnEntity(returnId1);
        BuyerReturnDeliveryEntity deliveryEntity = initDeliveryEntity(deliveryId1);
        deliveryEntity.setOutletId(outletId);
        returnEntity.setReturnDelivery(deliveryEntity);
        List<BuyerReturnItemEntity> items = initReturnOrderItemEntity(returnEntity, itemId1);
        when(buyerReturnItemDao.findByReturnIds(List.of(returnId1))).thenReturn(items);
        when(buyerReturnDao.findPartsByFilter(Mockito.any(BuyerReturnFilter.class)))
                .thenReturn(List.of(returnEntity));
        mockReportShopError();

        List<BuyerReturnViewModel> results = factory.loadByFilter(filter);

        assertThat(extractReturnIds(results)).containsOnly(returnId1);
        assertThat(extractItemIds(results)).containsOnly(itemId1);
        assertThat(extractDeliveryIds(results)).containsOnly(deliveryId1);
    }

    private List<Long> extractReturnIds(List<BuyerReturnViewModel> returns) {
        return returns.stream()
                .map(BuyerReturnViewModel::getId)
                .collect(Collectors.toList());
    }

    private List<Long> extractItemIds(List<BuyerReturnViewModel> returns) {
        return returns.stream()
                .map(BuyerReturnViewModel::getOrderItems)
                .flatMap(Collection::stream)
                .map(BuyerReturnOrderItemViewModel::getId)
                .collect(Collectors.toList());
    }

    private List<Long> extractDeliveryIds(List<BuyerReturnViewModel> returns) {
        return returns.stream()
                .map(BuyerReturnViewModel::getDelivery)
                .filter(Objects::nonNull)
                .map(BuyerReturnDeliveryViewModel::getId)
                .collect(Collectors.toList());
    }

    private void mockReportShopError() {
        when(reportSearchService.searchShopOutlets(
                any(), isNull(), Mockito.eq(Set.of(outletId)), any(), Mockito.eq(false)))
                .thenThrow(RuntimeException.class);
    }

    private ShopOutlet defaultOutlet() {
        ShopOutlet outlet = new ShopOutlet();
        outlet.setId(outletId);
        return outlet;
    }

    private void mockShopOutlet() {
        ShopOutlet outlet = defaultOutlet();
        when(reportSearchService.searchShopOutlets(
                any(), isNull(), Mockito.eq(Set.of(outletId)), any(), Mockito.eq(false)))
                .thenReturn(List.of(outlet));
    }

    private BuyerReturnDeliveryEntity initDeliveryEntity(Long deliveryId1) {
        return initDeliveryEntity(deliveryId1, null);
    }

    private BuyerReturnDeliveryEntity initDeliveryEntity(Long deliveryId1, LocalDateTime statusUpdateDate) {
        BuyerReturnDeliveryEntity returnDelivery = new BuyerReturnDeliveryEntity();
        returnDelivery.setId(deliveryId1);
        returnDelivery.setDeliveryServiceId(deliveryServiceId1);
        returnDelivery.setType(DeliveryType.PICKUP);
        returnDelivery.setStatus(ReturnDeliveryStatus.CREATED);
        returnDelivery.setStatusUpdateDate(statusUpdateDate);
        return returnDelivery;
    }

    private BuyerReturnDeliveryEntity initDeliveryEntityByTemplate(BuyerReturnDeliveryViewModel template) {
        BuyerReturnDeliveryEntity returnDelivery = new BuyerReturnDeliveryEntity();
        returnDelivery.setId(template.getId());
        returnDelivery.setDeliveryServiceId(template.getDeliveryServiceId());
        returnDelivery.setType(template.getType());
        returnDelivery.setStatus(template.getStatus());
        returnDelivery.setStatusUpdateDate(template.getStatusUpdateDate());
        switch (template.getType()) {
            case POST:
                BuyerReturnPostDeliveryViewModel postTemplate = (BuyerReturnPostDeliveryViewModel) template;
                returnDelivery.setOutletId(postTemplate.getOutlet().getId());
                returnDelivery.setPostTrackCode(postTemplate.getPostTrackCode());
                break;
            case PICKUP:
                BuyerReturnPickupDeliveryViewModel pickupTemplate = (BuyerReturnPickupDeliveryViewModel) template;
                returnDelivery.setOutletId(pickupTemplate.getOutlet().getId());
                break;
            case DELIVERY:
                BuyerReturnCourierDeliveryViewModel courierTemplate = (BuyerReturnCourierDeliveryViewModel) template;
                returnDelivery.setDates(courierTemplate.getDates());
                returnDelivery.setSenderAddress(courierTemplate.getSenderAddress());
                break;
            default:
                throw new IllegalStateException("Unknown delivery type " + template.getType());
        }
        return returnDelivery;
    }

    private BuyerReturnEntity initReturnEntity(long id) {
        BuyerReturnEntity returnEntity = new BuyerReturnEntity();
        returnEntity.setId(id);
        returnEntity.setOrderId(123L);
        returnEntity.setStatus(ReturnStatus.STARTED_BY_USER);
        returnEntity.setCreatedDate(LocalDateTime.now());
        returnEntity.setStatusUpdatedDate(LocalDateTime.now());
        returnEntity.setLargeSize(false);
        returnEntity.setFastReturn(true);
        returnEntity.setOrderCreatedDate(LocalDateTime.now());
        returnEntity.setOrderFulfilment(true);
        returnEntity.setOrderDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        returnEntity.setOrderDeliveryPrice(BigDecimal.valueOf(3));
        returnEntity.setOrderCardNumber("220220****3085");
        returnEntity.setOrderPaymentType(PaymentType.PREPAID);
        returnEntity.setFeatures(Set.of(ReturnFeature.SHOP_HOTLINE));
        return returnEntity;
    }

    private List<BuyerReturnItemEntity> initReturnOrderItemEntity(
            BuyerReturnEntity returnEntity,
            long... returnItemsIds
    ) {
        List<BuyerReturnItemEntity> items = new ArrayList<>();
        for (long itemId : returnItemsIds) {
            BuyerReturnItemEntity item = new BuyerReturnItemEntity();
            item.setId(itemId);
            item.setReturnId(returnEntity.getId());
            item.setOrderItemId(orderItemId);
            item.setCount(1);
            item.setReasonType(ReturnReasonType.WRONG_ITEM);
            item.setOrderItemCashBackAmount(BigDecimal.valueOf(8));
            item.setCountAtOrder(4);
            item.setOrderItemBuyerPrice(BigDecimal.valueOf(10));
            item.setSupplierId(supplierId);
            items.add(item);
        }
        return items;
    }

    private List<BuyerReturnItemEntity> initReturnDeliveryItemEntity(
            BuyerReturnEntity returnEntity,
            long... returnItemsIds
    ) {
        List<BuyerReturnItemEntity> items = new ArrayList<>();
        for (long itemId : returnItemsIds) {
            BuyerReturnItemEntity item = new BuyerReturnItemEntity();
            item.setId(itemId);
            item.setReturnId(returnEntity.getId());
            item.setOrderDeliveryId(deliveryServiceId1);
            item.setCount(1);
            items.add(item);
        }
        return items;
    }
}
