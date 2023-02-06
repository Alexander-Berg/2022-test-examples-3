package ru.yandex.market.wms.packing.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.common.model.enums.ItrnSourceType;
import ru.yandex.market.wms.common.model.enums.LocationType;
import ru.yandex.market.wms.common.model.enums.PickDetailStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Loc;
import ru.yandex.market.wms.common.spring.dao.entity.PickDetail;
import ru.yandex.market.wms.common.spring.dao.implementation.LocDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.PickDetailDao;
import ru.yandex.market.wms.common.spring.exception.LocationNotFoundException;
import ru.yandex.market.wms.common.spring.pojo.ItrnCreateParams;
import ru.yandex.market.wms.common.spring.service.SerialInventoryService;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse;
import ru.yandex.market.wms.packing.dto.PackingHintsDTO;
import ru.yandex.market.wms.packing.enums.PackingHintCode;
import ru.yandex.market.wms.packing.exception.LocationHasWrongTypeException;
import ru.yandex.market.wms.packing.exception.PickDetailNotPackedException;
import ru.yandex.market.wms.packing.exception.PickDetailsNotFoundException;
import ru.yandex.market.wms.packing.pojo.LocSorter;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.response.SorterOrderCreationResponse;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.response.SorterOrderCreationStatus;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PackingServiceTest extends IntegrationTest {

    @Autowired
    private PackingService packingService;

    @Autowired
    private SettingsService settings;

    @MockBean
    @Autowired
    private ShippingIntegrationService shippingIntegrationService;

    @MockBean
    @Autowired
    private SerialInventoryService serialInventoryService;

    @MockBean
    @Autowired
    private PickDetailDao pickDetailDao;

    @MockBean
    @Autowired
    private LocDAO locDao;

    @Test
    public void toCloseParcelResponseWithHintForSuccessfulSorterOder() {
        when(settings.getMaxWeightWhenUseTray()).thenReturn(0.15);
        setupAdjacentSorterLock();

        CloseParcelResponse response = packingService.toCloseParcelResponse(
                SorterOrderCreationResponse.builder()
                        .status(SorterOrderCreationStatus.OK)
                        .message("Created successfully")
                        .build(),
                Collections.singletonList(PackingHintCode.HINT_MAX_WEIGHT_WHEN_USE_TRAY.getCode())
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals(response.getState(), CloseParcelResponse.CreateSorterOrderState.CONVEYOR),
                () -> Assertions.assertEquals(1, response.getHints().size()),
                () -> Assertions.assertEquals(1, response.getHintsData().size()),
                () -> Assertions.assertEquals(
                        PackingHintCode.HINT_MAX_WEIGHT_WHEN_USE_TRAY.getCode(),
                        response.getHints().get(0)),
                () -> Assertions.assertEquals(
                        List.of(PackingHintsDTO.builder()
                                .code(PackingHintCode.HINT_MAX_WEIGHT_WHEN_USE_TRAY.getCode())
                                .data(Collections.emptyMap())
                                .build()
                        ),
                        response.getHintsData()
                )
        );
    }

    @Test
    public void toCloseParcelResponseWithHintForFailedSorterOrder() {
        when(settings.getMaxWeightWhenUseTray()).thenReturn(0.15);
        setupAdjacentSorterLock();

        CloseParcelResponse response = packingService.toCloseParcelResponse(
                SorterOrderCreationResponse.builder()
                        .status(SorterOrderCreationStatus.FAILED)
                        .build(),
                Collections.singletonList(PackingHintCode.HINT_MAX_WEIGHT_WHEN_USE_TRAY.getCode())
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals(response.getState(), CloseParcelResponse.CreateSorterOrderState.PACK),
                () -> Assertions.assertEquals(1, response.getHints().size()),
                () -> Assertions.assertEquals(1, response.getHintsData().size()),
                () -> Assertions.assertTrue(
                        response.getHints().contains(PackingHintCode.HINT_MAX_WEIGHT_WHEN_USE_TRAY.getCode())),
                () -> Assertions.assertEquals(
                        List.of(PackingHintsDTO.builder()
                                .code(PackingHintCode.HINT_MAX_WEIGHT_WHEN_USE_TRAY.getCode())
                                .data(Collections.emptyMap())
                                .build()
                        ),
                        response.getHintsData()
                )
        );
    }

    @Test
    public void toCloseParcelResponseWithoutHint() {
        when(settings.getMaxWeightWhenUseTray()).thenReturn(0.15);
        setupAdjacentSorterLock();

        CloseParcelResponse response = packingService.toCloseParcelResponse(
                SorterOrderCreationResponse.builder()
                        .status(SorterOrderCreationStatus.OK)
                        .message("Created successfully")
                        .build(),
                Collections.emptyList()
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals(response.getState(), CloseParcelResponse.CreateSorterOrderState.CONVEYOR),
                () -> Assertions.assertEquals(0, response.getHints().size())
        );
    }

    @Test
    public void toCloseParcelResponseWithFailedSorterOrder() {
        when(settings.getMaxWeightWhenUseTray()).thenReturn(0.15);
        setupAdjacentSorterLock();

        CloseParcelResponse response = packingService.toCloseParcelResponse(
                SorterOrderCreationResponse.builder()
                        .status(SorterOrderCreationStatus.FAILED)
                        .build(),
                Collections.emptyList()
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals(response.getState(), CloseParcelResponse.CreateSorterOrderState.PACK),
                () -> Assertions.assertEquals(0, response.getHints().size())
        );
    }

    private void setupAdjacentSorterLock() {
        when(shippingIntegrationService.getAdjacentSorterLoc("UPACK1_01")).thenReturn(Optional.of(
                LocSorter.builder()
                        .loc("SR1_CH1_01")
                        .putawayzone("SSORT_ZONE")
                        .build()));
    }

    @Test
    public void moveOutboundToPackedCellWhenPickDetailsNotFound() {
        final String parcelId = "PARCEL_1";
        when(pickDetailDao.getPickDetailByIds(eq(List.of(parcelId)))).thenReturn(Collections.emptyList());

        var exception = Assertions.assertThrows(
                PickDetailsNotFoundException.class,
                () ->  packingService.moveWithdrawalToPackedCell(parcelId, null)
        );
        Assertions.assertEquals("400 BAD_REQUEST \"Не найдены детали отбора для посылки " + parcelId + "\"",
                exception.getMessage());
    }

    @Test
    public void moveOutboundToPackedCellWhenPickDetailsAreNotPacked() {
        final String parcelId = "PARCEL_1";
        var pickDetails = List.of(
                PickDetail.builder()
                        .pickDetailKey("PD-1")
                        .status(PickDetailStatus.PACKED)
                        .build(),
                PickDetail.builder()
                        .pickDetailKey("PD-2")
                        .status(PickDetailStatus.PICKED)
                        .build()
        );
        when(pickDetailDao.getPickDetailByIds(eq(List.of(parcelId)))).thenReturn(pickDetails);

        var exception = Assertions.assertThrows(
                PickDetailNotPackedException.class,
                () ->  packingService.moveWithdrawalToPackedCell(parcelId, null)
        );
        Assertions.assertEquals("400 BAD_REQUEST \"Упакованы не все детали отбора: PD-2\"", exception.getMessage());
     }

    @Test
    public void moveOutboundToPackedCellWhenLocNotFound() {
        final String parcelId = "PARCEL_1";
        final String packedLocKey = "PACKED_LOC_1";
        var pickDetails = List.of(
                PickDetail.builder()
                        .status(PickDetailStatus.PACKED)
                        .build(),
                PickDetail.builder()
                        .status(PickDetailStatus.PACKED)
                        .build()
        );
        when(pickDetailDao.getPickDetailByIds(eq(List.of(parcelId)))).thenReturn(pickDetails);
        when(locDao.findById(packedLocKey)).thenReturn(Optional.empty());

        var exception = Assertions.assertThrows(
                LocationNotFoundException.class,
                () ->  packingService.moveWithdrawalToPackedCell(parcelId, packedLocKey)
        );
        Assertions.assertEquals(
                "404 NOT_FOUND \"Локация " + packedLocKey + " не найдена\"",
                exception.getMessage()
        );
    }

    @Test
    public void moveOutboundToPackedCellWhenLocHasWrongType() {
        final String parcelId = "PARCEL_1";
        final String packedLocKey = "PACKED_LOC_1";
        var pickDetails = List.of(
                PickDetail.builder()
                        .status(PickDetailStatus.PACKED)
                        .build(),
                PickDetail.builder()
                        .status(PickDetailStatus.PACKED)
                        .build()
        );
        var loc = Loc.builder()
                .loc(packedLocKey)
                .locationType(LocationType.DROP)
                .build();

        when(pickDetailDao.getPickDetailByIds(eq(List.of(parcelId)))).thenReturn(pickDetails);
        when(locDao.findById(packedLocKey)).thenReturn(Optional.of(loc));

        var exception = Assertions.assertThrows(
                LocationHasWrongTypeException.class,
                () ->  packingService.moveWithdrawalToPackedCell(parcelId, packedLocKey)
        );
        Assertions.assertEquals(
                "400 BAD_REQUEST \"Локация " + packedLocKey + " имеет тип не "
                        + LocationType.PACKED.getCode() + "\"",
                exception.getMessage()
        );
    }

    @Test
    public void moveOutboundToPackedCellHappyPath() {
        final String parcelId = "PARCEL_1";
        final String packedLocKey = "PACKED_LOC_1";
        final String fromLocKey = "FROM_LOC_1";
        var pickDetails = List.of(
                PickDetail.builder()
                        .status(PickDetailStatus.PACKED)
                        .loc(fromLocKey)
                        .build()
        );
        var loc = Loc.builder()
                .loc(packedLocKey)
                .locationType(LocationType.PACKED)
                .build();

        when(pickDetailDao.getPickDetailByIds(eq(List.of(parcelId)))).thenReturn(pickDetails);
        when(locDao.findById(packedLocKey)).thenReturn(Optional.of(loc));

        packingService.moveWithdrawalToPackedCell(parcelId, packedLocKey);

        verify(serialInventoryService, times(1))
                .moveIdToLoc(
                        eq(parcelId),
                        eq(fromLocKey),
                        eq(packedLocKey),
                        argThat(new ItrnCreateParamsIsPackingConsOutbound(parcelId)),
                        eq(true),
                        eq("TEST")
                );
    }

    private static final class ItrnCreateParamsIsPackingConsOutbound implements ArgumentMatcher<ItrnCreateParams> {

        private final String sourceKey;

        ItrnCreateParamsIsPackingConsOutbound(String sourceKey) {
            this.sourceKey = sourceKey;
        }

        @Override
        public boolean matches(ItrnCreateParams argument) {
            return argument.getSourceType().equals(ItrnSourceType.PACKING_CONS_WITHDRAWAL.getCode())
                    && argument.getSourceKey().equals(sourceKey);
        }
    }
}
