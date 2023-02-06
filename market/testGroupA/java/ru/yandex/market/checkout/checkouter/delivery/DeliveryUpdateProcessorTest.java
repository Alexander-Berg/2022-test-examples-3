package ru.yandex.market.checkout.checkouter.delivery;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.outlet.DeliveryOutletService;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ShopDeliveryParcelsUpdateProcessor;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.order.MarketReportInfoFetcher;
import ru.yandex.market.checkout.checkouter.order.MarketReportSearchService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.delivery.DeliveryUpdateActions;
import ru.yandex.market.checkout.checkouter.order.delivery.DeliveryUpdateOptions;
import ru.yandex.market.checkout.checkouter.order.delivery.ParcelUpdateActions;
import ru.yandex.market.checkout.checkouter.service.business.OrderFinancialService;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.common.pay.FinancialValidator;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.common.util.collections.CollectionUtils.first;

@ExtendWith(MockitoExtension.class)
public class DeliveryUpdateProcessorTest {

    private static final long OLD_SHIPMENT_ID = 45;
    private static final long ORDER_ITEM_ID = 1;
    @Mock
    OrderFinancialService orderFinancialService;
    @Mock
    private CheckouterFeatureReader checkouterFeatureReader;
    @Mock
    private CheckouterFeatureWriter checkouterFeatureWriter;

    private DeliveryUpdateProcessor deliveryUpdateProcessor;
    // Bootstrap data
    private Order order;
    private Delivery newDelivery;
    @Mock
    private DeliveryOutletService deliveryOutletService;
    @Mock
    private ShopDeliveryServicesService shopDeliveryServicesService;
    @Mock
    private MarketReportInfoFetcher marketReportInfoFetcher;
    @Mock
    private MarketReportSearchService marketReportSearchService;
    @Mock
    private ParcelsUpdateProcessor parcelsUpdateProcessor;
    @Mock
    private PersonalDataService personalDataService;

