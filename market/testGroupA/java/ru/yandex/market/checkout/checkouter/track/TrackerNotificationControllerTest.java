package ru.yandex.market.checkout.checkouter.track;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrackCheckpoint;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.DeliveryTrackCheckpointProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackMetaProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.ParcelItemProvider;
import ru.yandex.market.checkout.test.providers.ParcelProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.clickAndCollectOrderParameters;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.postpaidBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

/**
 * @author apershukov
 */
public class TrackerNotificationControllerTest extends AbstractWebTestBase {

    public static final String TRACK_CODE = "TRACK_CODE";

    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private OrderWritingDao orderWritingDao;
    @Autowired
    private WireMockServer stockStorageMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    protected OrderPayHelper orderPayHelper;
    @Autowired
    private OrderServiceHelper orderServiceHelper;

    /**
     * Новые чекпойнты добавляются корректно к заказу с несколькими посылками
     * <p>
     * Подготовка
     * 1. Добавить заказ
     * 2. Добавить отправление в заказ
     * 3. Добавить в отправление трек с трек-кодом "iddqd" службой доставки 123 и тркерным id 100500
     * <p>
     * Действия
     * 1. Сделать запрос к POST /notify-tracks с телом вида "notify-tracks-request-body.json"
     * <p>
     * Проверки
     * 1. У заказа по-прежнему два отправления
     * 2. Чекпойнт был успешно запушен к треку в отправлении A
     * 3. Трек в отправлении B не потерялся
     */

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.NOTIFY_TRACKS)
    @DisplayName("Новые чекпойнты добавляются корректно к заказу с несколькими посылками")
    @Test
    public void testPushCheckpointsForMultiShipmentOrder() throws Exception {
        // Подготосвка
        var parameters = clickAndCollectOrderParameters();

        parameters.getOrder().setDelivery(DeliveryProvider.shopSelfDelivery().build());
        final Order order = orderCreateHelper.createOrder(parameters);
        final String orderForMeta = String.valueOf(order.getId());
        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(orderForMeta);

        //Ну тут вроде как должно быть 2 парцела, создаем второй
        Delivery deliveryRequest = new Delivery();
        deliveryRequest.setParcels(Arrays.asList(new Parcel(), new Parcel()));
        long parcelId = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryRequest)
                .getDelivery().getParcels().get(0).getId();

        Track track = new Track("another_" + TRACK_CODE, DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        track.setDeliveryServiceType(DeliveryServiceType.SORTING_CENTER);
        track = orderDeliveryHelper.addTrack(order.getId(), parcelId, track, ClientInfo.SYSTEM,
                new ResultActionsContainer().andExpect(status().is(200))
        );
        //set deliveryTrackId equal to '100500' as in notify-track's checkpoint
        orderUpdateService.updateTrackSetTrackerId(
                order.getId(),
                track.getBusinessId(),
                DeliveryTrackMetaProvider.getDeliveryTrackMeta(orderForMeta).getId()
        );

        // Действие
        notifyTracksHelper.notifyTracks(deliveryTrack);

        // Проверки
        final Order readOrder = orderService.getOrder(order.getId());

        List<Parcel> parcels = readOrder.getDelivery().getParcels();
        assertThat(parcels, hasSize(2));
        assertThat(parcels, hasItem(allOf(
                hasProperty("tracks", hasSize(1)),
                hasProperty("tracks", hasItem(
                        allOf(
                                hasProperty("checkpoints", hasSize(1)),
                                hasProperty("checkpoints",
                                        hasItems(
                                                hasProperty("deliveryCheckpointStatus",
                                                        comparesEqualTo(1))
                                        )
                                )
                        )
                ))
        )));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.NOTIFY_TRACKS)
    @DisplayName("Пушим чекпойнты почтовому заказу")
    @Test
    public void testPushCheckpointsForMardo() throws Exception {
        //checkouter-20
        // Подготовка
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID);
        order = orderService.getOrder(order.getId());
        Parcel shipmentWithTracks = ParcelProvider.createParcelWithTracksAndItems(
                Collections.singletonList(
                        TrackProvider.createTrack("TEST_TRACK_KODE", 1L, 1717L)
                ),
                Collections.singletonList(
                        ParcelItemProvider.buildParcelItem(1L, 2)
                )
        );
        order.getDelivery().setParcels(Collections.singletonList(
                shipmentWithTracks
        ));

        order = orderServiceHelper.saveOrder(order);

        shopService.updateMeta(order.getShopId(), ShopSettingsHelper.getDefaultMeta());
        orderPayHelper.payForOrder(order);

        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.PROCESSING, order.getStatus());


        //step 1
        pushCorrectCheckpoint(order);

        //step 2
        pushMoreCheckpoints(order);
    }

    private void pushMoreCheckpoints(Order order) throws Exception {
        DeliveryTrack deliveryTrack;
        List<Parcel> shipments;
        Parcel shipment; //действие
        deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                order.getDelivery().getParcels().get(0).getTracks().get(0).getTrackerId(),
                "TEST_TRACK_KODE",
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(18),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(19)
        );

        notifyTracksHelper.notifyTracks(deliveryTrack);

        // Проверки
        order = orderService.getOrder(order.getId());

        shipments = order.getDelivery().getParcels();
        assertThat(shipments, hasSize(1));
        shipment = shipments.get(0);
        assertThat(shipment.getTracks().get(0).getCheckpoints(), hasSize(3));
        TrackCheckpoint lastCheckpoint = shipment.getTracks().get(0).getCheckpoints().stream()
                .max(Comparator.comparingLong(TrackCheckpoint::getTrackerCheckpointId))
                .get();
        assertThat(shipment.getTracks().get(0).getActualCheckpointId(), is(lastCheckpoint.getId()));


        List<Integer> checkpointStatuses = shipment.getTracks().get(0).getCheckpoints().stream()
                .map(TrackCheckpoint::getDeliveryCheckpointStatus)
                .collect(Collectors.toList());

        assertTrue(checkpointStatuses.containsAll(Arrays.asList(17, 18, 19)));
    }

    @Epic(Epics.DELIVERY)
    @Story(Stories.NOTIFY_TRACKS)
    @DisplayName("Пушим некорректный чекпойнт почтовому заказу")
    @Test
    public void testNoPushBadCheckpointsForMardo() throws Exception {
        //checkouter-21
        // Подготовка
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID);
        order = orderService.getOrder(order.getId());
        Parcel shipmentWithTracks = ParcelProvider.createParcelWithTracksAndItems(
                Collections.singletonList(
                        TrackProvider.createTrack("TEST_TRACK_KODE", 1L, 1717L)
                ),
                Collections.singletonList(
                        ParcelItemProvider.buildParcelItem(1L, 2)
                )
        );
        order.getDelivery().setParcels(Collections.singletonList(
                shipmentWithTracks
        ));

        order = orderServiceHelper.saveOrder(order);

        shopService.updateMeta(order.getShopId(), ShopSettingsHelper.getDefaultMeta());
        orderPayHelper.payForOrder(order);

        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        //step 1
        TrackCheckpoint checkpoint = pushCorrectCheckpoint(order);

        //step 2
        DeliveryTrackCheckpoint deliveryTrackCheckpoint = pushDuplicateCheckpoint(order, checkpoint);

        //step 3
        pushNotFoundCheckpoint(order, checkpoint, deliveryTrackCheckpoint);
    }

    @Step("Запушить ручкой /notify-tracks чекпойнт с для несуществующего trackId")
    private void pushNotFoundCheckpoint(
            Order order,
            TrackCheckpoint checkpoint,
            DeliveryTrackCheckpoint deliveryTrackCheckpoint
    ) throws Exception {
        DeliveryTrack deliveryTrack;
        List<Parcel> shipments;
        Parcel shipment;
        List<Integer> checkpointStatuses; //действие
        deliveryTrackCheckpoint.setId(checkpoint.getTrackerCheckpointId());
        deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                //ставим треку несуществующих track.id
                order.getDelivery().getParcels().get(0).getTracks().get(0).getTrackerId() + 10,
                "TEST_TRACK_KODE",
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(19)
        );

        notifyTracksHelper.notifyTracks(deliveryTrack);

        // Проверки
        order = orderService.getOrder(order.getId());

        shipments = order.getDelivery().getParcels();
        assertThat(shipments, hasSize(1));
        shipment = shipments.get(0);
        assertThat(shipment.getTracks().get(0).getCheckpoints(), hasSize(1));


        checkpointStatuses = shipment.getTracks().get(0).getCheckpoints().stream()
                .map(TrackCheckpoint::getDeliveryCheckpointStatus)
                .collect(Collectors.toList());

        assertTrue(checkpointStatuses.containsAll(Collections.singletonList(17)));
    }

    @Nonnull
    @Step("Запушить ручкой /notify-tracks чекпойнт с deliveryTrackCheckpoints.id, который уже был")
    private DeliveryTrackCheckpoint pushDuplicateCheckpoint(Order order, TrackCheckpoint checkpoint) throws Exception {
        DeliveryTrack deliveryTrack;
        List<Parcel> shipments;
        Parcel shipment; //действие
        DeliveryTrackCheckpoint deliveryTrackCheckpoint = DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(18);
        //берем trackId старого чекпоинта
        deliveryTrackCheckpoint.setId(checkpoint.getTrackerCheckpointId());
        deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                order.getDelivery().getParcels().get(0).getTracks().get(0).getTrackerId(),
                "TEST_TRACK_KODE",
                deliveryTrackCheckpoint
        );

        notifyTracksHelper.notifyTracks(deliveryTrack);

        // Проверки
        order = orderService.getOrder(order.getId());

        shipments = order.getDelivery().getParcels();
        assertThat(shipments, hasSize(1));
        shipment = shipments.get(0);
        assertThat(shipment.getTracks().get(0).getCheckpoints(), hasSize(1));


        List<Integer> checkpointStatuses = shipment.getTracks().get(0).getCheckpoints().stream()
                .map(TrackCheckpoint::getDeliveryCheckpointStatus)
                .collect(Collectors.toList());

        assertTrue(checkpointStatuses.containsAll(Collections.singletonList(17)));
        return deliveryTrackCheckpoint;
    }

    @Nonnull
    @Step("Запушить один чекпойнт ручкой /notify-tracks")
    private TrackCheckpoint pushCorrectCheckpoint(Order order) throws Exception {
        Parcel shipment; //действие
        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                order.getDelivery().getParcels().get(0).getTracks().get(0).getTrackerId(),
                "TEST_TRACK_KODE",
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(17)
        );

        notifyTracksHelper.notifyTracks(deliveryTrack);

        // Проверки
        order = orderService.getOrder(order.getId());

        List<Parcel> shipments = order.getDelivery().getParcels();
        assertThat(shipments, hasSize(1));

        shipment = shipments.get(0);
        assertThat(shipment.getTracks(), hasSize(1));
        assertThat(shipment.getTracks().get(0).getCheckpoints(), hasSize(1));
        assertThat(
                shipment.getTracks().get(0).getActualCheckpointId(),
                is(shipment.getTracks().get(0).getCheckpoints().get(0).getId())
        );

        TrackCheckpoint checkpoint = shipment.getTracks().get(0).getCheckpoints().get(0);
        assertEquals(17, (int) checkpoint.getDeliveryCheckpointStatus());
        return checkpoint;
    }

    /**
     * Нельзя запушить чекпойнт с отрицательным трекерным id
     * <p>
     * Подготовка
     * 1. Добавить заказ
     * 2. Добавить отправление в заказ
     * 3. Добавить в отправление трек с трек-кодом "iddqd" службой доставки 123 и трекерным id 100500
     * <p>
     * Действие
     * 1. Сделать запрос к POST /notify-tracks с телом вида "invalid-checkpoint-id-request-body.json"
     * <p>
     * Проверки
     * 1. Код ответа на запрос - 200
     * 2. В теле ответа указано что пуш трека окончился неудачей
     * 3. В теле ответа содержится сообщение об ошибке: "Value of delivery track checkpointId (-1) must be greater
     * than 0"
     */

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.NOTIFY_TRACKS)
    @DisplayName("Нельзя запушить чекпойнт с отрицательным трекерным id")
    @Test
    public void testValidateTrackerId() throws Exception {
        // Подготовка
        Order order = createOrderWithTrack();
        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(-1, DeliveryTrackCheckpointProvider
                        .DEFAULT_DELIVERY_CHECKPOINT_STATUS)
        );

        //Действие
        notifyTracksHelper.notifyTracksForActions(deliveryTrack)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("ERROR"))
                .andExpect(jsonPath("$.results[0].message")
                        .value("Value of delivery track checkpointId (-1) must be greater than 0"));
    }

    /**
     * Нельзя запушить чекпойнт с отрицательным трекерным id
     * <p>
     * Подготовка
     * 1. Добавить заказ
     * 2. Добавить отправление в заказ
     * 3. Добавить в отправление трек с трек-кодом "iddqd" службой доставки 123 и трекерным id 100500
     * <p>
     * Действие
     * 1. Сделать запрос к POST /notify-tracks с телом вида "notify-tracks-request-body-cancel-checkpoint.json"
     * (Чекпоинт 410 "Заказ отменен")
     * <p>
     * Проверки
     * 1. Код ответа на запрос - 200
     * 2. В теле ответа указано что пуш трека окончился неудачей
     * 3. В теле ответа содержится сообщение об ошибке: "Value of delivery track checkpointId (-1) must be greater
     * than 0"
     */

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.NOTIFY_TRACKS)
    @DisplayName("Стоки анфризятся по чекпоинту CANCELED(410)")
    @Test
    public void testUnfreezeStocksByCanceledCheckpoint() throws Exception {

        stockStorageMock.stubFor(delete(urlPathMatching("/order/[0-9]+"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));

        // Подготовка
        Order order = createFulfilmentOrderWithTrack();

        //Действие
        notifyCancelCheckpoint(order, DeliveryCheckpointStatus.CANCELED.getId());

        stockStorageMock.verify(1, deleteRequestedFor(urlPathEqualTo("/order/" + order.getId())));
    }

    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.NOTIFY_TRACKS)
    @DisplayName("Стоки анфризятся по чекпоинту SERVICE_CENTER_CANCELED(105)")
    @Test
    public void testUnfreezeStocksByServiceCanceledCheckpoint() throws Exception {

        stockStorageMock.stubFor(delete(urlPathMatching("/order/[0-9]+"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));

        // Подготовка
        Order order = createFulfilmentOrderWithTrack();

        //Действие
        notifyCancelCheckpoint(order, DeliveryCheckpointStatus.SERVICE_CENTER_CANCELED.getId());

        stockStorageMock.verify(1, deleteRequestedFor(urlPathEqualTo("/order/" + order.getId())));
    }


    @Tag(Tags.FULFILMENT)
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.NOTIFY_TRACKS)
    @DisplayName("Можно пушить чекпоинты отмены после отмены заказа")
    @Test
    public void testCanceledCheckpointsAfterCancel() throws Exception {
        // Подготовка
        stockStorageMock.stubFor(delete(urlPathMatching("/order/[0-9]+"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));
        Order order = createFulfilmentOrderWithTrack();
        setupBeenCalled(order.getId());
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.PROCESSING_EXPIRED);
        stockStorageMock.verify(0, deleteRequestedFor(urlPathEqualTo("/order/" + order.getId())));
        assertEquals(OrderStatus.CANCELLED, orderService.getOrder(order.getId()).getStatus(), "Order was not canceled");

        //Действие
        notifyCancelCheckpoint(order, DeliveryCheckpointStatus.CANCELED.getId());

        stockStorageMock.verify(1, deleteRequestedFor(urlPathEqualTo("/order/" + order.getId())));
    }

    private void notifyCancelCheckpoint(Order order, int deliveryCheckpointStatus) throws Exception {
        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(deliveryCheckpointStatus)
        );
        notifyTracksHelper.notifyTracks(deliveryTrack);
    }

    private Order createOrderWithTrack() throws Exception {
        Order order = orderCreateHelper.createOrder(postpaidBlueOrderParameters());

        //то выше создается трек это фигня и не работает
        Delivery deliveryRequest = new Delivery();
        Parcel parcel = new Parcel();
        deliveryRequest.setParcels(Collections.singletonList(parcel));
        long parcelId = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryRequest)
                .getDelivery().getParcels().get(0).getId();

        Track track = new Track(TRACK_CODE, DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        track.setDeliveryServiceType(DeliveryServiceType.SORTING_CENTER);
        track = orderDeliveryHelper.addTrack(order.getId(), parcelId, track, ClientInfo.SYSTEM,
                new ResultActionsContainer().andExpect(status().is(200))
        );
        //set deliveryTrackId equal to '100500' as in notify-track's checkpoint
        orderUpdateService.updateTrackSetTrackerId(
                order.getId(),
                track.getBusinessId(),
                DeliveryTrackMetaProvider.getDeliveryTrackMeta("any").getId()
        );

        return orderService.getOrder(order.getId());
    }

    private Order createFulfilmentOrderWithTrack() throws Exception {
        Order order = orderCreateHelper.createOrder(postpaidBlueOrderParameters());

        //то выше создается трек это фигня и не работает
        Delivery deliveryRequest = new Delivery();
        Parcel parcel = new Parcel();
        deliveryRequest.setParcels(Collections.singletonList(parcel));
        long parcelId = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryRequest)
                .getDelivery().getParcels().get(0).getId();

        Track track = new Track(TRACK_CODE, DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        track.setDeliveryServiceType(DeliveryServiceType.SORTING_CENTER);
        track = orderDeliveryHelper.addTrack(order.getId(), parcelId, track, ClientInfo.SYSTEM,
                new ResultActionsContainer().andExpect(status().is(200))
        );

        //set deliveryTrackId equal to '100500' as in notify-track's checkpoint
        orderUpdateService.updateTrackSetTrackerId(
                order.getId(),
                track.getBusinessId(),
                DeliveryTrackMetaProvider.getDeliveryTrackMeta("any").getId()
        );

        return orderService.getOrder(order.getId());
    }

    private void setupBeenCalled(Long orderId) {
        transactionTemplate.execute(ts -> {
            orderWritingDao.updateOrderBuyerBeenCalled(orderId, true);
            return null;
        });
    }
}
