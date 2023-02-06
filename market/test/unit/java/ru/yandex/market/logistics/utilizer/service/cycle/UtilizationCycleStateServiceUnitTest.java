package ru.yandex.market.logistics.utilizer.service.cycle;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.logistics.utilizer.base.SoftAssertionSupport;
import ru.yandex.market.logistics.utilizer.domain.entity.UtilizationCycleState;
import ru.yandex.market.logistics.utilizer.domain.enums.StockType;
import ru.yandex.market.logistics.utilizer.domain.enums.UtilizationCycleStateStatus;
import ru.yandex.market.logistics.utilizer.domain.internal.StockTypeAndWarehouse;
import ru.yandex.market.logistics.utilizer.repo.UtilizationCycleStateRepository;
import ru.yandex.market.logistics.utilizer.service.lms.LmsService;
import ru.yandex.market.logistics.utilizer.service.mds.MdsS3Service;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UtilizationCycleStateServiceUnitTest extends SoftAssertionSupport {

    private static final long VENDOR_ID = 100500L;
    private static final long WAREHOUSE_ID = 172L;

    private UtilizationCycleStateRepository repository;
    private MdsS3Service mdsS3Service;
    private LmsService lmsService;
    private UtilizationCycleStateService utilizationCycleStateService;

    @BeforeEach
    public void init() {
        repository = mock(UtilizationCycleStateRepository.class);
        mdsS3Service = mock(MdsS3Service.class);
        lmsService = mock(LmsService.class);

        utilizationCycleStateService = new UtilizationCycleStateService(
                repository,
                mdsS3Service,
                lmsService
        );

        when(mdsS3Service.uploadFile(any(), any())).thenAnswer(invocation -> {
            String fileName = invocation.getArgument(0);
            return new URL("https://" + fileName);
        });
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void createNewUtilizationCycleState() {
        InputStream stream = mock(InputStream.class);
        StockTypeAndWarehouse stockTypeAndWarehouse = spy(getStockTypeAndWarehouse(StockType.DEFECT));
        UtilizationCycleState expected = getUtilizationCycleState(VENDOR_ID, stockTypeAndWarehouse);

        when(repository.findByVendorIdAndWarehouseIdAndStockType(
                eq(VENDOR_ID), eq(WAREHOUSE_ID), eq(StockType.DEFECT))
        ).thenReturn(Optional.empty());
        when(repository.save(any(UtilizationCycleState.class))).then(returnsFirstArg());

        UtilizationCycleState actual =
                utilizationCycleStateService.addActiveStateForCycle(VENDOR_ID, stream, stockTypeAndWarehouse);

        softly.assertThat(actual).isEqualToComparingFieldByField(expected);
        verify(stockTypeAndWarehouse, times(3)).getWarehouseId();
        verify(stockTypeAndWarehouse, times(3)).getStockType();
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void updateOnlyFileUrlForUtilizationCycleState() {
        InputStream stream = mock(InputStream.class);
        StockTypeAndWarehouse stockTypeAndWarehouse = spy(getStockTypeAndWarehouse(StockType.DEFECT));
        UtilizationCycleState expected = getUtilizationCycleState(VENDOR_ID, stockTypeAndWarehouse);

        when(repository.findByVendorIdAndWarehouseIdAndStockType(
                eq(VENDOR_ID), eq(WAREHOUSE_ID), eq(StockType.DEFECT))
        ).thenReturn(Optional.of(expected));
        when(repository.save(any(UtilizationCycleState.class))).then(returnsFirstArg());

        UtilizationCycleState actual =
                utilizationCycleStateService.addActiveStateForCycle(VENDOR_ID, stream, stockTypeAndWarehouse);

        softly.assertThat(actual).isEqualToComparingFieldByField(expected);
        verify(stockTypeAndWarehouse, times(2)).getWarehouseId();
        verify(stockTypeAndWarehouse, times(2)).getStockType();
    }

    @Test
    void disableStatesForEqualsStockAndWarehouses() {
        InputStream stream = mock(InputStream.class);
        StockTypeAndWarehouse defect = getStockTypeAndWarehouse(StockType.DEFECT);
        StockTypeAndWarehouse surplus = getStockTypeAndWarehouse(StockType.SURPLUS);
        List<UtilizationCycleState> activeStates = List.of(
                getUtilizationCycleState(VENDOR_ID, defect),
                getUtilizationCycleState(VENDOR_ID, surplus)
        );

        when(repository.findByVendorIdAndStatus(eq(VENDOR_ID), eq(UtilizationCycleStateStatus.ACTIVE)))
                .thenReturn(activeStates);
        when(repository.saveAll(anyList())).then(returnsFirstArg());

        List<UtilizationCycleState> actual =
                utilizationCycleStateService.disableStates(VENDOR_ID, Set.of(defect, surplus), stream);

        softly.assertThat(actual).isEmpty();
        verifyNoMoreInteractions(mdsS3Service);
        verify(repository).saveAll(eq(Collections.emptyList()));
    }

    @Test
    void disableStatesForNotEqualsStockAndWarehouses() {
        InputStream stream = mock(InputStream.class);
        StockTypeAndWarehouse defect = getStockTypeAndWarehouse(StockType.DEFECT);
        StockTypeAndWarehouse surplus = getStockTypeAndWarehouse(StockType.SURPLUS);
        List<UtilizationCycleState> activeStates = List.of(
                getUtilizationCycleState(VENDOR_ID, defect),
                getUtilizationCycleState(VENDOR_ID, surplus)
        );

        when(repository.findByVendorIdAndStatus(eq(VENDOR_ID), eq(UtilizationCycleStateStatus.ACTIVE)))
                .thenReturn(activeStates);
        when(repository.saveAll(anyList())).then(returnsFirstArg());

        List<UtilizationCycleState> actual =
                utilizationCycleStateService.disableStates(VENDOR_ID, Set.of(defect), stream);

        UtilizationCycleState utilizationCycleState = activeStates.get(1);
        utilizationCycleState.setStatus(UtilizationCycleStateStatus.DEPRECATED);
        List<UtilizationCycleState> expected = List.of(utilizationCycleState);

        softly.assertThat(actual).isEqualTo(expected);
        verify(mdsS3Service).uploadFile(eq("utilization_vendor_100500_warehouse_172_stocktype_SURPLUS.xlsx"),
                any(StreamContentProvider.class)
        );
        verify(repository).saveAll(eq(expected));
    }

    private UtilizationCycleState getUtilizationCycleState(long vendorId,
                                                           StockTypeAndWarehouse stockTypeAndWarehouse) {
        return UtilizationCycleState.builder()
                .stockType(stockTypeAndWarehouse.getStockType())
                .warehouseId(stockTypeAndWarehouse.getWarehouseId())
                .fileUrl("https://utilization_vendor_100500_warehouse_172_stocktype_DEFECT.xlsx")
                .vendorId(vendorId)
                .status(UtilizationCycleStateStatus.ACTIVE)
                .build();
    }

    private StockTypeAndWarehouse getStockTypeAndWarehouse(StockType stockType) {
        return StockTypeAndWarehouse.builder()
                .stockType(stockType)
                .warehouseId(WAREHOUSE_ID)
                .build();
    }
}