    private static Delivery createNewDeliveryWithTrack() {
        Parcel shipment = new Parcel();
        shipment.setId(45L);
        shipment.addTrack(TrackProvider.createTrack("qwerty", 99L));

        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(shipment));
        return newDelivery;
    }

    @BeforeEach
    public void setUp() {
        DeliveryValidator deliveryValidator = new DeliveryValidator();
        deliveryValidator.setFinancialValidator(new FinancialValidator());
        deliveryValidator.setCheckouterFeatureReader(checkouterFeatureReader);
        deliveryValidator.setPersonalDataService(personalDataService);

        parcelsUpdateProcessor = new ShopDeliveryParcelsUpdateProcessor(
                new SelfDeliveryPossibleUpdateProvider(),
                Clock.systemDefaultZone(),
                checkouterFeatureReader
        );

        deliveryUpdateProcessor = new DeliveryUpdateProcessor(
                deliveryOutletService,
                deliveryValidator,
                shopDeliveryServicesService,
                marketReportInfoFetcher,
                marketReportSearchService,
                orderFinancialService,
                checkouterFeatureReader,
                personalDataService);

        AddressImpl oldShopAddress = new AddressImpl();
        oldShopAddress.setCountry("Россия");
        oldShopAddress.setCity("Москва");
        oldShopAddress.setHouse("15");
        oldShopAddress.setRecipient("Лев Толстой");
        oldShopAddress.setPhone("+79272403522");

        Delivery oldDelivery = new Delivery();
        oldDelivery.setType(DeliveryType.DELIVERY);
        oldDelivery.setServiceName("ООО Рога и Копыта");
        oldDelivery.setPrice(BigDecimal.valueOf(100));
        oldDelivery.setBuyerPrice(BigDecimal.valueOf(100));
        oldDelivery.setShopAddress(oldShopAddress);

        Parcel oldShipment = new Parcel();
        oldShipment.setId(OLD_SHIPMENT_ID);
        oldDelivery.setParcels(Collections.singletonList(oldShipment));

        Calendar fromCalendar = Calendar.getInstance();
        fromCalendar.set(2017, Calendar.JANUARY, 1);

        Calendar toCalendar = Calendar.getInstance();
        toCalendar.set(2017, Calendar.JANUARY, 2);

        oldDelivery.setDeliveryDates(new DeliveryDates(fromCalendar.getTime(), toCalendar.getTime()));

        order = new Order();
        order.setId(135135L);
        order.setStatus(OrderStatus.PROCESSING);
        order.setDelivery(oldDelivery);

        OrderItem item = new OrderItem();
        item.setId(ORDER_ITEM_ID);
        item.setCount(5);
        order.addItem(item);

        newDelivery = createNewDeliveryWithTrack();

        PersAddress address = new PersAddress();
        address.setCountry("Rus");
        address.setCity("Msc");
        address.setHouse("123");

        Mockito.lenient().when(personalDataService.retrieve(Mockito.any()))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null, address, null));

        Mockito.lenient().when(personalDataService.getPersAddress(Mockito.any())).thenReturn(address);
    }

    /**
     * Не падает с ошибкой в случае если треки не переданы
     */
    @Test
    public void shouldNotFailOnNullTracks() {
        newDelivery.getParcels().get(0).setTracks(null);

        validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
    }

    /**
     * Можно обновлять треки заказа в статусе UNPAID если задана специальная опция обновления
     */
    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void shouldAllowToUpdateInInappropriateStatusesIfExplicitlyAskedFor() {
        order.setStatus(OrderStatus.UNPAID);
        order.setGlobal(true);

        Track track = new Track();
        track.setDeliveryServiceId(99L);
        track.setTrackCode("asdasd");

        Delivery newDelivery = new Delivery();
        newDelivery.addTrack(track);

        deliveryUpdateProcessor.process(order, newDelivery, ClientInfo.SYSTEM,
                DeliveryUpdateOptions.IGNORE_STATUS_CHECK, parcelsUpdateProcessor);
    }

    //  --------------------------------------- self delivery: ---------------------------------------------------------

    /**
     * Для роли SYSTEM доступно обновление треков глобального заказа, доставляемом собственной службой доставки
     */
    @Test
    public void shouldAllowTracksForSelfDeliveredNonGlobal() {
        order.getDelivery().getParcels().get(0).setId(OLD_SHIPMENT_ID);

        checkAllowTracksForSelfDeliveredGlobal();
    }

    /**
     * Для роли SYSTEM доступно обновление треков глобального заказа
     */
    @Test
    public void shouldAllowTracksForSelfDeliveredGlobal() {
        order.setGlobal(true);
        newDelivery.getParcels().get(0).setId(OLD_SHIPMENT_ID);

        checkAllowTracksForSelfDeliveredGlobal();
    }

    private void checkAllowTracksForSelfDeliveredGlobal() {
        DeliveryUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
        List<ParcelUpdateActions> shipmentsToUpdate = actions.getParcelsUpdateActions().getParcelUpdates();
        assertThat(shipmentsToUpdate, hasSize(1));

        List<Track> tracks = shipmentsToUpdate.get(0).getTracksToInsert();
        assertThat(tracks, hasSize(1));
        tracks.forEach(track -> assertEquals(TrackStatus.NEW, track.getStatus()));
    }

    /**
     * Для роли USER недоступно добавление треков
     */
    @Test
    public void shouldNotAllowUserToAddTrack() {
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            order.setGlobal(true);
            order.getDelivery().getParcels().get(0).setId(OLD_SHIPMENT_ID);

            validateDelivery(order, newDelivery, new ClientInfo(ClientRole.USER, 2345L));
        });
    }

    /**
     * Магазину можно добавлять треки к своему глобальному заказу, доставляемому
     * собственной службой доставки
     */
    @Test
    public void shouldAllowShopToAddTrackForSelfDelivered() {
        order.setGlobal(true);

        Parcel oldShipment = new Parcel();
        oldShipment.setId(OLD_SHIPMENT_ID);
        order.getDelivery().setParcels(Collections.singletonList(oldShipment));

        newDelivery.getParcels().get(0).setId(OLD_SHIPMENT_ID);

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, new ClientInfo(ClientRole.SHOP, 2345L))
                .getParcelsUpdateActions();

        List<ParcelUpdateActions> shipmentsToUpdate = actions.getParcelUpdates();
        assertThat(shipmentsToUpdate, hasSize(1));
        assertEquals(OLD_SHIPMENT_ID, (long) shipmentsToUpdate.get(0).getParcel().getId());

        List<Track> tracks = shipmentsToUpdate.get(0).getTracksToInsert();
        assertThat(tracks, hasSize(1));
    }

    /**
     * Сохранение тех же самых треков недолжно приводить к ошибке
     */
    @Test
    public void resaveSameTrackShouldNotFailForShopRole() {
        order.setGlobal(true);

        Delivery oldDelivery = order.getDelivery();
        oldDelivery.setParcels(newDelivery.getParcels());
        oldDelivery.getParcels().get(0).getTracks().get(0).setId(111L);

        validateDelivery(order, newDelivery, new ClientInfo(ClientRole.SHOP, 2345L));

        List<Parcel> parcels = order.getDelivery().getParcels();
        assertNotNull(parcels);

        assertEquals(1, parcels.size());

        Parcel shipment = parcels.get(0);
        Collection<Track> tracks = shipment.getTracks();

        assertThat(tracks, hasSize(1));
        assertEquals("qwerty", first(shipment.getTracks()).getTrackCode());
        assertEquals(99L, (long) first(shipment.getTracks()).getDeliveryServiceId());
    }

    //  --------------------------------------- market delivery: -------------------------------------------------------

    /**
     * Магазину можно добавлять треки к заказу, доставляемому сторонней службой доставки
     */
    @Test
    public void shouldAllowShopToAddTrackForMarketDelivered() {
        order.getDelivery().setDeliveryServiceId(DeliveryProvider.RUSPOSTPICKUP_DELIVERY_SERVICE_ID);

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, new ClientInfo(ClientRole.SHOP, 2345L))
                .getParcelsUpdateActions();

        checkParcelTracksToInsert(actions);
    }

    /**
     * Магазину можно редактировать треки заказа, доставляемого сторонней службой доставки
     */
    @Test
    public void shouldAllowShopToEditTrackForMarketDelivered() {
        order.getDelivery().setDeliveryServiceId(DeliveryProvider.RUSPOSTPICKUP_DELIVERY_SERVICE_ID);
        order.getDelivery().setTracks(newDelivery.getTracks());

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, new ClientInfo(ClientRole.SHOP, 2345L))
                .getParcelsUpdateActions();

        checkParcelTracksToInsert(actions);
    }

    private void checkParcelTracksToInsert(ParcelsUpdateActions actions) {
        List<ParcelUpdateActions> parcelUpdates = actions.getParcelUpdates();
        assertThat(parcelUpdates, hasSize(1));
        assertThat(parcelUpdates.get(0).getTracksToInsert(), hasSize(1));
    }

    // ----------------------------------------- track editing: --------------------------------------------------------

    /**
     * Роли SYSTEM разрешена установка трекерного id трека
     */
    @Test
    public void shouldAllowSystemToEditTrackerId() {
        order.setGlobal(true);
        Track track = new Track();
        track.setId(111L);
        track.setDeliveryServiceId(99L);
        track.setTrackCode("qwerty");
        track.setStatus(TrackStatus.STARTED);

        Parcel oldShipment = new Parcel();
        oldShipment.setId(45L);
        oldShipment.addTrack(track);

        order.getDelivery().setParcels(Collections.singletonList(oldShipment));

        Parcel newShipment = first(newDelivery.getParcels());
        newShipment.setId(45L);
        newShipment.getTracks().forEach(t -> t.setTrackerId(123L));

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM)
                .getParcelsUpdateActions();
        ParcelUpdateActions parcelUpdateActions = actions.getParcelUpdates().get(0);

        List<Track> toUpdate = parcelUpdateActions.getTracksToUpdate();
        assertThat(toUpdate, hasSize(1));
        assertEquals(123L, (long) toUpdate.get(0).getTrackerId());
        assertEquals(TrackStatus.STARTED, toUpdate.get(0).getStatus());
    }

    /**
     * Магазину нельзя устанавливать трекерный id трека
     */
    @Test
    public void shouldNotAllowShopToEditTrackerId() {
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            order.setGlobal(true);

            Track track = new Track();
            track.setDeliveryServiceId(99L);
            track.setTrackCode("qwerty");

            Parcel oldShipment = new Parcel();
            oldShipment.setId(45L);
            oldShipment.addTrack(track);

            order.getDelivery().setParcels(Collections.singletonList(oldShipment));

            Parcel newShipment = first(newDelivery.getParcels());
            newShipment.setId(45L);
            newShipment.getTracks().forEach(t -> t.setTrackerId(123L));

            validateDelivery(order, newDelivery, new ClientInfo(ClientRole.SHOP, 12345L));
        });
    }

    /**
     * Несколько новых отправлений с заданныеми треками и товарами сохраняуются корректно
     */
    @Test
    public void testUpdateWithMultipleShipments() {
        order.setGlobal(true);
        order.getDelivery().setDeliveryServiceId(123L);

        Parcel shipment1 = new Parcel();
        shipment1.addTrack(TrackProvider.createTrack("iddqd1", 123L));
        shipment1.setParcelItems(Collections.singletonList(
                new ParcelItem(ORDER_ITEM_ID, 1)
        ));

        Parcel shipment2 = new Parcel();
        shipment2.addTrack(TrackProvider.createTrack("iddqd2", 123L));
        shipment2.setParcelItems(Collections.singletonList(
                new ParcelItem(ORDER_ITEM_ID, 2)
        ));

        newDelivery.setParcels(null);
        newDelivery.setParcels(Arrays.asList(
                order.getDelivery().getParcels().get(0),
                shipment1,
                shipment2
        ));

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM)
                .getParcelsUpdateActions();

        assertNull(actions.getParcelUpdates());
        assertThat(actions.getParcelsToRebind(), hasSize(1));
        assertEquals(OLD_SHIPMENT_ID, (long) actions.getParcelsToRebind().get(0).getId());

        List<Parcel> parcels = actions.getParcelsToInsert();
        assertNotNull(parcels);

        assertEquals(2, parcels.size());

        shipment1 = parcels.get(0);

        assertEquals(1, shipment1.getTracks().size());
        assertEquals("iddqd1", first(shipment1.getTracks()).getTrackCode());
        assertEquals(123L, (long) first(shipment1.getTracks()).getDeliveryServiceId());

        assertEquals(1, shipment1.getParcelItems().size());
        assertEquals(ORDER_ITEM_ID, (long) shipment1.getParcelItems().get(0).getItemId());
        assertEquals(1, (long) shipment1.getParcelItems().get(0).getCount());

        shipment2 = parcels.get(1);

        assertEquals(1, shipment2.getTracks().size());
        assertEquals("iddqd2", first(shipment2.getTracks()).getTrackCode());
        assertEquals(123L, (long) first(shipment2.getTracks()).getDeliveryServiceId());

        assertEquals(1, shipment2.getParcelItems().size());
        assertEquals(ORDER_ITEM_ID, (long) shipment2.getParcelItems().get(0).getItemId());
        assertEquals(2, (long) shipment2.getParcelItems().get(0).getCount());
    }

    /**
     * В случае если отправления не указаны, все существующие отправления помещаются
     * в список на привязку
     */
    @Test
    public void testIgnoreShipmentsIfNull() {
        order.setGlobal(true);

        newDelivery.setParcels(null);

        Parcel shipment = new Parcel();
        shipment.setId(OLD_SHIPMENT_ID);
        shipment.addTrack(TrackProvider.createTrack("iddqd1", 123L));
        order.getDelivery().setParcels(Collections.singletonList(shipment));

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM)
                .getParcelsUpdateActions();

        assertNull(actions.getParcelsToInsert());
        assertNull(actions.getParcelUpdates());
        assertThat(actions.getParcelsToRebind(), hasSize(1));
        assertEquals(OLD_SHIPMENT_ID, (long) actions.getParcelsToRebind().get(0).getId());
    }

    /**
     * В случае если указана пустая коллукция отправлений все существующие отправления
     * не попадают не в один список (т.е. идут на удаление)
     */
    @Test
    public void testDeleteAllShipments() {
        order.setGlobal(true);

        newDelivery.setParcels(Collections.emptyList());

        Parcel shipment = new Parcel();
        shipment.setId(45L);
        shipment.addTrack(TrackProvider.createTrack("iddqd1", 123L));
        order.getDelivery().setParcels(Collections.singletonList(shipment));

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM)
                .getParcelsUpdateActions();

        assertTrue(actions.notEmpty());
        assertNull(actions.getParcelUpdates());
        assertNull(actions.getParcelsToInsert());
        assertNull(actions.getParcelsToRebind());
        assertTrue(actions.hasDeletedParcels());
    }

    /**
     * При добавлении товара к уже существующему отправлению он помещяется в список на вставку
     */
    @Test
    public void testInsertNewParcelItem() {
        order.setGlobal(true);

        Parcel newShipment = newDelivery.getParcels().get(0);
        newShipment.setId(OLD_SHIPMENT_ID);
        newShipment.setTracks(null);
        newShipment.setParcelItems(Collections.singletonList(
                new ParcelItem(ORDER_ITEM_ID, 2)
        ));

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM)
                .getParcelsUpdateActions();

        assertTrue(actions.notEmpty());

        assertNull(actions.getParcelsToInsert());
        assertNull(actions.getParcelsToRebind());
        assertThat(actions.getParcelUpdates(), hasSize(1));

        ParcelUpdateActions parcelUpdateActions = actions.getParcelUpdates().get(0);
        assertEquals(OLD_SHIPMENT_ID, (long) parcelUpdateActions.getParcel().getId());

        assertThat(parcelUpdateActions.getItemsToInsert(), hasSize(1));

        ParcelItem item = parcelUpdateActions.getItemsToInsert().get(0);
        assertEquals(ORDER_ITEM_ID, (long) item.getItemId());
        assertEquals(2, (int) item.getCount());
        assertEquals(OLD_SHIPMENT_ID, (long) item.getParcelId());
    }

    /**
     * При редактировании товара он помещается в список на обновление
     */
    @Test
    public void testEditParcelItem() {
        order.setGlobal(true);

        Parcel oldShipment = order.getDelivery().getParcels().get(0);
        oldShipment.setParcelItems(Collections.singletonList(
                new ParcelItem(ORDER_ITEM_ID, 2)
        ));

        Parcel newShipment = newDelivery.getParcels().get(0);
        newShipment.setId(OLD_SHIPMENT_ID);
        newShipment.setTracks(null);
        newShipment.setParcelItems(Collections.singletonList(
                new ParcelItem(ORDER_ITEM_ID, 5)
        ));

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM)
                .getParcelsUpdateActions();
        assertTrue(actions.notEmpty());

        assertNull(actions.getParcelsToInsert());
        assertNull(actions.getParcelsToRebind());
        assertThat(actions.getParcelUpdates(), hasSize(1));
        ParcelUpdateActions parcelUpdateActions = actions.getParcelUpdates().get(0);

        assertNull(parcelUpdateActions.getItemsToInsert());
        assertNull(parcelUpdateActions.getItemsToRebind());
        assertThat(parcelUpdateActions.getItemsToUpdate(), hasSize(1));

        ParcelItem item = parcelUpdateActions.getItemsToUpdate().get(0);
        assertEquals(ORDER_ITEM_ID, (long) item.getItemId());
        assertEquals(5, (int) item.getCount());
    }

    /**
     * Товар, который не был указан в запросе не попадает ни в один из
     * списков (т.е. удет на удаление)
     */
    @Test
    public void testDeleteParcelItem() {
        order.setGlobal(true);

        OrderItem secondItem = new OrderItem();
        secondItem.setId(2L);
        secondItem.setCount(2);
        secondItem.setFeedOfferId(new FeedOfferId("another", 1L));
        order.addItem(secondItem);

        Parcel oldShipment = order.getDelivery().getParcels().get(0);
        oldShipment.setParcelItems(Arrays.asList(
                new ParcelItem(ORDER_ITEM_ID, 2),
                new ParcelItem(secondItem.getId(), 2)
        ));

        Parcel newShipment = newDelivery.getParcels().get(0);
        newShipment.setTracks(null);
        newShipment.setId(OLD_SHIPMENT_ID);
        newShipment.setParcelItems(Collections.singletonList(
                new ParcelItem(ORDER_ITEM_ID, 2)
        ));

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM)
                .getParcelsUpdateActions();

        assertTrue(actions.notEmpty());
        assertNull(actions.getParcelsToInsert());
        assertNull(actions.getParcelsToRebind());
        assertThat(actions.getParcelUpdates(), hasSize(1));

        ParcelUpdateActions parcelUpdateActions = actions.getParcelUpdates().get(0);
        assertEquals(OLD_SHIPMENT_ID, (long) parcelUpdateActions.getParcel().getId());

        assertTrue(parcelUpdateActions.hasItemsToDelete());
        assertNull(parcelUpdateActions.getItemsToInsert());
        assertNull(parcelUpdateActions.getItemsToUpdate());

        List<ParcelItem> items = parcelUpdateActions.getItemsToRebind();
        assertThat(items, hasSize(1));

        ParcelItem item = parcelUpdateActions.getItemsToRebind().get(0);
        assertEquals(ORDER_ITEM_ID, (long) item.getItemId());
    }

    /**
     * В случае если ни один товар не указан все существующие товары попадают в список привязки
     */
    @Test
    public void testIgnoreItemsIfNotPassed() {
        order.setGlobal(true);

        Parcel newShipment = newDelivery.getParcels().get(0);
        newShipment.setId(OLD_SHIPMENT_ID);
        newShipment.setParcelItems(null);
        newShipment.setTracks(null);

        Parcel oldShipment = order.getDelivery().getParcels().get(0);
        oldShipment.setParcelItems(Collections.singletonList(
                new ParcelItem(ORDER_ITEM_ID, 2)
        ));

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM)
                .getParcelsUpdateActions();
        assertNull(actions.getParcelsToInsert());
        assertNull(actions.getParcelUpdates());
        assertThat(actions.getParcelsToRebind(), hasSize(1));
        assertEquals(OLD_SHIPMENT_ID, (long) actions.getParcelsToRebind().get(0).getId());
    }

    /**
     * Нельзя указать отрицательное количество товара
     */
    @Test
    public void testDenyItemWithNegativeCount() {
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            order.setGlobal(true);

            Parcel newShipment = newDelivery.getParcels().get(0);
            newShipment.setId(OLD_SHIPMENT_ID);
            newShipment.setParcelItems(Collections.singletonList(
                    new ParcelItem(ORDER_ITEM_ID, -1)
            ));

            validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
        });
    }

    /**
     * Нельзя доавить товар в отправление если его нет в заказе
     */
    @Test
    public void testDenyItemIfItDoesNotExistInOrder() {
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            order.setGlobal(true);

            Parcel newShipment = newDelivery.getParcels().get(0);
            newShipment.setId(OLD_SHIPMENT_ID);
            newShipment.setParcelItems(Collections.singletonList(
                    new ParcelItem(2L, 2)
            ));

            validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
        });
    }

    /**
     * Нельзя добавить в отправления больше единиц товара чем он есть в заказе
     */
    @Test
    public void testDenyItemsIfMaxCountExceeded() {
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            order.setGlobal(true);

            Parcel firstShipment = new Parcel();
            firstShipment.setParcelItems(Collections.singletonList(
                    new ParcelItem(ORDER_ITEM_ID, 4)
            ));

            Parcel secondShipment = new Parcel();
            secondShipment.setParcelItems(Collections.singletonList(
                    new ParcelItem(ORDER_ITEM_ID, 2)
            ));

            newDelivery.setParcels(Arrays.asList(firstShipment, secondShipment));

            validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
        });
    }

    /**
     * Идентафикатор товара в заказе обязателен для указания при добавлении товара в отправление
     */
    @Test
    public void testDenyItemsWithNullId() {
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            order.setGlobal(true);

            Parcel newShipment = newDelivery.getParcels().get(0);
            newShipment.setParcelItems(Collections.singletonList(
                    new ParcelItem(null, 2)
            ));

            validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
        });
    }

    /**
     * Количество товара обязательно для указания
     */
    @Test
    public void testDenyItemsWithNullCount() {
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            order.setGlobal(true);

            Parcel newShipment = newDelivery.getParcels().get(0);
            newShipment.setParcelItems(Collections.singletonList(
                    new ParcelItem(ORDER_ITEM_ID, null)
            ));

            validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
        });
    }

    /**
     * Обратная совместимость.
     * В случае если треки указаны как в доставке так и в отправлении
     * треки в доставке игнорируются
     */
    @Test
    public void testIgnoreTrackFromDeliveryIfShipmentTrackSpecified() {
        order.setGlobal(true);

        newDelivery.getParcels().get(0).setId(OLD_SHIPMENT_ID);
        newDelivery.addTrack(TrackProvider.createTrack("iddqd2", 123L));

        ParcelsUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM)
                .getParcelsUpdateActions();

        assertNull(actions.getParcelsToInsert());

        assertTrue(actions.notEmpty());
        List<Track> tracks = actions.getParcelUpdates().get(0).getTracksToInsert();
        assertThat(tracks, hasSize(1));
        assertEquals("qwerty", tracks.get(0).getTrackCode());
        assertEquals(99L, (long) tracks.get(0).getDeliveryServiceId());
    }

    /**
     * Переданный в опциях тип событий помешается в результат обработки
     */
    @Test
    public void testEventTypeSetting() {
        order.setGlobal(true);

        Parcel newShipment = newDelivery.getParcels().get(0);
        newShipment.setId(OLD_SHIPMENT_ID);
        newShipment.setParcelItems(Collections.singletonList(
                new ParcelItem(ORDER_ITEM_ID, 2)
        ));

        DeliveryUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
        assertEquals(HistoryEventType.ORDER_DELIVERY_UPDATED, actions.getHistoryEventType());
    }

    /**
     * Проверка того что в случае изменения свойств доставки в результат обработки
     * устанавливается флаг необходимости обновления доставки в БД
     */
    @Test
    public void testNotEmptyActionOnDeliveryDatesChange() {
        order.setGlobal(true);

        newDelivery.setDeliveryDates(new DeliveryDates());
        Calendar fromCalendar = Calendar.getInstance();
        fromCalendar.set(2017, Calendar.JUNE, 1);

        Calendar toCalendar = Calendar.getInstance();
        toCalendar.set(2017, Calendar.JUNE, 2);

        newDelivery.setDeliveryDates(new DeliveryDates(fromCalendar.getTime(), toCalendar.getTime()));
        newDelivery.setParcels(null);

        DeliveryUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
        assertTrue(actions.notEmpty());
    }

    /**
     * Трек не указанный в запросе идет на удаление
     */
    @Test
    public void testDeleteTrack() {
        order.setGlobal(true);

        Parcel oldShipment = order.getDelivery().getParcels().get(0);

        Track firstTrack = TrackProvider.createTrack("qwerty", 99L);
        firstTrack.setId(1L);
        firstTrack.setStatus(TrackStatus.NEW);

        Track secondTrack = TrackProvider.createTrack("iddqd", 123L);
        secondTrack.setId(2L);
        secondTrack.setStatus(TrackStatus.NEW);

        oldShipment.setTracks(Arrays.asList(firstTrack, secondTrack));

        Parcel newShipment = newDelivery.getParcels().get(0);
        newShipment.setId(OLD_SHIPMENT_ID);

        DeliveryUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
        assertTrue(actions.notEmpty());

        ParcelsUpdateActions shipmentsActions = actions.getParcelsUpdateActions();
        assertNull(shipmentsActions.getParcelsToInsert());
        assertNull(shipmentsActions.getParcelsToRebind());
        assertThat(shipmentsActions.getParcelUpdates(), hasSize(1));

        ParcelUpdateActions parcelUpdateActions = shipmentsActions.getParcelUpdates().get(0);
        assertEquals(OLD_SHIPMENT_ID, (long) parcelUpdateActions.getParcel().getId());

        assertTrue(parcelUpdateActions.notEmpty());
        assertNull(parcelUpdateActions.getTracksToInsert());
        assertNull(parcelUpdateActions.getTracksToUpdate());

        assertThat(parcelUpdateActions.getTracksToRebind(), hasSize(1));
        assertEquals("qwerty", parcelUpdateActions.getTracksToRebind().get(0).getTrackCode());
        assertTrue(parcelUpdateActions.hasTracksToDelete());
    }

    /**
     * Если новая дата доставки совпадает со старой, то ивент не создается (action.notEmpty()=false)
     */
    @Test
    public void testSameDeliveryDate() {
        newDelivery.setDeliveryDates(order.getDelivery().getDeliveryDates());
        newDelivery.setParcels(order.getDelivery().getParcels());
        DeliveryUpdateActions actions = validateDelivery(order, newDelivery, ClientInfo.SYSTEM);
        assertFalse(actions.notEmpty());
    }

    /**
     * Дату доставки может изменить оператор колл-центра
     */
    @Test
    public void testDeliveryChangeByCrmInProcessing() {
        newDelivery.setDeliveryDates(new DeliveryDates(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)),
                Date.from(Instant.now().minus(2, ChronoUnit.DAYS))));
        newDelivery.setParcels(order.getDelivery().getParcels());
        DeliveryUpdateActions actions = validateDelivery(order, newDelivery,
                ClientInfo.createFromJson(ClientRole.CALL_CENTER_OPERATOR, BuyerProvider.UID, BuyerProvider.UID, 0L,
                        null));
        assertTrue(actions.notEmpty());
        assertEquals(order.getDelivery().getDeliveryDates(), newDelivery.getDeliveryDates());
    }

    @Test
    public void testDeliveryChangeByCrmInPending() {
        order.setStatus(OrderStatus.PENDING);
        newDelivery.setDeliveryDates(new DeliveryDates(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)),
                Date.from(Instant.now().minus(2, ChronoUnit.DAYS))));
        newDelivery.setParcels(order.getDelivery().getParcels());
        DeliveryUpdateActions actions = validateDelivery(order, newDelivery,
                ClientInfo.createFromJson(ClientRole.CALL_CENTER_OPERATOR, BuyerProvider.UID, BuyerProvider.UID, 0L,
                        null));
        assertTrue(actions.notEmpty());
        assertEquals(order.getDelivery().getDeliveryDates(), newDelivery.getDeliveryDates());
    }


    private DeliveryUpdateActions validateDelivery(Order order,
                                                   Delivery newDelivery,
                                                   ClientInfo clientInfo) {
        return deliveryUpdateProcessor.process(order, newDelivery, clientInfo,
                DeliveryUpdateOptions.EMPTY, parcelsUpdateProcessor);
    }
}
