package ru.yandex.market.wms.autostart.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.exception.BatchDetailNotFoundException;
import ru.yandex.market.wms.autostart.exception.IncorrectPickDetailStatusException;
import ru.yandex.market.wms.autostart.exception.MissingPickDetailsException;
import ru.yandex.market.wms.autostart.exception.OrdersInMultipleWavesException;
import ru.yandex.market.wms.autostart.exception.WaveDetailsNotFoundException;
import ru.yandex.market.wms.autostart.exception.WavesNotFoundException;
import ru.yandex.market.wms.common.model.enums.PickDetailStatus;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.pojo.OrderDetailKey;
import ru.yandex.market.wms.common.spring.utils.columnFilters.PickDetailKeyFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.ALLOW_PARTIAL_REMOVE_WAVE;

class RemoveOrderFromWaveServiceTest extends AutostartIntegrationTest {
    @Autowired
    private RemoveOrderFromWaveService removeOrderFromWaveService;

    @Autowired
    @MockBean
    protected DbConfigService configService;

    @BeforeEach
    public void setUp() {
        Mockito.reset(configService);
        Mockito.when(configService.getConfigAsBoolean(ALLOW_PARTIAL_REMOVE_WAVE)).thenReturn(true);
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/before.xml")
    @ExpectedDatabase(
            value = "/service/removeFromWave/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            columnFilters = PickDetailKeyFilter.class
    )
    public void testRemoveOneOrderWithShortedItemOfTwoInWave() {
        removeOrderFromWaveService.removeFromWave(List.of("00000001"), "TESTUSER");
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/remove-wave-before.xml")
    @ExpectedDatabase(
            value = "/service/removeFromWave/remove-wave-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            columnFilters = PickDetailKeyFilter.class
    )
    public void testRemoveWave() {
        removeOrderFromWaveService.removeFromWave(List.of("00000001"), "TESTUSER");
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/remove-wave-before.xml")
    @ExpectedDatabase(
            value = "/service/removeFromWave/remove-wave-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            columnFilters = PickDetailKeyFilter.class
    )
    public void testRemoveWaveWithResult() {
        final RemoveOrderFromWaveService.RemovingResult removingResult =
                removeOrderFromWaveService.removeFromWaveWithResult(List.of("00000001"), "TESTUSER");
        assertTrue(removingResult.isSuccess());
        assertNull(removingResult.error());
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/remove-two-orders-before.xml")
    @ExpectedDatabase(
            value = "/service/removeFromWave/remove-two-orders-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            columnFilters = PickDetailKeyFilter.class
    )
    public void testRemoveTwoOrders() {
        removeOrderFromWaveService.removeFromWave(List.of("00000001", "00000002"), "TESTUSER");
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/remove-order-with-batched-before.xml")
    @ExpectedDatabase(
            value = "/service/removeFromWave/remove-order-with-batched-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            columnFilters = PickDetailKeyFilter.class
    )
    public void testRemoveOneOrderWithBatchedOrderExists() {
        removeOrderFromWaveService.removeFromWave(List.of("00000001"), "TESTUSER");
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/remove-partially-shipped-order.xml")
    public void testRemovePartiallyShippedOrderWithResultAndDisabledPartialUnreserve() {
        Mockito.when(configService.getConfigAsBoolean(ALLOW_PARTIAL_REMOVE_WAVE)).thenReturn(false);

        final RemoveOrderFromWaveService.RemovingResult removingResult =
                removeOrderFromWaveService.removeFromWaveWithResult(List.of("00000001"), "TESTUSER");
        assertFalse(removingResult.isSuccess());
        assertNotNull(removingResult.error());
        assertEquals(IncorrectPickDetailStatusException.class, removingResult.error().getClass());
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/remove-partially-shipped-order.xml")
    @ExpectedDatabase(
            value = "/service/removeFromWave/remove-partially-shipped-order-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            columnFilters = PickDetailKeyFilter.class
    )
    public void testRemovePartiallyDroppedOrderWithResult() {
        final RemoveOrderFromWaveService.RemovingResult removingResult =
                removeOrderFromWaveService.removeFromWaveWithResult(List.of("00000001"), "TESTUSER");
        assertTrue(removingResult.isSuccess());
        assertNull(removingResult.error());
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/remove-partially-packed-order.xml")
    @ExpectedDatabase(
            value = "/service/removeFromWave/remove-partially-packed-order-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            columnFilters = PickDetailKeyFilter.class
    )
    public void testRemovePartiallyPackedOrderWithResult() {
        final RemoveOrderFromWaveService.RemovingResult removingResult =
                removeOrderFromWaveService.removeFromWaveWithResult(List.of("00000001"), "TESTUSER");
        assertTrue(removingResult.isSuccess());
        assertNull(removingResult.error());
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/remove-partially-shipped-order.xml")
    @ExpectedDatabase(
            value = "/service/removeFromWave/remove-partially-shipped-order-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            columnFilters = PickDetailKeyFilter.class
    )
    public void testRemovePartiallyShippedOrder() {
        removeOrderFromWaveService.removeFromWave(List.of("00000001"), "TESTUSER");
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/wave-details-not-found.xml")
    public void testWaveDetailsNotFound() {
        WaveDetailsNotFoundException actualException = assertThrows(WaveDetailsNotFoundException.class,
                () -> removeOrderFromWaveService.removeFromWave(List.of("00000001"), "TESTUSER"));
        assertEquals(Map.of("orderKeys", List.of("00000001")), actualException.wmsErrorData());
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/waves-not-found.xml")
    public void testWavesNotFound() {
        WavesNotFoundException actualException = assertThrows(WavesNotFoundException.class,
                () -> removeOrderFromWaveService.removeFromWave(List.of("00000001"), "TESTUSER"));
        assertEquals(Map.of("orderKeys", List.of("00000001")), actualException.wmsErrorData());
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/orders-in-different-waves.xml")
    @SuppressWarnings("unchecked")
    public void testOrdersInDifferentWaves() {
        OrdersInMultipleWavesException actualException = assertThrows(OrdersInMultipleWavesException.class,
                () -> removeOrderFromWaveService.removeFromWave(List.of("00000001", "00000002"), "TESTUSER"));
        Map<String, List<String>> expectedData = Map.of(
                "orderKeys", List.of("00000001", "00000002"),
                "waveKeys", List.of("W0000001", "W0000002")
        );
        assertEquals(2, actualException.wmsErrorData().size());
        expectedData.forEach((key, list) -> {
            Collection<String> actualCollection = (Collection<String>) actualException.wmsErrorData().get(key);
            assertTrue(list.size() == actualCollection.size()
                    && list.containsAll(actualCollection));
        });
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/batch-detail-not-found.xml")
    public void testBatchDetailNotFound() {
        BatchDetailNotFoundException actualException = assertThrows(BatchDetailNotFoundException.class,
                () -> removeOrderFromWaveService.removeFromWave(List.of("00000001"), "TESTUSER"));
        OrderDetailKey incorrectBatchDetailKey = new OrderDetailKey("B0404", "404");
        assertEquals(Map.of("batchDetailKey", incorrectBatchDetailKey.toString()), actualException.wmsErrorData());
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/missing-pick-details.xml")
    public void testMissingPickDetails() {
        MissingPickDetailsException actualException = assertThrows(MissingPickDetailsException.class,
                () -> removeOrderFromWaveService.removeFromWave(List.of("00000001"), "TESTUSER"));
        Map<String, Object> expectedData = Map.of(
                "batchDetailKey", new OrderDetailKey("B0000001", "01").toString(),
                "cntToRemove", 1,
                "actualCnt", 0
        );
        assertEquals(expectedData, actualException.wmsErrorData());
    }

    @Test
    @DatabaseSetup("/service/removeFromWave/common.xml")
    @DatabaseSetup("/service/removeFromWave/incorrect-pick-details-status.xml")
    public void testIncorrectPickDetailStatus() {
        IncorrectPickDetailStatusException actualException = assertThrows(IncorrectPickDetailStatusException.class,
                () -> removeOrderFromWaveService.removeFromWave(List.of("00000001"), "TESTUSER"));
        Map<String, Object> expectedErrorData = Map.of(
                "orderKeysWithStatuses", Map.of("B0000001", Set.of(PickDetailStatus.CLOSED)).toString()
        );
        assertEquals(expectedErrorData, actualException.wmsErrorData());
    }
}
