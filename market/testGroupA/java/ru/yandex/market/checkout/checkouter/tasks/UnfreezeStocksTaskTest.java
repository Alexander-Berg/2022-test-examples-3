package ru.yandex.market.checkout.checkouter.tasks;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.certificate.CertificateProvider;
import ru.yandex.market.checkout.checkouter.storage.OrderEntityGroup;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.checkouter.storage.track.checkpoint.TrackCheckpointWritingDao;
import ru.yandex.market.checkout.checkouter.tasks.unfreeze.UnfreezeStocksTask;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.test.providers.TrackCheckpointProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.common.zk.ZooClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.checkout.test.providers.OrderProvider.getFulfillmentOrderWithYandexDelivery;

/**
 * https://testpalm.yandex-team.ru/testcase/checkouter-199
 * <p>
 * Created by asafev on 02/11/2017.
 */
public class UnfreezeStocksTaskTest extends AbstractWebTestBase {

    private static final Long FULFILMENT_SHOP_ID = 123L;
    private static final String SKU_PREFIX = "sku_";
    private static final String SHOP_SKU_PREFIX = "shop_sku_";

    @Autowired
    private ZooTask itemsUnfreezeTask;

    @Autowired
    private OrderWritingDao writingDao;

    @Autowired
    private OrderReadingDao readingDao;

    @Autowired
    private Storage storage;

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    @Autowired
    private TrackCheckpointWritingDao trackCheckpointWritingDao;

    @Autowired
    private UnfreezeStocksTask unfreezeStocksTask;

    @BeforeEach
    public void setUp() {
        stockStorageConfigurer.mockOkForUnfreeze();

        unfreezeStocksTask.setOrderWritingDao(writingDao);
        unfreezeStocksTask.setOrderReadingDao(readingDao);
    }

    @Test
    public void simpleUnfreeze() throws Exception {
        prepareOrderForUnfreeze(prepareOrder(122));

        itemsUnfreezeTask.runOnce();

        assertOrdersToUnfreezeCount(0);
    }

    @Test
    public void simpleUnfreezeHang() throws Exception {
        unfreezeStocksTask = new UnfreezeStocksTask(4, ChronoUnit.HOURS, 500);
        unfreezeStocksTask.setOrderWritingDao(writingDao);
        unfreezeStocksTask.setOrderReadingDao(readingDao);

        prepareOrderForUnfreeze(prepareOrder(122));

        try {
            unfreezeStocksTask.run(hangRunner(), () -> false);
            fail("Monitoring not set!");
        } catch (Exception ex) {
            //ignore its ok!
        }
        assertOrdersToUnfreezeCount(1);
    }

    @Test
    public void certificateUnfreeze() throws Exception {
        Order order = prepareOrderWithExternalCertificate(126);
        addTrackWithDeliveryCheckpointStatus(order, 20);
        prepareOrderForUnfreeze(order);

        itemsUnfreezeTask.runOnce();

        assertOrdersToUnfreezeCount(0);
    }

    private void prepareOrderForUnfreeze(Order order) {
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        storage.updateEntityGroup(new OrderEntityGroup(orderId), () -> {
            writingDao.insertOrderUnfreezeStocks(orderId, LocalDateTime.now(getClock()));
            List<Parcel> parcels = order.getDelivery().getParcels();
            if (CollectionUtils.isEmpty(parcels)) {
                return null;
            }
            List<TrackCheckpoint> checkpoints = Iterables.getOnlyElement(parcels).getTracks()
                    .stream()
                    .flatMap(t -> t.getCheckpoints().stream())
                    .collect(Collectors.toList());
            trackCheckpointWritingDao.insertCheckpoints(checkpoints, 123L);
            return null;
        });
        setFixedTime(getClock().instant().plus(1, ChronoUnit.HOURS));
        assertOrdersToUnfreezeCount(1);
    }

    private Order prepareOrder(Integer id) {
        Order order = getFulfillmentOrderWithYandexDelivery();
        order.setId((long) id);
        order.getItems().clear();
        order.addItem(FulfilmentProvider.buildFulfilmentItem(
                id.toString(),
                FULFILMENT_SHOP_ID,
                SHOP_SKU_PREFIX + id,
                SKU_PREFIX + id
        ));
        return order;
    }

    private Order prepareOrderWithExternalCertificate(Integer id) {
        Order order = getFulfillmentOrderWithYandexDelivery();
        order.setId((long) id);
        order.getItems().clear();
        order.addItem(
                FulfilmentProvider.buildFulfilmentItem(
                        id.toString(),
                        FULFILMENT_SHOP_ID,
                        SHOP_SKU_PREFIX + id,
                        SKU_PREFIX + id
                )
        );

        order.setExternalCertificate(CertificateProvider.getDefaultCertificate());
        return order;
    }

    private void addTrackWithDeliveryCheckpointStatus(Order order, int statusId) {
        if (order.getDelivery().getParcels() == null) {
            order.getDelivery().setParcels(Collections.singletonList(new Parcel()));
        }
        Parcel shipment = order.getDelivery().getParcels().get(0);
        Track track = TrackProvider.createTrack();
        TrackCheckpoint checkpoint = TrackCheckpointProvider.createCheckpoint();
        checkpoint.setDeliveryCheckpointStatus(statusId);
        track.setCheckpoints(Collections.singletonList(checkpoint));

        shipment.setTracks(Collections.singletonList(track));
    }

    private ZooTask hangRunner() throws Exception {
        ZooTask zooTask = new ZooTask();
        ZooClient zooClient = Mockito.mock(ZooClient.class);

        zooTask.setZooClient(zooClient);
        zooTask.setClock(TestableClock.getInstance());
        doReturn(
                "E64ED35CB981DAE0852AACF72A3BD3719794A6294E02F308D701C8A38229C" +
                        "10E84597D5AFAED1B9AA0987522A2D7876374FB5CC13C29A0666124413531265F1C")
                .when(zooClient)
                .getStringData(anyString(), anyString());
        return zooTask;
    }

    private void assertOrdersToUnfreezeCount(int count) {
        List<Order> ordersWithUnfreezeStocksFlag =
                readingDao.getOrdersWithUnfreezeStocksFlag(LocalDateTime.now(getClock()), 50);
        assertThat(ordersWithUnfreezeStocksFlag, hasSize(count));
    }
}
