package ru.yandex.market.wms.autostart.async;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.core.base.request.ReserveWaveRequest;
import ru.yandex.market.wms.ordermanagement.client.OrderManagementClient;
import ru.yandex.market.wms.ordermanagement.core.model.OrderShortageItem;
import ru.yandex.market.wms.ordermanagement.core.model.OrderShortageItemDetail;
import ru.yandex.market.wms.ordermanagement.core.model.OrdersShortageRequest;
import ru.yandex.market.wms.replenishment.client.ReplenishmentClient;
import ru.yandex.market.wms.replenishment.core.dto.stock.SkuStockWithAnyHold;
import ru.yandex.market.wms.replenishment.core.dto.stock.StocksWithHoldsRequest;
import ru.yandex.market.wms.replenishment.core.dto.stock.StocksWithHoldsResponse;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class AsyncWaveReserveLargeTest extends TestcontainersConfiguration {

    private static final String USER = "TEST";

    @Autowired
    private ReserveWaveConsumer reserveWaveConsumer;
    @SpyBean
    @Autowired
    private SecurityDataProvider securityDataProvider;

    @MockBean
    @Autowired
    private ReplenishmentClient replenishmentClient;

    @MockBean
    @Autowired
    private OrderManagementClient orderManagementClient;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(replenishmentClient, orderManagementClient);
    }

    @AfterEach
    void after() {
        //При обработке сообщений из очереди не заполнен security context => юзера брать оттуда не стоит
        Mockito.verify(securityDataProvider, VerificationModeFactory.noInteractions()).getUser();
        Mockito.verify(securityDataProvider, VerificationModeFactory.noInteractions()).getToken();
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/before-reserve-wave.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/rereserve/after-reserve-wave.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWave() {
        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-001", null, USER), null);
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/before-reserve-wave.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/rereserve/before-reserve-wave.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWaveNotExist() {
        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-013", null, USER), null);
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/before-reserve-wave.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/rereserve/after-reserve-wave.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWaveWithNotExistingAssignment() {
        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-001", "AS131234513", USER), null);
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/before-reserve-wave.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/rereserve/after-reserve-wave.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWaveWithNotSuitableAssignment() {
        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-001", "AS009", USER), null);
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/before-reserve-wave.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/rereserve/after-reserve-wave-reuse-assignment.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWaveWithSuitableAssignment() {
        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-001", "AS000", USER), null);
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/lost-withdrawal/before-reserve-wave-rereserved.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/lost-withdrawal/after-reserve-wave-rereserved.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWaveSmallWithdrawalsSuccessfulRereserve() {
        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-001", "AS000", USER), null);

        Mockito.verify(replenishmentClient, Mockito.times(0)).getStocks(Mockito.any());
        Mockito.verify(orderManagementClient, Mockito.times(0)).shortage(Mockito.any());
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/lost-withdrawal/before-reserve-wave-with-stocks.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/lost-withdrawal/after-reserve-wave-with-stocks.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWaveSmallWithdrawalsWithAllStocks() {
        SkuId sku = SkuId.of("STORER-001", "SKU-001");
        StocksWithHoldsRequest request = new StocksWithHoldsRequest(Set.of(sku), Set.of(InventoryHoldStatus.DAMAGE));
        StocksWithHoldsResponse response = new StocksWithHoldsResponse(Set.of(new SkuStockWithAnyHold(sku, 1, 2)));
        Mockito.when(replenishmentClient.getStocks(request)).thenReturn(response);

        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-001", null, USER), null);

        Mockito.verify(replenishmentClient, Mockito.times(1)).getStocks(request);
        Mockito.verify(orderManagementClient, Mockito.times(0)).shortage(Mockito.any());
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/lost-withdrawal/before-reserve-wave-part-stocks.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/lost-withdrawal/after-reserve-wave-part-stocks.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWaveSmallWithdrawalsWithPartialStocks() {
        SkuId sku = SkuId.of("STORER-001", "SKU-001");
        SkuId sku2 = SkuId.of("STORER-001", "SKU-002");
        StocksWithHoldsRequest request =
                new StocksWithHoldsRequest(Set.of(sku, sku2), Set.of(InventoryHoldStatus.DAMAGE));
        StocksWithHoldsResponse response = new StocksWithHoldsResponse(
                Set.of(new SkuStockWithAnyHold(sku, 0, 1), new SkuStockWithAnyHold(sku2, 0, 0))
        );
        Mockito.when(replenishmentClient.getStocks(request)).thenReturn(response);

        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-001", null, USER), null);

        OrdersShortageRequest correctionRequest = new OrdersShortageRequest(
                List.of(new OrderShortageItem(
                        "ORDER-002",
                        List.of(new OrderShortageItemDetail("7", 0), new OrderShortageItemDetail("8", 1)))
                )
        );
        Mockito.verify(orderManagementClient, Mockito.times(0)).shortage(correctionRequest);
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/lost-withdrawal/" +
            "before-reserve-wave-big-withdrawal-part-stocks.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/lost-withdrawal/" +
                    "after-reserve-wave-big-withdrawal-part-stocks.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWaveBigWithdrawalWithPartialStocks() {
        SkuId sku = SkuId.of("STORER-001", "SKU-001");
        StocksWithHoldsRequest request =
                new StocksWithHoldsRequest(Set.of(sku), Set.of(InventoryHoldStatus.PLAN_UTILIZATION));
        StocksWithHoldsResponse response = new StocksWithHoldsResponse(Set.of(new SkuStockWithAnyHold(sku, 1, 1)));
        Mockito.when(replenishmentClient.getStocks(request)).thenReturn(response);

        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-001", null, USER), null);

        OrdersShortageRequest correctionRequest = new OrdersShortageRequest(
                List.of(new OrderShortageItem("ORDER-001", List.of(new OrderShortageItemDetail("1", 1))))
        );
        Mockito.verify(orderManagementClient, Mockito.times(1)).shortage(correctionRequest);
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/lost-withdrawal/" +
            "before-reserve-wave-without-stocks.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/lost-withdrawal/" +
                    "after-reserve-wave-without-stocks.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWaveSmallWithdrawalsWithoutStocks() {
        SkuId sku = SkuId.of("STORER-001", "SKU-001");
        StocksWithHoldsRequest request = new StocksWithHoldsRequest(Set.of(sku), Set.of(InventoryHoldStatus.DAMAGE));
        StocksWithHoldsResponse response = new StocksWithHoldsResponse(Set.of(new SkuStockWithAnyHold(sku, 0, 0)));
        Mockito.when(replenishmentClient.getStocks(request)).thenReturn(response);

        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-001", null, USER), null);

        OrdersShortageRequest correctionRequest = new OrdersShortageRequest(
                List.of(
                        new OrderShortageItem("ORDER-002", List.of(new OrderShortageItemDetail("7", 2))),
                        new OrderShortageItem("ORDER-001", List.of(new OrderShortageItemDetail("7", 1)))
                )
        );
        Mockito.verify(orderManagementClient, Mockito.times(1)).shortage(correctionRequest);
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/lost-withdrawal/" +
            "before-reserve-wave-with-locked-uits.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/lost-withdrawal/" +
                    "after-reserve-wave-with-locked-uits.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testAsyncReserveWaveWithdrawalStocksWithLockedUits() {
        SkuId sku = SkuId.of("STORER-001", "SKU-001");
        StocksWithHoldsRequest request = new StocksWithHoldsRequest(Set.of(sku), Collections.emptySet());
        StocksWithHoldsResponse response = new StocksWithHoldsResponse(Set.of(new SkuStockWithAnyHold(sku, 0, 0)));
        Mockito.when(replenishmentClient.getStocks(request)).thenReturn(response);

        reserveWaveConsumer.receiveMessage(new ReserveWaveRequest("WAVE-001", null, USER), null);

        OrdersShortageRequest correctionRequest = new OrdersShortageRequest(
                List.of(
                        new OrderShortageItem("ORDER-002", List.of(new OrderShortageItemDetail("7", 2))),
                        new OrderShortageItem("ORDER-001", List.of(new OrderShortageItemDetail("7", 1)))
                )
        );
        Mockito.verify(orderManagementClient, Mockito.times(1)).shortage(correctionRequest);
    }
}
