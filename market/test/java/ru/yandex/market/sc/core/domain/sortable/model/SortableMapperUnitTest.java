package ru.yandex.market.sc.core.domain.sortable.model;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.sc.core.domain.lot.model.ApiSortableDto;
import ru.yandex.market.sc.core.domain.lot.repository.LotSize;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortableMapperUnitTest {

    @Test
    void mapXdocPalletToLotApi_wrongArguments() {
        Sortable sortable = createSortable(SortableType.XDOC_PALLET, SortableStatus.KEEPED_DIRECT);
        assertThatThrownBy(() -> SortableMapper.mapXdocPalletToLotApi(sortable, LotStatus.PROCESSING))
                .isInstanceOf(TplIllegalArgumentException.class)
                .hasMessage(String.format("Невозможный статус для паллеты %s с типом %s - %s",
                        sortable.getRequiredBarcodeOrThrow(),
                        sortable.getType(),
                        LotStatus.PROCESSING));
    }

    @Test
    void mapXdocPalletToLotApi_readyForPacking() {
        Sortable sortable = createSortable(SortableType.XDOC_BASKET, SortableStatus.KEEPED_DIRECT);
        assertThat(SortableMapper.mapXdocPalletToLotApi(sortable, LotStatus.PROCESSING))
                .isEqualTo(expectedResult(sortable, LotStatus.PROCESSING,
                        Set.of(SortableAPIAction.READY_FOR_PACKING)));
    }

    @Test
    void mapXdocPalletToLotApi_readyForShipment() {
        Sortable sortable = createSortable(SortableType.XDOC_PALLET, SortableStatus.SORTED_DIRECT);
        assertThat(SortableMapper.mapXdocPalletToLotApi(sortable, null))
                .isEqualTo(expectedResult(sortable, LotStatus.PROCESSING,
                        Set.of(SortableAPIAction.READY_FOR_SHIPMENT)));
    }

    @Test
    void mapXdocPalletToLotApi_notReadyForShipment() {
        Sortable sortable = createSortable(SortableType.XDOC_PALLET, SortableStatus.PREPARED_DIRECT);
        assertThat(SortableMapper.mapXdocPalletToLotApi(sortable, null))
                .isEqualTo(expectedResult(sortable, LotStatus.READY,
                        Set.of(SortableAPIAction.NOT_READY_FOR_SHIPMENT)));
    }

    @Test
    void mapXdocPalletToLotApi_preparedBasket() {
        Sortable sortable = createSortable(SortableType.XDOC_BASKET, SortableStatus.PREPARED_DIRECT);
        assertThat(SortableMapper.mapXdocPalletToLotApi(sortable, null))
                .isEqualTo(expectedResult(sortable, LotStatus.READY,
                        Set.of(SortableAPIAction.NOT_READY_FOR_SHIPMENT)));
    }

    @Test
    void mapXdocPalletToLotApi_sortedBasket() {
        Sortable sortable = createSortable(SortableType.XDOC_BASKET, SortableStatus.SORTED_DIRECT);
        assertThat(SortableMapper.mapXdocPalletToLotApi(sortable, null))
                .isEqualTo(expectedResult(sortable, LotStatus.PROCESSING,
                        Set.of(SortableAPIAction.READY_FOR_SHIPMENT)));
    }

    @Test
    void mapXdocPalletToLotApi_keptPallet() {
        Sortable sortable = createSortable(SortableType.XDOC_PALLET, SortableStatus.KEEPED_DIRECT);
        assertThatThrownBy(() -> SortableMapper.mapXdocPalletToLotApi(sortable, null))
                .isInstanceOf(TplIllegalArgumentException.class)
                .hasMessage("Некорректный статус паллеты: " + sortable.getStatus());
    }

    @Test
    void mapXdocPalletToLotApi_wrongStatus() {
        Sortable sortable = createSortable(SortableType.XDOC_PALLET, SortableStatus.ARRIVED_DIRECT);
        assertThatThrownBy(() -> SortableMapper.mapXdocPalletToLotApi(sortable, null))
                .isInstanceOf(TplIllegalArgumentException.class)
                .hasMessage("Illegal sortable status: " + sortable.getStatus());
    }


    private ApiSortableDto expectedResult(Sortable sortable, LotStatus lotStatus, Set<SortableAPIAction> actions) {
        return new ApiSortableDto(
                sortable.getId(),
                sortable.getType(),
                sortable.getRequiredBarcodeOrThrow(),
                sortable.getRequiredBarcodeOrThrow(),
                lotStatus,
                sortable.getStatus(),
                null,
                actions,
                lotStatus == LotStatus.READY,
                LotSize.NORMAL
        );
    }

    private Sortable createSortable(SortableType sortableType, SortableStatus sortableStatus) {
        return new Sortable(
                TestFactory.sortingCenter(),
                "XDOC-123",
                sortableType,
                sortableStatus,
                LocalDate.now(),
                null,
                null,
                DirectFlowType.TRANSIT,
                null,
                null,
                null,
                null,
                null,
                null,
                1000L
        );
    }
}
